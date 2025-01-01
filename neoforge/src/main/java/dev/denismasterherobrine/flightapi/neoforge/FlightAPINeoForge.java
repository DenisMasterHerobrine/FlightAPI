package dev.denismasterherobrine.flightapi.neoforge;

import net.neoforged.fml.common.Mod;

import dev.denismasterherobrine.flightapi.FlightMain;

@Mod(FlightMain.MOD_ID)
public final class FlightAPINeoForge {
    public FlightAPINeoForge() {
        // Run our common setup.
        FlightMain.init();
    }
}
