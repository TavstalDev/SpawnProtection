package io.github.tavstaldev.spawnProtection.utils;

import io.github.tavstaldev.minecorelib.core.PluginTranslator;
import io.github.tavstaldev.spawnProtection.SpawnProtection;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Utility class for time-related operations.
 */
public class TimeUtil {

    /**
     * Formats a given timestamp into a localized string representation of days, hours, minutes, and seconds.
     *
     * @param player    The player for whom the time should be localized.
     * @param timestamp The timestamp in milliseconds to be formatted.
     * @return A localized string representing the formatted time.
     */
    public static String formatDate(Player player, long timestamp) {
        final PluginTranslator translator = SpawnProtection.Instance.getTranslator();
        final long seconds = timestamp / 1000L;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(translator.localize(player, "time.days", Map.of("time", String.valueOf(days)))).append(" ");
        }
        if (hours > 0) {
            sb.append(translator.localize(player, "time.hours", Map.of("time", String.valueOf(hours)))).append(" ");
        }
        if (minutes > 0) {
            sb.append(translator.localize(player, "time.minutes", Map.of("time", String.valueOf(minutes)))).append(" ");
        }
        if (secs > 0 || (days == 0 && hours == 0 && minutes == 0)) {
            sb.append(translator.localize(player, "time.seconds", Map.of("time", String.valueOf(secs)))).append(" ");
        }

        return sb.toString().trim();
    }
}
