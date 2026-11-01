package com.pvparena;

import com.pvparena.arena.Arena;
import com.pvparena.commands.ArenaCommands;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class ArenaJoinTest {

    private ServerMock server;
    private PVPArenaPlugin plugin;
    private ArenaCommands commands;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(PVPArenaPlugin.class);
        commands = new ArenaCommands(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void joinRejectsPlayersAfterArenaReachesCapacity() {
        Arena arena = plugin.getArenaManager().createArena("capacity");
        arena.setMaxPlayers(1);

        PlayerMock first = server.addPlayer("First");
        PlayerMock second = server.addPlayer("Second");
        second.getInventory().addItem(new ItemStack(Material.DIAMOND));

        commands.onCommand(first, null, "arena", new String[] { "join", "capacity" });
        commands.onCommand(second, null, "arena", new String[] { "join", "capacity" });

        assertEquals(1, arena.getPlayerCount());
        assertFalse(arena.isInArena(second), "a full arena must not accept another player");
        assertEquals(Material.DIAMOND, second.getInventory().getItem(0).getType(),
                "a rejected join must not clear the player's inventory");
        assertTrue(second.nextMessage().contains("full"));
    }
}
