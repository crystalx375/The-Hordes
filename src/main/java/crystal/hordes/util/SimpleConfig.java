package crystal.hordes.util;/*
 * Copyright (c) 2021 magistermaks
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * Edited some code - crystalx0375
 */

import com.google.gson.JsonSyntaxException;
import crystal.hordes.TheHordes;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class SimpleConfig {

    private static final Logger LOGGER = LogManager.getLogger("SimpleConfig");
    private final HashMap<String, String> config = new HashMap<>();
    private final ConfigRequest request;
    private boolean broken = false;

    public interface DefaultConfig {
        String get( String namespace );

        static String empty(String namespace) {
            return "";
        }
    }

    public static class ConfigRequest {

        private final File file;
        private final String filename;
        private DefaultConfig provider;
        private int version;

        private ConfigRequest(File file, String filename) {
            this.file = file;
            this.filename = filename;
            this.provider = DefaultConfig::empty;
        }

        /**
         * Sets the default config provider, used to generate the
         * config if it's missing.
         *
         * @param provider default config provider
         * @return current config request object
         * @see DefaultConfig
         */
        public ConfigRequest provider( DefaultConfig provider ) {
            this.provider = provider;
            return this;
        }

        public ConfigRequest version(int version) {
            this.version = version;
            return this;
        }
        /**
         * Loads the config from the filesystem.
         *
         * @return config object
         * @see SimpleConfig
         */
        public SimpleConfig request() {
            return new SimpleConfig( this );
        }

        private String getConfig() {
            return provider.get( filename ) + "\n";
        }

    }

    /**
     * Creates new config request object, ideally `namespace`
     * should be the name of the mod id of the requesting mod
     *
     * @param filename - name of the config file
     * @return new config request object
     */
    public static ConfigRequest of(String filename) {
        Path path = FabricLoader.getInstance().getConfigDir();
        return new ConfigRequest( path.resolve( filename + ".properties" ).toFile(), filename );
    }

    /**
     * @author Crystal
     * Added folder with config
     */
    public static ConfigRequest of(String folder, String filename) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(folder);
        return new ConfigRequest( path.resolve( filename + ".properties" ).toFile(), filename );
    }

    private void createConfig() throws IOException {
        // try creating missing files
        Path parent = request.file.toPath().getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(request.file.toPath())) {
            Files.createFile(request.file.toPath());
        }

        // write default config data
        try (PrintWriter writer = new PrintWriter(request.file, StandardCharsets.UTF_8)) {
            writer.println("# <--- Dont change version below --->");
            writer.println("version = " + request.version);
            writer.write( request.getConfig() );
        } catch (IOException e) {
            LOGGER.error("{} failed to generate!", request.file, e);
            broken = true;
        }
    }

    private void loadConfig() {
        try (Scanner reader = new Scanner(request.file)){
            for( int line = 1; reader.hasNextLine(); line ++ ) {
                parseConfigEntry( reader.nextLine(), line );
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("{} Failed to load config", request.file, e);
        }
    }

    private void parseConfigEntry(String entry, int line) {
        if( !entry.isEmpty() && !entry.startsWith( "#" ) ) {
            String[] parts = entry.split("=", 2);
            if( parts.length == 2 ) {
                config.put( parts[0].trim(), parts[1].trim() );
            } else {
                throw new JsonSyntaxException("Syntax error in config file on line " + line + "!");
            }
        }
    }

    private SimpleConfig( ConfigRequest request ) {
        this.request = request;
        String identifier = "Config '" + request.filename + "'";

        if (request.file.exists() && isOutdated() ) {
            LOGGER.warn("{} is outdated, backing up and regenerating...", identifier);
            save();
        }

        if(!request.file.exists()) {
            LOGGER.info("{} is missing, generating default one...", identifier);

            try {
                createConfig();
            } catch (IOException e) {
                LOGGER.error("{} failed to generate!", identifier);
                LOGGER.trace( e );
                broken = true;
            }
        }

        if(!broken) {
            try {
                loadConfig();
            } catch (Exception e) {
                LOGGER.error("{} failed to load!", identifier);
                LOGGER.trace( e );
                broken = true;
            }
        }

    }

    /**
     * Queries a value from config, returns `null` if the
     * key does not exist.
     *
     * @return  value corresponding to the given key
     * @see     SimpleConfig#getOrDefault
     */
    public String get(String key) {
        return config.get(key);
    }

    /**
     * Returns string value from config corresponding to the given
     * key, or the default string if the key is missing.
     *
     * @return  value corresponding to the given key, or the default value
     */
    public String getOrDefault(String key, String def) {
        String val = get(key);
        return val == null ? def : val;
    }

    /**
     * Returns integer value from config corresponding to the given
     * key, or the default integer if the key is missing or invalid.
     *
     * @return  value corresponding to the given key, or the default value
     */
    public int getOrDefault(String key, int def) {
        try {
            return Integer.parseInt( get(key) );
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Returns boolean value from config corresponding to the given
     * key, or the default boolean if the key is missing.
     *
     * @return  value corresponding to the given key, or the default value
     */
    public boolean getOrDefault(String key, boolean def) {
        String val = get(key);
        if( val != null ) {
            return val.equalsIgnoreCase("true");
        }

        return def;
    }

    /**
     * Returns double value from config corresponding to the given
     * key, or the default string if the key is missing or invalid.
     *
     * @return  value corresponding to the given key, or the default value
     */
    public float getOrDefault(String key, float def) {
        try {
            return Float.parseFloat(get(key));
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Returns double value from config corresponding to the given
     * key, or the default string if the key is missing or invalid.
     *
     * @return  value corresponding to the given key, or the default value
     */
    public double getOrDefault(String key, double def) {
        try {
            return Double.parseDouble(get(key));
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * @author Crystal
     * Recreate file and create .old backup
     */
    public void save() {
        Path source = request.file.toPath();
        Path backup = source.resolveSibling(source.getFileName() + ".old");
        delete(source);
        backup(source, backup);
    }

    /**
     * @author Crystal
     * Checking version
     * @return when is outdate
     */
    public boolean isOutdated() {
        try (Scanner reader = new Scanner(request.file)) {
            while (reader.hasNextLine()) {
                String s = reader.nextLine();
                if (s.isEmpty() || (s.startsWith("#") && !s.contains("version"))) continue;
                if (s.startsWith("version = ")) {
                    int v = Integer.parseInt(s.split("=")[1].trim());
                    return v < request.version;
                }
            }
        } catch (Exception e) {
            return true;
        }
        return true;
    }

    /**
     * @author Crystal
     * Created for dynamical config
     * E.g. Mod Menu + Cloth Config
     * @param path path of file
     * @param changes changes map with key and val
     */
    public static void writer(Path path, Map<String, Object> changes) {
        try {
            if (!Files.exists(path)) return;

            List<String> l = Files.readAllLines(path);
            List<String> newLines = new ArrayList<>();

            for (String line : l) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("#") && trimmed.contains("=")) {
                    String key = trimmed.split("=")[0].trim();
                    if (changes.containsKey(key)) {
                        newLines.add(key + " = " + changes.get(key));
                        continue;
                    }
                }
                newLines.add(line);
            }
            Files.write(path, newLines);
            TheHordes.LOGGER.info("Saved config");
        } catch (IOException e) {
            TheHordes.LOGGER.error("Failed to save config - path: {}", path);
        }
    }

    /**
     * If any error occurred during loading or reading from the config
     * a 'broken' flag is set, indicating that the config's state
     * is undefined and should be discarded using `delete()`
     *
     * @return the 'broken' flag of the configuration
     */
    public boolean isBroken() {
        return broken;
    }

    /**
     * @author Crystal
     * If flag broken is true -> trying to delete file
     * @param path File path
     */
    public void deleteBrokenFile(Path path) {
        if (isBroken()) {
            try {
                Files.delete(path);
                LOGGER.info("Deleted broken file {}", path);
                broken = false;
            } catch (IOException e) {
                LOGGER.error("Failed to delete broken file {}", path, e);
                broken = true;
            }
        }
    }

    private void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOGGER.error("Failed to delete file");
        }
    }
    private void backup(Path source, Path backup) {
        try {
            Files.move(source, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Failed to create backup for {}", source, e);
            broken = true;
        }
    }
}