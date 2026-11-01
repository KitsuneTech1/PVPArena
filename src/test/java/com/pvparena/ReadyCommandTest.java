package com.pvparena;

import com.pvparena.arena.Arena;
import com.pvparena.arena.ArenaState;
import com.pvparena.commands.ArenaCommands;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug 4: /arena ready must actually mark the player ready (it was a stub).
 */
class ReadyCommandTest {

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
    void readyCommandMarksPlayerReady() {
        Arena arena = plugin.getArenaManager().createArena("readytest");
        arena.setState(ArenaState.LOBBY);

        PlayerMock player = server.addPlayer("Ready1");
        arena.addToRedTeam(player);

        assertFalse(arena.isReady(player), "player should not be ready before the command");

        ArenaCommands commands = new ArenaCommands(plugin);
        boolean handled = commands.onCommand(player, null, "arena", new String[] { "ready" });

        assertTrue(handled, "command should be handled");
        assertTrue(arena.isReady(player), "player must be marked ready after /arena ready");
        assertEquals(1, arena.getReadyCount());

        // Observable feedback to the player
        assertTrue(player.nextMessage().toLowerCase().contains("ready"),
                "player should be told they are ready");
    }

    @Test
    void readyCommandFailsWhenNotInArena() {
        PlayerMock player = server.addPlayer("Loner");

        ArenaCommands commands = new ArenaCommands(plugin);
        commands.onCommand(player, null, "arena", new String[] { "ready" });

        assertTrue(player.nextMessage().toLowerCase().contains("not in an arena"),
                "player not in an arena should be told so");
    }

    @Test
    void readyCommandRejectsSpectators() {
        Arena arena = plugin.getArenaManager().createArena("spectatorready");
        arena.setState(ArenaState.LOBBY);

        PlayerMock spectator = server.addPlayer("Spectator");
        arena.addSpectator(spectator);

        ArenaCommands commands = new ArenaCommands(plugin);
        boolean handled = commands.onCommand(spectator, null, "arena", new String[] { "ready" });

        assertTrue(handled, "command should be handled");
        assertFalse(arena.isReady(spectator), "spectators must not be marked ready");
        assertEquals(0, arena.getReadyCount(), "spectators must not affect ready count");
        assertTrue(spectator.nextMessage().toLowerCase().contains("spectator"),
                "spectators should be told they cannot ready up");
    }
}
