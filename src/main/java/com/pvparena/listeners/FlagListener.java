package com.pvparena.listeners;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import com.pvparena.arena.ArenaState;
import com.pvparena.game.CaptureTheFlag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listens for flag interactions in CTF mode.
 */
public class FlagListener implements Listener {

    private final PVPArenaPlugin plugin;

    public FlagListener(PVPArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check if this is a flag banner
        if (!isBanner(block.getType())) {
            return;
        }

        Arena arena = findPlayerArena(player);
        if (arena == null || arena.getState() != ArenaState.ACTIVE) {
            return;
        }

        if (!(arena.getActiveGameMode() instanceof CaptureTheFlag ctf)) {
            return;
        }

        // Determine which flag this is
        String flagTeam = getFlagTeam(arena, block.getLocation());
        if (flagTeam == null) {
            return; // Not a game flag
        }

        // Cancel the break - we handle flag pickup differently
        event.setCancelled(true);

        // Try to pick up the flag
        ctf.tryPickupFlag(player, flagTeam);

        // Remove the banner block
        block.setType(Material.AIR);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null)
            return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        // Check if this is a flag banner
        if (!isBanner(block.getType())) {
            return;
        }

        Arena arena = findPlayerArena(player);
        if (arena == null || arena.getState() != ArenaState.ACTIVE) {
            return;
        }

        if (!(arena.getActiveGameMode() instanceof CaptureTheFlag ctf)) {
            return;
        }

        // Determine which flag this is
        String flagTeam = getFlagTeam(arena, block.getLocation());
        if (flagTeam == null) {
            // Check if it's a dropped flag (not at base)
            flagTeam = getDroppedFlagTeam(ctf, block.getLocation());
        }

        if (flagTeam == null)
            return;

        String playerTeam = arena.getTeam(player);
        if (playerTeam == null)
            return;

        // Own team flag - try to return if dropped
        if (playerTeam.equals(flagTeam)) {
            if (!ctf.isFlagAtBase(flagTeam)) {
                ctf.tryReturnFlag(player, flagTeam);
                block.setType(Material.AIR);
            }
        } else {
            // Enemy flag - try to pick up
            ctf.tryPickupFlag(player, flagTeam);
            block.setType(Material.AIR);
        }

        event.setCancelled(true);
    }

    /**
     * Checks if a material is a banner.
     */
    private boolean isBanner(Material material) {
        return material.name().endsWith("_BANNER");
    }

    /**
     * Determines which team's flag is at a location.
     */
    private String getFlagTeam(Arena arena, Location location) {
        if (isAtLocation(location, arena.getRedFlagLocation())) {
            return "red";
        }
        if (isAtLocation(location, arena.getBlueFlagLocation())) {
            return "blue";
        }
        return null;
    }

    /**
     * Checks if location matches a dropped flag.
     */
    private String getDroppedFlagTeam(CaptureTheFlag ctf, Location location) {
        Location droppedRed = ctf.getDroppedFlagLocation("red");
        Location droppedBlue = ctf.getDroppedFlagLocation("blue");

        if (droppedRed != null && isAtLocation(location, droppedRed)) {
            return "red";
        }
        if (droppedBlue != null && isAtLocation(location, droppedBlue)) {
            return "blue";
        }
        return null;
    }

    /**
     * Checks if two locations are at the same block.
     */
    private boolean isAtLocation(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null)
            return false;
        if (loc1.getWorld() != loc2.getWorld())
            return false;
        return loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
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
