package com.pvparena.util;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import com.pvparena.game.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles delayed respawn countdown for players.
 */
public class RespawnTask extends BukkitRunnable {

    private final PVPArenaPlugin plugin;
    private final Arena arena;
    private final Player player;
    private int countdown;

    public RespawnTask(PVPArenaPlugin plugin, Arena arena, Player player, int seconds) {
        this.plugin = plugin;
        this.arena = arena;
        this.player = player;
        this.countdown = seconds;
    }

    /**
     * Starts the respawn countdown.
     */
    public void start() {
        this.runTaskTimer(plugin, 0L, 20L);
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }

        GameMode gameMode = arena.getActiveGameMode();
        if (gameMode == null || gameMode.isGameEnded()) {
            cancel();
            return;
        }

        if (countdown <= 0) {
            // Respawn the player
            gameMode.handlePlayerRespawn(player);
            cancel();
            return;
        }

        // Show countdown
        player.sendTitle("§c§lRESPAWNING", "§7in " + countdown + "...", 0, 25, 5);

        countdown--;
    }
}
