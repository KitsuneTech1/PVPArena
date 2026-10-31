package com.pvparena.stats;

import com.pvparena.PVPArenaPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Persistent, cross-match player stats backed by a flatfile YAML
 * (stats.yml in the plugin data folder). Increments are written back on
 * change and the whole set is flushed on disable.
 */
public class ProfileManager {

    private final PVPArenaPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerProfile> profiles;

    public ProfileManager(PVPArenaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        this.profiles = new HashMap<>();
        load();
    }

    /**
     * Loads all profiles from stats.yml.
     */
    public void load() {
        profiles.clear();
        if (!file.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            return;
        }

        for (String key : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection sec = players.getConfigurationSection(key);
            if (sec == null) {
                continue;
            }
            PlayerProfile profile = new PlayerProfile(uuid, sec.getString("name", "Unknown"));
            profile.setKills(sec.getInt("kills"));
            profile.setDeaths(sec.getInt("deaths"));
            profile.setWins(sec.getInt("wins"));
            profile.setCaptures(sec.getInt("captures"));
            profile.setGamesPlayed(sec.getInt("games"));
            profiles.put(uuid, profile);
        }
    }

    /**
     * Writes all profiles to stats.yml.
     */
    public void save() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        FileConfiguration config = new YamlConfiguration();
        for (PlayerProfile profile : profiles.values()) {
            String base = "players." + profile.getUuid();
            config.set(base + ".name", profile.getName());
            config.set(base + ".kills", profile.getKills());
            config.set(base + ".deaths", profile.getDeaths());
            config.set(base + ".wins", profile.getWins());
            config.set(base + ".captures", profile.getCaptures());
            config.set(base + ".games", profile.getGamesPlayed());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save stats.yml", e);
        }
    }

    /**
     * Gets a profile by UUID, creating a blank one if absent.
     */
    public PlayerProfile getOrCreate(UUID uuid, String name) {
        PlayerProfile profile = profiles.computeIfAbsent(uuid, k -> new PlayerProfile(uuid, name));
        if (name != null) {
            profile.setName(name);
        }
        return profile;
    }

    /**
     * Gets a profile by UUID, or null if none exists.
     */
    public PlayerProfile getProfile(UUID uuid) {
        return profiles.get(uuid);
    }

    /**
     * Gets a profile by (case-insensitive) player name, or null if none exists.
     */
    public PlayerProfile getProfileByName(String name) {
        if (name == null) {
            return null;
        }
        for (PlayerProfile profile : profiles.values()) {
            if (name.equalsIgnoreCase(profile.getName())) {
                return profile;
            }
        }
        return null;
    }

    // --- Stat recording (each flushes to disk) ---

    public void recordKill(Player player) {
        getOrCreate(player.getUniqueId(), player.getName()).addKill();
        save();
    }

    public void recordDeath(Player player) {
        getOrCreate(player.getUniqueId(), player.getName()).addDeath();
        save();
    }

    public void recordWin(Player player) {
        getOrCreate(player.getUniqueId(), player.getName()).addWin();
        save();
    }

    public void recordCapture(Player player) {
        getOrCreate(player.getUniqueId(), player.getName()).addCapture();
        save();
    }

    public void recordGamePlayed(Player player) {
        getOrCreate(player.getUniqueId(), player.getName()).addGamePlayed();
        save();
    }

    /**
     * Returns the top profiles by a named stat (kills, wins, captures, ...),
     * highest first, capped at limit.
     */
    public List<PlayerProfile> getLeaderboard(String stat, int limit) {
        List<PlayerProfile> sorted = new ArrayList<>(profiles.values());
        sorted.sort((a, b) -> Integer.compare(b.getStat(stat), a.getStat(stat)));
        if (sorted.size() > limit) {
            return new ArrayList<>(sorted.subList(0, limit));
        }
        return sorted;
    }
}
