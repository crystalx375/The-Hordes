package crystal.hordes.util;

import crystal.hordes.TheHordes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import java.io.File;

import static crystal.hordes.config.HordesConfig.*;
import static crystal.hordes.event.Despawner.delayTimer;
import static crystal.hordes.event.Despawner.internalDespawnTimer;

public class Nbt {
    public static final File DATA_FILE = FabricLoader.getInstance().getConfigDir().resolve("hordes_data.dat").toFile();
    /**
     * По названию понятно, просто записываем и читаем файл с инфой (.nbt)
     */
    public static void loadState() {
        if (!DATA_FILE.exists()) return;
        try {
            final NbtCompound nbt = NbtIo.read(DATA_FILE.toPath());
            if (nbt != null) {
                int ticks = nbt.getInt("Ticks");
                final boolean ACTIVE = nbt.getBoolean("Active");
                int waveTimer = nbt.getInt("WaveTimer");
                int i = nbt.getInt("WaveIndex");
                delayTimer = nbt.getInt("DelayTimer");
                internalDespawnTimer = nbt.getInt("internalDespawnTimer");
                if (DEBUG) TheHordes.LOGGER.info("[NBT] Loaded state: ticks = {}, active = {}, waveTimer: {}, i: {}, delayTimer: {} and internalDespawnTimer: {}", ticks, ACTIVE, waveTimer, i, delayTimer, internalDespawnTimer);
            }
        } catch (Exception e) {
            TheHordes.LOGGER.error("Failed to load state");
        }
    }
}
