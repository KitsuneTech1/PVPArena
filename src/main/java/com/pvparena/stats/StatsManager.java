package com.pvparena.stats;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages match statistics and post-game sign displays.
 */
public class StatsManager {

    private final PVPArenaPlugin plugin;
    private final Map<String, MatchStats> activeStats; // Arena name -> stats

    public StatsManager(PVPArenaPlugin plugin) {
        this.plugin = plugin;
        this.activeStats = new HashMap<>();
    }

    /**
     * Starts tracking stats for an arena match.
     */
    public MatchStats startTracking(Arena arena) {
        MatchStats stats = new MatchStats(arena.getName());
        activeStats.put(arena.getName().toLowerCase(), stats);
        return stats;
    }

    /**
     * Gets active stats for an arena.
     */
    public MatchStats getStats(Arena arena) {
        return activeStats.get(arena.getName().toLowerCase());
    }

    /**
     * Ends tracking and updates signs.
     */
    public void endTracking(Arena arena) {
        MatchStats stats = activeStats.remove(arena.getName().toLowerCase());
        if (stats != null) {
            updateSigns(arena, stats);
        }
    }

    /**
     * Records a kill.
     */
    public void recordKill(Arena arena, Player killer) {
        MatchStats stats = getStats(arena);
        if (stats != null) {
            stats.addKill(killer.getUniqueId(), killer.getName());
        }
    }

    /**
     * Records a death.
     */
    public void recordDeath(Arena arena, Player victim) {
        MatchStats stats = getStats(arena);
        if (stats != null) {
            stats.addDeath(victim.getUniqueId(), victim.getName());
        }
    }

    /**
     * Records a flag capture.
     */
    public void recordCapture(Arena arena, Player capturer) {
        MatchStats stats = getStats(arena);
        if (stats != null) {
            stats.addCapture(capturer.getUniqueId(), capturer.getName());
        }
    }

    /**
     * Updates stat signs in the arena.
     */
    private void updateSigns(Arena arena, MatchStats stats) {
        Map<String, Location> signs = arena.getStatsSigns();

        for (Map.Entry<String, Location> entry : signs.entrySet()) {
            String type = entry.getKey();
            Location loc = entry.getValue();

            Block block = loc.getBlock();
            if (!(block.getState() instanceof Sign sign)) {
                continue;
            }

            String[] lines = getSignLines(type, stats);
            Side side = Side.FRONT;

            for (int i = 0; i < 4 && i < lines.length; i++) {
                sign.getSide(side).setLine(i, lines[i]);
            }

            sign.update();
        }
    }

    /**
     * Generates sign content based on stat type.
     */
    private String[] getSignLines(String type, MatchStats stats) {
        switch (type.toLowerCase()) {
            case "kills":
                List<Map.Entry<String, Integer>> topKills = stats.getTopKills(3);
                String[] killLines = new String[4];
                killLines[0] = "§l§6[KILLS]";
                for (int i = 0; i < 3; i++) {
                    if (i < topKills.size()) {
                        Map.Entry<String, Integer> entry = topKills.get(i);
                        killLines[i + 1] = (i + 1) + ". " + entry.getKey() + ": " + entry.getValue();
                    } else {
                        killLines[i + 1] = "";
                    }
                }
                return killLines;

            case "deaths":
                List<Map.Entry<String, Integer>> topDeaths = stats.getTopDeaths(3);
                String[] deathLines = new String[4];
                deathLines[0] = "§l§c[DEATHS]";
                for (int i = 0; i < 3; i++) {
                    if (i < topDeaths.size()) {
                        Map.Entry<String, Integer> entry = topDeaths.get(i);
                        deathLines[i + 1] = (i + 1) + ". " + entry.getKey() + ": " + entry.getValue();
                    } else {
                        deathLines[i + 1] = "";
                    }
                }
                return deathLines;

            case "captures":
                List<Map.Entry<String, Integer>> topCaptures = stats.getTopCaptures(3);
                String[] captureLines = new String[4];
                captureLines[0] = "§l§b[CAPTURES]";
                for (int i = 0; i < 3; i++) {
                    if (i < topCaptures.size()) {
                        Map.Entry<String, Integer> entry = topCaptures.get(i);
                        captureLines[i + 1] = (i + 1) + ". " + entry.getKey() + ": " + entry.getValue();
                    } else {
                        captureLines[i + 1] = "";
                    }
                }
                return captureLines;

            case "mvp":
                String mvp = stats.getMVP();
                int mvpKills = stats.getPlayerKills(mvp);
                int mvpCaptures = stats.getPlayerCaptures(mvp);
                return new String[] {
                        "§l§e[MVP]",
                        "§6🏆 " + (mvp != null ? mvp : "N/A"),
                        mvpKills + " Kills",
                        mvpCaptures + " Captures"
                };

            case "items":
                int totalItems = stats.getTotalItemsUsed();
                return new String[] {
                        "§l§d[ITEMS USED]",
                        "Total: " + totalItems,
                        "items consumed",
                        "this match"
                };

            default:
                return new String[] { "[" + type + "]", "", "", "" };
        }
    }
}
