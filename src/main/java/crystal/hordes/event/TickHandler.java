package crystal.hordes.event;

import crystal.hordes.TheHordes;
import crystal.hordes.config.HordesConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.Map;

import static crystal.hordes.event.Despawner.checkForDespawn;
import static crystal.hordes.event.Despawner.despawnTimer;
import static crystal.hordes.event.HordesManager.endHorde;
import static crystal.hordes.event.HordesManager.startHorde;
import static crystal.hordes.config.HordesConfig.*;
import static crystal.hordes.util.Nbt.loadState;
import static crystal.hordes.util.Nbt.saveState;

public class TickHandler {
    static boolean firstTick = true;
    static boolean wave = false;

    private static void worldTick(ServerWorld world, Map<String, Integer> mobPool) {
        HordesConfig cfg = HordesConfig.get();
        if (world.getPlayers().isEmpty()) return;

        if (!active && ticks >= cfg.daysBetweenHordes * 24000) {
            boolean nightCheck = (world.isNight() || !HordesConfig.required_night);
            if (nightCheck) startHorde(world);
        } else {
            if (ticks > cfg.hordeDuration - cfg.waveInterval + UPDATE_TIME * 2) {
                endHorde(world);
                return;
            }

            if (wave) {
                boolean canSpawn = false;
                if (world.getRegistryKey() == World.OVERWORLD && cfg.spawnInOverworld) canSpawn = true;
                else if (world.getRegistryKey() == World.NETHER && cfg.spawnInNether) canSpawn = true;
                else if (world.getRegistryKey() == World.END && cfg.spawnInEnd) canSpawn = true;

                if (canSpawn) {
                    SpawnWave.spawnWave(world, mobPool);
                    if (DEBUG) TheHordes.LOGGER.info("[TickHandler] Spawning wave in dimension: " + world.getRegistryKey().getValue());
                } else {
                    TheHordes.LOGGER.warn("Cant spawn, because config spawn in that dimension = " + canSpawn + " (" + world.getRegistryKey().getValue().toString() + ")");
                }
            }
        }
    }

    public static void onServerTick(MinecraftServer server) {
        if (server.getTicks() % UPDATE_TIME != 0) return;
        ticks += UPDATE_TIME;
        if (DEBUG) TheHordes.LOGGER.info("[TickHandler] ticks: " + ticks);
        if (active) waveTimer += UPDATE_TIME;
        if (!firstTick && server.getTicks() % 1200 == 0) {
            saveState();
        }
        HordesConfig cfg = HordesConfig.get();
        if (active && (waveTimer >= cfg.waveInterval || i < 1)) {
            wave = true;
            i++;
        }

        cfg = HordesConfig.get();
        for (ServerWorld world : server.getWorlds()) {
            Map<String, Integer> mobPool;
            if (world.getRegistryKey() == World.NETHER) mobPool = cfg.nether;
            else if (world.getRegistryKey() == World.END) mobPool = cfg.end;
            else mobPool = cfg.overworld;

            worldTick(world, mobPool);
        }

        if (wave) {
            waveTimer = 0;
            wave = false;
            saveState();
        }

        if (server.getCurrentPlayerCount() == 0) return;
        if (firstTick) {
            loadState();
            firstTick = false;
        }
        despawnTimer();
        checkForDespawn();
    }
}