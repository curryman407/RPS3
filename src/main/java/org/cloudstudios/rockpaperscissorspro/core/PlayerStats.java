package org.cloudstudios.rockpaperscissorspro.core;

public final class PlayerStats {

    private int played;
    private int won;
    private int lost;
    private int tied;

    public PlayerStats() {}

    public PlayerStats(final int played, final int won, final int lost, final int tied) {
        this.played = played;
        this.won    = won;
        this.lost   = lost;
        this.tied   = tied;
    }

    public int getPlayed() { return played; }
    public int getWon()    { return won; }
    public int getLost()   { return lost; }
    public int getTied()   { return tied; }

    public void incrementPlayed() { played++; }
    public void incrementWon()    { won++; }
    public void incrementLost()   { lost++; }
    public void incrementTied()   { tied++; }

    public String getWinPercent() {
        if (played == 0) return "0.0%";
        return String.format("%.1f%%", (won  / (double) played) * 100);
    }

    public String getLossPercent() {
        if (played == 0) return "0.0%";
        return String.format("%.1f%%", (lost / (double) played) * 100);
    }
}
