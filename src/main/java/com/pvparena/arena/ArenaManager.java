package com.pvparena.arena;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.kit.KitConfig;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages all arenas - creation, deletion, saving, and loading.
 */
public class ArenaManager {

    private final PVPArenaPlugin plugin;
    private final Map<String, Arena> arenas;
    private final File arenasFolder;

    public ArenaManager(PVPArenaPlugin plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.arenasFolder = new File(plugin.getDataFolder(), "arenas");

        if (!arenasFolder.exists()) {
            arenasFolder.mkdirs();
        }
    }

    /**
     * Creates a new arena with the given name.
     */
    public Arena createArena(String name) {
        if (arenas.containsKey(name.toLowerCase())) {
            return null; // Arena already exists
        }

        Arena arena = new Arena(name);
        arenas.put(name.toLowerCase(), arena);
        saveArena(arena);
        return arena;
    }

    /**
     * Deletes an arena by name.
     */
    public boolean deleteArena(String name) {
        Arena arena = arenas.remove(name.toLowerCase());
        if (arena != null) {
            File arenaFile = new File(arenasFolder, name.toLowerCase() + ".yml");
            if (arenaFile.exists()) {
                arenaFile.delete();
            }
            return true;
        }
        return false;
    }

    /**
     * Gets an arena by name.
     */
    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    /**
     * Gets all arenas.
     */
    public Map<String, Arena> getArenas() {
        return arenas;
    }

    /**
     * Checks if an arena exists.
     */
    public boolean arenaExists(String name) {
        return arenas.containsKey(name.toLowerCase());
    }

    /**
     * Saves all arenas to disk.
     */
    public void saveArenas() {
        for (Arena arena : arenas.values()) {
            saveArena(arena);
        }
    }

    /**
     * Saves a single arena to disk.
     */
    public void saveArena(Arena arena) {
        File arenaFile = new File(arenasFolder, arena.getName().toLowerCase() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(arenaFile);

        config.set("name", arena.getName());
        config.set("gameMode", arena.getGameModeType().name());
        config.set("respawnTime", arena.getRespawnTime());
        config.set("countdownTime", arena.getCountdownTime());
        config.set("maxPlayers", arena.getMaxPlayers());
        config.set("minPlayers", arena.getMinPlayers());

        // Spawns
        saveLocation(config, "spawns.red", arena.getRedSpawn());
        saveLocation(config, "spawns.blue", arena.getBlueSpawn());
        saveLocation(config, "spawns.lobby", arena.getLobbySpawn());
        saveLocation(config, "spawns.spectator", arena.getSpectatorSpawn());

        // FFA spawns
        config.set("spawns.ffa", null);
        for (int i = 0; i < arena.getFfaSpawns().size(); i++) {
            saveLocation(config, "spawns.ffa." + i, arena.getFfaSpawns().get(i));
        }

        // Boundaries
        saveLocation(config, "boundaries.corner1", arena.getCorner1());
        saveLocation(config, "boundaries.corner2", arena.getCorner2());

        // Chests
        saveLocation(config, "chests.redSupply", arena.getRedSupplyChest());
        saveLocation(config, "chests.blueSupply", arena.getBlueSupplyChest());
        saveLocation(config, "chests.betting", arena.getBettingChest());

        // Trash chests
        config.set("chests.trash", null);
        for (int i = 0; i < arena.getTrashChests().size(); i++) {
            saveLocation(config, "chests.trash." + i, arena.getTrashChests().get(i));
        }

        // CTF specific
        saveLocation(config, "ctf.redFlag", arena.getRedFlagLocation());
        saveLocation(config, "ctf.blueFlag", arena.getBlueFlagLocation());
        saveLocation(config, "ctf.redCapture", arena.getRedCaptureZone());
        saveLocation(config, "ctf.blueCapture", arena.getBlueCaptureZone());

        // Stats signs
        for (Map.Entry<String, Location> entry : arena.getStatsSigns().entrySet()) {
            saveLocation(config, "signs." + entry.getKey(), entry.getValue());
        }

        // Kit (the per-life cost is defined by the kit's item set, so this must persist)
        saveKit(config, arena.getKitConfig());

        try {
            config.save(arenaFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save arena: " + arena.getName(), e);
        }
    }

    /**
     * Loads all arenas from disk.
     */
    public void loadArenas() {
        File[] files = arenasFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            loadArena(file);
        }

        plugin.getLogger().info("Loaded " + arenas.size() + " arena(s).");
    }

    /**
     * Loads a single arena from a file.
     */
    private void loadArena(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        String name = config.getString("name");
        if (name == null)
            return;

        Arena arena = new Arena(name);

        // Game mode
        String gameMode = config.getString("gameMode", "CAPTURE_THE_FLAG");
        try {
            arena.setGameModeType(com.pvparena.game.GameModeType.valueOf(gameMode));
        } catch (IllegalArgumentException e) {
            arena.setGameModeType(com.pvparena.game.GameModeType.CAPTURE_THE_FLAG);
        }

        // Config
        arena.setRespawnTime(config.getInt("respawnTime", 5));
        arena.setCountdownTime(config.getInt("countdownTime", 10));
        arena.setMaxPlayers(config.getInt("maxPlayers", 16));
        arena.setMinPlayers(config.getInt("minPlayers", 2));

        // Spawns
        arena.setRedSpawn(loadLocation(config, "spawns.red"));
        arena.setBlueSpawn(loadLocation(config, "spawns.blue"));
        arena.setLobbySpawn(loadLocation(config, "spawns.lobby"));
        arena.setSpectatorSpawn(loadLocation(config, "spawns.spectator"));

        // FFA spawns
        if (config.isConfigurationSection("spawns.ffa")) {
            for (String key : config.getConfigurationSection("spawns.ffa").getKeys(false)) {
                Location loc = loadLocation(config, "spawns.ffa." + key);
                if (loc != null)
                    arena.addFfaSpawn(loc);
            }
        }

        // Boundaries
        arena.setCorner1(loadLocation(config, "boundaries.corner1"));
        arena.setCorner2(loadLocation(config, "boundaries.corner2"));

        // Chests
        arena.setRedSupplyChest(loadLocation(config, "chests.redSupply"));
        arena.setBlueSupplyChest(loadLocation(config, "chests.blueSupply"));
        arena.setBettingChest(loadLocation(config, "chests.betting"));

        // Trash chests
        if (config.isConfigurationSection("chests.trash")) {
            for (String key : config.getConfigurationSection("chests.trash").getKeys(false)) {
                Location loc = loadLocation(config, "chests.trash." + key);
                if (loc != null)
                    arena.addTrashChest(loc);
            }
        }

        // CTF
        arena.setRedFlagLocation(loadLocation(config, "ctf.redFlag"));
        arena.setBlueFlagLocation(loadLocation(config, "ctf.blueFlag"));
        arena.setRedCaptureZone(loadLocation(config, "ctf.redCapture"));
        arena.setBlueCaptureZone(loadLocation(config, "ctf.blueCapture"));

        // Stats signs
        if (config.isConfigurationSection("signs")) {
            for (String key : config.getConfigurationSection("signs").getKeys(false)) {
                Location loc = loadLocation(config, "signs." + key);
                if (loc != null)
                    arena.setStatsSign(key, loc);
            }
        }

        // Kit
        KitConfig kit = loadKit(config);
        if (kit != null) {
            arena.setKitConfig(kit);
        }

        arenas.put(name.toLowerCase(), arena);
    }

    /**
     * Serializes an arena's kit (items that define the per-life cost) into config.
     */
    private void saveKit(FileConfiguration config, KitConfig kit) {
        config.set("kit", null);
        if (kit == null) {
            return;
        }

        config.set("kit.helmet", kit.getHelmet());
        config.set("kit.chestplate", kit.getChestplate());
        config.set("kit.leggings", kit.getLeggings());
        config.set("kit.boots", kit.getBoots());
        config.set("kit.mainHand", kit.getMainHand());
        config.set("kit.offHand", kit.getOffHand());
        config.set("kit.inventory", new java.util.ArrayList<>(kit.getInventory()));
    }

    /**
     * Restores a kit (items + derived per-life cost) from config, or null if none saved.
     */
    private KitConfig loadKit(FileConfiguration config) {
        if (!config.isConfigurationSection("kit")) {
            return null;
        }

        KitConfig kit = new KitConfig();
        kit.setHelmet(config.getItemStack("kit.helmet"));
        kit.setChestplate(config.getItemStack("kit.chestplate"));
        kit.setLeggings(config.getItemStack("kit.leggings"));
        kit.setBoots(config.getItemStack("kit.boots"));
        kit.setMainHand(config.getItemStack("kit.mainHand"));
        kit.setOffHand(config.getItemStack("kit.offHand"));

        java.util.List<?> invList = config.getList("kit.inventory");
        if (invList != null) {
            for (Object obj : invList) {
                if (obj instanceof org.bukkit.inventory.ItemStack item) {
                    kit.addInventoryItem(item);
                }
            }
        }

        return kit;
    }

    private void saveLocation(FileConfiguration config, String path, Location location) {
        if (location == null) {
            config.set(path, null);
            return;
        }

        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }

    private Location loadLocation(FileConfiguration config, String path) {
        if (!config.isConfigurationSection(path)) {
            return null;
        }

        String worldName = config.getString(path + ".world");
        if (worldName == null)
            return null;

        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world == null)
            return null;

        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        float yaw = (float) config.getDouble(path + ".yaw");
        float pitch = (float) config.getDouble(path + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }
}
