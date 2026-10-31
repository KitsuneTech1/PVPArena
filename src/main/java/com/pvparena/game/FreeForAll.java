package com.pvparena.game;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Free For All game mode (Hunger Games style).
 * Last player standing wins.
 */
public class FreeForAll extends GameMode {

    private final Set<UUID> eliminated;

    public FreeForAll(PVPArenaPlugin plugin, Arena arena) {
        super(plugin, arena);
        this.eliminated = new HashSet<>();
    }

    @Override
    protected void onGameStart() {
        // Initialize lives for each player based on FFA supply
        // For FFA, we could use a single supply chest or give fixed lives
        for (Player player : getActivePlayers()) {
            teamSupply.setPlayerLives(player, 1); // Single life by default
        }

        // Announce game start
        for (Player player : getActivePlayers()) {
            player.sendMessage("§6§l=== FREE FOR ALL ===");
            player.sendMessage("§7Last player standing wins!");
            player.sendMessage("§7Everyone for themselves!");
        }
    }

    @Override
    protected void onPlayerDeath(Player victim, Player killer) {
        eliminated.add(victim.getUniqueId());

        int remaining = getAlivePlayers().size();

        for (Player p : getActivePlayers()) {
            String msg = "§c" + victim.getName() + " §7has been eliminated!";
            if (killer != null) {
                msg = "§c" + victim.getName() + " §7was killed by §e" + killer.getName() + "§7!";
            }
            p.sendMessage(msg);
            p.sendMessage("§7" + remaining + " players remaining.");
        }
    }

    @Override
    protected void onPlayerRespawn(Player player) {
        // handlePlayerRespawn only reaches here after a life was successfully
        // consumed, so the player still has lives and rejoins the fight.
        // Players who are out of lives are made spectators upstream instead.
        eliminated.remove(player.getUniqueId());
    }

    @Override
    protected void onGameEnd(String winningTeam) {
        eliminated.clear();
    }

    @Override
    public void checkWinCondition() {
        java.util.List<Player> alive = getAlivePlayers();

        if (alive.size() == 1) {
            Player winner = alive.get(0);
            announceWinner(winner);
            endGame(winner.getName());
        } else if (alive.isEmpty()) {
            endGame(null);
        }
    }

    /**
     * Gets list of players who are still alive.
     */
    private java.util.List<Player> getAlivePlayers() {
        java.util.List<Player> alive = new java.util.ArrayList<>();
        for (Player player : getActivePlayers()) {
            if (!eliminated.contains(player.getUniqueId())) {
                alive.add(player);
            }
        }
        return alive;
    }

    /**
     * Announces the winner.
     */
    private void announceWinner(Player winner) {
        for (Player p : getAllParticipants()) {
            p.sendTitle("§6§l" + winner.getName(), "§eWINS THE GAME!", 10, 70, 20);
        }
    }

    @Override
    public String getDisplayName() {
        return "Free For All";
    }

    /**
     * Checks if a player is eliminated.
     */
    public boolean isEliminated(Player player) {
        return eliminated.contains(player.getUniqueId());
    }

    /**
     * Gets count of remaining players.
     */
    public int getRemainingPlayers() {
        return getAlivePlayers().size();
    }
}
