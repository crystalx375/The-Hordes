package crystal.hordes.mixin;

import crystal.hordes.TheHordes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static crystal.hordes.config.HordesConfig.DEBUG;
import static crystal.hordes.config.HordesConfig.active;
import static crystal.hordes.config.HordesConfig.i;
import static crystal.hordes.config.HordesConfig.ticks;
import static crystal.hordes.config.HordesConfig.waveTimer;
import static crystal.hordes.event.Despawner.delayTimer;
import static crystal.hordes.event.Despawner.internalDespawnTimer;
import static crystal.hordes.util.Nbt.DATA_FILE;

@Mixin(ServerWorld.class)
public class ServerMixin {
    @Inject(method = "save", at = @At("HEAD"))
    private void saveHordes(ProgressListener progressListener, boolean flush, boolean savingDisabled, CallbackInfo ci) {
        try {
            NbtCompound nbt = new NbtCompound();
            nbt.putInt("Ticks", ticks);
            nbt.putBoolean("Active", active);
            nbt.putInt("WaveTimer", waveTimer);
            nbt.putInt("WaveIndex", i);
            nbt.putInt("DelayTimer", delayTimer);
            nbt.putInt("internalDespawnTimer", internalDespawnTimer);

            NbtIo.write(nbt, DATA_FILE);
            if (DEBUG) TheHordes.LOGGER.info("[NBT] Saved state: ticks = " + ticks + ", active = " + active + ", waveTimer: " + waveTimer + ", i: " + i + ", delayTimer: " + delayTimer + " and internalDespawnTimer: " + internalDespawnTimer);
        } catch (Exception e) {
            TheHordes.LOGGER.error("Failed to save state: " + e.getMessage());
        }
    }
}
