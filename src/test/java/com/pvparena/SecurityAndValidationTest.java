package com.pvparena;

import com.pvparena.arena.Arena;
import com.pvparena.arena.ArenaManager;
import com.pvparena.arena.ArenaState;
import com.pvparena.commands.ArenaCommands;
import org.bukkit.Location;
import org.bukkit.permissions.PermissionAttachment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAndValidationTest {

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
    void arenaNamesCannotEscapeTheArenaDirectory() {
        ArenaManager manager = plugin.getArenaManager();

        assertNull(manager.createArena("../outside"));
        assertNull(manager.createArena("C:\\outside"));
        assertNull(manager.getArena("../outside"));
        assertFalse(manager.arenaExists("../outside"));
    }

    @Test
    void activeArenasCannotBeDeletedAndOrphanPlayers() {
        Arena arena = plugin.getArenaManager().createArena("activearena");
        PlayerMock player = server.addPlayer("Participant");
        arena.addToRedTeam(player);
        arena.setState(ArenaState.ACTIVE);

        assertFalse(plugin.getArenaManager().deleteArena("activearena"));
        assertSame(arena, plugin.getArenaManager().getArena("activearena"));
    }

    @Test
    void joinPermissionIsActuallyEnforced() {
        Arena arena = plugin.getArenaManager().createArena("permissionarena");
        PlayerMock player = server.addPlayer("Denied");
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachment.setPermission("pvparena.join", false);

        new ArenaCommands(plugin).onCommand(player, null, "arena", new String[] { "join", arena.getName() });

        assertFalse(arena.isInArena(player));
        assertTrue(player.nextMessage().toLowerCase().contains("permission"));
    }

    @Test
    void timingConfigurationIsBounded() {
        Arena arena = plugin.getArenaManager().createArena("timingarena");

        arena.setRespawnTime(-1);
        arena.setCountdownTime(Integer.MAX_VALUE);

        assertEquals(0, arena.getRespawnTime());
        assertEquals(Arena.MAX_CONFIG_SECONDS, arena.getCountdownTime());
    }

    @Test
    void savingALocationWithoutAWorldDoesNotCrash() {
        Arena arena = plugin.getArenaManager().createArena("nullworldarena");
        arena.setLobbySpawn(new Location(null, 0, 0, 0));

        assertDoesNotThrow(() -> plugin.getArenaManager().saveArena(arena));
    }
}
