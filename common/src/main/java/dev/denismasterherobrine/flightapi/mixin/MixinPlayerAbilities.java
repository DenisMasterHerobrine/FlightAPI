package dev.denismasterherobrine.flightapi.mixin;

import dev.denismasterherobrine.flightapi.manager.FlightManager;
import dev.denismasterherobrine.flightapi.manager.IFlightPlayerAbilities;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerAbilities.class)
public abstract class MixinPlayerAbilities implements IFlightPlayerAbilities {

    private static final Logger LOGGER = LoggerFactory.getLogger("FlightApi|MixinPlayerAbilities");

    @Shadow public boolean flying;
    @Shadow public boolean allowFlying;

    @Unique
    private PlayerEntity flightapi$owner;

    @Override
    public PlayerEntity getFlightOwner() {
        return flightapi$owner;
    }

    @Override
    public void setFlightOwner(PlayerEntity player) {
        this.flightapi$owner = player;
    }

    @Redirect(
            method = "readNbt",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/player/PlayerAbilities;flying:Z",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private void flightapi$redirectFlyingSet(PlayerAbilities instance, boolean newValue) {
        PlayerEntity owner = this.getFlightOwner();
        if (owner == null) {
            this.flying = newValue;
            LOGGER.debug("[MixinPlayerAbilities] No owner found, setting flying={} as Vanilla", newValue);
            return;
        }

        String currentOwner = FlightManager.getInstance().getCurrentOwner(owner.getGameProfile().getId()).orElse(null);

        if (currentOwner == null) {
            this.flying = newValue;
            LOGGER.debug("[MixinPlayerAbilities] No FlightAPI owner, letting NBT set flying={}", newValue);
        } else {
            LOGGER.debug("[MixinPlayerAbilities] Flight is owned by {}, ignoring NBT flying={}", currentOwner, newValue);
        }
    }

    @Redirect(
            method = "readNbt",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/player/PlayerAbilities;allowFlying:Z",
                    opcode = Opcodes.PUTFIELD
            )
    )

    private void flightapi$redirectAllowFlyingSet(PlayerAbilities instance, boolean newValue) {
        PlayerEntity owner = this.getFlightOwner();

        if (owner == null) {
            this.allowFlying = newValue;
            LOGGER.debug("[MixinPlayerAbilities] No owner found, setting allowFlying={} as Vanilla", newValue);
            return;
        }

        String currentOwner = FlightManager.getInstance().getCurrentOwner(owner.getGameProfile().getId()).orElse(null);

        if (currentOwner == null) {
            this.allowFlying = newValue;
            LOGGER.debug("[MixinPlayerAbilities] No FlightApi owner, letting NBT set allowFlying={}", newValue);
        } else {
            LOGGER.debug("[MixinPlayerAbilities] Flight owned by {}, ignoring NBT allowFlying={}", currentOwner, newValue);
        }
    }
}

