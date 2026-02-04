package crystal.hordes.mixin;

import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.mob.HostileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BowAttackGoal.class)
public interface BowAccessor {
    @Accessor("attackInterval")
    void setAttackInterval(int interval);
    @Accessor("actor")
    HostileEntity getActor();
}
