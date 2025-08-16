package dev.denismasterherobrine.flightapi.fabric;

import dev.denismasterherobrine.flightapi.command.FlightAPICommand;
import net.fabricmc.api.ModInitializer;

import dev.denismasterherobrine.flightapi.FlightMain;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class FlightAPIFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        FlightMain.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> FlightAPICommand.register(dispatcher));
    }
}
