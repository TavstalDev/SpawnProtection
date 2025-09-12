package io.github.tavstaldev.spawnProtection.events;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import io.github.tavstaldev.spawnProtection.SpawnProtection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.LocalDateTime;
import java.util.Map;

public class PlayerEventListener implements Listener {

    public static void init() {
        Bukkit.getPluginManager().registerEvents(new PlayerEventListener(), SpawnProtection.Instance);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        var time = SpawnProtection.Instance.getProtectedPlayers().get(player.getUniqueId());
        if (time != null) {
            if (time.isAfter(LocalDateTime.now()))
                return;
            else {
                SpawnProtection.Instance.getProtectedPlayers().remove(player.getUniqueId());
                SpawnProtection.Instance.sendLocalizedMsg(player, "protection-expired");
            }
        }

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        ApplicableRegionSet fromRegions = query.getApplicableRegions(BukkitAdapter.adapt(event.getFrom()));

        ApplicableRegionSet toRegions = query.getApplicableRegions(BukkitAdapter.adapt(event.getTo()));

        if (fromRegions.getRegions().stream().noneMatch(region -> region.getFlag(SpawnProtection.EnableSpawnProtectionFlag) == StateFlag.State.ALLOW))
            return;
        if (toRegions.getRegions().stream().anyMatch(region -> region.getFlag(SpawnProtection.EnableSpawnProtectionFlag) == StateFlag.State.ALLOW))
            return;
        SpawnProtection.Instance.getProtectedPlayers().put(player.getUniqueId(), LocalDateTime.now().plusSeconds(3));
        SpawnProtection.Instance.sendLocalizedMsg(player, "player-protected", Map.of("time", "3"));
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        var time = SpawnProtection.Instance.getProtectedPlayers().get(player.getUniqueId());
        if (time == null)
            return;

        if (time.isBefore(LocalDateTime.now())) {
            SpawnProtection.Instance.getProtectedPlayers().remove(player.getUniqueId());
            return;
        }

        event.setCancelled(true);
        if (!(event instanceof EntityDamageByEntityEvent damageByEntityEvent))
            return;

        if (!(damageByEntityEvent.getDamager() instanceof Player damager))
            return;

        SpawnProtection.Instance.sendLocalizedMsg(damager, "target-protected", Map.of("player", player.getName()));
    }
}
