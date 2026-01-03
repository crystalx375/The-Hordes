package crystal.hordes.mixin;

import crystal.hordes.HordesAccessor;
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

import static crystal.hordes.config.HordesConfig.getHordeZombies;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity implements HordesAccessor {
    /**
     * Я не хочу делать это говно нахуй
     * Я ПОТРАТИЛ ДОХУЯ ВРЕМЕНИ НА ФИКС АААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААА
     */
    @Shadow @Final protected GoalSelector goalSelector;
    @Shadow @Final protected GoalSelector targetSelector;

    @Unique private boolean isHordeMob = false;
    @Unique private UUID hordeId = null;
    @Unique private UUID targetPlayerUuid = null;

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void the_Hordes$setHordeZombie(boolean value, UUID clusterId, UUID playerUuid) {
        this.isHordeMob = value;
        this.hordeId = clusterId;
        this.targetPlayerUuid = playerUuid;
        if (value) {
            this.applyHordeLogic();
        }
    }

    @Override
    public boolean the_Hordes$isHordeZombie() { return this.isHordeMob; }
    @Override
    public UUID the_Hordes$getTargetPlayerUuid() { return this.targetPlayerUuid; }

    @Unique
    private boolean canTargetPlayer(PlayerEntity player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }
    // Меняю, так как через revenge goal не получилось
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void onSetTarget(LivingEntity target, CallbackInfo ci) {
        if (this.isHordeMob && target instanceof HordesAccessor accessor) {
            if (accessor.the_Hordes$isHordeZombie()) {
                UUID playerUuid = accessor.the_Hordes$getTargetPlayerUuid();
                if (HordesConfig.onlyTargetPlayers) {
                    ci.cancel();
                } else {
                    if (playerUuid != null && playerUuid.equals(this.targetPlayerUuid)) {
                        ci.cancel();
                    }
                }
            }
        }
    }

    @Unique
    private void applyHordeLogic() {
        MobEntity host = (MobEntity)(Object)this;

        var rangeAttr = this.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
        if (rangeAttr != null) {
            rangeAttr.setBaseValue(64.0);
        }
        this.goalSelector.getGoals().removeIf(goal ->
                goal.getGoal() instanceof StepAndDestroyBlockGoal ||
                        goal.getGoal() instanceof MoveThroughVillageGoal ||
                        goal.getGoal() instanceof AvoidSunlightGoal
        );
        this.goalSelector.getGoals().removeIf(goal -> goal.getGoal() instanceof RevengeGoal);
        this.goalSelector.getGoals().removeIf(goal -> goal.getGoal() instanceof ActiveTargetGoal);

        this.targetSelector.add(1, new ActiveTargetGoal<>(host, PlayerEntity.class, 10, false, true, null));

        if (!HordesConfig.onlyTargetPlayers) {
            this.targetSelector.add(2, new ActiveTargetGoal<>(host, MobEntity.class, 10, false, true,
                    (entity) -> {
                        if (entity == host || !entity.isAlive()) return false;
                        if (entity instanceof PlayerEntity) return true;
                        return !(entity instanceof HordesAccessor accessor) || !accessor.the_Hordes$isHordeZombie();
                    }));

            this.targetSelector.add(3, new ActiveTargetGoal<>(host, MobEntity.class, 10, false, true,
                    (entity) -> {
                        if (entity == host || !entity.isAlive()) return false;
                        if (entity instanceof HordesAccessor accessor && accessor.the_Hordes$isHordeZombie()) {
                            UUID otherPlayerUuid = accessor.the_Hordes$getTargetPlayerUuid();
                            return otherPlayerUuid != null && !otherPlayerUuid.equals(this.targetPlayerUuid);
                        }
                        return false;
                    }));
        }

        if (host instanceof Angerable angerable) {
            angerable.setAngryAt(null);
            angerable.setAngerTime(0);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void writeHordeData(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("IsHordeMob", this.isHordeMob);
        if (this.hordeId != null) nbt.putUuid("HordeId", this.hordeId);
        if (this.targetPlayerUuid != null) nbt.putUuid("TargetPlayerUuid", this.targetPlayerUuid);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void readHordeData(NbtCompound nbt, CallbackInfo ci) {
        this.isHordeMob = nbt.getBoolean("IsHordeMob");
        if (nbt.contains("HordeId")) this.hordeId = nbt.getUuid("HordeId");
        if (nbt.contains("TargetPlayerUuid")) this.targetPlayerUuid = nbt.getUuid("TargetPlayerUuid");
        if (this.isHordeMob) {
            getHordeZombies().add((MobEntity)(Object)this);
            this.applyHordeLogic();
        }
    }
}