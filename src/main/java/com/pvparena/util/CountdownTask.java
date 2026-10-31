package com.pvparena.util;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import com.pvparena.arena.ArenaState;
import com.pvparena.game.CaptureTheFlag;
import com.pvparena.game.Elimination;
import com.pvparena.game.FreeForAll;
import com.pvparena.game.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Handles the pre-game countdown with player freezing.
 */
public class CountdownTask extends BukkitRunnable {

    private final PVPArenaPlugin plugin;
    private final Arena arena;
    private int countdown;

    public CountdownTask(PVPArenaPlugin plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.countdown = arena.getCountdownTime();
    }

    /**
     * Starts the countdown.
     */
    public void start() {
        arena.setState(ArenaState.COUNTDOWN);

        // Teleport and freeze all players
        for (UUID uuid : arena.getAllPlayers()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                // Teleport to spawn
                player.teleport(arena.getSpawnForPlayer(player));

                // Freeze player (slowness + jump boost negative)
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                        (countdown + 2) * 20, 255, false, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,
                        (countdown + 2) * 20, 128, false, false, false));
            }
        }

        // Start the countdown task
        this.runTaskTimer(plugin, 0L, 20L); // Run every second
    }

    @Override
    public void run() {
        if (countdown <= 0) {
            // Start the game!
            startGame();
            cancel();
            return;
        }

        // Show countdown to all players
        for (UUID uuid : arena.getAllPlayers()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                // Title countdown
                String color = countdown <= 3 ? "§c" : "§e";
                player.sendTitle(color + "§l" + countdown, "§7Get ready!", 0, 25, 5);

                // Play tick sound
                plugin.getSoundManager().playCountdownTick(player);
            }
        }

        countdown--;
    }

    /**
     * Called when countdown reaches zero.
     */
    private void startGame() {
        // Remove freeze effects
        for (UUID uuid : arena.getAllPlayers()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.removePotionEffect(PotionEffectType.JUMP_BOOST);

                // Show FIGHT title
                player.sendTitle("§a§lFIGHT!", "§7The game has begun!", 0, 40, 10);

                // Play start sound
                plugin.getSoundManager().playGameStart(player);
            }
        }

        // Create and start the appropriate game mode
        GameMode gameMode = createGameMode();
        arena.setActiveGameMode(gameMode);
        gameMode.startGame();
    }

    /**
     * Creates the game mode instance based on arena config.
     */
    private GameMode createGameMode() {
        switch (arena.getGameModeType()) {
            case CAPTURE_THE_FLAG:
                return new CaptureTheFlag(plugin, arena);
            case ELIMINATION:
                return new Elimination(plugin, arena);
            case FREE_FOR_ALL:
                return new FreeForAll(plugin, arena);
            default:
                return new CaptureTheFlag(plugin, arena);
        }
    }
}
