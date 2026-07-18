package crystal.hordes.mixin;

import crystal.hordes.TheHordes;
import crystal.hordes.config.HordesConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static crystal.hordes.event.Despawner.delayTimer;
import static crystal.hordes.event.Despawner.internalDespawnTimer;
import static crystal.hordes.util.Nbt.DATA_FILE;

@Mixin(ServerWorld.class)
public class ServerMixin {
    @Inject(method = "save", at = @At("HEAD"))
    private void saveHordes(ProgressListener progressListener, boolean flush, boolean savingDisabled, CallbackInfo ci) {
        try {
            final NbtCompound nbt = new NbtCompound();
            nbt.putInt("Ticks", HordesConfig.getTicks());
            nbt.putBoolean("Active", HordesConfig.isActive());
            nbt.putInt("WaveTimer", HordesConfig.getWaveTimer());
            nbt.putInt("WaveIndex", HordesConfig.getI());
            nbt.putInt("DelayTimer", delayTimer);
            nbt.putInt("internalDespawnTimer", internalDespawnTimer);

            NbtIo.write(nbt, DATA_FILE.toPath());
            if (HordesConfig.DEBUG) TheHordes.LOGGER.info("[NBT] Saved state: ticks = {}, active = {}, waveTimer = {}, i = {}, delayTimer = {}, and internalDespawnTimer = {}", HordesConfig.getTicks(), HordesConfig.isActive(), HordesConfig.getWaveTimer(), HordesConfig.getI(), delayTimer, internalDespawnTimer);
        } catch (Exception e) {
            TheHordes.LOGGER.error("Failed to save state");
        }
    }
}
