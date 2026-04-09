package crystal.hordes.mixin;

import crystal.hordes.IHordes;
import crystal.hordes.event.Despawner;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

import static crystal.hordes.config.HordesConfig.*;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements IHordes {
    /**
     * Изменяем дроп для хорд 💝💘💖
     */
    @Shadow protected int playerHitTimer;
    @Shadow protected abstract boolean shouldDropLoot();
    @Shadow protected abstract void dropLoot(DamageSource source, boolean causedByPlayer);
    @Shadow protected abstract void dropInventory();
    @Unique private boolean wasHit = false;
    @Unique private boolean isHordeZombie;
    @Unique private UUID targetPlayerUuid;

    public LivingEntityMixin(net.minecraft.entity.EntityType<?> type, net.minecraft.world.World world) {
        super(type, world);
    }
    @Override
    public void the_Hordes$setHordeZombie(boolean value, UUID clusterId, UUID targetPlayerUuid) {
        this.isHordeZombie = value;
        this.targetPlayerUuid = targetPlayerUuid;
    }

    @Override
    public boolean the_Hordes$isHordeZombie() { return isHordeZombie; }

    @Override
    public UUID the_Hordes$getTargetPlayerUuid() { return targetPlayerUuid; }

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    protected void dropLootHordes(ServerWorld world, DamageSource source, CallbackInfo ci) {
        boolean despawned = Despawner.delayTimer == -2 && getHordeZombies().isEmpty();
        if (despawned) return;

        if (source.getSource() instanceof IHordes || source.getAttacker() instanceof IHordes) {
            Entity attacker = source.getAttacker();
            boolean causedByPlayer = this.playerHitTimer > 0;

            if (!(attacker instanceof PlayerEntity)) {
                ci.cancel();
                return;
            }

            if (this.shouldDropLoot() && this.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
                this.dropLoot(source, causedByPlayer);
            }

            ExperienceOrbEntity.spawn(world, this.getPos(), EnchantmentHelper.getMobExperience(world, attacker, this, 10));
            this.dropInventory();

            ci.cancel();
        }
    }

    @Inject(method = "damage", at = @At("TAIL"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getAttacker() instanceof PlayerEntity) {
            this.wasHit = true;
        }

        if (this.isHordeZombie && (Object) this instanceof Angerable angerable) {
            angerable.setAngryAt(null);
            angerable.setAngerTime(0);
        }
    }
    @Inject(method = "drop", at = @At("HEAD"))
    private void adjustLootLogic(ServerWorld world, DamageSource damageSource, CallbackInfo ci) {
        if (this.wasHit) {
            this.playerHitTimer = 600;
        }
    }
}