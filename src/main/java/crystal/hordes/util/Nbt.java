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
    private static final File DATA_FILE = FabricLoader.getInstance().getConfigDir().resolve("hordes_data.dat").toFile();
    /**
     * Создаем файл
     */
    public static void saveState() {
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
    /**
     * Вытаскиваем файл
     */
    public static void loadState() {
        if (!DATA_FILE.exists()) return;
        try {
            NbtCompound nbt = NbtIo.read(DATA_FILE);
            if (nbt != null) {
                ticks = nbt.getInt("Ticks");
                active = nbt.getBoolean("Active");
                waveTimer = nbt.getInt("WaveTimer");
                i = (byte) nbt.getInt("WaveIndex");
                delayTimer = nbt.getInt("DelayTimer");
                internalDespawnTimer = nbt.getInt("internalDespawnTimer");
                TheHordes.LOGGER.info("[NBT] Loaded state: ticks = " + ticks + ", active = " + active + ", waveTimer: " + waveTimer + ", i: " + i + ", delayTimer: " + delayTimer + " and internalDespawnTimer: " + internalDespawnTimer);
            }
        } catch (Exception e) {
            TheHordes.LOGGER.error("Failed to load state: " + e.getMessage());
        }
    }
}
