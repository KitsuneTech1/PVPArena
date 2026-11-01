package com.pvparena;

import com.pvparena.arena.Arena;
import com.pvparena.chest.ChestManager;
import com.pvparena.kit.KitConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class ChestSafetyTest {

    private ServerMock server;
    private PVPArenaPlugin plugin;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(PVPArenaPlugin.class);
        world = server.addSimpleWorld("chestsafety");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void duplicateKitMaterialsAreCountedTogether() {
        Location location = new Location(world, 0, 64, 0);
        Block block = location.getBlock();
        block.setType(Material.CHEST);
        Inventory inventory = ((Chest) block.getState()).getInventory();
        inventory.addItem(new ItemStack(Material.DIAMOND, 1));

        KitConfig kit = new KitConfig();
        kit.setMainHand(new ItemStack(Material.DIAMOND, 1));
        kit.addInventoryItem(new ItemStack(Material.DIAMOND, 1));

        ChestManager chestManager = plugin.getChestManager();
        assertEquals(0, chestManager.countAvailableLives(location, kit));
        assertFalse(chestManager.consumeLifeFromChest(location, kit));
        assertEquals(1, inventory.getItem(0).getAmount());
    }

    @Test
    void wagerOverflowStaysInThePot() {
        Arena arena = plugin.getArenaManager().createArena("overflowarena");
        PlayerMock winner = server.addPlayer("FullWinner");
        arena.addToRedTeam(winner);

        Location location = new Location(world, 10, 64, 0);
        Block block = location.getBlock();
        block.setType(Material.CHEST);
        arena.setBettingChest(location);
        Inventory pot = ((Chest) block.getState()).getInventory();
        pot.addItem(new ItemStack(Material.DIAMOND, 5));

        for (int i = 0; i < winner.getInventory().getSize(); i++) {
            winner.getInventory().setItem(i, new ItemStack(Material.STONE, 64));
        }

        new com.pvparena.game.Elimination(plugin, arena).endGame("red");

        assertEquals(5, pot.getItem(0).getAmount(),
                "a full winner inventory must not destroy unpaid wager items");
    }
}
