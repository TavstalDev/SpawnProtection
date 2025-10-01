package io.github.tavstaldev.spawnProtection.managers;

import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Manages the caching of player data.
 * This class provides methods to add, remove, and retrieve player data
 * stored in a cache for efficient access.
 */
public class PlayerCacheManager {
    private static final Map<UUID, LocalDateTime> _protections = new HashMap<>();
    private static final Set<UUID> _teleportingPlayers = new HashSet<>();
    private static final Set<UUID> _markedForRemoval = new HashSet<>();

    //#region Protections
    public static void setProtection(UUID playerId, LocalDateTime time) {
        _protections.put(playerId, time);
    }

    public static void removeProtection(UUID playerId) {
        _protections.remove(playerId);
    }

    public static @Nullable Boolean isProtected(UUID playerId) {
        LocalDateTime cooldownTime = _protections.get(playerId);
        if (cooldownTime == null) {
            return null; // No cooldown set for this player
        }
        return LocalDateTime.now().isBefore(cooldownTime); // Check if current time is before the cooldown time
    }
    //#endregion

    //#region Teleporting
    public static void addTeleporting(UUID playerId) {
        _teleportingPlayers.add(playerId);
    }

    public static void removeTeleporting(UUID playerId) {
        _teleportingPlayers.remove(playerId);
    }

    public static boolean isTeleporting(UUID playerId) {
        return _teleportingPlayers.contains(playerId);
    }
    //#endregion

    //#region Mark for removal
    public static void markForRemoval(UUID playerId) {
        _markedForRemoval.add(playerId);
    }

    public static void unmarkForRemoval(UUID playerId) {
        _markedForRemoval.remove(playerId);
    }

    public static boolean isMarkedForRemoval(UUID playerId) {
        return _markedForRemoval.contains(playerId);
    }

    public static boolean isMarkedForRemovalEmpty() {
        return _markedForRemoval.isEmpty();
    }

    public static Set<UUID> getMarkedForRemovalSet() {
        return new HashSet<>(_markedForRemoval); // Return a copy to prevent external modification
    }
    //#endregion
}