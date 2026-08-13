package crystal.hordes.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import crystal.hordes.TheHordes;
import crystal.hordes.util.SimpleConfig;
import net.minecraft.entity.mob.MobEntity;

public class HordesConfig {
    private static final int VERSION = 1;
    private static HordesConfig instance;

    private static int i = 0;
    private static boolean active = false;
    private static int ticks = 0;
    private static int waveTimer = 0;
    private static boolean isDespawning = false;
    private static final Set<MobEntity> MOB_ENTITIES = new HashSet<>();

    public static final int DAYS_BETWEEN_HORDES;
    public static final int HORDE_DURATION;
    public static final int WAVE_INTERVAL;
    public static final int ZOMBIES_PER_WAVE;
    public static final int DELAY_TICKS;
    public static final int DESPAWN_INTERVAL_TICKS;
    public static final double FACTOR_SIZE;
    public static final int PER_DESPAWN;
    public static final int MIN_RADIUS;
    public static final int MAX_RADIUS;
    public static final int HORDES_LIMIT_PER_PLAYER;
    public static final float MAX_CLUSTER;
    public static final float MIN_CLUSTER;
    public static final boolean REQUIRED_NIGHT;

    public static final Map<String, Integer> OVERWORLD;
    public static final Map<String, Integer> NETHER;
    public static final Map<String, Integer> END;

    public static final boolean SPAWN_IN_OVERWORLD;
    public static final boolean SPAWN_IN_NETHER;
    public static final boolean SPAWN_IN_END;
    public static final boolean ONLY_TARGET_PLAYERS;

    public static final boolean DEBUG;
    public static final boolean ENABLE_SKELETON_MIXIN;
    public static final float ADJUST_ACCURACY_CHANCE;

    public static final int REQUIRED_LIGHT_LEVEL = 5;
    public static final int UPDATE_TIME = 50;

    static {
        HordesConfig helper = new HordesConfig();

        SimpleConfig config = SimpleConfig.of("hordes_common")
                .provider(helper::defaultConfig)
                .version(VERSION)
                .request();

        DAYS_BETWEEN_HORDES = config.getOrDefault("days_between_hordes", 7);
        HORDE_DURATION = config.getOrDefault("hordes_duration", 6000);
        WAVE_INTERVAL = config.getOrDefault("wave_interval", 1000);
        ZOMBIES_PER_WAVE = config.getOrDefault("mobs_per_wave", 50);
        HORDES_LIMIT_PER_PLAYER = config.getOrDefault("hordes_limit_per_player", 160);

        ONLY_TARGET_PLAYERS = config.getOrDefault("only_target_players", true);
        MIN_RADIUS = config.getOrDefault("min_spawn_radius", 40);
        MAX_RADIUS = config.getOrDefault("max_spawn_radius", 50);

        SPAWN_IN_OVERWORLD = config.getOrDefault("spawn_in_overworld", true);
        SPAWN_IN_NETHER = config.getOrDefault("spawn_in_nether", true);
        SPAWN_IN_END = config.getOrDefault("spawn_in_end", true);

        REQUIRED_NIGHT = config.getOrDefault("required_night", true);
        DELAY_TICKS = config.getOrDefault("delay_before_despawn", 12000);

        ENABLE_SKELETON_MIXIN = config.getOrDefault("enable_skeleton_adjust", true);
        ADJUST_ACCURACY_CHANCE = ((Double) config.getOrDefault("adjust_accuracy_chance", 0.05)).floatValue();

        OVERWORLD = helper.parseMobMap(config.getOrDefault("mobs.overworld", "minecraft:zombie:5, minecraft:skeleton:1"));
        NETHER = helper.parseMobMap(config.getOrDefault("mobs.nether", "minecraft:zombified_piglin:30, minecraft:hoglin:5, minecraft:ghast:1"));
        END = helper.parseMobMap(config.getOrDefault("mobs.end", "minecraft:phantom:10"));

        DESPAWN_INTERVAL_TICKS = config.getOrDefault("despawn_interval", 50);
        PER_DESPAWN = config.getOrDefault("mobs_per_despawn", 1);
        FACTOR_SIZE = config.getOrDefault("factor_size", 0.05D);
        DEBUG = config.getOrDefault("debug", false);

        MAX_CLUSTER = (float) (5 + 0.1 * ZOMBIES_PER_WAVE);
        MIN_CLUSTER = (float) (2 + 0.05 * ZOMBIES_PER_WAVE);
    }
    private HordesConfig() {}

    private String defaultConfig(String filename) {
        return """
                # The-Hordes
                
                # (20 ticks = 1 second)
                # (1 day = 24000 ticks)
                # Time when hordes event starts
                days_between_hordes = 7
                
                # Hordes duration in ticks (default: 6000)
                hordes_duration = 6000
                # Wave interval in ticks (default: 1000)
                # hordes_duration / wave_interval = number of waves
                # 6000 / 1000 = 6 waves
                wave_interval = 1000
                # Mobs per 1 wave (for each player) (default: 50)
                mobs_per_wave = 50
                
                # Limit for 1 player (default: 160)
                hordes_limit_per_player = 160
                
                # Will the hordes only attack the player? (bool) (default: true)
                # If false, hordes from different players, having no targets, will start attacking themselves
                only_target_players = true
                
                # Skeletons in horde
                # Will be higher range and adjust? (bool) (default: true)
                enable_skeleton_adjust = true
                # Chance to arrow adjust accuracy (float) (default: 0.05)
                adjust_accuracy_chance = 0.05
                
                # Spawn radius around the player (default: min = 40, max = 50)
                min_spawn_radius = 40
                max_spawn_radius = 50
                
                # Spawn hordes in dimension? (bool) (default: true)
                # If false, waves doesnt spawn in that dimension
                spawn_in_overworld = true
                spawn_in_nether = true
                spawn_in_end = true

                # Need night for start hordes? (bool) (default: true)
                required_night = true
                
                # Delay before despawn in ticks (default: 12000)
                delay_before_despawn = 12000
                
                
                # I dont recommend changing anything below unless you understand why
                
                # Mob selection
                # Written as - id:weight, id:weight, id:weight ...etc (minecraft:zombie:5, minecraft:skeleton:1)
                # minecraft:zombie:5, minecraft:skeleton:1
                #                  ^                     ^
                #                weight                weight
                # The spawn chance is calculated as the mob weight / total weight
                # So chance of spawn zombie = 5 / 6 = 0.83... and chance of spawn skeleton = 1 / 6 = 0.17...
                # <--- I didn't change the AI for those (brain), which not selected here, so there might be problems with ai goals --->
                # Mobs selection in overworld (default: minecraft:zombie:5, minecraft:skeleton:1)
                mobs.overworld = minecraft:zombie:5, minecraft:skeleton:1
                # Mobs selection in nether (default: minecraft:zombified_piglin:30, minecraft:hoglin:5, minecraft:ghast:1)
                mobs.nether = minecraft:zombified_piglin:30, minecraft:hoglin:5, minecraft:ghast:1
                # Mobs selection in end (default: minecraft:phantom:10)
                mobs.end = minecraft:phantom:10
                
                # After delay -> despawn interval, after which despawn occurs (default: 50)
                despawn_interval = 50
                
                # Count despawn: mobs_per_despawn + (total_count_hordes * factor_size)
                # if after delay we have 100 mobs in horde -> after delay_interval -> count = 1 + (100 * 0.05) = 6 despawn mobs
                # For shutdown both 0
                # (default: 1)
                mobs_per_despawn = 1
                # (default: 0.05)
                factor_size = 0.05
                
                # Show debug (bool)
                # Why you use it?
                # Submit issue https://github.com/crystalx375/The-Hordes
                debug = false
                """;
    }

    private Map<String, Integer> parseMobMap(String raw) {
        Map<String, Integer> map = new HashMap<>();
        try {
            String cleanRaw = raw.replace(" ", "");
            String[] pairs = cleanRaw.split(",");
            for (String pair : pairs) {
                String[] parts = pair.split(":");
                if (parts.length >= 3) {
                    String id = parts[0] + ":" + parts[1];
                    int weight = Integer.parseInt(parts[2]);
                    map.put(id, weight);
                } else if (parts.length == 2) {
                    map.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
        } catch (Exception e) {
            TheHordes.LOGGER.error("Failed to parse mob map: {}", raw);
        }
        return map.isEmpty() ? Map.of("minecraft:zombie", 100) : map;
    }

    public static Set<MobEntity> getSetMobEntities() { return MOB_ENTITIES; }
    public static int getI() { return i; }
    public static void setI(int value) { i = value; }
    public static boolean isActive() { return active; }
    public static void setActive(boolean value) { active = value; }
    public static int getTicks() { return ticks; }
    public static void setTicks(int value) { ticks = value; }
    public static int getWaveTimer() { return waveTimer; }
    public static void setWaveTimer(int value) { waveTimer = value; }
    public static boolean getIsDespawning() { return isDespawning; }
    public static void setIsDespawning(boolean value) { isDespawning = value; }

    public static HordesConfig get() {
        if (instance == null) {
            instance = new HordesConfig();
        }
        return instance;
    }
}