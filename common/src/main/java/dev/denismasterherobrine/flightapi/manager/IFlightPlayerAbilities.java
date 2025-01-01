package dev.denismasterherobrine.flightapi.manager;

import net.minecraft.entity.player.PlayerEntity;

public interface IFlightPlayerAbilities {
    PlayerEntity getFlightOwner();
    void setFlightOwner(PlayerEntity player);
}
