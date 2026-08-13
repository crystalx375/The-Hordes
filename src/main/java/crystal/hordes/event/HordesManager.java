package crystal.hordes.event;

import crystal.hordes.config.HordesConfig;
import crystal.hordes.TheHordes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameRules;

import static crystal.hordes.config.HordesConfig.*;

public class HordesManager {
    /**
     * Связующее всех классов для спавна орды
     */
    private static int sleepPercentage = 100;

    public static void startHorde(ServerWorld world) {
        if (world == null || HordesConfig.isActive()) return;
        HordesConfig.setActive(true);
        HordesConfig.setTicks(0);
        HordesConfig.setWaveTimer(0);
        HordesConfig.setI(0);

        final GameRules rules = world.getGameRules();
        sleepPercentage = rules.getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
        rules.get(GameRules.PLAYERS_SLEEPING_PERCENTAGE).set(101, world.getServer());

        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList())
        {
            player.sendMessage(Text.literal("§4Hordes is coming...").formatted(Formatting.DARK_RED), true);
        }
        TheHordes.LOGGER.info("Hordes started in {}", world.getRegistryKey().getValue());
    }


    public static void endHorde(ServerWorld world) {
        if (world == null || !HordesConfig.isActive()) return;
        getSetMobEntities().removeIf(mob -> mob == null || !mob.isAlive());
        Despawner.startDespawnTimer();

        HordesConfig.setActive(false);
        HordesConfig.setTicks(0);
        HordesConfig.setWaveTimer(0);
        HordesConfig.setI(0);

        world.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE).set(sleepPercentage, world.getServer());

        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList())
        {
            player.sendMessage(Text.literal("§4Hordes ended...").formatted(Formatting.DARK_RED), true);
        }

        TheHordes.LOGGER.info("Hordes ended in {}", world.getRegistryKey().getValue());
        TheHordes.LOGGER.info("Waiting for delay: {} ticks", HordesConfig.DELAY_TICKS);
    }
}