package crystal.hordes.mixin;

import crystal.hordes.HordesAccessor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.mob.HostileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BowAttackGoal.class)
public abstract class BowAttackGoalMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void updateDynamicInterval(CallbackInfo ci) {
        BowAttackGoal<?> goal = (BowAttackGoal<?>)(Object)this;
        BowAccessor accessor = (BowAccessor) goal;
        HostileEntity actor = accessor.getActor();

        if (actor instanceof HordesAccessor horde && horde.the_Hordes$isHordeZombie()) {
            LivingEntity target = actor.getTarget();

            if (target != null) {
                double distance = actor.distanceTo(target);

                if (distance <= 12.0) {
                    int interval = (int) (distance);
                    accessor.setAttackInterval(interval);
                } else {
                    accessor.setAttackInterval(20);
                }
            }
        }
    }
}