package io.github.tavstaldev.spawnProtection.events;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import io.github.tavstaldev.spawnProtection.SpawnProtection;
import io.github.tavstaldev.spawnProtection.managers.PlayerCacheManager;
import io.github.tavstaldev.spawnProtection.utils.TimeUtil;
import io.github.tavstaldev.spawnProtection.utils.VanishUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

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
        if (isUnderProtection != null && !isUnderProtection) {
            // Protection has expired, return
            return;
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
        long milisec = SpawnProtection.config().protectionDuration * 1000L;
        var currentTime = System.currentTimeMillis();
        PlayerCacheManager.setProtection(player.getUniqueId(), currentTime + milisec);
        if (isUnderProtection != null) // Prevent spamming the message
            return;
        SpawnProtection.Instance.sendLocalizedMsg(player, "player-protected", Map.of("time", TimeUtil.formatDate(player, milisec)));
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
     * Handles the EntityDamageByEntityEvent to manage spawn protection logic during player interactions.
     * This method ensures that protected players cannot be damaged and notifies the involved entities accordingly.
     *
     * @param event The EntityDamageByEntityEvent triggered when an entity damages another entity.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerEntityDamage(EntityDamageByEntityEvent event) {
        // Exit if the event is already cancelled
        if (event.isCancelled())
            return;

        // Exit if the damaged entity is not a player
        if (!(event.getEntity() instanceof Player player))
        {
            if (!(event.getDamager() instanceof Player damager))
                return;
            var isProtected = PlayerCacheManager.isProtected(damager.getUniqueId());
            if (isProtected == null)
                return;

            if (!isProtected) {
                PlayerCacheManager.removeProtection(damager.getUniqueId());
                return;
            }
            // Cancel the event to prevent damage
            event.setCancelled(true);
            return;
        }

        // Check if the damaged player is under protection
        var isProtected = PlayerCacheManager.isProtected(player.getUniqueId());

        // Handle non-player damagers
        Player damager = null;
        if (event.getDamager() instanceof Player p) {
            damager = p;
        }
        else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player p) {
                damager = p;
            }
        }

        if (damager == null) {
            // Remove protection if it has expired
            if (isProtected == null)
                return;

            if (!isProtected) {
                PlayerCacheManager.removeProtection(player.getUniqueId());
                return;
            }
            // Cancel the event to prevent damage
            event.setCancelled(true);
            return;
        }

        // Exit if the damager is the same as the damaged player
        if (damager.equals(player))
            return;

        // Check if the damager is under protection
        var isDamagerProtected = PlayerCacheManager.isProtected(damager.getUniqueId());
        if (isDamagerProtected != null && isDamagerProtected) {
            // Notify the damager that they are protected and cancel the event
            SpawnProtection.Instance.sendLocalizedMsg(damager, "you-are-protected");
            event.setCancelled(true);
            return;
        }

        if (isProtected == null)
            return;

        // Remove protection from the damaged player if it has expired
        if (!isProtected) {
            PlayerCacheManager.removeProtection(player.getUniqueId());
            return;
        }

        // Cancel the event to prevent damage to the protected player
        event.setCancelled(true);

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