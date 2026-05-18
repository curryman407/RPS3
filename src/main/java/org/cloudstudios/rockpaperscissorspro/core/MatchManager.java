package org.cloudstudios.rockpaperscissorspro.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.cloudstudios.rockpaperscissorspro.RockPaperScissorsPro;
import org.cloudstudios.rockpaperscissorspro.config.ConfigManager;
import org.cloudstudios.rockpaperscissorspro.config.MessageManager;
import org.cloudstudios.rockpaperscissorspro.cooldown.CooldownManager;
import org.cloudstudios.rockpaperscissorspro.gui.RPSGui;
import org.cloudstudios.rockpaperscissorspro.reward.BetManager;
import org.cloudstudios.rockpaperscissorspro.reward.RewardManager;
import org.cloudstudios.rockpaperscissorspro.util.ColorUtil;
import org.cloudstudios.rockpaperscissorspro.util.SoundUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public final class MatchManager {

    private final RockPaperScissorsPro plugin;
    private final ConfigManager        configManager;
    private final MessageManager       messageManager;
    private final CooldownManager      cooldownManager;
    private final RewardManager        rewardManager;
    private final BetManager           betManager;
    private final RPSGui               rpsGui;
    private final SoundUtil            soundUtil;
    private final StatsManager         statsManager;

    private GameLobbyManager gameLobbyManager;

    private final Map<UUID, UUID>       pendingChallenges = new HashMap<>();

    private final Map<UUID, BukkitTask> pendingTimeouts   = new HashMap<>();

    private final Map<UUID, RPSMatch>   activeMatches     = new HashMap<>();

    public MatchManager(final RockPaperScissorsPro plugin,
                        final ConfigManager configManager,
                        final MessageManager messageManager,
                        final CooldownManager cooldownManager,
                        final RewardManager rewardManager,
                        final BetManager betManager,
                        final RPSGui rpsGui,
                        final SoundUtil soundUtil,
                        final StatsManager statsManager) {
        this.plugin          = plugin;
        this.configManager   = configManager;
        this.messageManager  = messageManager;
        this.cooldownManager = cooldownManager;
        this.rewardManager   = rewardManager;
        this.betManager      = betManager;
        this.rpsGui          = rpsGui;
        this.soundUtil       = soundUtil;
        this.statsManager    = statsManager;
    }

    public void setGameLobbyManager(final GameLobbyManager glm) {
        this.gameLobbyManager = glm;
    }



    public void createChallenge(final UUID challengerUuid,
                                 final UUID challengedUuid,
                                 final long betAmount) {
        pendingChallenges.put(challengedUuid, challengerUuid);
        betManager.setPendingBet(challengedUuid, betAmount);

        final int timeoutSec = configManager.getChallengeTimeout();
        final BukkitTask task = new BukkitRunnable() {
            @Override public void run() {
                if (challengerUuid.equals(pendingChallenges.get(challengedUuid))) {
                    pendingChallenges.remove(challengedUuid);
                    pendingTimeouts.remove(challengerUuid);
                    betManager.clearPendingBet(challengedUuid);
                    final Player cr = Bukkit.getPlayer(challengerUuid);
                    final Player cd = Bukkit.getPlayer(challengedUuid);
                    if (cr != null) messageManager.send(cr, "challenge-timeout-challenger",
                            Map.of("seconds", String.valueOf(timeoutSec)));
                    if (cd != null) messageManager.send(cd, "challenge-timeout-challenged");
                }
            }
        }.runTaskLater(plugin, timeoutSec * 20L);
        pendingTimeouts.put(challengerUuid, task);

        final Player challenger = Bukkit.getPlayer(challengerUuid);
        final Player challenged = Bukkit.getPlayer(challengedUuid);
        if (challenger != null) soundUtil.play(challenger, "challenge-sent");
        if (challenged != null) soundUtil.play(challenged, "challenge-sent");
    }


    public boolean acceptChallenge(final UUID challengedUuid) {
        final UUID challengerUuid = pendingChallenges.remove(challengedUuid);
        if (challengerUuid == null) return false;

        final long betAmount = betManager.getPendingBet(challengedUuid);
        betManager.clearPendingBet(challengedUuid);

        final BukkitTask timeout = pendingTimeouts.remove(challengerUuid);
        if (timeout != null) timeout.cancel();

        final Player challenger = Bukkit.getPlayer(challengerUuid);
        final Player challenged = Bukkit.getPlayer(challengedUuid);

        if (challenger == null || !challenger.isOnline()) {
            if (challenged != null) messageManager.send(challenged, "player-not-found",
                    Map.of("player", challengerUuid.toString()));
            return false;
        }
        if (challenged == null || !challenged.isOnline()) {
            messageManager.send(challenger, "player-not-found",
                    Map.of("player", challengedUuid.toString()));
            return false;
        }
        if (isInMatch(challengerUuid)) { messageManager.send(challenged, "challenger-in-match"); return false; }
        if (isInMatch(challengedUuid)) { messageManager.send(challenger, "target-in-match",
                Map.of("player", challenged.getName())); return false; }


        if (betAmount > 0) {
            final String fmtAmt = String.format("%,d", betAmount);
            if (!betManager.hasBalance(challenged, betAmount)) {
                messageManager.send(challenged, "bet-you-insufficient-funds-on-accept", Map.of("amount", fmtAmt));
                messageManager.send(challenger, "bet-opponent-insufficient-funds",
                        Map.of("player", challenged.getName(), "amount", fmtAmt));
                return false;
            }
            if (!betManager.hasBalance(challenger, betAmount)) {
                messageManager.send(challenged, "bet-opponent-insufficient-funds",
                        Map.of("player", challenger.getName(), "amount", fmtAmt));
                messageManager.send(challenger, "bet-you-insufficient-funds-on-accept", Map.of("amount", fmtAmt));
                return false;
            }
        }

        messageManager.send(challenged, "challenge-accepted",        Map.of("challenger", challenger.getName()));
        messageManager.send(challenger, "challenge-accepted-notify", Map.of("player",     challenged.getName()));
        soundUtil.play(challenger, "challenge-accepted");
        soundUtil.play(challenged, "challenge-accepted");

        startMatch(challengerUuid, challengedUuid, betAmount, challenger, challenged);
        return true;
    }


    public boolean denyChallenge(final UUID challengedUuid) {
        final UUID challengerUuid = pendingChallenges.remove(challengedUuid);
        if (challengerUuid == null) return false;

        final BukkitTask t = pendingTimeouts.remove(challengerUuid);
        if (t != null) t.cancel();
        betManager.clearPendingBet(challengedUuid);

        final Player challenger = Bukkit.getPlayer(challengerUuid);
        final Player challenged = Bukkit.getPlayer(challengedUuid);
        final String cName  = challenger != null ? challenger.getName() : "Unknown";
        final String cdName = challenged != null ? challenged.getName() : "Unknown";
        if (challenged != null) messageManager.send(challenged, "challenge-denied",       Map.of("player",     cName));
        if (challenger != null) messageManager.send(challenger, "challenge-denied-notify", Map.of("challenger", cdName));
        return true;
    }



    public void startMatch(final UUID challengerUuid,
                            final UUID challengedUuid,
                            final long betAmount,
                            final Player challenger,
                            final Player challenged) {

        if (betAmount > 0) {
            final String fmtAmt = String.format("%,d", betAmount);
            if (!betManager.hasBalance(challenger, betAmount)) {
                messageManager.send(challenger, "bet-you-insufficient-funds-on-accept", Map.of("amount", fmtAmt));
                messageManager.send(challenged, "bet-opponent-insufficient-funds",
                        Map.of("player", challenger.getName(), "amount", fmtAmt));
                return;
            }
            if (!betManager.hasBalance(challenged, betAmount)) {
                messageManager.send(challenged, "bet-you-insufficient-funds-on-accept", Map.of("amount", fmtAmt));
                messageManager.send(challenger, "bet-opponent-insufficient-funds",
                        Map.of("player", challenged.getName(), "amount", fmtAmt));
                return;
            }
        }

        if (gameLobbyManager != null) {
            gameLobbyManager.cancelAllLobbiesForPlayer(challengerUuid, false);
            gameLobbyManager.cancelAllLobbiesForPlayer(challengedUuid, false);
        }

        final RPSMatch match = new RPSMatch(challengerUuid, challengedUuid, betAmount);
        activeMatches.put(challengerUuid, match);
        activeMatches.put(challengedUuid, match);

        if (betAmount > 0) {
            betManager.takeBet(challenger, betAmount);
            betManager.takeBet(challenged, betAmount);
            final String fmt = String.format("%,d", betAmount);
            messageManager.send(challenger, "bet-taken", Map.of("amount", fmt));
            messageManager.send(challenged, "bet-taken", Map.of("amount", fmt));
        }

        startCountdown(match);
    }


    public void startCountdown(final RPSMatch match) {
        match.setState(MatchState.COUNTDOWN);
        final int countdownSec = configManager.getCountdownDuration();

        final BukkitTask task = new BukkitRunnable() {
            int remaining = countdownSec;

            @Override public void run() {
                final Player cr = Bukkit.getPlayer(match.getChallenger());
                final Player cd = Bukkit.getPlayer(match.getChallenged());
                if (cr == null || !cr.isOnline() || cd == null || !cd.isOnline()) {
                    cancel();
                    handleAbort(match);
                    return;
                }
                if (remaining > 0) {
                    final String s = String.valueOf(remaining);
                    messageManager.sendActionBar(cr, "countdown-actionbar", Map.of("seconds", s));
                    messageManager.sendActionBar(cd, "countdown-actionbar", Map.of("seconds", s));
                    soundUtil.play(cr, "countdown");
                    soundUtil.play(cd, "countdown");
                    remaining--;
                } else {
                    messageManager.sendActionBar(cr, "countdown-go");
                    messageManager.sendActionBar(cd, "countdown-go");
                    match.setState(MatchState.SELECTING);
                    rpsGui.openGui(cr, cd.getName());
                    rpsGui.openGui(cd, cr.getName());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        match.setActiveTask(task);
    }


    public void submitChoice(final UUID playerUuid, final RPSChoice choice) {
        final RPSMatch match = activeMatches.get(playerUuid);
        if (match == null || match.getState() != MatchState.SELECTING || match.hasChosen(playerUuid)) return;

        match.setChoice(playerUuid, choice);

        final UUID   opponentUuid = match.getOpponent(playerUuid);
        final Player player       = Bukkit.getPlayer(playerUuid);
        final Player opponent     = opponentUuid != null ? Bukkit.getPlayer(opponentUuid) : null;

        soundUtil.play(player, "selection-made");
        if (player != null) {
            messageManager.send(player, "you-chose", Map.of("choice", choice.getDisplayName()));
            if (!match.hasChosen(opponentUuid)) {
                final String oppName = opponent != null ? opponent.getName() : "Opponent";
                messageManager.sendActionBar(player, "waiting-for-opponent", Map.of("opponent", oppName));
            }
        }

        if (match.bothChosen()) resolveMatch(match);
    }



    private void resolveMatch(final RPSMatch match) {
        final RPSChoice cChoice = match.getChallengerChoice();
        final RPSChoice dChoice = match.getChallengedChoice();
        final Player challenger = Bukkit.getPlayer(match.getChallenger());
        final Player challenged = Bukkit.getPlayer(match.getChallenged());

        if (cChoice == dChoice) {
            match.incrementTieCount();
            if (match.getTieCount() >= configManager.getMaxTieReplays()) {
                declareDraw(match, challenger, challenged);
            } else {
                declareTie(match, challenger, challenged, cChoice);
            }
            return;
        }

        final boolean challengerWins = cChoice.beats(dChoice);
        final Player winner      = challengerWins ? challenger : challenged;
        final Player loser       = challengerWins ? challenged : challenger;
        final UUID   winnerUuid  = challengerWins ? match.getChallenger() : match.getChallenged();
        final UUID   loserUuid   = challengerWins ? match.getChallenged() : match.getChallenger();
        final RPSChoice winChoice  = challengerWins ? cChoice : dChoice;
        final RPSChoice loseChoice = challengerWins ? dChoice : cChoice;

        final String winnerName = winner != null ? winner.getName() : "Unknown";
        final String loserName  = loser  != null ? loser.getName()  : "Unknown";
        final Map<String, String> rep = Map.of(
                "winner", winnerName, "loser", loserName,
                "winner_choice", winChoice.getDisplayName(),
                "loser_choice",  loseChoice.getDisplayName());

        if (winner != null) {
            messageManager.sendActionBar(winner, "result-win",      rep);
            messageManager.send(winner,          "result-win-chat", rep);
            soundUtil.play(winner, "win");
        }
        if (loser != null) {
            messageManager.sendActionBar(loser, "result-lose",      rep);
            messageManager.send(loser,          "result-lose-chat", rep);
            soundUtil.play(loser, "lose");
        }

        if (match.hasBet() && winner != null) {
            betManager.giveWinnings(winner, match.getBetAmount());
            final String fmtAmt   = String.format("%,d", match.getBetAmount());
            final String fmtTotal = String.format("%,d", match.getBetAmount() * 2);
            messageManager.send(winner, "bet-won",  Map.of("amount", fmtAmt, "total", fmtTotal));
            if (loser != null) messageManager.send(loser, "bet-lost", Map.of("amount", fmtAmt));
        }

        statsManager.recordWin(winnerUuid);
        statsManager.recordLoss(loserUuid);
        rewardManager.executeMatchRewards(winner, loser);
        cooldownManager.setCooldown(match.getChallenger());
        cooldownManager.setCooldown(match.getChallenged());
        finishMatch(match);
        sendRematchSuggestion(challenger, challenged);
    }

    private void declareTie(final RPSMatch match,
                             final Player challenger, final Player challenged,
                             final RPSChoice choice) {
        final Map<String, String> rep = Map.of(
                "winner_choice", choice.getDisplayName(),
                "loser_choice",  choice.getDisplayName());
        if (challenger != null) { messageManager.send(challenger, "result-tie-chat", rep); soundUtil.play(challenger, "tie"); }
        if (challenged != null) { messageManager.send(challenged, "result-tie-chat", rep); soundUtil.play(challenged, "tie"); }
        match.resetForReplay();
        new BukkitRunnable() {
            @Override public void run() {
                if (activeMatches.containsKey(match.getChallenger())) startCountdown(match);
            }
        }.runTaskLater(plugin, 40L);
    }

    private void declareDraw(final RPSMatch match,
                              final Player challenger, final Player challenged) {
        if (challenger != null) { messageManager.send(challenger, "result-draw-chat"); messageManager.sendActionBar(challenger, "result-draw"); soundUtil.play(challenger, "tie"); }
        if (challenged != null) { messageManager.send(challenged, "result-draw-chat"); messageManager.sendActionBar(challenged, "result-draw"); soundUtil.play(challenged, "tie"); }

        if (match.hasBet()) {
            betManager.refundBet(challenger, match.getBetAmount());
            betManager.refundBet(challenged, match.getBetAmount());
            final String fmt = String.format("%,d", match.getBetAmount());
            if (challenger != null) messageManager.send(challenger, "bet-refunded", Map.of("amount", fmt));
            if (challenged != null) messageManager.send(challenged, "bet-refunded", Map.of("amount", fmt));
        }

        statsManager.recordTie(match.getChallenger());
        statsManager.recordTie(match.getChallenged());
        cooldownManager.setCooldown(match.getChallenger());
        cooldownManager.setCooldown(match.getChallenged());
        finishMatch(match);
        sendRematchSuggestion(challenger, challenged);
    }


    public void handlePlayerQuit(final UUID quittingUuid) {
        pendingTimeouts.computeIfPresent(quittingUuid, (u, t) -> { t.cancel(); return null; });
        pendingChallenges.entrySet().removeIf(e -> e.getValue().equals(quittingUuid));
        if (pendingChallenges.containsKey(quittingUuid)) {
            betManager.clearPendingBet(quittingUuid);
            pendingChallenges.remove(quittingUuid);
        }

        final RPSMatch match = activeMatches.get(quittingUuid);
        if (match == null) return;
        match.cancelActiveTask();

        final UUID   opponentUuid = match.getOpponent(quittingUuid);
        final Player opponent     = opponentUuid != null ? Bukkit.getPlayer(opponentUuid) : null;

        if (opponent != null && opponent.isOnline()) {
            opponent.sendMessage(ColorUtil.translate("&a&lYour opponent disconnected. You win!"));
            soundUtil.play(opponent, "win");
            if (match.hasBet()) {
                betManager.giveWinnings(opponent, match.getBetAmount());
                messageManager.send(opponent, "bet-won-forfeit",
                        Map.of("amount", String.format("%,d", match.getBetAmount()),
                               "total",  String.format("%,d", match.getBetAmount() * 2)));
            }
            if (opponentUuid != null) statsManager.recordWin(opponentUuid);
            statsManager.recordLoss(quittingUuid);
            rewardManager.executeMatchRewards(opponent, null);
        }

        cooldownManager.setCooldown(quittingUuid);
        if (opponentUuid != null) cooldownManager.setCooldown(opponentUuid);
        finishMatch(match);
    }

    public void handleAbort(final RPSMatch match) {
        match.cancelActiveTask();
        finishMatch(match);
    }

    private void finishMatch(final RPSMatch match) {
        match.setState(MatchState.FINISHED);
        match.cancelActiveTask();
        activeMatches.remove(match.getChallenger());
        activeMatches.remove(match.getChallenged());
    }



    private void sendRematchSuggestion(final Player challenger, final Player challenged) {
        if (challenger == null || challenged == null) return;
        if (challenger.isOnline()) {
            challenger.sendMessage(
                    ColorUtil.translate("&7Rematch? ").append(
                            Component.text("[Challenge Again]")
                                    .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                                    .clickEvent(ClickEvent.runCommand("/rps challenge " + challenged.getName()))));
        }
        if (challenged.isOnline()) {
            challenged.sendMessage(
                    ColorUtil.translate("&7Rematch? ").append(
                            Component.text("[Challenge Again]")
                                    .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                                    .clickEvent(ClickEvent.runCommand("/rps challenge " + challenger.getName()))));
        }
    }



    public boolean  isInMatch(final UUID uuid)             { return activeMatches.containsKey(uuid); }
    public RPSMatch getMatch(final UUID uuid)               { return activeMatches.get(uuid); }
    public boolean  hasPendingChallenge(final UUID uuid)   { return pendingChallenges.containsValue(uuid); }
    public boolean  hasIncomingChallenge(final UUID uuid)  { return pendingChallenges.containsKey(uuid); }
    public UUID     getChallengerFor(final UUID uuid)      { return pendingChallenges.get(uuid); }
    public long     getPendingBetFor(final UUID uuid)      { return betManager.getPendingBet(uuid); }

    public void shutdown() {
        pendingTimeouts.values().forEach(t -> { if (t != null && !t.isCancelled()) t.cancel(); });
        pendingTimeouts.clear();
        pendingChallenges.clear();
        betManager.clearAll();
        activeMatches.values().stream().distinct().forEach(RPSMatch::cancelActiveTask);
        activeMatches.clear();
    }
}
