package crystal.hordes.mixin;

import crystal.hordes.IHordes;
import crystal.hordes.config.HordesConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.function.Predicate;

import static crystal.hordes.config.HordesConfig.getHordeZombies;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity implements IHordes {
    @Shadow @Final protected GoalSelector goalSelector;
    @Shadow @Final protected GoalSelector targetSelector;

    @Unique private static final String HORDE_ID = "HordeId";
    @Unique private static final String TARGET_PLAYER_UUID = "TargetPlayerUuid";

    @Unique private boolean isHordeMob = false;
    @Unique private UUID clusterId = null;
    @Unique private UUID targetPlayerUuid = null;

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void the_Hordes$setHordeZombie(boolean horde, UUID clusterId, UUID playerUuid) {
        this.isHordeMob = horde;
        this.clusterId = clusterId;
        this.targetPlayerUuid = playerUuid;
        if (horde) {
            this.applyHordeLogic();
        }
    }

    @Override public boolean the_Hordes$isHordeZombie() { return this.isHordeMob; }
    @Override public UUID the_Hordes$getTargetPlayerUuid() { return this.targetPlayerUuid; }

    @Unique
    private boolean canTargetPlayer(PlayerEntity player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }
    // Меняю, так как через revenge goal не получилось
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void onSetTarget(LivingEntity target, CallbackInfo ci) {
        if (this.isHordeMob) {
            if (target instanceof IHordes accessor && accessor.the_Hordes$isHordeZombie()) {
                final UUID playerUuid = accessor.the_Hordes$getTargetPlayerUuid();
                if (playerUuid != null && playerUuid.equals(this.targetPlayerUuid)) {
                    ci.cancel();
                    return;
                }
            }

            if (HordesConfig.ONLY_TARGET_PLAYERS && !(target instanceof PlayerEntity)) {
                ci.cancel();
            }
        }
    }

    @Unique
    private void applyHordeLogic() {
        final MobEntity mob = (MobEntity) (Object) this;
        final var rangeAttr = this.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);

        if (rangeAttr != null) {
            rangeAttr.setBaseValue(64.0);
        }

        this.goalSelector.getGoals().removeIf(goal ->
                goal.getGoal() instanceof StepAndDestroyBlockGoal
                        || goal.getGoal() instanceof MoveThroughVillageGoal
                        || goal.getGoal() instanceof AvoidSunlightGoal
        );
        this.targetSelector.getGoals().removeIf(goal ->
                goal.getGoal() instanceof RevengeGoal ||
                        goal.getGoal() instanceof ActiveTargetGoal
        );

        this.targetSelector.add(1, new ActiveTargetGoal<>(mob, PlayerEntity.class, 10, false, true, null));
        setTargetSelector(mob);
    }

    @Unique private void setTargetSelector(final MobEntity mob) {
        if (!HordesConfig.ONLY_TARGET_PLAYERS) {
            this.targetSelector.add(2, new ActiveTargetGoal<>(mob, MobEntity.class, 10, false, true,
                    entity -> {
                        if (entity == mob || !entity.isAlive()) return false;
                        return !(entity instanceof IHordes i) || !i.the_Hordes$isHordeZombie();
                    })
            );
        }

        if (mob instanceof Angerable angerable)
        {
            angerable.setAngryAt(null);
            angerable.setAngerTime(0);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void writeHordeData(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("IsHordeMob", this.isHordeMob);
        if (this.clusterId != null) nbt.putUuid(HORDE_ID, this.clusterId);
        if (this.targetPlayerUuid != null) nbt.putUuid(TARGET_PLAYER_UUID, this.targetPlayerUuid);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void readHordeData(NbtCompound nbt, CallbackInfo ci) {
        this.isHordeMob = nbt.getBoolean("IsHordeMob");
        if (nbt.contains(HORDE_ID)) this.clusterId = nbt.getUuid(HORDE_ID);
        if (nbt.contains(TARGET_PLAYER_UUID)) this.targetPlayerUuid = nbt.getUuid(TARGET_PLAYER_UUID);
        if (this.isHordeMob) {
            getHordeZombies().add((MobEntity)(Object)this);
            this.applyHordeLogic();
        }
    }
}