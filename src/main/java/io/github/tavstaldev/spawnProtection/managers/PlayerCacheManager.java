package io.github.tavstaldev.spawnProtection.managers;

import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Manager class for handling player-related cache data.
 * This class provides methods to manage player protections, teleporting states,
 * and marking players for removal.
 */
public class PlayerCacheManager {
    // Map to store player protections with their expiration times.
    private static final Map<UUID, Long> _protections = new HashMap<>();
    // Set to store players who are currently teleporting.
    private static final Set<UUID> _teleportingPlayers = new HashSet<>();
    // Set to store players marked for removal.
    private static final Set<UUID> _markedForRemoval = new HashSet<>();

    //#region Protections

    /**
     * Sets a protection cooldown for a player.
     *
     * @param playerId The UUID of the player.
     * @param time The expiration time of the protection.
     */
    public static void setProtection(UUID playerId, long time) {
        _protections.put(playerId, time);
    }

    /**
     * Removes the protection cooldown for a player.
     *
     * @param playerId The UUID of the player.
     */
    public static void removeProtection(UUID playerId) {
        _protections.remove(playerId);
    }

    /**
     * Checks if a player is currently protected.
     *
     * @param playerId The UUID of the player.
     * @return True if the player is protected, false if not, or null if no protection is set.
     */
    public static @Nullable Boolean isProtected(UUID playerId) {
        if (!_protections.containsKey(playerId))
            return null;

        long cooldownTime = _protections.get(playerId);
        var currentTime = System.currentTimeMillis();
        return currentTime < cooldownTime; // Check if current time is before the cooldown time
    }

    /**
     * Retrieves a copy of the map containing player protections and their expiration times.
     * This method ensures that the original map remains unmodifiable by external callers.
     *
     * @return A new map containing the UUIDs of players as keys and their protection expiration times as values.
     */
    public static Map<UUID, Long> getProtections() {
        return new HashMap<>(_protections);
    }
    //#endregion

    //#region Teleporting

    /**
     * Marks a player as teleporting.
     *
     * @param playerId The UUID of the player.
     */
    public static void addTeleporting(UUID playerId) {
        _teleportingPlayers.add(playerId);
    }

    /**
     * Removes the teleporting status of a player.
     *
     * @param playerId The UUID of the player.
     */
    public static void removeTeleporting(UUID playerId) {
        _teleportingPlayers.remove(playerId);
    }

    /**
     * Checks if a player is currently teleporting.
     *
     * @param playerId The UUID of the player.
     * @return True if the player is teleporting, false otherwise.
     */
    public static boolean isTeleporting(UUID playerId) {
        return _teleportingPlayers.contains(playerId);
    }
    //#endregion

    //#region Mark for removal

    /**
     * Marks a player for removal.
     *
     * @param playerId The UUID of the player.
     */
    public static void markForRemoval(UUID playerId) {
        _markedForRemoval.add(playerId);
    }

    /**
     * Unmarks a player from the removal list.
     *
     * @param playerId The UUID of the player.
     */
    public static void unmarkForRemoval(UUID playerId) {
        _markedForRemoval.remove(playerId);
    }

    /**
     * Checks if a player is marked for removal.
     *
     * @param playerId The UUID of the player.
     * @return True if the player is marked for removal, false otherwise.
     */
    public static boolean isMarkedForRemoval(UUID playerId) {
        return _markedForRemoval.contains(playerId);
    }

    /**
     * Checks if the removal list is empty.
     *
     * @return True if no players are marked for removal, false otherwise.
     */
    public static boolean isMarkedForRemovalEmpty() {
        return _markedForRemoval.isEmpty();
    }

    /**
     * Retrieves a copy of the set of players marked for removal.
     *
     * @return A copy of the set of UUIDs of players marked for removal.
     */
    public static Set<UUID> getMarkedForRemovalSet() {
        return new HashSet<>(_markedForRemoval); // Return a copy to prevent external modification
    }
    //#endregion
}