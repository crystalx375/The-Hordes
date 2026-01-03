package crystal.hordes.mixin;

import crystal.hordes.HordesAccessor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HoglinBrain;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(HoglinBrain.class)
public abstract class HoglinMixin {

    @Inject(method = "getNearestVisibleTargetablePlayer", at = @At("RETURN"), cancellable = true)
    private static void hordeFocusPlayer(HoglinEntity hoglin, CallbackInfoReturnable<Optional<? extends LivingEntity>> cir) {
        if (((HordesAccessor) hoglin).the_Hordes$isHordeZombie()) {
            if (cir.getReturnValue().isEmpty()) {
                PlayerEntity p = hoglin.getWorld().getClosestPlayer(hoglin.getX(), hoglin.getY(), hoglin.getZ(), 64.0, false);
                if (p != null && !p.isCreative() && !p.isSpectator()) {
                    cir.setReturnValue(Optional.of(p));
                }
            }
        }
    }
}
