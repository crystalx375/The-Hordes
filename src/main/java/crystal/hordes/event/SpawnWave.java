package crystal.hordes.event;

import crystal.hordes.TheHordes;
import crystal.hordes.config.HordesConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;

import static crystal.hordes.config.HordesConfig.*;
import static crystal.hordes.event.HordesVariations.prepareMob;

public class SpawnWave {
    /**
     * Спавним орду, определяя игроков и давай ордам айди
     * Проверка спавна для орды (перепроверка)
     * Также реализация веса орды
     */
    public static void spawnWave(ServerWorld world, Map<String, Integer> mobPool) {
        int playerCount = world.getServer().getPlayerManager().getPlayerList().size();
        if (playerCount == 0) return;

        HordesConfig cfg = get();
        HordesConfig.getHordeZombies().removeIf(mob -> mob == null || !mob.isAlive() || mob.isRemoved());
        Random rnd = world.getRandom();

        boolean isNether = world.getRegistryKey() == World.NETHER;
        int globalLimit = cfg.hordesLimitPerPlayer * playerCount;
        int currentHordeCount = HordesConfig.getHordeZombies().size();

        if (currentHordeCount >= globalLimit) {
            TheHordes.LOGGER.info("[Hordes] Spawn canceled: " + currentHordeCount + " >= " + globalLimit);
            return;
        }

        // Для каждого игрока (орды) даем свой айди
        // Ищим места вокруг pos
        // + чуть проверок
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getWorld() != world) continue;

            UUID playerUuid = player.getUuid();
            UUID clusterId = UUID.randomUUID();
            EntityType<?> type;
            BlockPos basePos = SpawnPos.findSpawnAroundPlayer(world, player, null, rnd);
            if (basePos == null) continue;
            int toSpawn = cfg.zombiesPerWave;
            int attempts = 0;
            while (toSpawn > 0 && attempts < toSpawn * 10) {
                attempts++;
                type = getRandomMobByWeight(mobPool, rnd);
                int x = rnd.nextBetween((int) -minCluster, (int) maxCluster);
                int z = rnd.nextBetween((int) -minCluster, (int) maxCluster);
                BlockPos finalPos;
                int tx = basePos.getX() + x;
                int tz = basePos.getZ() + z;
                if (isNether) {
                    finalPos = SpawnPos.findSurfaceInNether(world, player, tx, basePos.getY(), tz);
                } else {
                    int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, tx, tz);
                    if (world.getLightLevel(LightType.BLOCK, basePos) > requiredLightLevel) continue;
                    finalPos = new BlockPos(tx, y, tz);
                }
                if (finalPos == null) continue;
                // Перепроверка из-за смещения ahh
                if (SpawnPos.isValidSpawn(world, type, finalPos)) {
                    Entity entity = type.create(world);
                    // Даем случайные атрибуты мобу
                    if (entity instanceof MobEntity mob) {
                        prepareMob(mob, clusterId, playerUuid, finalPos, world, rnd);
                        if ((type == EntityType.SKELETON && rnd.nextFloat() < 0.2f) ||
                                (type == EntityType.ZOMBIE && rnd.nextFloat() < 0.01f)) {

                            ZombieHorseEntity horse = EntityType.ZOMBIE_HORSE.create(world);
                            if (horse != null) {
                                horse.setTame(true);
                                prepareMob(horse, clusterId, playerUuid, finalPos, world, rnd);
                                world.spawnEntity(horse);
                                mob.startRiding(horse);
                            }
                        }
                        if (DEBUG) {
                            TheHordes.LOGGER.info("[SpawnWave] Spawning: " + mob);
                        }
                        world.spawnEntity(mob);
                        HordesConfig.getHordeZombies().add(mob);
                    }
                    toSpawn--;
                }
                player.playSound(SoundEvents.AMBIENT_CAVE.value(), SoundCategory.AMBIENT, 2.0f, 0.8f);
            }
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