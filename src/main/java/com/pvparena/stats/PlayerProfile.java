package com.pvparena.stats;

import java.util.UUID;

/**
 * Lifetime, cross-match statistics for a single player.
 */
public class PlayerProfile {

    private final UUID uuid;
    private String name;
    private int kills;
    private int deaths;
    private int wins;
    private int captures;
    private int gamesPlayed;

    public PlayerProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getCaptures() {
        return captures;
    }

    public void setCaptures(int captures) {
        this.captures = captures;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public void addKill() {
        kills++;
    }

    public void addDeath() {
        deaths++;
    }

    public void addWin() {
        wins++;
    }

    public void addCapture() {
        captures++;
    }

    public void addGamePlayed() {
        gamesPlayed++;
    }

    /**
     * Kill/death ratio, using kills when the player has no deaths yet.
     */
    public double getKillDeathRatio() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

    /**
     * Returns the value for a named leaderboard stat, or -1 for an unknown one.
     */
    public int getStat(String stat) {
        return switch (stat.toLowerCase()) {
            case "kills" -> kills;
            case "deaths" -> deaths;
            case "wins" -> wins;
            case "captures" -> captures;
            case "games", "gamesplayed" -> gamesPlayed;
            default -> -1;
        };
    }
}
