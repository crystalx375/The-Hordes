package crystal.hordes.event;

import crystal.hordes.HordesAccessor;
import crystal.hordes.config.HordesConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.UUID;

public class HordesVariations {
    /**
     * Подготовка мобов
     * используется в SpawnWave
     */
    public static MobEntity spawnHordes(ServerWorld world, ServerPlayerEntity player, EntityType<?> type, BlockPos finalPos) {
        Entity entity = type.create(world);
        UUID playerUuid = player.getUuid();
        UUID clusterId = UUID.randomUUID();
        Random rnd = world.random;
        if (entity instanceof MobEntity mob) {
            prepareMob(mob, clusterId, playerUuid, finalPos, world, rnd);
            if ((type == EntityType.SKELETON && rnd.nextFloat() < 0.2f) || (type == EntityType.ZOMBIE && rnd.nextFloat() < 0.01f)) {
                ZombieHorseEntity horse = EntityType.ZOMBIE_HORSE.create(world);
                if (horse != null) {
                    horse.setTame(true);
                    prepareMob(horse, clusterId, playerUuid, finalPos, world, rnd);
                    world.spawnEntity(horse);
                    mob.startRiding(horse);
                }
            }
            world.spawnEntity(mob);
            HordesConfig.getHordeZombies().add(mob);

            return mob;
        }
        return null;
    }

    private static void giveHordeEquipment(MobEntity mob, Random rnd) {
        // Даем вещи мобам (криво)
        // Я порофлил когда с лошади броня выпала
        if (mob instanceof GhastEntity) return;
        if (rnd.nextFloat() < 0.2f) mob.equipStack(EquipmentSlot.HEAD, new ItemStack(rnd.nextFloat() > 0.8 ? Items.IRON_HELMET : Items.LEATHER_HELMET));
        if (rnd.nextFloat() < 0.3f)  mob.equipStack(EquipmentSlot.CHEST, new ItemStack(rnd.nextFloat() > 0.9 ? Items.IRON_CHESTPLATE : Items.LEATHER_CHESTPLATE));
        if (rnd.nextFloat() < 0.2f) mob.equipStack(EquipmentSlot.LEGS, new ItemStack(rnd.nextFloat() > 0.7 ? Items.IRON_LEGGINGS : Items.LEATHER_LEGGINGS));
        if (rnd.nextFloat() < 0.3f)  mob.equipStack(EquipmentSlot.FEET, new ItemStack(rnd.nextFloat() > 0.3 ? Items.IRON_BOOTS : Items.LEATHER_BOOTS));
        // Чарим лук для скелета
        if (mob instanceof net.minecraft.entity.mob.AbstractSkeletonEntity) {
            if (rnd.nextFloat() < 0.2f) {
                ItemStack bow = new ItemStack(Items.BOW);
                bow.addEnchantment(net.minecraft.enchantment.Enchantments.POWER, rnd.nextBetween(1, 5));
                mob.equipStack(EquipmentSlot.MAINHAND, bow);
            }
        }
        // В ручку даем мечик
        if (mob.getType() == EntityType.ZOMBIE && rnd.nextFloat() < 0.15f) {
            mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(rnd.nextBoolean() ? Items.STONE_SWORD : Items.IRON_SWORD));
        }
    }


    private static void prepareMob(MobEntity mob, UUID clusterId, UUID playerUuid, BlockPos pos, ServerWorld world, Random rnd) {
        double yOffset = (mob instanceof GhastEntity) ? 2.0 : 0.1;
        mob.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + yOffset, pos.getZ() + 0.5, rnd.nextFloat() * 360f, 0f);
        ((HordesAccessor) mob).the_Hordes$setHordeZombie(true, clusterId, playerUuid);
        mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null, null);

        if (mob instanceof PiglinEntity piglin) {
            piglin.setImmuneToZombification(true);
        }

        if (!(mob instanceof GhastEntity)) {
            mob.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
            mob.setPathfindingPenalty(PathNodeType.WATER_BORDER, -1.0F);
            giveHordeEquipment(mob, rnd);
        }
    }
}