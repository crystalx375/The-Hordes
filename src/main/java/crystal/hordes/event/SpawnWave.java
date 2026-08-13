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

import static crystal.hordes.event.HordesVariations.spawnMob;
import static crystal.hordes.event.SpawnPos.spawnCluster;

public class SpawnWave {
    private static final int PER_WAVE = HordesConfig.ZOMBIES_PER_WAVE;

    /**
     * Спавним орду, определяя игроков и давай ордам айди
     * Проверка спавна для орды (перепроверка)
     * Также реализация веса орды
     */
    public static void spawnWave(ServerWorld world, Map<String, Integer> mobPool) {
        final int playerCount = world.getServer().getPlayerManager().getPlayerList().size();
        if (playerCount == 0) return;

        HordesConfig.getSetMobEntities().removeIf(mob -> mob == null || !mob.isAlive() || mob.isRemoved());

        final Random rnd = world.getRandom();
        final int CURRENT_HORDE_COUNT = HordesConfig.getSetMobEntities().size();
        final int GLOBAL_LIMIT = HordesConfig.HORDES_LIMIT_PER_PLAYER * playerCount;

        if (CURRENT_HORDE_COUNT >= GLOBAL_LIMIT)
        {
            TheHordes.LOGGER.info("Spawn canceled: {} >= {}", CURRENT_HORDE_COUNT, GLOBAL_LIMIT);
            return;
        }

        // Для каждого игрока (орды) даем свой айди
        // Ищим места вокруг pos
        // + чуть проверок
        for (ServerPlayerEntity player : world.getPlayers())
        {
            if (player.getWorld() != world) continue;

            final BlockPos basePos = SpawnPos.findSpawnAroundPlayer(world, player, null, rnd);
            if (basePos == null) continue;

            spawnHordeWave(world, basePos, player, mobPool, rnd);
            player.getWorld().playSound(
                    null,
                    basePos,
                    SoundEvents.AMBIENT_NETHER_WASTES_MOOD.value(),
                    SoundCategory.AMBIENT,
                    5f, 1f
            );
        }
    }

    private static void spawnHordeWave(ServerWorld world, BlockPos basePos, ServerPlayerEntity player, Map<String, Integer> mobPool, Random rnd) {
        int attempts = 0;
        int count = PER_WAVE;

        while (count > 0 && attempts < count * 10) {
            attempts++;

            final EntityType<?> type = getRandomMobByWeight(mobPool, rnd);
            final BlockPos finalPos = spawnCluster(world, basePos, player);

            if (finalPos == null || (!SpawnPos.isValidSpawn(world, type, finalPos))) continue;

            if (rnd.nextFloat() < 0.2)
            {
                world.playSound(
                        null,
                        finalPos,
                        SoundEvents.ENTITY_ZOMBIE_AMBIENT,
                        SoundCategory.AMBIENT,
                        1f, 1f
                );
            }
            final MobEntity mob = spawnMob(world, player, type, finalPos);
            if (HordesConfig.DEBUG) TheHordes.LOGGER.info("[SpawnWave] Spawning: {}", mob);

            count--;
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