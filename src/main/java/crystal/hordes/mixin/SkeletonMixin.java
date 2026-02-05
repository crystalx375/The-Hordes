package crystal.hordes.mixin;

import crystal.hordes.HordesAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractSkeletonEntity.class)
public abstract class SkeletonMixin {
    @Redirect(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/PersistentProjectileEntity;setVelocity(DDDFF)V"
            )
    )

    private void adjustAccuracy(PersistentProjectileEntity arrow, double x, double y, double z, float speed, float divergence) {
        AbstractSkeletonEntity skeleton = (AbstractSkeletonEntity) (Object) this;
        if (skeleton instanceof HordesAccessor horde && horde.the_Hordes$isHordeZombie()) {
            Entity target = arrow.getOwner() instanceof MobEntity mob ? mob.getTarget() : null;
            if (target != null) {
                double dX = target.getX() - skeleton.getX();
                double dZ = target.getZ() - skeleton.getZ();
                double distance = Math.sqrt(dX * dX + dZ * dZ);
                y -= distance * 0.12F;
                if (distance > 45) y += distance * 0.03F;
                else if (distance > 54) y += distance * 0.04F;
                arrow.setVelocity(x, y, z, 3.5F, 0F);
            }
        } else {
            arrow.setVelocity(x, y, z, speed, divergence);
        }
    }
}
