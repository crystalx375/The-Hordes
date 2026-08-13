package crystal.hordes.event;

import crystal.hordes.TheHordes;
import crystal.hordes.config.HordesConfig;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.Iterator;
import java.util.Set;

import static crystal.hordes.config.HordesConfig.*;


public class Despawner {
    private static final Set<MobEntity> SET_MOB_ENTITIES = HordesConfig.getSetMobEntities();
    private static boolean forceDespawn = false;
    /**
     * Я заебался делать комменты для никого
     * Здесь просто деспавн, который вызывается в TickHandler + HordesManager
     */
    public static void checkIfEmpty() {
        if (SET_MOB_ENTITIES.isEmpty())
        {
            forceDespawn = false;
            HordesConfig.setIsDespawning(false);
        } else {
            HordesConfig.setIsDespawning(true);
        }

    }

    public static void startDespawnTimer() {
        HordesConfig.setIsDespawning(true);
    }

    public static void despawnTimer(final int ticks) {
        if (HordesConfig.getIsDespawning() && ticks >= DELAY_TICKS || forceDespawn) {
            TheHordes.LOGGER.info("Despawning: {}", SET_MOB_ENTITIES.size());
            checkIfEmpty();
            despawn();
        }
    }

    private static void despawn() {
        final Iterator<MobEntity> i = SET_MOB_ENTITIES.iterator();
        int count = 0;

        while (i.hasNext() && count < (int) (PER_DESPAWN + SET_MOB_ENTITIES.size() * FACTOR_SIZE)) {
           final MobEntity mobEntity = i.next();

            if (mobEntity == null
                    || !mobEntity.isAlive()
                    || mobEntity.isRemoved())
            {
                i.remove();
                continue;
            }

            if (mobEntity.getWorld() instanceof ServerWorld world)
            {
                mobEntity.getWorld().playSound(
                        null,
                        BlockPos.ofFloored(mobEntity.getPos()),
                        SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE,
                        SoundCategory.AMBIENT,
                        0.3f, 1f
                );
                world.spawnParticles(
                        ParticleTypes.SMOKE,
                        mobEntity.getX(), mobEntity.getY() + 1, mobEntity.getZ(),
                        10,
                        0.2, 0.5, 0.2,
                        0.05
                );

                mobEntity.discard();
                i.remove();

                count++;
            }
        }
    }

    public static void forceDespawn(ServerWorld world) {
        forceDespawn = true;
        HordesManager.endHorde(world);
        TheHordes.LOGGER.info("Force Despawning...");
    }
}
