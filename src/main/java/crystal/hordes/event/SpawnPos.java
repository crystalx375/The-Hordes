package crystal.hordes.event;

import crystal.hordes.TheHordes;
import crystal.hordes.config.HordesConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import static crystal.hordes.config.HordesConfig.*;
import static crystal.hordes.config.HordesConfig.maxCluster;
import static crystal.hordes.config.HordesConfig.minCluster;


public class SpawnPos {
    /**
     * Поиск места по радиусу
     * Также поиск для незера разделен так как там нельзя topY сделать (крышу ада всегда будет возвращать)
     */
    public static BlockPos findSpawnAroundPlayer(ServerWorld world, ServerPlayerEntity player, EntityType<?> type, Random rnd) {
        boolean isNether = world.getRegistryKey() == World.NETHER;
        int minR = isNether ? HordesConfig.minRadius / 2 : HordesConfig.minRadius;
        int maxR = isNether ? HordesConfig.maxRadius / 2 : HordesConfig.maxRadius;
        EntityType<?> checkType = type != null ? type : EntityType.ZOMBIE;

        for (int tries = 0; tries < 75; tries++) {
            double angle = rnd.nextDouble() * 2.0 * Math.PI;
            double r = Math.sqrt(rnd.nextDouble() * (maxR * maxR - minR * minR) + (minR * minR));
            int x = (int) (r * Math.cos(angle));
            int z = (int) (r * Math.sin(angle));

            BlockPos targetPos = player.getBlockPos().add(x, 0, z);
            BlockPos finalPos;

            if (isNether) {
                int y = Math.min((int)player.getY() + 20, 130);
                finalPos = findSurfaceInNether(world, player, targetPos.getX(), y, targetPos.getZ());
            } else {
                int surface = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, targetPos.getX(), targetPos.getZ());
                finalPos = new BlockPos(targetPos.getX(), surface, targetPos.getZ());
            }

            if (finalPos != null && isValidSpawn(world, checkType, finalPos)) {
                if (areaCheck(world, finalPos)) {
                    TheHordes.LOGGER.info("[Hordes] Spawn: " + finalPos + " For player: " + player.getName().getString());
                    return finalPos;
                }
            }
        }
        TheHordes.LOGGER.warn("[Hordes] Spawn is not valid");
        return null;
    }
    /**
     * Проверки на место спавна
     */
    public static boolean isValidSpawn(ServerWorld world, EntityType<?> type, BlockPos pos) {
        Entity temp = type.create(world);
        if (temp == null) return false;
        boolean isNether = world.getRegistryKey() == World.NETHER;
        net.minecraft.util.math.Box box = temp.getType().getDimensions().getBoxAt(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        return world.isSpaceEmpty(temp, box)
                && !world.containsFluid(box)
                && world.getBlockState(pos.down()).isSolidBlock(world, pos.down())
                && world.getFluidState(pos).isEmpty()
                && world.getFluidState(pos.down()).isEmpty()
                && (world.getLightLevel(LightType.BLOCK, pos) <= HordesConfig.requiredLightLevel || isNether);
    }

    // Ищем валидные места для спавна в кластере
    public static BlockPos spawnCluster(ServerWorld world, BlockPos basePos, ServerPlayerEntity player) {
        Random rnd = world.getRandom();
        boolean isNether = world.getRegistryKey() == World.NETHER;

        double r = Math.sqrt(rnd.nextBetween((int) -minCluster, (int) maxCluster));
        double angle = rnd.nextDouble() * 2.0 * Math.PI;
        int x = (int) (r * Math.cos(angle));
        int z = (int) (r * Math.sin(angle));
        int tx = basePos.getX() + x;
        int tz = basePos.getZ() + z;
        BlockPos finalPos;

        if (isNether) {
            finalPos = SpawnPos.findSurfaceInNether(world, player, tx, basePos.getY(), tz);
        } else {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, tx, tz);
            if (world.getLightLevel(LightType.BLOCK, basePos) > requiredLightLevel) return null;
            finalPos = new BlockPos(tx, y, tz);
        }
        return finalPos;
    }

    private static BlockPos findSurfaceInNether(ServerWorld world, ServerPlayerEntity player, int x, int y, int z) {
        BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);
        while (mutable.getY() > player.getY() - 10) {
            if (world.isAir(mutable) && world.isAir(mutable.up()) && world.getBlockState(mutable.down()).isSolidBlock(world, mutable.down())) {
                return mutable.toImmutable();
            }
            mutable.move(0, -1, 0);
        }
        return null;
    }


    private static boolean areaCheck(ServerWorld world, BlockPos pos) {
        int valid = 0;
        int total = 0;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = -7; x <= 7; x += 1) {
            for (int z = -7; z <= 7; z += 1) {
                total++;
                mutable.set(pos.getX() + x, pos.getY(), pos.getZ() + z);
                if (world.getBlockState(mutable.down()).isSolidBlock(world, mutable.down())) {
                    valid++;
                }
            }
        }
        return (double) valid / total > 0.5;
    }
}