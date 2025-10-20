package io.github.tavstaldev.spawnProtection;

import io.github.tavstaldev.minecorelib.config.ConfigurationBase;

public class SPConfiguration extends ConfigurationBase {
    public SPConfiguration() {
        super(SpawnProtection.Instance, "config.yml", null);
    }

    public String prefix;
    public boolean checkForUpdates, debug;

    public int protectionDuration;

    @Override
    protected void loadDefaults() {
        // General
        resolve("locale", "eng");
        resolve("usePlayerLocale", true);
        checkForUpdates = resolveGet("checkForUpdates", true);
        debug = resolveGet("debug", false);
        prefix = resolveGet("prefix", "&bSpawn&3Protection &8»");

        protectionDuration = resolveGet("protectionDuration", 3);
    }
}