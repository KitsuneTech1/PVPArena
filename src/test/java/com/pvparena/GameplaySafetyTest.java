package com.pvparena;

import com.pvparena.arena.Arena;
import com.pvparena.arena.ArenaState;
import com.pvparena.commands.ArenaCommands;
import com.pvparena.game.Elimination;
import com.pvparena.kit.KitConfig;
import com.pvparena.listeners.PlayerMoveListener;
import com.pvparena.game.CaptureTheFlag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class GameplaySafetyTest {

    private ServerMock server;
    private PVPArenaPlugin plugin;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(PVPArenaPlugin.class);
        world = server.addSimpleWorld("gameplaysafety");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void respawnWithoutAConfiguredSpawnDoesNotCrash() {
        PlayerMock player = server.addPlayer("Respawner");
        Arena arena = plugin.getArenaManager().createArena("spawnlessarena");
        arena.addToRedTeam(player);

        KitConfig kit = new KitConfig();
        kit.setMainHand(new ItemStack(Material.DIAMOND_SWORD));
        arena.setKitConfig(kit);
        Location supplyLocation = new Location(world, 0, 64, 0);
        Block supplyBlock = supplyLocation.getBlock();
        supplyBlock.setType(Material.CHEST);
        ((Chest) supplyBlock.getState()).getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
        arena.setRedSupplyChest(supplyLocation);

        Elimination game = new Elimination(plugin, arena);
        arena.setActiveGameMode(game);
        game.startGame();

        assertDoesNotThrow(() -> game.handlePlayerRespawn(player));
    }

    @Test
    void playerQuitDropsCarriedFlagAndRestoresTheOriginalBlock() {
        PlayerMock red = server.addPlayer("Quitter");
        PlayerMock blue = server.addPlayer("Opponent");
        Arena arena = plugin.getArenaManager().createArena("quitflagarena");
        Location redFlag = new Location(world, 0, 64, 0);
        Location blueFlag = new Location(world, 10, 64, 0);
        redFlag.getBlock().setType(Material.STONE);
        blueFlag.getBlock().setType(Material.STONE);
        arena.setRedFlagLocation(redFlag);
        arena.setBlueFlagLocation(blueFlag);
        arena.addToRedTeam(red);
        arena.addToBlueTeam(blue);

        CaptureTheFlag game = new CaptureTheFlag(plugin, arena);
        arena.setActiveGameMode(game);
        game.startGame();
        game.getTeamSupply().addTeamLives("red", 5);
        game.getTeamSupply().addTeamLives("blue", 5);

        assertTrue(game.tryPickupFlag(red, "blue"));
        new PlayerMoveListener(plugin).onPlayerQuit(new PlayerQuitEvent(red, "quit"));

        assertFalse(game.isCarryingFlag(red));
        assertNotNull(game.getDroppedFlagLocation("blue"));
        assertEquals(Material.STONE, blueFlag.getBlock().getType(),
                "ending a flag placement must restore the block underneath it");
    }

    @Test
    void manualLeaveDropsCarriedFlag() {
        PlayerMock red = server.addPlayer("Leaver");
        Arena arena = plugin.getArenaManager().createArena("leaveflagarena");
        arena.addToRedTeam(red);
        Location redFlag = new Location(world, 0, 64, 0);
        Location blueFlag = new Location(world, 5, 64, 0);
        redFlag.getBlock().setType(Material.STONE);
        blueFlag.getBlock().setType(Material.STONE);
        arena.setRedFlagLocation(redFlag);
        arena.setBlueFlagLocation(blueFlag);
        CaptureTheFlag game = new CaptureTheFlag(plugin, arena);
        arena.setActiveGameMode(game);
        arena.setState(ArenaState.ACTIVE);
        game.startGame();
        game.getTeamSupply().addTeamLives("red", 5);
        game.getTeamSupply().addTeamLives("blue", 5);

        assertTrue(game.tryPickupFlag(red, "blue"));
        new ArenaCommands(plugin).onCommand(red, null, "arena", new String[] { "leave" });

        assertFalse(game.isCarryingFlag(red));
        assertNotNull(game.getDroppedFlagLocation("blue"));
    }

    @Test
    void endingAfterFlagPickupPreservesTheRestoredBaseBlock() {
        PlayerMock red = server.addPlayer("Winner");
        PlayerMock blue = server.addPlayer("Carrier");
        Arena arena = plugin.getArenaManager().createArena("endflagarena");
        arena.addToRedTeam(red);
        arena.addToBlueTeam(blue);
        Location redFlag = new Location(world, 0, 64, 0);
        Location blueFlag = new Location(world, 5, 64, 0);
        redFlag.getBlock().setType(Material.STONE);
        blueFlag.getBlock().setType(Material.STONE);
        arena.setRedFlagLocation(redFlag);
        arena.setBlueFlagLocation(blueFlag);
        CaptureTheFlag game = new CaptureTheFlag(plugin, arena);
        arena.setActiveGameMode(game);
        game.startGame();

        assertTrue(game.tryPickupFlag(red, "blue"));
        game.endGame("red");

        assertEquals(Material.STONE, blueFlag.getBlock().getType());
    }

    @Test
    void deathMovesArmorToTrashOnlyOnce() {
        PlayerMock player = server.addPlayer("Armored");
        Arena arena = plugin.getArenaManager().createArena("trasharena");
        arena.addToRedTeam(player);
        Location trashLocation = new Location(world, 20, 64, 0);
        Block trashBlock = trashLocation.getBlock();
        trashBlock.setType(Material.CHEST);
        arena.addTrashChest(trashLocation);

        Elimination game = new Elimination(plugin, arena);
        arena.setActiveGameMode(game);
        game.startGame();
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        game.handlePlayerDeath(player, null);

        int helmetCount = 0;
        for (ItemStack item : ((Chest) trashBlock.getState()).getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND_HELMET) {
                helmetCount += item.getAmount();
            }
        }
        assertEquals(1, helmetCount);
    }

    @Test
    void boundariesRejectLocationsInAnotherWorld() {
        World otherWorld = server.addSimpleWorld("otherworld");
        Arena arena = new Arena("boundaryarena");
        arena.setCorner1(new Location(world, 0, 0, 0));
        arena.setCorner2(new Location(world, 10, 10, 10));

        assertFalse(arena.isInsideBoundary(new Location(otherWorld, 5, 5, 5)));
    }
}
