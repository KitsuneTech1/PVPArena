package com.pvparena.config;

import com.pvparena.PVPArenaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads user-facing strings from messages.yml so server owners can customize
 * them. Falls back to sensible baked-in defaults for any missing key, and
 * translates legacy '&' color codes into section-sign codes on the way out.
 */
public class MessageManager {

    private final PVPArenaPlugin plugin;
    private final File file;
    private final Map<String, String> defaults;
    private FileConfiguration config;

    public MessageManager(PVPArenaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        this.defaults = new HashMap<>();
        registerDefaults();
        load();
    }

    /**
     * Baked-in defaults. These are the fallback for any key absent from the
     * on-disk messages.yml, and the source for the shipped resource file.
     */
    private void registerDefaults() {
        // General / errors
        defaults.put("error.not-in-arena", "&cYou are not in an arena!");
        defaults.put("error.arena-not-found", "&cArena not found!");
        defaults.put("error.players-only", "&cThis command can only be used by players!");
        defaults.put("error.no-permission", "&cYou don't have permission to do that!");

        // Join / leave
        defaults.put("join.success", "&aYou joined arena '{arena}'!");
        defaults.put("join.team", "&7You are on {color}{team} &7team.");
        defaults.put("leave.success", "&aYou left the arena.");

        // Ready
        defaults.put("ready.marked", "&aYou are ready! &7({ready}/{total})");
        defaults.put("ready.all", "&aAll players are ready! Starting the match...");

        // Match start / end
        defaults.put("match.victory-title", "&a&lVICTORY!");
        defaults.put("match.victory-subtitle", "&eYour team won!");
        defaults.put("match.defeat-title", "&c&lDEFEAT");
        defaults.put("match.defeat-subtitle", "&7Better luck next time!");
        defaults.put("match.wager-won", "&6You won the wager pot!");

        // Scores (CTF)
        defaults.put("ctf.captured", "{color}{player} &6&lCAPTURED &7a flag!");
        defaults.put("ctf.score", "&cRed: {red} &7| &9Blue: {blue}");

        // Stats / leaderboard
        defaults.put("stats.header", "&6&l=== STATS: {player} ===");
        defaults.put("stats.line", "&7{stat}: &f{value}");
        defaults.put("stats.none", "&7No stats recorded for {player}.");
        defaults.put("leaderboard.header", "&6&l=== TOP {stat} ===");
        defaults.put("leaderboard.line", "&e{rank}. &f{player} &7- &f{value}");
        defaults.put("leaderboard.empty", "&7No stats recorded yet.");
        defaults.put("leaderboard.usage", "&cUsage: /arena leaderboard <kills|wins|captures>");
    }

    /**
     * Loads messages.yml from disk, writing the default resource first if it
     * does not exist yet.
     */
    public void load() {
        if (!file.exists()) {
            writeDefaultFile();
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Writes a messages.yml populated with the baked-in defaults.
     */
    private void writeDefaultFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        FileConfiguration out = new YamlConfiguration();
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            out.set(entry.getKey(), entry.getValue());
        }
        try {
            out.save(file);
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Could not write default messages.yml: " + e.getMessage());
        }
    }

    /**
     * Resolves a message by key. Uses the on-disk value, then the baked-in
     * default, and finally the key itself so a missing key never returns null
     * or throws. '&' color codes are translated to section-sign codes.
     */
    public String get(String key) {
        String raw = null;
        if (config != null) {
            raw = config.getString(key);
        }
        if (raw == null) {
            raw = defaults.get(key);
        }
        if (raw == null) {
            // Missing key: fall back safely to the key itself.
            return key;
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    /**
     * Resolves a message and substitutes {placeholder} tokens.
     */
    public String get(String key, Map<String, String> placeholders) {
        String message = get(key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    /**
     * Convenience overload for alternating key/value placeholder pairs.
     */
    public String format(String key, String... pairs) {
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            placeholders.put(pairs[i], pairs[i + 1]);
        }
        return get(key, placeholders);
    }
}
