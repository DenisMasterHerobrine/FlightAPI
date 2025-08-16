package dev.denismasterherobrine.flightapi.manager;

import java.util.*;

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

        if (modId == null || modId.isBlank()) {
            LOGGER.warn("[FlightManager] Invalid modId! modId cannot be null or blank on request.");
            return false;
        }

        UUID playerUuid = player.getGameProfile().getId();

        flightQueues.putIfAbsent(playerUuid, new LinkedList<>());
        Queue<String> queue = flightQueues.get(playerUuid);

        String currentOwner = currentOwners.get(playerUuid);
        if (modId.equals(currentOwner)) {
            LOGGER.info("[FlightManager] {} already owns flight for {}", modId, playerUuid);
            setPlayerFlightEnabled(player, true);
            return true;
        }

        if (currentOwner == null) {
            currentOwners.put(playerUuid, modId);
            LOGGER.info("[FlightManager] {} got flight control immediately for {}", modId, playerUuid);
            setPlayerFlightEnabled(player, true);
            return true;
        }

        if (!queue.contains(modId)) {
            queue.offer(modId);
            LOGGER.info("[FlightManager] {} queued for flight, current owner = {}", modId, currentOwner);
        }
        return false;
    }

    public synchronized void releaseFlightControl(String modId, ServerPlayerEntity player) {
        if (player == null) {
            LOGGER.warn("[FlightManager] Player is null, can't release flight");
            return;
        }

        if (modId == null || modId.isBlank()) {
            LOGGER.warn("[FlightManager] Invalid modId! modId cannot be null or blank on release.");
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

    public void setPlayerFlightEnabled(ServerPlayerEntity player, boolean enabled) {
        if (player == null) {
            LOGGER.debug("[FlightManager] Player not found on server.");
            return;
        }

        final boolean isSpectator = player.isSpectator();
        final boolean isCreative = player.getAbilities().creativeMode;

        // Players who can always fly keep that permission, otherwise it is set based on the mods request
        player.getAbilities().allowFlying = enabled || isSpectator || isCreative;

        if (isSpectator || enabled) {
            player.getAbilities().flying = true;
        } else if (!isCreative) {
            player.getAbilities().flying = false;
        }

        LOGGER.debug("[FlightManager] setFlightEnabled({}, {}) done", player.getGameProfile().getId(), enabled);
    }

    public synchronized Optional<List<String>> getFlightQueueSnapshot(UUID playerUuid) {
        Queue<String> queue = flightQueues.get(playerUuid);
        return (queue == null) ? Optional.empty() : Optional.of(List.copyOf(queue));
    }

    public synchronized void cancelQueuedRequest(String modId, UUID playerUuid) {
        Queue<String> queue = flightQueues.get(playerUuid);
        if (queue != null) queue.remove(modId);
    }

    public synchronized void purgePlayer(UUID playerUuid) {
        currentOwners.remove(playerUuid);
        flightQueues.remove(playerUuid);
    }
}