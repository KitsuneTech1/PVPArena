package com.pvparena.stats;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Holds statistics for a single match.
 */
public class MatchStats {

    private final String arenaName;
    private final Map<UUID, PlayerStats> playerStats;
    private int totalItemsUsed;
    private long startTime;
    private long endTime;

    public MatchStats(String arenaName) {
        this.arenaName = arenaName;
        this.playerStats = new HashMap<>();
        this.totalItemsUsed = 0;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Gets or creates stats for a player.
     */
    private PlayerStats getOrCreate(UUID uuid, String name) {
        return playerStats.computeIfAbsent(uuid, k -> new PlayerStats(name));
    }

    /**
     * Records a kill.
     */
    public void addKill(UUID uuid, String name) {
        getOrCreate(uuid, name).kills++;
    }

    /**
     * Records a death.
     */
    public void addDeath(UUID uuid, String name) {
        getOrCreate(uuid, name).deaths++;
    }

    /**
     * Records a flag capture.
     */
    public void addCapture(UUID uuid, String name) {
        getOrCreate(uuid, name).captures++;
    }

    /**
     * Adds to total items used counter.
     */
    public void addItemsUsed(int count) {
        totalItemsUsed += count;
    }

    /**
     * Gets top killers.
     */
    public List<Map.Entry<String, Integer>> getTopKills(int limit) {
        return playerStats.values().stream()
                .sorted((a, b) -> Integer.compare(b.kills, a.kills))
                .limit(limit)
                .map(p -> Map.entry(p.name, p.kills))
                .collect(Collectors.toList());
    }

    /**
     * Gets top deaths.
     */
    public List<Map.Entry<String, Integer>> getTopDeaths(int limit) {
        return playerStats.values().stream()
                .sorted((a, b) -> Integer.compare(b.deaths, a.deaths))
                .limit(limit)
                .map(p -> Map.entry(p.name, p.deaths))
                .collect(Collectors.toList());
    }

    /**
     * Gets top capturers.
     */
    public List<Map.Entry<String, Integer>> getTopCaptures(int limit) {
        return playerStats.values().stream()
                .sorted((a, b) -> Integer.compare(b.captures, a.captures))
                .limit(limit)
                .map(p -> Map.entry(p.name, p.captures))
                .collect(Collectors.toList());
    }

    /**
     * Gets the MVP (highest combined score).
     */
    public String getMVP() {
        return playerStats.values().stream()
                .max(Comparator.comparingInt(p -> p.kills * 2 + p.captures * 3))
                .map(p -> p.name)
                .orElse(null);
    }

    /**
     * Gets kills for a specific player by name.
     */
    public int getPlayerKills(String name) {
        if (name == null)
            return 0;
        return playerStats.values().stream()
                .filter(p -> p.name.equals(name))
                .findFirst()
                .map(p -> p.kills)
                .orElse(0);
    }

    /**
     * Gets captures for a specific player by name.
     */
    public int getPlayerCaptures(String name) {
        if (name == null)
            return 0;
        return playerStats.values().stream()
                .filter(p -> p.name.equals(name))
                .findFirst()
                .map(p -> p.captures)
                .orElse(0);
    }

    /**
     * Gets total items used.
     */
    public int getTotalItemsUsed() {
        return totalItemsUsed;
    }

    /**
     * Ends the match timer.
     */
    public void endMatch() {
        this.endTime = System.currentTimeMillis();
    }

    /**
     * Gets match duration in seconds.
     */
    public long getDurationSeconds() {
        long end = endTime > 0 ? endTime : System.currentTimeMillis();
        return (end - startTime) / 1000;
    }

    /**
     * Internal class for per-player stats.
     */
    private static class PlayerStats {
        final String name;
        int kills = 0;
        int deaths = 0;
        int captures = 0;

        PlayerStats(String name) {
            this.name = name;
        }
    }
}
