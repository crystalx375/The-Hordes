package crystal.hordes.event;

import crystal.hordes.TheHordes;
import crystal.hordes.config.HordesConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.Map;

import static crystal.hordes.config.HordesConfig.get;
import static crystal.hordes.event.HordesVariations.spawnHordes;
import static crystal.hordes.event.SpawnPos.spawnCluster;

public class SpawnWave {
    /**
     * Спавним орду, определяя игроков и давай ордам айди
     * Проверка спавна для орды (перепроверка)
     * Также реализация веса орды
     */
    public static void spawnWave(ServerWorld world, Map<String, Integer> mobPool) {

        final int playerCount = world.getServer().getPlayerManager().getPlayerList().size();
        if (playerCount == 0) return;

        final int currentHordeCount = HordesConfig.getHordeZombies().size();
        final Random rnd = world.getRandom();

        HordesConfig.getHordeZombies().removeIf(mob -> mob == null || !mob.isAlive() || mob.isRemoved());

        final int globalLimit = HordesConfig.HORDES_LIMIT_PER_PLAYER * playerCount;

        if (currentHordeCount >= globalLimit) {
            TheHordes.LOGGER.info("Spawn canceled: {} >= {}", currentHordeCount, globalLimit);
            return;
        }

        // Для каждого игрока (орды) даем свой айди
        // Ищим места вокруг pos
        // + чуть проверок
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getWorld() != world) continue;

            int toSpawn = HordesConfig.ZOMBIES_PER_WAVE;
            int attempts = 0;

            final BlockPos basePos = SpawnPos.findSpawnAroundPlayer(world, player, null, rnd);
            if (basePos == null) continue;
            spawnMobs(world, basePos, player, mobPool, toSpawn, attempts, rnd);
            player.getWorld().playSound(null, basePos, SoundEvents.AMBIENT_NETHER_WASTES_MOOD.value(), SoundCategory.AMBIENT, 5f, 1f);
        }
    }

    private static void spawnMobs(ServerWorld world, BlockPos basePos, ServerPlayerEntity player, Map<String, Integer> mobPool, int toSpawn, int attempts, Random rnd) {
        while (toSpawn > 0 && attempts < toSpawn * 10) {
            final EntityType<?> type = getRandomMobByWeight(mobPool, rnd);
            attempts++;

            final BlockPos finalPos = spawnCluster(world, basePos, player);
            if (finalPos == null || (!SpawnPos.isValidSpawn(world, type, finalPos))) continue;

            final MobEntity mob = spawnHordes(world, player, type, finalPos);
            if (rnd.nextFloat() < 0.5) world.playSound(null, finalPos, SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.AMBIENT, 1f, 1f);
            toSpawn--;

            if (get().DEBUG) TheHordes.LOGGER.info("[SpawnWave] Spawning: {}", mob);
        }
    }

    // Саппорт класс
    private static EntityType<?> getRandomMobByWeight(Map<String, Integer> mobMap, Random rnd) {
        final int totalWeight = mobMap.values().stream().mapToInt(Integer::intValue).sum();
        final int r = rnd.nextInt(totalWeight);
        int count = 0;
        for (Map.Entry<String, Integer> entry : mobMap.entrySet()) {
            count += entry.getValue();
            if (r < count) {
                return EntityType.get(entry.getKey().trim()).orElse(EntityType.ZOMBIE);
            }
        }
        return EntityType.ZOMBIE;
    }
}