package crystal.hordes.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import crystal.hordes.util.SimpleConfig;
import net.minecraft.entity.mob.MobEntity;

public class HordesConfig {
    private static HordesConfig INSTANCE;
    /**
     * Конфиг файл, использовал SimpleConfig и парс по ключам
     */
    public int daysBetweenHordes;
    public int hordeDuration;
    public int waveInterval;
    public int zombiesPerWave;
    public static int delayTicks;
    public static int DESPAWN_INTERVAL_TICKS;
    public static double FACTOR_SIZE;
    public static int PER_DESPAWN;
    public static int minRadius;
    public static int maxRadius;
    public int hordesLimitPerPlayer;
    public static float maxCluster;
    public static float minCluster;
    public static boolean required_night;

    public final Map<String, Integer> overworld;
    public final Map<String, Integer> nether;
    public final Map<String, Integer> end;

    public boolean spawnInOverworld;
    public boolean spawnInNether;
    public boolean spawnInEnd;
    public static boolean onlyTargetPlayers;

    public static byte i = 0;
    public static boolean active = false;
    public static int ticks = 0;
    public static int waveTimer = 0;
    // Свет при котором спавн может быть <= requiredLightLevel
    public static final int requiredLightLevel = 5;
    private static final Set<MobEntity> hordeZombies = new HashSet<>();
    public static Set<MobEntity> getHordeZombies() { return hordeZombies; }

    public static boolean DEBUG;
    public static final int UPDATE_TIME = 50;

    public boolean escapeByDimensionChange;

    // Использую SimpleConfig
    // https://github.com/magistermaks/fabric-simplelibs/blob/master/simple-config/SimpleConfig.java
    private HordesConfig() {
        SimpleConfig CONFIG = SimpleConfig.of("hordes_common").provider(this::defaultConfig).request();
        this.daysBetweenHordes = CONFIG.getOrDefault("days_between_hordes", 7);

        this.hordeDuration = CONFIG.getOrDefault("hordes_duration", 6000);
        this.waveInterval = CONFIG.getOrDefault("wave_interval", 1000);
        this.zombiesPerWave = CONFIG.getOrDefault("mobs_per_wave", 50);
        this.hordesLimitPerPlayer = CONFIG.getOrDefault("hordes_limit_per_player", 160);

        onlyTargetPlayers = CONFIG.getOrDefault("only_target_players", true);
        minRadius = CONFIG.getOrDefault("min_spawn_radius", 30);
        maxRadius = CONFIG.getOrDefault("max_spawn_radius", 45);

        this.spawnInOverworld = CONFIG.getOrDefault("spawn_in_overworld", true);
        this.spawnInNether = CONFIG.getOrDefault("spawn_in_nether", true);
        this.spawnInEnd = CONFIG.getOrDefault("spawn_in_end", true);

        required_night = CONFIG.getOrDefault("required_night", true);

        delayTicks = CONFIG.getOrDefault("delay_before_despawn", 12000);

        // (section) I don't recommend changing anything below unless you understand why
        this.overworld = parseMobMap(CONFIG.getOrDefault("mobs.overworld", "minecraft:zombie:5, minecraft:skeleton:1"));
        this.nether = parseMobMap(CONFIG.getOrDefault("mobs.nether", "minecraft:zombified_piglin:30, minecraft:hoglin:5, minecraft:ghast:1"));
        this.end = parseMobMap(CONFIG.getOrDefault("mobs.end", "minecraft:phantom:10"));
        DESPAWN_INTERVAL_TICKS = CONFIG.getOrDefault("despawn_interval", 50);
        PER_DESPAWN = CONFIG.getOrDefault("mobs_per_despawn", 1);
        FACTOR_SIZE = CONFIG.getOrDefault("factor_size", 0.05D);
        DEBUG = CONFIG.getOrDefault("debug", false);

        // Используется для адаптивного размера кластера
        // Сделано для того, чтобы колизия между мобами при спавне была минимальная
        // Размеры кластера
        maxCluster = (float) (5 + 0.1 * this.zombiesPerWave);
        minCluster = (float) (2 + 0.05 * this.zombiesPerWave);
    }
    private String defaultConfig(String filename) {
        return """
                # Hordes Mod Configuration 0.1 (1.0)
                # <------ The config is not updating, so delete it when you updating the mod version ------>
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
                
                # Spawn radius around the player (default: min = 30, max = 45)
                min_spawn_radius = 30
                max_spawn_radius = 45
                
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
            System.err.println("[Hordes] Failed to parse mob map: " + raw);
        }
        return map.isEmpty() ? Map.of("minecraft:zombie", 100) : map;
    }
    // Инициализация
    public static HordesConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new HordesConfig();
        }
        return INSTANCE;
    }
}