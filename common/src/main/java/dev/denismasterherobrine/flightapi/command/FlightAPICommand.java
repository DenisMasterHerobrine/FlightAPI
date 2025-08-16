package dev.denismasterherobrine.flightapi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.denismasterherobrine.flightapi.api.FlightAPI;
import dev.denismasterherobrine.flightapi.manager.FlightManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FlightAPICommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("FlightManager");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("flightapi")
                .requires(src -> src.hasPermissionLevel(2));

        // /flightapi status [player]
        root.then(CommandManager.literal("status")
                .executes(ctx -> status(ctx, selfOrError(ctx)))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> status(ctx, EntityArgumentType.getPlayer(ctx, "player")))));

        // /flightapi owner [player]
        root.then(CommandManager.literal("owner")
                .executes(ctx -> owner(ctx, selfOrError(ctx)))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> owner(ctx, EntityArgumentType.getPlayer(ctx, "player")))));

        // /flightapi queue [player]
        root.then(CommandManager.literal("queue")
                .executes(ctx -> queue(ctx, selfOrError(ctx)))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> queue(ctx, EntityArgumentType.getPlayer(ctx, "player")))));

        // /flightapi release <modId> [player]
        root.then(CommandManager.literal("release")
                .then(CommandManager.argument("modId", StringArgumentType.word())
                        .executes(ctx -> release(ctx, selfOrError(ctx), StringArgumentType.getString(ctx, "modId")))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(ctx -> release(ctx,
                                        EntityArgumentType.getPlayer(ctx, "player"),
                                        StringArgumentType.getString(ctx, "modId"))))));

        // /flightapi cancel <modId> [player]
        root.then(CommandManager.literal("cancel")
                .then(CommandManager.argument("modId", StringArgumentType.word())
                        .executes(ctx -> cancel(ctx, selfOrError(ctx), StringArgumentType.getString(ctx, "modId")))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(ctx -> cancel(ctx,
                                        EntityArgumentType.getPlayer(ctx, "player"),
                                        StringArgumentType.getString(ctx, "modId"))))));

        // /flightapi purge [player]
        root.then(CommandManager.literal("purge")
                .executes(ctx -> purge(ctx, selfOrError(ctx)))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> purge(ctx, EntityArgumentType.getPlayer(ctx, "player")))));

        dispatcher.register(root);
        LOGGER.info("[FlightAPI] commands registered");
    }

    private static int status(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity p) {
        final UUID id = p.getUuid();
        final String name = p.getGameProfile().getName();

        final Optional<String> owner = FlightAPI.getCurrentOwner(id);
        final List<String> queue = FlightAPI.getFlightQueue(id).orElse(List.of());

        final boolean creative = p.getAbilities().creativeMode;
        final boolean spectator = p.isSpectator();
        final boolean allowFlying = p.getAbilities().allowFlying;
        final boolean flying = p.getAbilities().flying;

        final Text msg = Text.literal("FlightAPI status for ")
                .append(Text.literal(name).formatted(Formatting.AQUA))
                .append(Text.literal("\nOwner: ").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(owner.orElse(null))).formatted(Formatting.YELLOW))
                .append(Text.literal("\nQueue: ").formatted(Formatting.GRAY))
                .append(Text.literal(queue.toString()).formatted(Formatting.YELLOW))
                .append(Text.literal("\nallowFlying=").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(allowFlying)).formatted(Formatting.GREEN))
                .append(Text.literal("  flying=").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(flying)).formatted(Formatting.GREEN))
                .append(Text.literal("\ncreative=").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(creative)).formatted(Formatting.GOLD))
                .append(Text.literal("  spectator=").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(spectator)).formatted(Formatting.GOLD));

        ctx.getSource().sendFeedback(() -> msg, false);

        LOGGER.info(
                "[FlightAPI] status: player={} owner='{}' queue={} allowFlying={} flying={} creative={} spectator={}",
                name, owner.orElse(null), queue, allowFlying, flying, creative, spectator
        );
        return 1;
    }


    private static int owner(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        String owner = FlightAPI.getCurrentOwner(player.getUuid())
                .orElse("No flight owner found for player " + player.getName());

        ctx.getSource().sendFeedback(
                () -> Text.literal("Owner: ").append(Text.literal(owner).formatted(Formatting.YELLOW)),
                true
        );

        return 1;
    }

    private static int queue(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        List<String> queue = FlightAPI.getFlightQueue(player.getUuid()).orElse(List.of());

        ctx.getSource().sendFeedback(
                () -> Text.literal("Flight queue for " + player.getName().getString() + ": ").append(Text.literal(String.join(", ", queue)).formatted(Formatting.YELLOW)),
                true
        );

        return 1;
    }

    private static int release(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, String modId) {
        FlightAPI.releaseFlight(modId, player);

        ctx.getSource().sendFeedback(
                () -> Text.literal("Attempted to release flight control for " + player.getName().getString() + " by " + modId).formatted(Formatting.GREEN),
                true
        );

        return 1;
    }

    private static int cancel(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, String modId) {
        FlightAPI.cancelFlightRequest(modId, player.getUuid());

        ctx.getSource().sendFeedback(
                () -> Text.literal("Attempted to cancel flight request for " + player.getName().getString() + " by " + modId).formatted(Formatting.RED),
                true
        );

        return 1;
    }

    private static int purge(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        FlightManager.getInstance().purgePlayer(player.getUuid());

        ctx.getSource().sendFeedback(
                () -> Text.literal("Purged flight data for " + player.getName().getString()).formatted(Formatting.RED),
                true
        );

        return 1;
    }

    private static ServerPlayerEntity selfOrError(CommandContext<ServerCommandSource> ctx) {
        try {
            return ctx.getSource().getPlayer();
        } catch (Exception noPlayer) {
            ctx.getSource().sendError(Text.literal("Must specify <player> when running from console."));
            throw new IllegalStateException("No player in context");
        }
    }
}
