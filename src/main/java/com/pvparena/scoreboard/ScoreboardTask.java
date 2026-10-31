package com.pvparena.scoreboard;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import com.pvparena.arena.ArenaState;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Ticks the live sidebar once per second so the match timer keeps counting
 * even when nothing else happens. Cancels itself when the match ends.
 */
public class ScoreboardTask extends BukkitRunnable {

    private final PVPArenaPlugin plugin;
    private final Arena arena;

    public ScoreboardTask(PVPArenaPlugin plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    public void start() {
        this.runTaskTimer(plugin, 20L, 20L);
    }

    @Override
    public void run() {
        if (arena.getState() != ArenaState.ACTIVE
                || arena.getActiveGameMode() == null
                || arena.getActiveGameMode().isGameEnded()) {
            cancel();
            return;
        }
        plugin.getSidebarManager().updateAll(arena);
    }
}
