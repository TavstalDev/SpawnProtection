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

/**
 * Main class for the SpawnProtection plugin.
 * This plugin integrates with WorldGuard to provide spawn protection functionality.
 */
public final class SpawnProtection extends PluginBase {
    // Singleton instance of the plugin.
    public static SpawnProtection Instance;

    // Custom WorldGuard flag to enable/disable spawn protection.
    public static StateFlag EnableSpawnProtectionFlag;

    // Task for cleaning player caches periodically.
    private CacheCleanTask cacheCleanTask;

    public static SPConfiguration config() {
        return (SPConfiguration)Instance._config;
    }

    /**
     * Constructor for the SpawnProtection plugin.
     * Initializes the plugin with a link to the latest release.
     */
    public SpawnProtection() {
        super(false, "https://github.com/TavstalDev/SpawnProtection/releases/latest");
    }

    /**
     * Called when the plugin is enabled.
     * Initializes the plugin, checks dependencies, and starts necessary tasks.
     */
    @Override
    public void onEnable() {
        Instance = this;

        // Load configuration and translator.
        _config = new SPConfiguration();
        _translator = new PluginTranslator(this, new String[]{"hun"});
        _logger.info(String.format("Loading %s...", getProjectName()));

        // Check if the Minecraft version is supported.
        if (VersionUtils.isLegacy()) {
            _logger.error("The plugin is not compatible with legacy versions of Minecraft. Please use a newer version of the game.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Ensure the WorldGuard plugin is available.
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            _logger.error("WorldGuard plugin not found! This plugin requires WorldGuard to function properly. Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        } else {
            _logger.ok("WorldGuard plugin found and hooked into it.");
        }

        // Register event listeners.
        PlayerEventListener.init();

        // Generate the default configuration file.
        saveDefaultConfig();

        // Load localization files.
        if (!_translator.load()) {
            _logger.error("Failed to load localizations... Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Register and start the cache cleanup task.
        if (cacheCleanTask != null && !cacheCleanTask.isCancelled())
            cacheCleanTask.cancel();
        cacheCleanTask = new CacheCleanTask(); // Runs every 5 minutes.
        cacheCleanTask.runTaskTimer(this, 0, 5 * 60 * 20);

        _logger.ok(String.format("%s has been successfully loaded.", getProjectName()));
    }

    /**
     * Called when the plugin is disabled.
     * Cleans up resources and stops running tasks.
     */
    @Override
    public void onDisable() {
        if (cacheCleanTask != null && !cacheCleanTask.isCancelled())
            cacheCleanTask.cancel();
        _logger.info(String.format("%s has been successfully unloaded.", getProjectName()));
    }

    /**
     * Called when the plugin is loaded.
     * Registers custom flags with the WorldGuard flag registry.
     */
    @Override
    public void onLoad() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            // Register a custom flag for spawn protection.
            StateFlag flag = new StateFlag("spawn-protection-enabled", true);
            registry.register(flag);
            EnableSpawnProtectionFlag = flag;
        } catch (FlagConflictException e) {
            _logger.error("Failed to register flags! Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }
}