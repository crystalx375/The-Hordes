package crystal.hordes.mixin;

import crystal.hordes.IHordes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

import static crystal.hordes.config.HordesConfig.adjustAccuracyChance;
import static crystal.hordes.config.HordesConfig.enableSkeletonMixin;

@Mixin(AbstractSkeletonEntity.class)
public abstract class SkeletonMixin {
    @Unique private static boolean high = false;

    @Shadow @Final @Mutable private BowAttackGoal<AbstractSkeletonEntity> bowAttackGoal;

    @Unique
    private static double random(int range) {
        Random rnd = new Random();
        float f = rnd.nextFloat();
        if (f > adjustAccuracyChance) {
            double d = Math.sqrt(range);
            double r = d / 2;
            return r * r;
        }
        return range;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initCustomGoal(EntityType entityType, World world, CallbackInfo ci) {
        if (enableSkeletonMixin) {
            this.bowAttackGoal = new BowAttackGoal<>((AbstractSkeletonEntity) (Object) this, 1.0D, 20, 40F);
        }
    }

    @Inject(method = "shootAt", at = @At("HEAD"), cancellable = true)
    private void fix(LivingEntity target, float pullProgress, CallbackInfo ci) {
        if (!(enableSkeletonMixin)) return;
        AbstractSkeletonEntity skeleton = (AbstractSkeletonEntity) (Object) this;
        if (target != null) {
            double distanceSq = skeleton.squaredDistanceTo(target);
            if (distanceSq > random(1600)) {
                ci.cancel();
            }
        }
    }

    @Redirect(
            method = "shootAt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/PersistentProjectileEntity;setVelocity(DDDFF)V"
            )
    )
    private void adjustAccuracy(PersistentProjectileEntity instance, double x, double y, double z, float power, float uncertainty) {
        AbstractSkeletonEntity skeleton = (AbstractSkeletonEntity) (Object) this;
        if (enableSkeletonMixin && skeleton instanceof IHordes horde && horde.the_Hordes$isHordeZombie()) {
            Entity target = skeleton.getTarget();
            if (target != null) {
                double dX = target.getX() - skeleton.getX();
                double dZ = target.getZ() - skeleton.getZ();
                double distance = Math.sqrt(dX * dX + dZ * dZ);

                y -= distance * 0.05;
                instance.setVelocity(x, y, z, (float) (1.8 + distance / 50), 3f);
                return;
            }
        }
        instance.setVelocity(x, y, z, power, uncertainty);
    }
}
