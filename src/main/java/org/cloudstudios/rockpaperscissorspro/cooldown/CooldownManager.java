package org.cloudstudios.rockpaperscissorspro.cooldown;

import org.cloudstudios.rockpaperscissorspro.config.ConfigManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownManager {

    private final Map<UUID, Long> cooldownExpiry = new ConcurrentHashMap<>();
    private final ConfigManager   configManager;

    public CooldownManager(final ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean isOnCooldown(final UUID uuid) {
        if (!configManager.isCooldownEnabled()) return false;
        final Long expiry = cooldownExpiry.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() < expiry) return true;
        cooldownExpiry.remove(uuid);
        return false;
    }

    public long getRemainingSeconds(final UUID uuid) {
        if (!configManager.isCooldownEnabled()) return 0;
        final Long expiry = cooldownExpiry.get(uuid);
        if (expiry == null) return 0;
        final long rem = expiry - System.currentTimeMillis();
        return rem <= 0 ? 0 : (long) Math.ceil(rem / 1000.0);
    }

    public void setCooldown(final UUID uuid) {
        if (!configManager.isCooldownEnabled()) return;
        cooldownExpiry.put(uuid,
                System.currentTimeMillis() + configManager.getCooldownDuration() * 1000L);
    }

    public void clearCooldown(final UUID uuid) { cooldownExpiry.remove(uuid); }
    public void clearAll()                      { cooldownExpiry.clear(); }
}
