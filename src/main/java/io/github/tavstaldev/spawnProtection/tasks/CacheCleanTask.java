package io.github.tavstaldev.spawnProtection.tasks;

import io.github.tavstaldev.spawnProtection.managers.PlayerCacheManager;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Task for cleaning up player cache data.
 * This task is scheduled to run periodically and removes expired or unnecessary player data
 * from the cache managed by the PlayerCacheManager.
 */
public class CacheCleanTask extends BukkitRunnable {

    /**
     * Executes the cache cleanup logic.
     * This method is called periodically to check and remove players marked for removal
     * from the cache. It ensures that only unprotected and non-teleporting players are removed.
     */
    @Override
    public void run() {
        // If there are no players marked for removal, exit early.
        if (PlayerCacheManager.isMarkedForRemovalEmpty())
            return;

        // Iterate through the set of players marked for removal.
        for (var playerId : PlayerCacheManager.getMarkedForRemovalSet()) {
            // Check if the player is protected. Skip if the player is protected or the protection status is null.
            var protection = PlayerCacheManager.isProtected(playerId);
            if (protection == null || protection)
                continue;

            // Remove the player's protection status.
            PlayerCacheManager.removeProtection(playerId);

            // If the player is teleporting, remove their teleporting status.
            if (PlayerCacheManager.isTeleporting(playerId))
                PlayerCacheManager.removeTeleporting(playerId);

            // Unmark the player from the removal list.
            PlayerCacheManager.unmarkForRemoval(playerId);
        }
    }
}
