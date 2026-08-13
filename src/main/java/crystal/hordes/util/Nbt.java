package crystal.hordes.util;

import crystal.hordes.TheHordes;
import crystal.hordes.config.HordesConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import java.io.File;

import static crystal.hordes.config.HordesConfig.*;

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
                final int TICKS = nbt.getInt("Ticks");
                final boolean ACTIVE = nbt.getBoolean("Active");
                final int WAVE_TIMER = nbt.getInt("WaveTimer");
                final int I = nbt.getInt("WaveIndex");

                HordesConfig.setTicks(TICKS);
                HordesConfig.setActive(ACTIVE);
                HordesConfig.setWaveTimer(WAVE_TIMER);
                HordesConfig.setI(I);

                if (DEBUG) TheHordes.LOGGER.info("[NBT] Loaded state: TICKS = {}, active = {}, waveTimer: {}, i: {}", TICKS, ACTIVE, WAVE_TIMER, I);
            }
        } catch (Exception e) {
            TheHordes.LOGGER.error("Failed to load state");
        }
    }
}
