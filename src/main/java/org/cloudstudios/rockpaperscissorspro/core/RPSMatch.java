package org.cloudstudios.rockpaperscissorspro.core;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;


public final class RPSMatch {

    private final UUID challenger;
    private final UUID challenged;
    private final long betAmount;

    private RPSChoice  challengerChoice;
    private RPSChoice  challengedChoice;
    private MatchState state;
    private BukkitTask activeTask;
    private int        tieCount;

    public RPSMatch(final UUID challenger, final UUID challenged, final long betAmount) {
        this.challenger = challenger;
        this.challenged = challenged;
        this.betAmount  = betAmount;
        this.state      = MatchState.COUNTDOWN;
    }

    public UUID       getChallenger()     { return challenger; }
    public UUID       getChallenged()     { return challenged; }
    public long       getBetAmount()      { return betAmount; }
    public boolean    hasBet()            { return betAmount > 0; }
    public MatchState getState()          { return state; }
    public void       setState(MatchState s) { this.state = s; }
    public int        getTieCount()       { return tieCount; }
    public void       incrementTieCount() { tieCount++; }

    public RPSChoice getChoice(final UUID uuid) {
        if (uuid.equals(challenger)) return challengerChoice;
        if (uuid.equals(challenged)) return challengedChoice;
        return null;
    }

    public void setChoice(final UUID uuid, final RPSChoice choice) {
        if (uuid.equals(challenger))      challengerChoice = choice;
        else if (uuid.equals(challenged)) challengedChoice = choice;
    }

    public boolean hasChosen(final UUID uuid) { return getChoice(uuid) != null; }
    public boolean bothChosen()               { return challengerChoice != null && challengedChoice != null; }

    public boolean isParticipant(final UUID uuid) {
        return challenger.equals(uuid) || challenged.equals(uuid);
    }

    public UUID getOpponent(final UUID uuid) {
        if (uuid.equals(challenger)) return challenged;
        if (uuid.equals(challenged)) return challenger;
        return null;
    }

    public void resetForReplay() {
        challengerChoice = null;
        challengedChoice = null;
        state = MatchState.COUNTDOWN;
    }

    public RPSChoice  getChallengerChoice()          { return challengerChoice; }
    public RPSChoice  getChallengedChoice()          { return challengedChoice; }
    public BukkitTask getActiveTask()                { return activeTask; }
    public void       setActiveTask(BukkitTask t)    { this.activeTask = t; }

    public void cancelActiveTask() {
        if (activeTask != null && !activeTask.isCancelled()) {
            activeTask.cancel();
            activeTask = null;
        }
    }
}
