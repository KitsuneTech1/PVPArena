package com.pvparena.listeners;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import com.pvparena.arena.ArenaState;
import com.pvparena.game.GameMode;
import com.pvparena.util.RespawnTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listens for player death events in arenas.
 */
public class PlayerDeathListener implements Listener {

    private final PVPArenaPlugin plugin;
    private final Map<UUID, Arena> pendingRespawns;

    public PlayerDeathListener(PVPArenaPlugin plugin) {
        this.plugin = plugin;
        this.pendingRespawns = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Find the arena this player is in
        Arena arena = findPlayerArena(victim);
        if (arena == null || arena.getState() != ArenaState.ACTIVE) {
            return;
        }

        GameMode gameMode = arena.getActiveGameMode();
        if (gameMode == null)
            return;

        // Clear drops - the game mode sends items to the trash chest instead
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Handle death in game mode
        gameMode.handlePlayerDeath(victim, killer);

        // Mark for respawn handling
        pendingRespawns.put(victim.getUniqueId(), arena);

        // Clear death message
        event.setDeathMessage(null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Arena arena = pendingRespawns.remove(player.getUniqueId());
        if (arena == null)
            return;

        GameMode gameMode = arena.getActiveGameMode();
        if (gameMode == null || gameMode.isGameEnded())
            return;

        // Set respawn location
        if (arena.getLobbySpawn() != null) {
            event.setRespawnLocation(arena.getLobbySpawn());
        }

        // Schedule delayed respawn with countdown
        int respawnTime = arena.getRespawnTime();
        new RespawnTask(plugin, arena, player, respawnTime).start();
    }

    /**
     * Finds which arena a player is in.
     */
    private Arena findPlayerArena(Player player) {
        for (Arena arena : plugin.getArenaManager().getArenas().values()) {
            if (arena.isInArena(player)) {
                return arena;
            }
        }
        return null;
    }
}
