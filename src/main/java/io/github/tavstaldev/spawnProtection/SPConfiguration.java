package io.github.tavstaldev.spawnProtection;

import io.github.tavstaldev.minecorelib.config.ConfigurationBase;

public class SPConfiguration extends ConfigurationBase {
    public SPConfiguration() {
        super(SpawnProtection.Instance, "config.yml", null);
    }

    public String prefix;
    public boolean checkForUpdates, debug;

    @Override
    protected void loadDefaults() {
        // General
        resolve("locale", "hun");
        resolve("usePlayerLocale", false);
        checkForUpdates = resolveGet("checkForUpdates", false);
        debug = resolveGet("debug", false);
        prefix = resolveGet("prefix", "&bSpawn&3Protection &8»");
    }
}