package dev.denismasterherobrine.flightapi.neoforge;

import dev.denismasterherobrine.flightapi.FlightMain;
import dev.denismasterherobrine.flightapi.command.FlightAPICommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(FlightMain.MOD_ID)
public final class FlightAPINeoForge {
    public FlightAPINeoForge(IEventBus modBus) {
        // Run our common setup.
        FlightMain.init();
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        FlightAPICommand.register(event.getDispatcher());
    }
}
