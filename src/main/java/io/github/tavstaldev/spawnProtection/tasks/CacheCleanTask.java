package io.github.tavstaldev.spawnProtection.tasks;

import io.github.tavstaldev.spawnProtection.managers.PlayerCacheManager;
import org.bukkit.scheduler.BukkitRunnable;

public class CacheCleanTask extends BukkitRunnable {
    @Override
    public void run() {
        if (PlayerCacheManager.isMarkedForRemovalEmpty())
            return;

        for (var playerId : PlayerCacheManager.getMarkedForRemovalSet()) {
            var protection = PlayerCacheManager.isProtected(playerId);
            if (protection == null || protection)
                continue;

            PlayerCacheManager.removeProtection(playerId);
            if (PlayerCacheManager.isTeleporting(playerId))
                PlayerCacheManager.removeTeleporting(playerId);
            PlayerCacheManager.unmarkForRemoval(playerId);
        }
    }
}
