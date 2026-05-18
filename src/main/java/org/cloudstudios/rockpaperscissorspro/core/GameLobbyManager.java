package org.cloudstudios.rockpaperscissorspro.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.cloudstudios.rockpaperscissorspro.RockPaperScissorsPro;
import org.cloudstudios.rockpaperscissorspro.config.ConfigManager;
import org.cloudstudios.rockpaperscissorspro.config.MessageManager;
import org.cloudstudios.rockpaperscissorspro.gui.GamesGui;
import org.cloudstudios.rockpaperscissorspro.reward.BetManager;
import org.cloudstudios.rockpaperscissorspro.util.SoundUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public final class GameLobbyManager {

    private final RockPaperScissorsPro plugin;
    private final MatchManager         matchManager;
    private final MessageManager       messageManager;
    private final SoundUtil            soundUtil;
    private final ConfigManager        configManager;
    private final BetManager           betManager;


    private GamesGui gamesGui;

    private final Map<UUID, GameLobby> lobbies = new HashMap<>();

    private final Map<UUID, List<UUID>> lobbyByCreator = new HashMap<>();

    public GameLobbyManager(final RockPaperScissorsPro plugin,
                             final MatchManager matchManager,
                             final MessageManager messageManager,
                             final SoundUtil soundUtil,
                             final ConfigManager configManager,
                             final BetManager betManager) {
        this.plugin          = plugin;
        this.matchManager    = matchManager;
        this.messageManager  = messageManager;
        this.soundUtil       = soundUtil;
        this.configManager   = configManager;
        this.betManager      = betManager;
    }

    public void setGamesGui(final GamesGui gui) { this.gamesGui = gui; }



    public GameLobby createLobby(final Player creator, final long betAmount) {
        final GameLobby lobby = new GameLobby(creator.getUniqueId(), creator.getName(), betAmount);
        lobbies.put(lobby.getLobbyId(), lobby);
        lobbyByCreator.computeIfAbsent(creator.getUniqueId(), k -> new ArrayList<>())
                      .add(lobby.getLobbyId());

        final int timeoutSec = configManager.getLobbyTimeout();
        if (timeoutSec > 0) {

            final BukkitRunnable expiry = new BukkitRunnable() {
                @Override public void run() {
                    if (lobbies.containsKey(lobby.getLobbyId())) {
                        removeLobby(lobby.getLobbyId());
                        final Player p = Bukkit.getPlayer(creator.getUniqueId());
                        if (p != null && p.isOnline()) {
                            messageManager.send(p, "create-timeout",
                                    Map.of("seconds", String.valueOf(timeoutSec)));
                        }
                        refreshGui();
                    }
                }
            };
            final BukkitTask expiryTask = expiry.runTaskLater(plugin, timeoutSec * 20L);
            lobby.setExpiryTask(expiryTask);
        }

        refreshGui();
        return lobby;
    }


    public void cancelLobby(final UUID lobbyId, final boolean notify) {
        final GameLobby lobby = lobbies.get(lobbyId);
        if (lobby == null) return;
        removeLobby(lobbyId);

        if (notify) {
            final Player creator = Bukkit.getPlayer(lobby.getCreatorUuid());
            if (creator != null && creator.isOnline()) {
                messageManager.send(creator, "create-cancelled");
            }
        }
        refreshGui();
    }

    public void cancelAllLobbiesForPlayer(final UUID creatorUuid, final boolean notify) {
        final List<UUID> ids = lobbyByCreator.remove(creatorUuid);
        if (ids == null || ids.isEmpty()) return;

        for (final UUID id : ids) {
            final GameLobby lobby = lobbies.remove(id);
            if (lobby != null) lobby.cancelExpiryTask();
        }

        if (notify) {
            final Player p = Bukkit.getPlayer(creatorUuid);
            if (p != null && p.isOnline()) {
                messageManager.send(p, "create-cancelled");
            }
        }
        refreshGui();
    }

    public int getLobbyCount(final UUID creatorUuid) {
        final List<UUID> ids = lobbyByCreator.get(creatorUuid);
        return ids == null ? 0 : ids.size();
    }


    public int getMaxLobbies(final Player player) {
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("rps.max." + i)) return i;
        }
        return 1;
    }

    public boolean acceptLobby(final UUID lobbyId, final Player acceptor) {
        final GameLobby lobby = lobbies.get(lobbyId);
        if (lobby == null) {
            messageManager.send(acceptor, "game-not-found");
            return false;
        }

        if (lobby.getCreatorUuid().equals(acceptor.getUniqueId())) {
            messageManager.send(acceptor, "game-self-join");
            return false;
        }

        if (matchManager.isInMatch(acceptor.getUniqueId())) {
            messageManager.send(acceptor, "challenger-in-match");
            return false;
        }

        final Player creator = Bukkit.getPlayer(lobby.getCreatorUuid());
        if (creator == null || !creator.isOnline() || matchManager.isInMatch(lobby.getCreatorUuid())) {
            messageManager.send(acceptor, "game-creator-unavailable");
            removeLobby(lobbyId);
            refreshGui();
            return false;
        }


        if (lobby.getBetAmount() > 0) {
            final String fmtAmt = String.format("%,d", lobby.getBetAmount());
            if (!betManager.hasBalance(acceptor, lobby.getBetAmount())) {
                messageManager.send(acceptor, "bet-you-insufficient-funds-on-accept",
                        Map.of("amount", fmtAmt));
                messageManager.send(creator, "bet-opponent-insufficient-funds",
                        Map.of("player", acceptor.getName(), "amount", fmtAmt));
                return false;
            }
            if (!betManager.hasBalance(creator, lobby.getBetAmount())) {
                messageManager.send(acceptor, "bet-opponent-insufficient-funds",
                        Map.of("player", creator.getName(), "amount", fmtAmt));
                messageManager.send(creator, "bet-you-insufficient-funds-on-accept",
                        Map.of("amount", fmtAmt));
                removeLobby(lobbyId);
                refreshGui();
                return false;
            }
        }

        removeLobby(lobbyId);

        messageManager.send(acceptor, "game-accepted",        Map.of("player", creator.getName()));
        messageManager.send(creator,  "game-accepted-notify", Map.of("player", acceptor.getName()));
        soundUtil.play(creator,  "challenge-accepted");
        soundUtil.play(acceptor, "challenge-accepted");

        matchManager.startMatch(lobby.getCreatorUuid(), acceptor.getUniqueId(),
                lobby.getBetAmount(), creator, acceptor);

        refreshGui();
        return true;
    }


    public List<GameLobby> getAllLobbies()              { return new ArrayList<>(lobbies.values()); }
    public boolean         hasLobby(final UUID creator) { final List<UUID> ids = lobbyByCreator.get(creator); return ids != null && !ids.isEmpty(); }
    public GameLobby       getLobby(final UUID lobbyId) { return lobbies.get(lobbyId); }



    private void removeLobby(final UUID lobbyId) {
        final GameLobby lobby = lobbies.remove(lobbyId);
        if (lobby == null) return;
        lobby.cancelExpiryTask();
        final List<UUID> ids = lobbyByCreator.get(lobby.getCreatorUuid());
        if (ids != null) {
            ids.remove(lobbyId);
            if (ids.isEmpty()) lobbyByCreator.remove(lobby.getCreatorUuid());
        }
    }

    private void refreshGui() {
        if (gamesGui != null) gamesGui.refreshAllViewers();
    }

    public void shutdown() {
        lobbies.values().forEach(GameLobby::cancelExpiryTask);
        lobbies.clear();
        lobbyByCreator.clear();
    }
}
