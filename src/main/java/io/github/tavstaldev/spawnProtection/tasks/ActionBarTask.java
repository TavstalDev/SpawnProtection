package io.github.tavstaldev.spawnProtection.tasks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import io.github.tavstaldev.minecorelib.utils.ChatUtils;
import io.github.tavstaldev.spawnProtection.SpawnProtection;
import io.github.tavstaldev.spawnProtection.managers.PlayerCacheManager;
import io.github.tavstaldev.spawnProtection.utils.TimeUtil;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * A task that periodically updates players' action bars with their spawn protection status.
 * This task checks the remaining protection time for each player and displays the appropriate
 * message on their action bar. If the protection time has expired, it removes the protection
 * and notifies the player.
 */
public class ActionBarTask extends BukkitRunnable {

    /**
     * Executes the task logic. Iterates through all players with active spawn protection,
     * updates their action bar with the remaining protection time, and handles the expiration
     * of protection.
     */
    @Override
    public void run() {
        // Retrieve the current list of players with active protections
        var protections = PlayerCacheManager.getProtections();
        if (protections.isEmpty())
            return;

        var currentTime = System.currentTimeMillis();
        var translator = SpawnProtection.Instance.getTranslator();

        // Iterate through each player with active protection
        for (var uuid : protections.keySet()) {
            var player = SpawnProtection.Instance.getServer().getPlayer(uuid);
            if (player == null || !player.isOnline())
                continue;

            var protectionTime = protections.get(uuid);
            var timeLeft = protectionTime - currentTime;

            // Handle expired protection
            if (timeLeft <= 0) {
                PlayerCacheManager.removeProtection(uuid);
                RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                RegionQuery query = container.createQuery();
                var regions = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));

                // Skip if the player is in a region with spawn protection enabled
                if (regions.getRegions().stream().anyMatch(region -> region.getFlag(SpawnProtection.EnableSpawnProtectionFlag) == StateFlag.State.ALLOW))
                    continue;

                // Notify the player that their protection has expired
                player.sendActionBar(ChatUtils.translateColors(translator.localize(player, "action-bar-unprotected"), true));
                SpawnProtection.Instance.sendLocalizedMsg(player, "protection-expired");
                continue;
            }

            // Format the remaining time and update the player's action bar
            String time = TimeUtil.formatDate(player, timeLeft + 1000L); // Adding 1 second to avoid showing 0 seconds remaining
            player.sendActionBar(ChatUtils.translateColors(translator.localize(player, "action-bar-protected", Map.of("time", time)), true));
        }
    }
}