package org.cloudstudios.rockpaperscissorspro.core;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;


public final class GameLobby {

    private final UUID   lobbyId;
    private final UUID   creatorUuid;
    private final String creatorName;
    private final long   betAmount;
    private final long   createdAt;

    private BukkitTask expiryTask;

    public GameLobby(final UUID creatorUuid,
                     final String creatorName,
                     final long betAmount) {
        this.lobbyId     = UUID.randomUUID();
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.betAmount   = betAmount;
        this.createdAt   = System.currentTimeMillis();
    }

    public UUID   getLobbyId()     { return lobbyId; }
    public UUID   getCreatorUuid() { return creatorUuid; }
    public String getCreatorName() { return creatorName; }
    public long   getBetAmount()   { return betAmount; }
    public boolean hasBet()        { return betAmount > 0; }
    public long   getCreatedAt()   { return createdAt; }

    public BukkitTask getExpiryTask() { return expiryTask; }


    public void setExpiryTask(final BukkitTask task) { this.expiryTask = task; }

    public void cancelExpiryTask() {
        if (expiryTask != null && !expiryTask.isCancelled()) {
            expiryTask.cancel();
            expiryTask = null;
        }
    }
}
