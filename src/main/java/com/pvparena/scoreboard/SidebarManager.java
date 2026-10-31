package com.pvparena.scoreboard;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;
import java.util.UUID;

/**
 * Renders the {@link SidebarState} model onto each player's live Bukkit
 * sidebar scoreboard. All Bukkit calls are made on the caller's thread (the
 * main thread, since every trigger runs from game logic), and every entry point
 * is guarded so a headless server without scoreboard support degrades quietly
 * instead of breaking the match.
 */
public class SidebarManager {

    private final PVPArenaPlugin plugin;

    public SidebarManager(PVPArenaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Rebuilds and pushes the sidebar for every active player in the arena.
     */
    public void updateAll(Arena arena) {
        for (UUID uuid : arena.getAllPlayers()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                update(arena, player);
            }
        }
    }

    /**
     * Rebuilds and pushes the sidebar for a single player.
     */
    public void update(Arena arena, Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = safeManager();
        if (manager == null) {
            return;
        }
        try {
            SidebarState state = SidebarState.build(plugin, arena, player);
            Scoreboard board = player.getScoreboard();
            // If the player is still on the shared main scoreboard, give them
            // their own so per-player values (lives) do not collide.
            if (board == null || board == manager.getMainScoreboard()) {
                board = manager.getNewScoreboard();
            }

            Objective objective = board.getObjective("pvparena");
            if (objective == null) {
                objective = board.registerNewObjective("pvparena", "dummy", SidebarState.TITLE);
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            } else {
                // Clear old entries so stale lines do not linger.
                for (String entry : board.getEntries()) {
                    board.resetScores(entry);
                }
            }

            List<String> lines = state.getLines();
            int score = lines.size();
            for (String line : lines) {
                objective.getScore(line).setScore(score--);
            }

            player.setScoreboard(board);
        } catch (Throwable t) {
            // Scoreboards are cosmetic; never let a rendering issue break play.
            plugin.getLogger().fine("Sidebar update skipped: " + t.getMessage());
        }
    }

    /**
     * Clears a player's arena sidebar, returning them to the main scoreboard.
     */
    public void clear(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = safeManager();
        if (manager == null) {
            return;
        }
        try {
            player.setScoreboard(manager.getMainScoreboard());
        } catch (Throwable t) {
            plugin.getLogger().fine("Sidebar clear skipped: " + t.getMessage());
        }
    }

    /**
     * Clears the sidebar for everyone who took part in the match.
     */
    public void clearAll(Arena arena) {
        for (UUID uuid : arena.getAllPlayers()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                clear(player);
            }
        }
        for (UUID uuid : arena.getSpectators()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                clear(player);
            }
        }
    }

    private org.bukkit.scoreboard.ScoreboardManager safeManager() {
        try {
            return Bukkit.getScoreboardManager();
        } catch (Throwable t) {
            return null;
        }
    }
}
