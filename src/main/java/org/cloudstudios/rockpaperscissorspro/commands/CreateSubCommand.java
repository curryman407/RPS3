package org.cloudstudios.rockpaperscissorspro.commands;

import org.bukkit.entity.Player;
import org.cloudstudios.rockpaperscissorspro.config.MessageManager;
import org.cloudstudios.rockpaperscissorspro.core.GameLobbyManager;
import org.cloudstudios.rockpaperscissorspro.core.MatchManager;
import org.cloudstudios.rockpaperscissorspro.reward.BetManager;
import org.cloudstudios.rockpaperscissorspro.reward.BetManager.ValidationResult;
import org.cloudstudios.rockpaperscissorspro.util.ColorUtil;

import java.util.Map;


public final class CreateSubCommand {

    private final GameLobbyManager lobbyManager;
    private final MatchManager     matchManager;
    private final MessageManager   messageManager;
    private final BetManager       betManager;

    public CreateSubCommand(final GameLobbyManager lobbyManager,
                             final MatchManager matchManager,
                             final MessageManager messageManager,
                             final BetManager betManager) {
        this.lobbyManager  = lobbyManager;
        this.matchManager  = matchManager;
        this.messageManager = messageManager;
        this.betManager    = betManager;
    }

    public void execute(final Player player, final String[] args) {
        if (args.length < 1) {
            player.sendMessage(ColorUtil.translate("&cUsage: &e/rps create <amount>"));
            return;
        }

        if (matchManager.isInMatch(player.getUniqueId())) {
            messageManager.send(player, "create-already-in-match");
            return;
        }

        final int max = lobbyManager.getMaxLobbies(player);
        if (lobbyManager.getLobbyCount(player.getUniqueId()) >= max) {
            messageManager.send(player, "create-max-reached", Map.of("max", String.valueOf(max)));
            return;
        }

        final long betAmount = parseLong(args[0]);
        if (betAmount < 0) {
            messageManager.send(player, "bet-invalid-amount");
            return;
        }

        final ValidationResult vr = betManager.validate(betAmount);
        switch (vr) {
            case DISABLED  -> { messageManager.send(player, "bet-disabled");  return; }
            case BELOW_MIN -> { messageManager.send(player, "bet-too-low",
                    Map.of("min", String.format("%,d", betManager.getMinBet()))); return; }
            case ABOVE_MAX -> { messageManager.send(player, "bet-too-high",
                    Map.of("max", String.format("%,d", betManager.getMaxBet()))); return; }
            default -> {} // OK
        }


        if (!betManager.hasBalance(player, betAmount)) {
            messageManager.send(player, "bet-insufficient-funds",
                    Map.of("amount", String.format("%,d", betAmount)));
            return;
        }

        lobbyManager.createLobby(player, betAmount);
        messageManager.send(player, "create-success",
                Map.of("amount", String.format("%,d", betAmount)));
    }

    private static long parseLong(final String s) {
        try { final long v = Long.parseLong(s); return v < 0 ? -1L : v; }
        catch (final NumberFormatException e) { return -1L; }
    }
}
