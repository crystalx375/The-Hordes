package crystal.hordes.event;

import crystal.hordes.config.HordesConfig;
import crystal.hordes.TheHordes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameRules;

import java.util.*;

import static crystal.hordes.event.Despawner.*;
import static crystal.hordes.config.HordesConfig.*;
import static crystal.hordes.util.Nbt.saveState;

public class HordesManager {
    /**
     * Связующее всех классов для спавна орды
     */
    private static int SleepPercentage = 100;

    public static void startHorde(ServerWorld world) {
        if (world == null || active) return;
        active = true;
        ticks = 0;
        waveTimer = 0;
        i = 0;
        delayTimer = -9;
        GameRules rules = world.getGameRules();
        SleepPercentage = rules.getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
        rules.get(GameRules.PLAYERS_SLEEPING_PERCENTAGE).set(101, world.getServer());
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.literal("§4Hordes is coming...").formatted(Formatting.DARK_RED), true);
            player.playSound(SoundEvents.AMBIENT_NETHER_WASTES_MOOD.value(), SoundCategory.AMBIENT, 3.0f, 0.8f);
        }
        TheHordes.LOGGER.info("Hordes started in {}", world.getRegistryKey().getValue());
        saveState();
    }


    public static void endHorde(ServerWorld world) {
        if (world == null || !active) return;
        getHordeZombies().removeIf(mob -> mob == null || !mob.isAlive());
        active = false;
        ticks = 0;
        waveTimer = 0;
        i = 0;
        world.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE).set(SleepPercentage, world.getServer());
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.literal("§4Hordes ended...").formatted(Formatting.DARK_RED), true);
        }
        Despawner.startDespawnTimer();
        TheHordes.LOGGER.info("Hordes ended in {}", world.getRegistryKey().getValue());
        TheHordes.LOGGER.info("Waiting for delay: {} ticks", HordesConfig.delayTicks);
        saveState();
    }
}