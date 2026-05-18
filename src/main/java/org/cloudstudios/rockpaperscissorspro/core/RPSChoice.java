package org.cloudstudios.rockpaperscissorspro.core;

public enum RPSChoice {

    ROCK("Rock"),
    PAPER("Paper"),
    SCISSORS("Scissors");

    private final String displayName;

    RPSChoice(final String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public RPSChoice beats() {
        return switch (this) {
            case ROCK     -> SCISSORS;
            case PAPER    -> ROCK;
            case SCISSORS -> PAPER;
        };
    }

    public boolean beats(final RPSChoice other) {
        return other != null && this.beats() == other;
    }
}
