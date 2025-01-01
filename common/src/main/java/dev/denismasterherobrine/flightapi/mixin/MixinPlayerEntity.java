package dev.denismasterherobrine.flightapi.mixin;

import dev.denismasterherobrine.flightapi.manager.IFlightPlayerAbilities;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity {

    @Shadow
    private PlayerAbilities abilities;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void flightapi$onPlayerEntityInit(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        ((IFlightPlayerAbilities) this.abilities).setFlightOwner(self);
    }
}
