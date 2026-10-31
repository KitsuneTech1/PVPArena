package com.pvparena;

import com.pvparena.arena.Arena;
import com.pvparena.game.FreeForAll;
import com.pvparena.game.GameModeType;
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
 * Bug 3: in FFA, a player who respawns with lives remaining must keep playing,
 * not be turned into a spectator. A player out of lives becomes a spectator.
 */
class FreeForAllRespawnTest {

    private ServerMock server;
    private PVPArenaPlugin plugin;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(PVPArenaPlugin.class);
        world = server.addSimpleWorld("ffa");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Arena newFfaArena(PlayerMock... players) {
        Arena arena = plugin.getArenaManager().createArena("ffatest" + System.nanoTime());
        arena.setGameModeType(GameModeType.FREE_FOR_ALL);
        arena.setLobbySpawn(new Location(world, 0, 64, 0)); // respawn target so teleport does not NPE
        for (PlayerMock p : players) {
            arena.getAllPlayers().add(p.getUniqueId());
        }
        return arena;
    }

    @Test
    void playerWithLivesRespawnsAndKeepsPlaying() {
        PlayerMock a = server.addPlayer("Alpha");
        PlayerMock b = server.addPlayer("Bravo");
        Arena arena = newFfaArena(a, b);

        FreeForAll game = new FreeForAll(plugin, arena);
        arena.setActiveGameMode(game);
        game.startGame();

        // Give Alpha lives to spare
        game.getTeamSupply().setPlayerLives(a, 2);

        game.handlePlayerRespawn(a);

        assertFalse(arena.isSpectator(a), "player with lives left must NOT be a spectator");
        assertTrue(arena.getAllPlayers().contains(a.getUniqueId()),
                "player with lives left must still be an active participant");
        assertFalse(game.isEliminated(a), "respawned player must not be marked eliminated");
    }

    @Test
    void playerOutOfLivesBecomesSpectator() {
        PlayerMock a = server.addPlayer("Alpha");
        PlayerMock b = server.addPlayer("Bravo");
        PlayerMock c = server.addPlayer("Charlie");
        Arena arena = newFfaArena(a, b, c);

        FreeForAll game = new FreeForAll(plugin, arena);
        arena.setActiveGameMode(game);
        game.startGame();

        // Alpha is out of lives; Bravo and Charlie still have theirs so the
        // match does not end when Alpha drops out.
        game.getTeamSupply().setPlayerLives(a, 0);
        game.getTeamSupply().setPlayerLives(b, 3);
        game.getTeamSupply().setPlayerLives(c, 3);

        game.handlePlayerRespawn(a);

        assertTrue(arena.isSpectator(a), "player out of lives must become a spectator");
        assertFalse(arena.getAllPlayers().contains(a.getUniqueId()),
                "spectator should no longer be an active participant");
    }
}
