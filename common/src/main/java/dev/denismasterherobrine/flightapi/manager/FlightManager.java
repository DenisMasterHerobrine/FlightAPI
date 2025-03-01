package dev.denismasterherobrine.flightapi.manager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

import net.minecraft.server.network.ServerPlayerEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlightManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("FlightManager");
    private static final FlightManager INSTANCE = new FlightManager();

    private final Map<UUID, Queue<String>> flightQueues = new HashMap<>();
    private final Map<UUID, String> currentOwners = new HashMap<>();

    private FlightManager() {}

    public static FlightManager getInstance() {
        return INSTANCE;
    }

    public synchronized boolean requestFlightControl(String modId, ServerPlayerEntity player) {
        if (player == null) {
            LOGGER.warn("[FlightManager] Player is null, can't request flight");
            return false;
        }

        UUID playerUuid = player.getGameProfile().getId();

        flightQueues.putIfAbsent(playerUuid, new LinkedList<>());
        Queue<String> queue = flightQueues.get(playerUuid);

        String currentOwner = currentOwners.get(playerUuid);
        if (modId.equals(currentOwner)) {
            LOGGER.debug("[FlightManager] {} already owns flight for {}", modId, playerUuid);
            return true;
        }

        if (currentOwner == null) {
            currentOwners.put(playerUuid, modId);
            LOGGER.debug("[FlightManager] {} got flight control immediately for {}", modId, playerUuid);
            setPlayerFlightEnabled(player, true);
            return true;
        }

        if (!queue.contains(modId)) {
            queue.offer(modId);
            LOGGER.debug("[FlightManager] {} queued for flight, current owner = {}", modId, currentOwner);
        }
        return false;
    }

    public synchronized void releaseFlightControl(String modId, ServerPlayerEntity player) {
        if (player == null) {
            LOGGER.warn("[FlightManager] Player is null, can't release flight");
            return;
        }

        UUID playerUuid = player.getGameProfile().getId();

        String currentOwner = currentOwners.get(playerUuid);

        if (currentOwner == null) {
            return;
        }

        if (!currentOwner.equals(modId)) {
            return;
        }

        currentOwners.remove(playerUuid);
        Queue<String> queue = flightQueues.get(playerUuid);

        if (queue == null || queue.isEmpty()) {
            setPlayerFlightEnabled(player, false);
            LOGGER.debug("[FlightManager] No next mod in queue. Flight disabled for {}", playerUuid);
            return;
        }

        String nextOwner = queue.poll();
        currentOwners.put(playerUuid, nextOwner);
        LOGGER.debug("[FlightManager] {} took flight control for {}", nextOwner, playerUuid);
        setPlayerFlightEnabled(player, true);
    }

    public synchronized Optional<String> getCurrentOwner(UUID playerUuid) {
        return Optional.ofNullable(currentOwners.get(playerUuid));
    }

    private void setPlayerFlightEnabled(ServerPlayerEntity player, boolean enabled) {
        if (player == null) {
            LOGGER.debug("[FlightManager] Player not found on server.");
            return;
        }

        player.getAbilities().allowFlying = enabled;
        player.getAbilities().flying = enabled;
        player.sendAbilitiesUpdate();

        LOGGER.debug("[FlightManager] setFlightEnabled({}, {}) done", player.getGameProfile().getId(), enabled);
    }

    public Map<UUID, Queue<String>> getFlightQueues() {
        return flightQueues;
    }
}
