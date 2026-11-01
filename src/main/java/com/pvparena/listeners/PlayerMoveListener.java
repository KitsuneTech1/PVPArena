package com.pvparena.listeners;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import com.pvparena.arena.ArenaState;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for player movement for boundary checks and capture zones.
 */
public class PlayerMoveListener implements Listener {

    private final PVPArenaPlugin plugin;

    public PlayerMoveListener(PVPArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }

        boolean changedBlock = event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ();

        Player player = event.getPlayer();
        Arena arena = findPlayerArena(player);

        if (arena == null)
            return;

        // Check boundary violation
        if (arena.getState() == ArenaState.ACTIVE) {
            if (changedBlock) {
                checkBoundary(player, arena, event.getTo());
            }

            // Check capture zones for CTF
            checkCaptureZones(player, arena, event.getTo());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Arena arena = findPlayerArena(player);

        if (arena != null) {
            if (arena.getActiveGameMode() != null) {
                arena.getActiveGameMode().handlePlayerQuit(player);
            }

            // Restore inventory
            plugin.getInventoryManager().restoreInventory(player);

            // Remove from arena
            arena.removePlayer(player);

            // Check if game should end
            if (arena.getActiveGameMode() != null) {
                arena.getActiveGameMode().checkWinCondition();
            }
        }
    }

    /**
     * Checks if player is within arena boundaries.
     */
    private void checkBoundary(Player player, Arena arena, Location location) {
        if (arena.isInsideBoundary(location)) {
            return; // Player is inside, all good
        }

        // Player left the boundary - teleport back
        Location returnLoc;

        if (arena.isInArena(player) && !arena.isSpectator(player)) {
            // In-game player -> teleport to team spawn
            returnLoc = arena.getSpawnForPlayer(player);
        } else {
            // Spectator -> teleport to spectator area
            returnLoc = arena.getSpectatorSpawn();
        }

        if (returnLoc == null) {
            returnLoc = arena.getLobbySpawn();
        }

        if (returnLoc != null) {
            player.teleport(returnLoc);
            player.sendMessage("§cYou left the arena boundary!");
        }
    }

    /**
     * Checks capture zones for CTF mode.
     */
    private void checkCaptureZones(Player player, Arena arena, Location location) {
        if (!(arena.getActiveGameMode() instanceof com.pvparena.game.CaptureTheFlag ctf)) {
            return;
        }

        String team = arena.getTeam(player);
        if (team == null)
            return;

        // Check if player is in their capture zone
        Location captureZone = team.equals("red") ? arena.getRedCaptureZone() : arena.getBlueCaptureZone();

        if (captureZone != null && isNearLocation(location, captureZone, 3)) {
            // Player is at their capture zone
            if (ctf.isCarryingFlag(player)) {
                ctf.tryCapture(player);
            }
        }

        // Check if player is near enemy flag (for pickup)
        // This is handled by FlagListener via block interaction
    }

    /**
     * Checks if a location is near another location.
     */
    private boolean isNearLocation(Location loc1, Location loc2, double radius) {
        if (loc1 == null || loc2 == null || loc1.getWorld() == null || loc2.getWorld() == null
                || loc1.getWorld() != loc2.getWorld())
            return false;
        return loc1.distanceSquared(loc2) <= radius * radius;
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
