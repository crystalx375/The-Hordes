package crystal.hordes.event;

import crystal.hordes.TheHordes;
import crystal.hordes.config.HordesConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;


public class SpawnPos {
    private static final int MIN_RADIUS = HordesConfig.MIN_RADIUS;
    private static final int MAX_RADIUS = HordesConfig.MAX_RADIUS;
    /**
     * Поиск места по радиусу
     * Также поиск для незера разделен так как там нельзя topY сделать (крышу ада всегда будет возвращать)
     */
    public static BlockPos findSpawnAroundPlayer(ServerWorld world, ServerPlayerEntity player, EntityType<?> checkType, Random rnd) {
        final boolean isNether = world.getRegistryKey() == World.NETHER;
        final int minR = isNether ? MIN_RADIUS / 2 : MIN_RADIUS;
        final int maxR = isNether ? MAX_RADIUS / 2 : MAX_RADIUS;
        final EntityType<?> type = checkType != null ? checkType : EntityType.ZOMBIE;

        for (int tries = 0; tries < 75; tries++) {
            final double angle = rnd.nextDouble() * 2.0 * Math.PI;
            final double r = Math.sqrt(rnd.nextDouble() * (maxR * maxR - minR * minR) + (minR * minR));
            final int x = (int) (r * Math.cos(angle));
            final int z = (int) (r * Math.sin(angle));

           final BlockPos targetPos = player.getBlockPos().add(x, 0, z);
            BlockPos finalPos;

            if (isNether)
            {
                final int y = Math.min((int)player.getY() + 20, 130);
                finalPos = findSurfaceInNether(world, player, targetPos.getX(), y, targetPos.getZ());
            } else {
                final int surface = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, targetPos.getX(), targetPos.getZ());
                finalPos = new BlockPos(targetPos.getX(), surface, targetPos.getZ());
            }

            if (finalPos != null
                    && isValidSpawn(world, type, finalPos)
                    && areaCheck(world, finalPos))
            {
                TheHordes.LOGGER.info("Spawn: {} For player: {}", finalPos, player.getName().getString());
                return finalPos;
            }
        }
        TheHordes.LOGGER.warn("Spawn is not valid");
        return null;
    }
    /**
     * Проверки на место спавна
     */
    public static boolean isValidSpawn(ServerWorld world, EntityType<?> type, BlockPos pos) {
        final Entity temp = type.create(world);
        if (temp == null) return false;
        final boolean isNether = world.getRegistryKey() == World.NETHER;
        final Box box = temp.getType().getDimensions().getBoxAt(pos.getX() + 0.5D, pos.getY() + 1D, pos.getZ() + 0.5D);
        return world.isSpaceEmpty(temp, box)
                && !world.containsFluid(box)
                && world.getBlockState(pos.down()).isSolidBlock(world, pos.down())
                && world.getFluidState(pos).isEmpty()
                && world.getFluidState(pos.down()).isEmpty()
                && (world.getLightLevel(LightType.BLOCK, pos) <= HordesConfig.REQUIRED_LIGHT_LEVEL || isNether);
    }

    // Ищем валидные места для спавна в кластере
    public static BlockPos spawnCluster(ServerWorld world, BlockPos basePos, ServerPlayerEntity player) {
        final Random rnd = world.getRandom();
        final boolean isNether = world.getRegistryKey() == World.NETHER;

        final double r = Math.sqrt(rnd.nextBetween((int) -HordesConfig.MIN_CLUSTER, (int) HordesConfig.MAX_CLUSTER));
        final double angle = rnd.nextDouble() * 2.0 * Math.PI;
        final int tx = basePos.getX() + (int) (r * Math.cos(angle));
        final int tz = basePos.getZ() + (int) (r * Math.sin(angle));
        final BlockPos finalPos;

        if (isNether) {
            finalPos = SpawnPos.findSurfaceInNether(world, player, tx, basePos.getY(), tz);
        } else {
            final int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, tx, tz);
            if (world.getLightLevel(LightType.BLOCK, basePos) > HordesConfig.REQUIRED_LIGHT_LEVEL) return null;
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
        if (total == 0) return false;
        return (double) valid / total > 0.5;
    }
}