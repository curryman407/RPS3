package org.cloudstudios.rockpaperscissorspro.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.cloudstudios.rockpaperscissorspro.config.ConfigManager;
import org.cloudstudios.rockpaperscissorspro.config.MessageManager;
import org.cloudstudios.rockpaperscissorspro.cooldown.CooldownManager;
import org.cloudstudios.rockpaperscissorspro.core.MatchManager;
import org.cloudstudios.rockpaperscissorspro.reward.BetManager;
import org.cloudstudios.rockpaperscissorspro.reward.BetManager.ValidationResult;
import org.cloudstudios.rockpaperscissorspro.util.ColorUtil;

import java.util.Map;


public final class ChallengeSubCommand {

    private final MatchManager    matchManager;
    private final MessageManager  messageManager;
    private final CooldownManager cooldownManager;
    private final BetManager      betManager;
    private final ConfigManager   configManager;

    public ChallengeSubCommand(final MatchManager matchManager,
                                final MessageManager messageManager,
                                final CooldownManager cooldownManager,
                                final BetManager betManager,
                                final ConfigManager configManager) {
        this.matchManager    = matchManager;
        this.messageManager  = messageManager;
        this.cooldownManager = cooldownManager;
        this.betManager      = betManager;
        this.configManager   = configManager;
    }

    public void execute(final Player challenger, final String[] args) {
        if (args.length < 2) {
            challenger.sendMessage(ColorUtil.translate(
                    "&cUsage: &e/rps challenge <player> <amount>"));
            return;
        }

        final String targetName = args[0];

        if (challenger.getName().equalsIgnoreCase(targetName)) {
            messageManager.send(challenger, "self-challenge");
            return;
        }

        final Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            messageManager.send(challenger, "player-not-found", Map.of("player", targetName));
            return;
        }

        if (cooldownManager.isOnCooldown(challenger.getUniqueId())) {
            messageManager.send(challenger, "on-cooldown",
                    Map.of("seconds", String.valueOf(cooldownManager.getRemainingSeconds(challenger.getUniqueId()))));
            return;
        }

        if (matchManager.isInMatch(challenger.getUniqueId())) {
            messageManager.send(challenger, "challenger-in-match");
            return;
        }

        if (matchManager.hasPendingChallenge(challenger.getUniqueId())) {
            messageManager.send(challenger, "challenge-already-pending-sent", Map.of("player", targetName));
            return;
        }

        if (matchManager.isInMatch(target.getUniqueId())) {
            messageManager.send(challenger, "target-in-match", Map.of("player", target.getName()));
            return;
        }

        if (matchManager.hasIncomingChallenge(target.getUniqueId())) {
            messageManager.send(challenger, "challenge-already-pending-received");
            return;
        }

        final long betAmount = parseLong(args[1]);
        if (betAmount < 0) {
            messageManager.send(challenger, "bet-invalid-amount");
            return;
        }

        final ValidationResult vr = betManager.validate(betAmount);
        switch (vr) {
            case DISABLED  -> { messageManager.send(challenger, "bet-disabled");   return; }
            case BELOW_MIN -> { messageManager.send(challenger, "bet-too-low",
                    Map.of("min", String.format("%,d", betManager.getMinBet())));  return; }
            case ABOVE_MAX -> { messageManager.send(challenger, "bet-too-high",
                    Map.of("max", String.format("%,d", betManager.getMaxBet())));  return; }
            default -> {} // OK
        }


        if (!betManager.hasBalance(challenger, betAmount)) {
            messageManager.send(challenger, "bet-insufficient-funds",
                    Map.of("amount", String.format("%,d", betAmount)));
            return;
        }

        matchManager.createChallenge(challenger.getUniqueId(), target.getUniqueId(), betAmount);

        messageManager.send(challenger, "challenge-sent",
                Map.of("player",  target.getName(),
                       "amount",  String.format("%,d", betAmount)));

        sendClickableChallenge(target, challenger.getName(), betAmount);
    }

    private void sendClickableChallenge(final Player target,
                                         final String challengerName,
                                         final long betAmount) {
        final String prefix    = messageManager.getPrefix();
        final String infoText  = prefix + "&e" + challengerName
                + " &achallenged you to RPS for &6$" + String.format("%,d", betAmount) + "&a!";

        final Component msg = ColorUtil.translate(infoText)
                .appendSpace()
                .append(Component.text("[ACCEPT]")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/rps accept"))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Click to accept the challenge!"))))
                .appendSpace()
                .append(Component.text("[DENY]")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/rps deny"))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Click to deny the challenge."))));

        target.sendMessage(msg);
    }


    private static long parseLong(final String s) {
        try {
            final long v = Long.parseLong(s);
            return v < 0 ? -1L : v;
        } catch (final NumberFormatException e) {
            return -1L;
        }
    }
}
