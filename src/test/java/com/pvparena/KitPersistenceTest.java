package com.pvparena;

import com.pvparena.arena.Arena;
import com.pvparena.arena.ArenaManager;
import com.pvparena.kit.KitConfig;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug 1: kits (which define the per-life cost) must survive a save/load cycle.
 */
class KitPersistenceTest {

    private ServerMock server;
    private PVPArenaPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(PVPArenaPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void kitAndCostSurviveReload() {
        ArenaManager manager = plugin.getArenaManager();
        Arena arena = manager.createArena("kittest");
        assertNotNull(arena);

        // Build a kit with a known item set (the item set IS the per-life cost)
        KitConfig kit = new KitConfig();
        kit.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        kit.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        kit.setMainHand(new ItemStack(Material.DIAMOND_SWORD));
        kit.addInventoryItem(new ItemStack(Material.GOLDEN_APPLE, 2));
        kit.addInventoryItem(new ItemStack(Material.COOKED_BEEF, 8));
        arena.setKitConfig(kit);

        String expectedCost = kit.getCostString();
        assertNotEquals("No cost defined", expectedCost);

        // Persist to disk
        manager.saveArena(arena);

        // Load from disk with a completely fresh manager
        ArenaManager fresh = new ArenaManager(plugin);
        fresh.loadArenas();

        Arena loaded = fresh.getArena("kittest");
        assertNotNull(loaded, "arena should load from disk");

        KitConfig loadedKit = loaded.getKitConfig();
        assertNotNull(loadedKit, "kit must survive the reload (flagship bug)");

        // Items survive
        assertNotNull(loadedKit.getHelmet());
        assertEquals(Material.DIAMOND_HELMET, loadedKit.getHelmet().getType());
        assertEquals(Material.IRON_LEGGINGS, loadedKit.getLeggings().getType());
        assertNotNull(loadedKit.getMainHand());
        assertEquals(Material.DIAMOND_SWORD, loadedKit.getMainHand().getType());

        // Inventory items (amounts drive the cost) survive
        assertEquals(2, loadedKit.getInventory().size());
        boolean foundApples = loadedKit.getInventory().stream()
                .anyMatch(i -> i.getType() == Material.GOLDEN_APPLE && i.getAmount() == 2);
        assertTrue(foundApples, "golden apples x2 should survive with amount intact");

        // The derived per-life cost string is identical after reload
        assertEquals(expectedCost, loadedKit.getCostString(),
                "the per-life cost must be identical after reload");
    }
}
