package crystal.hordes.mixin;

import crystal.hordes.IHordes;
import crystal.hordes.config.HordesConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AbstractSkeletonEntity.class)
public abstract class SkeletonMixin {
    @Unique private static boolean high = false;

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void shootCheck(LivingEntity target, float pullProgress, CallbackInfo ci) {
        final AbstractSkeletonEntity skeleton = (AbstractSkeletonEntity) (Object) this;
        final double distance = skeleton.squaredDistanceTo(target);
        if (skeleton instanceof IHordes horde && horde.the_Hordes$isHordeZombie()) {
            final float c = skeleton.getRandom().nextFloat();
            int limit = 20 * 20;
            if (c < HordesConfig.ADJUST_ACCURACY_CHANCE) {
                limit = 45 * 45;
            }
            if (distance > limit) {
                ci.cancel();
            }
        }
    }

    @Redirect(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/PersistentProjectileEntity;setVelocity(DDDFF)V"
            )
    )
    private void adjustAccuracy(PersistentProjectileEntity arrow, double x, double y, double z, float speed, float divergence) {
        final AbstractSkeletonEntity skeleton = (AbstractSkeletonEntity) (Object) this;
        if (HordesConfig.ENABLE_SKELETON_MIXIN && skeleton instanceof IHordes horde && horde.the_Hordes$isHordeZombie()) {
            final Entity target = skeleton.getTarget();
            if (target != null) {
                final double dX = target.getX() - skeleton.getX();
                final double dZ = target.getZ() - skeleton.getZ();
                final double distance = Math.sqrt(dX * dX + dZ * dZ);

                y -= distance * 0.05;
                arrow.setVelocity(x, y, z, (float) (2 + distance / 50), 2f);
                return;
            }
        }
        arrow.setVelocity(x, y, z, speed, divergence);
    }
}
