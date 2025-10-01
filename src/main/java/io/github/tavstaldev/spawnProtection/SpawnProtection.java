package io.github.tavstaldev.spawnProtection;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import io.github.tavstaldev.minecorelib.PluginBase;
import io.github.tavstaldev.minecorelib.core.PluginTranslator;
import io.github.tavstaldev.minecorelib.utils.VersionUtils;
import io.github.tavstaldev.spawnProtection.events.PlayerEventListener;
import io.github.tavstaldev.spawnProtection.tasks.CacheCleanTask;
import org.bukkit.Bukkit;

import java.util.*;

public final class SpawnProtection extends PluginBase {
    public static SpawnProtection Instance;
    public static StateFlag EnableSpawnProtectionFlag;
    private CacheCleanTask cacheCleanTask; // Task for cleaning player caches.

    public SpawnProtection() {
        super(false, "https://github.com/TavstalDev/SpawnProtection/releases/latest");
    }

    @Override
    public void onEnable() {
        Instance = this;
        _config = new SPConfiguration();
        _translator = new PluginTranslator(this, new String[]{"hun"});
        _logger.info(String.format("Loading %s...", getProjectName()));

        if (VersionUtils.isLegacy()) {
            _logger.error("The plugin is not compatible with legacy versions of Minecraft. Please use a newer version of the game.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Check for WorldGuard
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            _logger.error("WorldGuard plugin not found! This plugin requires WorldGuard to function properly. Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        else {
            _logger.ok("WorldGuard plugin found and hooked into it.");
        }

        // Register Events
        PlayerEventListener.init();

        // Generate config file
        saveDefaultConfig();

        // Load Localizations
        if (!_translator.load())
        {
            _logger.error("Failed to load localizations... Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Register cache cleanup task.
        if (cacheCleanTask != null && !cacheCleanTask.isCancelled())
            cacheCleanTask.cancel();
        cacheCleanTask = new CacheCleanTask(); // Runs every 5 minutes
        cacheCleanTask.runTaskTimer(this, 0, 5 * 60 * 20);

        _logger.ok(String.format("%s has been successfully loaded.", getProjectName()));
    }

    @Override
    public void onDisable() {
        if (cacheCleanTask != null && !cacheCleanTask.isCancelled())
            cacheCleanTask.cancel();
        _logger.info(String.format("%s has been successfully unloaded.", getProjectName()));
    }

    @Override
    public void onLoad() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag("spawn-protection-enabled", true);
            registry.register(flag);
            EnableSpawnProtectionFlag = flag;
        } catch (FlagConflictException e) {
            _logger.error("Failed to register flags! Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }
}
