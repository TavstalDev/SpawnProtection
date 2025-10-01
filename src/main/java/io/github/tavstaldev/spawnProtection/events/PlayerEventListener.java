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

public class PlayerEventListener implements Listener {

    public static void init() {
        Bukkit.getPluginManager().registerEvents(new PlayerEventListener(), SpawnProtection.Instance);
    }

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
        if (PlayerCacheManager.isTeleporting(playerId))
        {
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

        if (fromRegions.getRegions().stream().noneMatch(region -> region.getFlag(SpawnProtection.EnableSpawnProtectionFlag) == StateFlag.State.ALLOW))
            return;
        if (toRegions.getRegions().stream().anyMatch(region -> region.getFlag(SpawnProtection.EnableSpawnProtectionFlag) == StateFlag.State.ALLOW))
            return;
        PlayerCacheManager.setProtection(player.getUniqueId(), LocalDateTime.now().plusSeconds(3));
        SpawnProtection.Instance.sendLocalizedMsg(player, "player-protected", Map.of("time", "3"));
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        PlayerCacheManager.addTeleporting(event.getPlayer().getUniqueId());
    }

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

        SpawnProtection.Instance.sendLocalizedMsg(damager, "target-protected", Map.of("player", player.getName()));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        if (PlayerCacheManager.isMarkedForRemoval(playerId))
            PlayerCacheManager.unmarkForRemoval(playerId);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerCacheManager.markForRemoval(event.getPlayer().getUniqueId());
    }
}
