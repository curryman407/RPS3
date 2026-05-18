package org.cloudstudios.rockpaperscissorspro.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.cloudstudios.rockpaperscissorspro.config.MessageManager;
import org.cloudstudios.rockpaperscissorspro.util.ColorUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RPSCommand implements CommandExecutor, TabCompleter {

    private final ChallengeSubCommand challengeSub;
    private final AcceptSubCommand    acceptSub;
    private final DenySubCommand      denySub;
    private final CreateSubCommand    createSub;
    private final GamesSubCommand     gamesSub;
    private final ReloadSubCommand    reloadSub;
    private final MessageManager      messageManager;

    public RPSCommand(final ChallengeSubCommand challengeSub,
                      final AcceptSubCommand    acceptSub,
                      final DenySubCommand      denySub,
                      final CreateSubCommand    createSub,
                      final GamesSubCommand     gamesSub,
                      final ReloadSubCommand    reloadSub,
                      final MessageManager      messageManager) {
        this.challengeSub  = challengeSub;
        this.acceptSub     = acceptSub;
        this.denySub       = denySub;
        this.createSub     = createSub;
        this.gamesSub      = gamesSub;
        this.reloadSub     = reloadSub;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd,
                              final String label, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ColorUtil.translate(
                    "&8[&c&lRPS&8] &7Usage: &e/rps <challenge|create|games|accept|deny|reload>"));
            return true;
        }

        final String sub = args[0].toLowerCase();
        switch (sub) {
            case "challenge" -> {
                if (!requirePlayer(sender)) return true;
                if (!requirePerm(sender, "rps.challenge")) return true;
                challengeSub.execute((Player) sender, Arrays.copyOfRange(args, 1, args.length));
            }
            case "accept" -> {
                if (!requirePlayer(sender)) return true;
                acceptSub.execute((Player) sender);
            }
            case "deny" -> {
                if (!requirePlayer(sender)) return true;
                denySub.execute((Player) sender);
            }
            case "create" -> {
                if (!requirePlayer(sender)) return true;
                if (!requirePerm(sender, "rps.create")) return true;
                createSub.execute((Player) sender, Arrays.copyOfRange(args, 1, args.length));
            }
            case "games" -> {
                if (!requirePlayer(sender)) return true;
                if (!requirePerm(sender, "rps.games")) return true;
                gamesSub.execute((Player) sender);
            }
            case "reload" -> {
                if (!requirePerm(sender, "rps.reload")) return true;
                reloadSub.execute(sender);
            }
            default -> messageManager.send(sender, "unknown-subcommand");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command cmd,
                                       final String alias, final String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            final List<String> subs = new ArrayList<>(List.of("challenge", "create", "games", "accept", "deny"));
            if (sender.hasPermission("rps.reload")) subs.add("reload");
            StringUtil.copyPartialMatches(args[0], subs, completions);

        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "challenge" -> org.bukkit.Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !p.getName().equals(sender instanceof Player sp ? sp.getName() : ""))
                        .map(org.bukkit.entity.Player::getName)
                        .filter(n -> StringUtil.startsWithIgnoreCase(n, args[1]))
                        .forEach(completions::add);
                case "create" -> completions.add("<amount>");
            }

        } else if (args.length == 3 && args[0].equalsIgnoreCase("challenge")) {
            completions.add("<amount>");
        }

        Collections.sort(completions);
        return completions;
    }

    private boolean requirePlayer(final CommandSender sender) {
        if (sender instanceof Player) return true;
        sender.sendMessage(ColorUtil.translate("&cThis command can only be run by a player."));
        return false;
    }

    private boolean requirePerm(final CommandSender sender, final String perm) {
        if (sender.hasPermission(perm)) return true;
        sender.sendMessage(ColorUtil.translate("&cYou don't have permission to do that."));
        return false;
    }
}
