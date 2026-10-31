package com.pvparena.game;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Elimination game mode (Duel / Team Deathmatch).
 * Last team or player standing wins.
 */
public class Elimination extends GameMode {

    private final Set<UUID> eliminated;

    public Elimination(PVPArenaPlugin plugin, Arena arena) {
        super(plugin, arena);
        this.eliminated = new HashSet<>();
    }

    @Override
    protected void onGameStart() {
        // Announce game start
        for (Player player : getActivePlayers()) {
            player.sendMessage("§6§l=== ELIMINATION ===");
            player.sendMessage("§7Last team standing wins!");
            player.sendMessage("§7Lives are limited by your team's supply chest.");
        }
    }

    @Override
    protected void onPlayerDeath(Player victim, Player killer) {
        // Check if player can respawn
        if (!teamSupply.hasLivesRemaining(victim)) {
            eliminated.add(victim.getUniqueId());

            for (Player p : getActivePlayers()) {
                p.sendMessage("§c" + victim.getName() + " §7has been eliminated!");
            }
        }
    }

    @Override
    protected void onPlayerRespawn(Player player) {
        // Nothing special
    }

    @Override
    protected void onGameEnd(String winningTeam) {
        eliminated.clear();
    }

    @Override
    public void checkWinCondition() {
        // Count alive players per team
        int redAlive = 0;
        int blueAlive = 0;

        for (Player player : getActivePlayers()) {
            if (eliminated.contains(player.getUniqueId()))
                continue;

            String team = arena.getTeam(player);
            if ("red".equals(team)) {
                redAlive++;
            } else if ("blue".equals(team)) {
                blueAlive++;
            }
        }

        // Check win conditions
        if (redAlive == 0 && blueAlive > 0) {
            endGame("blue");
        } else if (blueAlive == 0 && redAlive > 0) {
            endGame("red");
        } else if (redAlive == 0 && blueAlive == 0) {
            // Draw - nobody wins
            endGame(null);
        }
    }

    @Override
    public String getDisplayName() {
        return "Elimination";
    }

    /**
     * Checks if a player is eliminated.
     */
    public boolean isEliminated(Player player) {
        return eliminated.contains(player.getUniqueId());
    }

    /**
     * Gets count of alive players on a team.
     */
    public int getAliveCount(String team) {
        int count = 0;
        for (Player player : getActivePlayers()) {
            if (!eliminated.contains(player.getUniqueId()) &&
                    team.equals(arena.getTeam(player))) {
                count++;
            }
        }
        return count;
    }
}
