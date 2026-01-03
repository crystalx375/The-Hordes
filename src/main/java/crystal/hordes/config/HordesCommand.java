package crystal.hordes.config;

import crystal.hordes.event.Despawner;
import crystal.hordes.event.HordesManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

public class HordesCommand {
    /**
     * Регистрация команд, тупо спиздил и вставил, чуть поменяв функционал
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("hordes")
                    .requires(src -> src.hasPermissionLevel(3));


            root.then(CommandManager.literal("start").executes(ctx -> {
                ServerWorld world = ctx.getSource().getServer().getOverworld();
                HordesManager.startHorde(world);
                return 1;
            }));


            root.then(CommandManager.literal("stop").executes(ctx -> {
                ServerWorld world = ctx.getSource().getServer().getOverworld();
                HordesManager.endHorde(world);
                return 1;
            }));


            root.then(CommandManager.literal("despawn").executes(ctx -> {
                Despawner.command();
                return 1;
            }));

            dispatcher.register(root);
        });
    }
}