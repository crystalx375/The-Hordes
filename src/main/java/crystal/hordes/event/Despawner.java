package crystal.hordes.event;

import crystal.hordes.TheHordes;
import crystal.hordes.config.HordesConfig;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

import java.util.Iterator;
import java.util.Set;

import static crystal.hordes.config.HordesConfig.*;
import static crystal.hordes.config.HordesConfig.DEBUG;
import static crystal.hordes.config.HordesConfig.DESPAWN_INTERVAL_TICKS;
import static crystal.hordes.config.HordesConfig.UPDATE_TIME;


public class Despawner {
    public static int delayTimer;
    public static int internalDespawnTimer;
    private static final Set<MobEntity> zombies = HordesConfig.getHordeZombies();

    /**
     * Я заебался делать комменты для никого
     * Здесь просто деспавн, который вызывается в TickHandler + HordesManager
     */
    public static void startDespawnTimer() {
        delayTimer = -1;
        internalDespawnTimer = -1;
    }

    public static void despawnTimer() {
        if (delayTimer <= -2) return;
        if (delayTimer == -1) delayTimer = delayTicks;

        if (delayTimer > 0) {
            delayTimer -= UPDATE_TIME;
            if (delayTimer % 1000 == 0) {
                TheHordes.LOGGER.info("delayTimer: " + delayTimer);
            }
            return;
        }

        if (internalDespawnTimer == -1) { internalDespawnTimer = DESPAWN_INTERVAL_TICKS; }
        internalDespawnTimer -= UPDATE_TIME;

        if (internalDespawnTimer > 0) return;
        internalDespawnTimer = DESPAWN_INTERVAL_TICKS;
        despawn();
        TheHordes.LOGGER.info("Despawning: " + zombies.size());
    }

    public static void checkForDespawn() {
        if (zombies.isEmpty()) delayTimer = -2;
    }

    private static void despawn() {
        Iterator<MobEntity> iter = zombies.iterator();
        int count = 0;
        int limit = (int) (HordesConfig.PER_DESPAWN + zombies.size() * HordesConfig.FACTOR_SIZE);

        while (iter.hasNext() && count < limit) {
            MobEntity z = iter.next();
            if (z == null || !z.isAlive() || z.isRemoved()) {
                iter.remove();
                continue;
            }

            boolean playerNearby = z.getWorld().getPlayers().stream()
                    .anyMatch(player -> player.squaredDistanceTo(z) < 80 * 80);

            if (!playerNearby) {
                z.discard();
                iter.remove();
                if (DEBUG) TheHordes.LOGGER.info("[Despawner] Player not nearby, deleting hordes, now: " + zombies.size());
                continue;
            }

            if (z.getWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.SMOKE, z.getX(), z.getY() + 1, z.getZ(), 10, 0.2, 0.5, 0.2, 0.05);
                z.discard();
                iter.remove();
                count++;
            }
        }
    }

    public static void command() {
        delayTimer = 0;
        TheHordes.LOGGER.info("Force Despawning...");
    }
}
