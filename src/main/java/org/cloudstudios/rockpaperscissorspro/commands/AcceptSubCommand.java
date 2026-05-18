package org.cloudstudios.rockpaperscissorspro.commands;

import org.bukkit.entity.Player;
import org.cloudstudios.rockpaperscissorspro.config.MessageManager;
import org.cloudstudios.rockpaperscissorspro.core.MatchManager;

public final class AcceptSubCommand {

    private final MatchManager   matchManager;
    private final MessageManager messageManager;

    public AcceptSubCommand(final MatchManager matchManager, final MessageManager messageManager) {
        this.matchManager   = matchManager;
        this.messageManager = messageManager;
    }

    public void execute(final Player challenged) {
        if (!matchManager.hasIncomingChallenge(challenged.getUniqueId())) {
            messageManager.send(challenged, "challenge-not-found");
            return;
        }
        if (matchManager.isInMatch(challenged.getUniqueId())) {
            messageManager.send(challenged, "challenger-in-match");
            return;
        }
        matchManager.acceptChallenge(challenged.getUniqueId());
    }
}
