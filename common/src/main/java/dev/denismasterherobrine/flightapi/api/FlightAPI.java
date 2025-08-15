package dev.denismasterherobrine.flightapi.api;

import java.util.*;

import dev.denismasterherobrine.flightapi.manager.FlightManager;

import net.minecraft.server.network.ServerPlayerEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlightAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger("FlightAPI");
    private static final FlightManager FLIGHT_MANAGER = FlightManager.getInstance();

    /**
     * Request flight control for the specified player.
     * @param modId Your mod's conditional identifier (e.g. "angelring")
     * @param player The player who wants to fly
     * @return true if control was successfully obtained to the specified modId; false if already occupied by another owner
     **/
    public static boolean requestFlight(String modId, ServerPlayerEntity player) {
        LOGGER.debug("[FlightAPI] {} requested flight for player {}", modId, player);
        return FLIGHT_MANAGER.requestFlightControl(modId, player);
    }

    /**
     * Release flight control.
     * @param modId Your mod's conditional identifier (e.g. "angelring")
     * @param player The player who wants to stop flying.
     * If the modId is not the current owner, nothing will happen.
     */
    public static void releaseFlight(String modId, ServerPlayerEntity player) {
        LOGGER.debug("[FlightAPI] {} released flight for player {}", modId, player);
        FLIGHT_MANAGER.releaseFlightControl(modId, player);
    }

    /**
     * Find out which mod is currently keeping flight for this player.
     * @param playerUuid The player's UUID
     * @return The modId of the current flight owner, if any.
     * If no flight owner is found, Optional.empty() will be returned.
     */
    public static Optional<String> getCurrentOwner(UUID playerUuid) {
        return FLIGHT_MANAGER.getCurrentOwner(playerUuid);
    }

    /**
     * Get the flight queue for the specified player.
     * @param playerUuid The player's UUID
     * @return A list of modIds in the order they requested flight control.
     * If the player has no queue, Optional.empty() will be returned.
     */
    public static Optional<List<String>> getFlightQueue(UUID playerUuid) {
        return FLIGHT_MANAGER.getFlightQueueSnapshot(playerUuid);
    }

    public static void cancelFlightRequest(String modId, UUID playerUuid) {
        LOGGER.debug("[FlightAPI] Cancelling flight request for mod {} and player {}", modId, playerUuid);
        FLIGHT_MANAGER.cancelQueuedRequest(modId, playerUuid);
    }
}
