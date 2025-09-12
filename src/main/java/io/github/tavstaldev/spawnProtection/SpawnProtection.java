package io.github.tavstaldev.spawnProtection;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import io.github.tavstaldev.minecorelib.PluginBase;
import io.github.tavstaldev.minecorelib.core.PluginLogger;
import io.github.tavstaldev.minecorelib.core.PluginTranslator;
import io.github.tavstaldev.minecorelib.utils.VersionUtils;
import io.github.tavstaldev.spawnProtection.events.PlayerEventListener;
import org.bukkit.Bukkit;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public final class SpawnProtection extends PluginBase {
    public static SpawnProtection Instance;
    public static PluginLogger Logger() {
        return Instance.getCustomLogger();
    }
    public static PluginTranslator Translator() {
        return Instance.getTranslator();
    }
    public static StateFlag EnableSpawnProtectionFlag;
    private Map<UUID, LocalDateTime> protectedPlayers;
    public Map <UUID, LocalDateTime> getProtectedPlayers() {
        return protectedPlayers;
    }

    public SpawnProtection() {
        super(false, "https://github.com/TavstalDev/SpawnProtection/releases/latest");
        protectedPlayers = new java.util.HashMap<>();
    }

    @Override
    public void onEnable() {
        Instance = this;
        _config = new SPConfiguration();
        _translator = new PluginTranslator(this, new String[]{"hun"});
        _logger.Info(String.format("Loading %s...", getProjectName()));

        if (VersionUtils.isLegacy()) {
            _logger.Error("The plugin is not compatible with legacy versions of Minecraft. Please use a newer version of the game.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Check for WorldGuard
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            _logger.Error("WorldGuard plugin not found! This plugin requires WorldGuard to function properly. Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        else {
            _logger.Ok("WorldGuard plugin found and hooked into it.");
        }

        // Register Events
        PlayerEventListener.init();

        // Generate config file
        saveDefaultConfig();

        // Load Localizations
        if (!_translator.Load())
        {
            _logger.Error("Failed to load localizations... Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        _logger.Ok(String.format("%s has been successfully loaded.", getProjectName()));
    }

    @Override
    public void onLoad() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag("spawn-protection-enabled", true);
            registry.register(flag);
            EnableSpawnProtectionFlag = flag;
        } catch (FlagConflictException e) {
            _logger.Error("Failed to register flags! Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
    }
}
