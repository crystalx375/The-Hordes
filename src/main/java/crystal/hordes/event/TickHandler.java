package crystal.hordes.event;

import crystal.hordes.TheHordes;
import crystal.hordes.config.HordesConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.Map;

import static crystal.hordes.event.Despawner.despawnTimer;
import static crystal.hordes.event.HordesManager.endHorde;
import static crystal.hordes.event.HordesManager.startHorde;
import static crystal.hordes.event.SpawnWave.spawnWave;
import static crystal.hordes.util.Nbt.loadState;

public class TickHandler {
    private static boolean firstTick = true;
    private static boolean wave = false;

    private static void worldTick(ServerWorld world, Map<String, Integer> mobPool) {
        if (world.getPlayers().isEmpty()) return;

        final boolean active = HordesConfig.isActive();
        final int ticks = HordesConfig.getTicks();

        if (!active && ticks >= HordesConfig.DAYS_BETWEEN_HORDES * 24000) {
            final boolean nightCheck = (world.isNight() || !HordesConfig.REQUIRED_NIGHT);
            if (nightCheck) startHorde(world);
        } else if (active) {
            if (ticks > HordesConfig.HORDE_DURATION - HordesConfig.WAVE_INTERVAL + HordesConfig.UPDATE_TIME * 2) {
                endHorde(world);
                return;
            }

            if (wave) {
                checkSpawn(world, mobPool);
            }
        }
    }

    public static void onServerTick(MinecraftServer server) {
        if (server.getTicks() % HordesConfig.UPDATE_TIME != 0) return;

        if (firstTick) {
            loadState();
            firstTick = false;
        }

        final int currentTicks = HordesConfig.getTicks() + HordesConfig.UPDATE_TIME;
        HordesConfig.setTicks(currentTicks);

        final boolean active = HordesConfig.isActive();
        final int i = HordesConfig.getI();
        int waveTimer = HordesConfig.getWaveTimer();

        if (HordesConfig.DEBUG)
        {
            TheHordes.LOGGER.info("[TickHandler] ticks: {}, active: {}, waveTimer: {}, i: {}", currentTicks, active, waveTimer, i);
        }

        if (active)
        {
            waveTimer += HordesConfig.UPDATE_TIME;
            HordesConfig.setWaveTimer(waveTimer);
            if (waveTimer >= HordesConfig.WAVE_INTERVAL || i < 1)
            {
                wave = true;
                HordesConfig.setI(i + 1);
            }
        }


        for (ServerWorld world : server.getWorlds()) {
            final Map<String, Integer> mobPool;

            if (world.getRegistryKey() == World.NETHER) mobPool = HordesConfig.NETHER;
            else if (world.getRegistryKey() == World.END) mobPool = HordesConfig.END;
            else mobPool = HordesConfig.OVERWORLD;

            worldTick(world, mobPool);
        }

        if (wave)
        {
            HordesConfig.setWaveTimer(0);
            wave = false;
        }

        if (server.getCurrentPlayerCount() == 0) return;

        despawnTimer(currentTicks);
    }

    private static void checkSpawn(ServerWorld world, Map<String, Integer> mobPool) {
        boolean canSpawn = false;

        if (world.getRegistryKey() == World.OVERWORLD && HordesConfig.SPAWN_IN_OVERWORLD) canSpawn = true;
        else if (world.getRegistryKey() == World.NETHER && HordesConfig.SPAWN_IN_NETHER) canSpawn = true;
        else if (world.getRegistryKey() == World.END && HordesConfig.SPAWN_IN_END) canSpawn = true;

        if (canSpawn) {
            spawnWave(world, mobPool);
            if (HordesConfig.DEBUG) {
                TheHordes.LOGGER.info("[TickHandler] Spawning wave in dimension: {}", world.getRegistryKey().getValue());
            }
        } else {
            TheHordes.LOGGER.warn("Cant spawn, because config spawn in that dimension = {} ({})", canSpawn, world.getRegistryKey().getValue());
        }
    }
}