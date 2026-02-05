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

import static crystal.hordes.config.HordesConfig.DEBUG;
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

        int playerCount = world.getServer().getPlayerManager().getPlayerList().size();
        if (playerCount == 0) return;

        int currentHordeCount = HordesConfig.getHordeZombies().size();

        HordesConfig cfg = get();
        HordesConfig.getHordeZombies().removeIf(mob -> mob == null || !mob.isAlive() || mob.isRemoved());
        Random rnd = world.getRandom();

        int globalLimit = cfg.hordesLimitPerPlayer * playerCount;

        if (currentHordeCount >= globalLimit) {
            TheHordes.LOGGER.info("[Hordes] Spawn canceled: " + currentHordeCount + " >= " + globalLimit);
            return;
        }

        // Для каждого игрока (орды) даем свой айди
        // Ищим места вокруг pos
        // + чуть проверок
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getWorld() != world) continue;
            EntityType<?> type;

            int toSpawn = cfg.zombiesPerWave;
            int attempts = 0;

            BlockPos basePos = SpawnPos.findSpawnAroundPlayer(world, player, null, rnd);
            if (basePos == null) continue;

            while (toSpawn > 0 && attempts < toSpawn * 10) {
                // Здесь мы находим finalPos для каждого моба
                type = getRandomMobByWeight(mobPool, rnd);
                attempts++;

                BlockPos finalPos = spawnCluster(world, basePos, player);
                if (finalPos == null) continue;

                if (!SpawnPos.isValidSpawn(world, type, finalPos)) continue;

                // Даем случайные атрибуты мобу
                MobEntity mob = spawnHordes(world, player, type, finalPos);
                if (rnd.nextFloat() < 0.5) world.playSound(player, finalPos, SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.AMBIENT, 1f, 1f);
                toSpawn--;

                if (DEBUG) TheHordes.LOGGER.info("[SpawnWave] Spawning: " + mob);
            }
            player.playSound(SoundEvents.AMBIENT_NETHER_WASTES_MOOD.value(), SoundCategory.AMBIENT, 1, 1);
        }
    }

    // Саппорт класс
    private static EntityType<?> getRandomMobByWeight(Map<String, Integer> mobMap, Random rnd) {
        int totalWeight = mobMap.values().stream().mapToInt(Integer::intValue).sum();
        int r = rnd.nextInt(totalWeight);
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