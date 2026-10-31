package com.pvparena.chest;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import com.pvparena.kit.KitConfig;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks team lives (virtual supply) for an active game.
 */
public class TeamSupply {

    private final PVPArenaPlugin plugin;
    private final Arena arena;
    private final Map<String, Integer> teamLives; // "red" or "blue" -> lives remaining
    private final Map<UUID, Integer> ffaLives; // Player UUID -> lives (for FFA)

    public TeamSupply(PVPArenaPlugin plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.teamLives = new HashMap<>();
        this.ffaLives = new HashMap<>();
    }

    /**
     * Initializes team supplies by scanning supply chests.
     */
    public void initialize() {
        ChestManager chestManager = plugin.getChestManager();
        KitConfig kit = arena.getKitConfig();

        if (kit == null) {
            plugin.getLogger().warning("No kit configured for arena: " + arena.getName());
            return;
        }

        // Red team supply
        if (arena.getRedSupplyChest() != null) {
            int redLives = chestManager.countAvailableLives(arena.getRedSupplyChest(), kit);
            teamLives.put("red", redLives);
            plugin.getLogger().info("Red team has " + redLives + " lives available");
        }

        // Blue team supply
        if (arena.getBlueSupplyChest() != null) {
            int blueLives = chestManager.countAvailableLives(arena.getBlueSupplyChest(), kit);
            teamLives.put("blue", blueLives);
            plugin.getLogger().info("Blue team has " + blueLives + " lives available");
        }
    }

    /**
     * Tries to consume a life for a player's team.
     * Returns true if successful, false if no lives remaining.
     */
    public boolean tryConsumeLife(Player player) {
        String team = arena.getTeam(player);

        if (team != null) {
            // Team mode
            int lives = teamLives.getOrDefault(team, 0);
            if (lives > 0) {
                // Consume from physical chest
                Location supplyChest = team.equals("red") ? arena.getRedSupplyChest() : arena.getBlueSupplyChest();

                if (supplyChest != null) {
                    boolean consumed = plugin.getChestManager()
                            .consumeLifeFromChest(supplyChest, arena.getKitConfig());

                    if (consumed) {
                        teamLives.put(team, lives - 1);
                        return true;
                    }
                }
            }
            return false;
        } else {
            // FFA mode
            UUID uuid = player.getUniqueId();
            int lives = ffaLives.getOrDefault(uuid, 0);
            if (lives > 0) {
                ffaLives.put(uuid, lives - 1);
                return true;
            }
            return false;
        }
    }

    /**
     * Gets remaining lives for a team.
     */
    public int getTeamLives(String team) {
        return teamLives.getOrDefault(team, 0);
    }

    /**
     * Gets remaining lives for a player (FFA).
     */
    public int getPlayerLives(Player player) {
        return ffaLives.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Sets lives for a player (FFA mode).
     */
    public void setPlayerLives(Player player, int lives) {
        ffaLives.put(player.getUniqueId(), lives);
    }

    /**
     * Adds lives to a team.
     */
    public void addTeamLives(String team, int amount) {
        teamLives.merge(team, amount, Integer::sum);
    }

    /**
     * Checks if a team has any lives remaining.
     */
    public boolean hasLivesRemaining(String team) {
        return getTeamLives(team) > 0;
    }

    /**
     * Checks if a player has any lives remaining.
     */
    public boolean hasLivesRemaining(Player player) {
        String team = arena.getTeam(player);
        if (team != null) {
            return hasLivesRemaining(team);
        }
        return getPlayerLives(player) > 0;
    }

    /**
     * Gets a display string for team lives.
     */
    public String getLivesDisplay() {
        StringBuilder sb = new StringBuilder();

        if (teamLives.containsKey("red")) {
            sb.append("§c❤ Red: ").append(teamLives.get("red"));
        }
        if (teamLives.containsKey("blue")) {
            if (sb.length() > 0)
                sb.append(" §7| ");
            sb.append("§9❤ Blue: ").append(teamLives.get("blue"));
        }

        return sb.toString();
    }

    /**
     * Clears all tracked lives.
     */
    public void clear() {
        teamLives.clear();
        ffaLives.clear();
    }
}
