package io.github.tavstaldev.spawnProtection.events;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import io.github.tavstaldev.spawnProtection.SpawnProtection;
import io.github.tavstaldev.spawnProtection.managers.PlayerCacheManager;
import io.github.tavstaldev.spawnProtection.utils.VanishUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Event listener for handling player-related events in the SpawnProtection plugin.
 * This class listens to various player events and applies spawn protection logic.
 */
public class PlayerEventListener implements Listener {

    /**
     * Initializes the event listener by registering it with the Bukkit plugin manager.
     */
    public static void init() {
        Bukkit.getPluginManager().registerEvents(new PlayerEventListener(), SpawnProtection.Instance);
    }

    /**
     * Handles the PlayerMoveEvent to manage spawn protection logic when a player moves.
     *
     * @param event The PlayerMoveEvent triggered when a player moves.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        // Ignore if the player is in creative or spectator mode
        if (!(player.getGameMode() == GameMode.ADVENTURE || player.getGameMode() == GameMode.SURVIVAL))
            return;

        // Ignore if the player is in vanish
        if (VanishUtil.isVanished(player))
            return;

        var playerId = player.getUniqueId();

        // Ignore if the player is teleported
        if (PlayerCacheManager.isTeleporting(playerId)) {
            PlayerCacheManager.removeTeleporting(playerId);
            return;
        }

        var isUnderProtection = PlayerCacheManager.isProtected(playerId);
        if (isUnderProtection != null) {
            if (isUnderProtection)
                return;

            PlayerCacheManager.removeProtection(playerId);
            SpawnProtection.Instance.sendLocalizedMsg(player, "protection-expired");
        }

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        ApplicableRegionSet fromRegions = query.getApplicableRegions(BukkitAdapter.adapt(event.getFrom()));
        ApplicableRegionSet toRegions = query.getApplicableRegions(BukkitAdapter.adapt(event.getTo()));

        // Check if the player is leaving a region with spawn protection enabled
        if (fromRegions.getRegions().stream().noneMatch(region -> region.getFlag(SpawnProtection.EnableSpawnProtectionFlag) == StateFlag.State.ALLOW))
            return;
        if (toRegions.getRegions().stream().anyMatch(region -> region.getFlag(SpawnProtection.EnableSpawnProtectionFlag) == StateFlag.State.ALLOW))
            return;

        // Apply spawn protection for x seconds
        int seconds = SpawnProtection.config().protectionDuration;
        PlayerCacheManager.setProtection(player.getUniqueId(), LocalDateTime.now().plusSeconds(seconds));
        SpawnProtection.Instance.sendLocalizedMsg(player, "player-protected", Map.of("time", String.valueOf(seconds)));
    }

    /**
     * Handles the PlayerTeleportEvent to mark a player as teleporting.
     *
     * @param event The PlayerTeleportEvent triggered when a player teleports.
     */
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        PlayerCacheManager.addTeleporting(event.getPlayer().getUniqueId());
    }

    /**
     * Handles the EntityDamageEvent to cancel damage for protected players.
     *
     * @param event The EntityDamageEvent triggered when an entity takes damage.
     */
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        var isProtected = PlayerCacheManager.isProtected(player.getUniqueId());
        if (isProtected == null)
            return;

        if (!isProtected) {
            PlayerCacheManager.removeProtection(player.getUniqueId());
            return;
        }

        event.setCancelled(true);
        if (!(event instanceof EntityDamageByEntityEvent damageByEntityEvent))
            return;

        if (!(damageByEntityEvent.getDamager() instanceof Player damager))
            return;

        // Notify the damager that the target is protected
        SpawnProtection.Instance.sendLocalizedMsg(damager, "target-protected", Map.of("player", player.getName()));
    }

    /**
     * Handles the PlayerJoinEvent to unmark a player from the removal list.
     *
     * @param event The PlayerJoinEvent triggered when a player joins the server.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        if (PlayerCacheManager.isMarkedForRemoval(playerId))
            PlayerCacheManager.unmarkForRemoval(playerId);
    }

    /**
     * Handles the PlayerQuitEvent to mark a player for removal.
     *
     * @param event The PlayerQuitEvent triggered when a player leaves the server.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerCacheManager.markForRemoval(event.getPlayer().getUniqueId());
    }
}