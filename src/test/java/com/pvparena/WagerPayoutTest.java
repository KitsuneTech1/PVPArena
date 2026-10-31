package com.pvparena;

import com.pvparena.arena.Arena;
import com.pvparena.game.Elimination;
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

/**
 * Bug 2: when a match ends with a winner, the wager pot must be paid out to the
 * winner and the pot emptied.
 */
class WagerPayoutTest {

    private ServerMock server;
    private PVPArenaPlugin plugin;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(PVPArenaPlugin.class);
        world = server.addSimpleWorld("wager");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Inventory seedBettingChest(Arena arena, int x, int z) {
        Location loc = new Location(world, x, 64, z);
        Block block = loc.getBlock();
        block.setType(Material.CHEST);
        arena.setBettingChest(loc);
        Chest chest = (Chest) block.getState();
        return chest.getInventory();
    }

    private int countInInventory(Inventory inv, Material type) {
        int count = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == type) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @Test
    void winnerReceivesPotAndPotIsEmptied() {
        Arena arena = plugin.getArenaManager().createArena("wagerarena");

        PlayerMock red = server.addPlayer("RedWinner");
        PlayerMock blue = server.addPlayer("BlueLoser");
        arena.addToRedTeam(red);
        arena.addToBlueTeam(blue);

        // Seed the pot with a known set of items
        Inventory pot = seedBettingChest(arena, 10, 10);
        pot.addItem(new ItemStack(Material.DIAMOND, 5));
        pot.addItem(new ItemStack(Material.GOLD_INGOT, 12));

        Elimination game = new Elimination(plugin, arena);
        arena.setActiveGameMode(game);

        // Winner's inventory starts without those items
        assertEquals(0, countInInventory(red.getInventory(), Material.DIAMOND));

        // Match ends with red as the winner
        game.endGame("red");

        // Winner received the whole pot
        assertEquals(5, countInInventory(red.getInventory(), Material.DIAMOND),
                "winner should receive the pot's diamonds");
        assertEquals(12, countInInventory(red.getInventory(), Material.GOLD_INGOT),
                "winner should receive the pot's gold");

        // Loser received nothing from the pot
        assertEquals(0, countInInventory(blue.getInventory(), Material.DIAMOND),
                "loser should not receive pot items");

        // Pot is now empty
        Chest chest = (Chest) arena.getBettingChest().getBlock().getState();
        assertEquals(0, countInInventory(chest.getInventory(), Material.DIAMOND),
                "pot should be emptied after payout");
        assertEquals(0, countInInventory(chest.getInventory(), Material.GOLD_INGOT),
                "pot should be emptied after payout");
    }
}
