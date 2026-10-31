package com.pvparena;

import com.pvparena.arena.Arena;
import com.pvparena.game.CaptureTheFlag;
import com.pvparena.game.GameModeType;
import com.pvparena.scoreboard.SidebarState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feature 1: the live sidebar's data model reflects a capture and a life
 * change (the raw values pushed to the sidebar), and pushing to a real
 * scoreboard never breaks the match.
 */
class ScoreboardStateTest {

    private ServerMock server;
    private PVPArenaPlugin plugin;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(PVPArenaPlugin.class);
        world = server.addSimpleWorld("sb");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Arena newCtfArena(PlayerMock red, PlayerMock blue) {
        Arena arena = plugin.getArenaManager().createArena("sbarena");
        arena.setGameModeType(GameModeType.CAPTURE_THE_FLAG);
        arena.setLobbySpawn(new Location(world, 0, 64, 0));
        arena.addToRedTeam(red);
        arena.addToBlueTeam(blue);
        return arena;
    }

    @Test
    void stateReflectsCaptureAndLifeChange() {
        PlayerMock red = server.addPlayer("RedCarrier");
        PlayerMock blue = server.addPlayer("BlueDefender");
        Arena arena = newCtfArena(red, blue);

        CaptureTheFlag game = new CaptureTheFlag(plugin, arena);
        arena.setActiveGameMode(game);
        game.startGame();

        // Give both teams lives so the match keeps running.
        game.getTeamSupply().addTeamLives("red", 5);
        game.getTeamSupply().addTeamLives("blue", 5);

        // Initial snapshot: no captures, full lives.
        SidebarState before = SidebarState.build(plugin, arena, red);
        assertEquals("Capture The Flag", before.getModeName());
        assertTrue(before.isTeamMode());
        assertEquals(0, before.getRedScore(), "no captures yet");
        assertEquals(5, before.getOwnLives(), "red should see its 5 team lives");
        assertTrue(before.getLines().stream().anyMatch(l -> l.contains("Red")),
                "sidebar lines should include the team score");

        // Red captures the blue flag.
        game.tryPickupFlag(red, "blue");
        game.tryCapture(red);

        SidebarState afterCapture = SidebarState.build(plugin, arena, red);
        assertEquals(1, afterCapture.getRedScore(),
                "the sidebar model must reflect the capture");

        // A life is spent from red's supply.
        game.getTeamSupply().addTeamLives("red", -1);

        SidebarState afterLifeChange = SidebarState.build(plugin, arena, red);
        assertEquals(4, afterLifeChange.getOwnLives(),
                "the sidebar model must reflect the life change");
        // Capture value is unaffected by the life change.
        assertEquals(1, afterLifeChange.getRedScore());
    }

    @Test
    void pushingSidebarNeverThrowsAndCreatesObjectiveWhenSupported() {
        PlayerMock red = server.addPlayer("RedCarrier");
        PlayerMock blue = server.addPlayer("BlueDefender");
        Arena arena = newCtfArena(red, blue);

        CaptureTheFlag game = new CaptureTheFlag(plugin, arena);
        arena.setActiveGameMode(game);
        game.startGame();

        // Must not throw regardless of scoreboard support.
        assertDoesNotThrow(() -> plugin.getSidebarManager().update(arena, red));

        // If MockBukkit provides scoreboards, the sidebar objective should exist.
        if (Bukkit.getScoreboardManager() != null) {
            var board = red.getScoreboard();
            if (board != null && board != Bukkit.getScoreboardManager().getMainScoreboard()) {
                assertNotNull(board.getObjective("pvparena"),
                        "an active sidebar objective should be registered");
            }
        }

        // Clearing must also be safe.
        assertDoesNotThrow(() -> plugin.getSidebarManager().clear(red));
    }
}
