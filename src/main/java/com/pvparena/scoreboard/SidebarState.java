package com.pvparena.scoreboard;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import com.pvparena.chest.TeamSupply;
import com.pvparena.game.CaptureTheFlag;
import com.pvparena.game.Elimination;
import com.pvparena.game.FreeForAll;
import com.pvparena.game.GameMode;
import com.pvparena.stats.MatchStats;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-data snapshot of what a player's sidebar scoreboard should show during a
 * match: mode, team scores (CTF captures / Elimination alive-counts / FFA
 * leader), the player's own lives, and the match timer. Built fresh from the
 * live game state each time the sidebar is updated, and holds no Bukkit
 * scoreboard objects so it is trivially testable.
 */
public class SidebarState {

    public static final String TITLE = "§6§lPVP ARENA";

    private final String modeName;
    private final int redScore;
    private final int blueScore;
    private final boolean teamMode;
    private final String leaderName;
    private final int playersRemaining;
    private final int ownLives;
    private final long timerSeconds;

    private SidebarState(String modeName, boolean teamMode, int redScore, int blueScore,
                         String leaderName, int playersRemaining, int ownLives, long timerSeconds) {
        this.modeName = modeName;
        this.teamMode = teamMode;
        this.redScore = redScore;
        this.blueScore = blueScore;
        this.leaderName = leaderName;
        this.playersRemaining = playersRemaining;
        this.ownLives = ownLives;
        this.timerSeconds = timerSeconds;
    }

    /**
     * Builds a snapshot from the arena's active game for a specific player.
     */
    public static SidebarState build(PVPArenaPlugin plugin, Arena arena, Player player) {
        GameMode game = arena.getActiveGameMode();
        String modeName = game != null ? game.getDisplayName() : arena.getGameModeType().getDisplayName();

        int red = 0;
        int blue = 0;
        boolean teamMode = true;
        String leader = null;
        int remaining = 0;

        if (game instanceof CaptureTheFlag ctf) {
            red = ctf.getScore("red");
            blue = ctf.getScore("blue");
        } else if (game instanceof Elimination elim) {
            red = elim.getAliveCount("red");
            blue = elim.getAliveCount("blue");
        } else if (game instanceof FreeForAll ffa) {
            teamMode = false;
            remaining = ffa.getRemainingPlayers();
            leader = resolveLeader(plugin, arena);
        } else {
            teamMode = arena.getGameModeType() != com.pvparena.game.GameModeType.FREE_FOR_ALL;
        }

        int ownLives = resolveOwnLives(game, arena, player);
        long timer = resolveTimer(plugin, arena);

        return new SidebarState(modeName, teamMode, red, blue, leader, remaining, ownLives, timer);
    }

    private static int resolveOwnLives(GameMode game, Arena arena, Player player) {
        if (game == null || game.getTeamSupply() == null || player == null) {
            return 0;
        }
        TeamSupply supply = game.getTeamSupply();
        String team = arena.getTeam(player);
        if (team != null) {
            return supply.getTeamLives(team);
        }
        return supply.getPlayerLives(player);
    }

    private static long resolveTimer(PVPArenaPlugin plugin, Arena arena) {
        if (plugin.getStatsManager() == null) {
            return 0;
        }
        MatchStats stats = plugin.getStatsManager().getStats(arena);
        return stats != null ? stats.getDurationSeconds() : 0;
    }

    private static String resolveLeader(PVPArenaPlugin plugin, Arena arena) {
        if (plugin.getStatsManager() == null) {
            return null;
        }
        MatchStats stats = plugin.getStatsManager().getStats(arena);
        if (stats == null) {
            return null;
        }
        var top = stats.getTopKills(1);
        return top.isEmpty() ? null : top.get(0).getKey();
    }

    // --- Raw values (asserted by tests, consumed by the sidebar renderer) ---

    public String getModeName() {
        return modeName;
    }

    public boolean isTeamMode() {
        return teamMode;
    }

    public int getRedScore() {
        return redScore;
    }

    public int getBlueScore() {
        return blueScore;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public int getPlayersRemaining() {
        return playersRemaining;
    }

    public int getOwnLives() {
        return ownLives;
    }

    public long getTimerSeconds() {
        return timerSeconds;
    }

    /**
     * Formats the timer as mm:ss.
     */
    public String getTimerDisplay() {
        long minutes = timerSeconds / 60;
        long seconds = timerSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Renders the ordered sidebar lines (top to bottom). Each line is unique so
     * a Bukkit sidebar objective can score them without collisions.
     */
    public List<String> getLines() {
        List<String> lines = new ArrayList<>();
        lines.add("§7Mode: §f" + modeName);
        lines.add("§7Time: §f" + getTimerDisplay());
        lines.add("§8§m          "); // spacer
        if (teamMode) {
            lines.add("§cRed: §f" + redScore);
            lines.add("§9Blue: §f" + blueScore);
        } else {
            lines.add("§7Players left: §f" + playersRemaining);
            lines.add("§6Leader: §f" + (leaderName != null ? leaderName : "-"));
        }
        lines.add("§7Your lives: §a" + ownLives);
        return lines;
    }
}
