package dev.denismasterherobrine.flightapi.mixin;

import com.mojang.authlib.GameProfile;
import dev.denismasterherobrine.flightapi.manager.IFlightPlayerAbilities;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
    private void flightapi$onPlayerEntityInit(World world, BlockPos pos, float yaw, GameProfile gameProfile, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        ((IFlightPlayerAbilities) this.abilities).setFlightOwner(self);
    }
}
