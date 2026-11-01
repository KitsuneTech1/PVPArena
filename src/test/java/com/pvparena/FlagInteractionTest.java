package com.pvparena;

import com.pvparena.arena.Arena;
import com.pvparena.game.CaptureTheFlag;
import com.pvparena.listeners.FlagListener;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class FlagInteractionTest {

    private ServerMock server;
    private PVPArenaPlugin plugin;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(PVPArenaPlugin.class);
        world = server.addSimpleWorld("flags");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void breakingOwnFlagDoesNotDestroyIt() {
        PlayerMock red = server.addPlayer("Red");
        PlayerMock blue = server.addPlayer("Blue");
        Arena arena = plugin.getArenaManager().createArena("flagarena");
        Location redFlag = new Location(world, 0, 64, 0);
        Location blueFlag = new Location(world, 10, 64, 0);
        arena.setRedFlagLocation(redFlag);
        arena.setBlueFlagLocation(blueFlag);
        arena.addToRedTeam(red);
        arena.addToBlueTeam(blue);

        CaptureTheFlag game = new CaptureTheFlag(plugin, arena);
        arena.setActiveGameMode(game);
        game.startGame();

        blueFlag.getBlock().setType(Material.BLUE_BANNER);
        BlockBreakEvent event = new BlockBreakEvent(blueFlag.getBlock(), blue);
        new FlagListener(plugin).onBlockBreak(event);

        assertTrue(event.isCancelled());
        assertEquals(Material.BLUE_BANNER, blueFlag.getBlock().getType(),
                "a team must not be able to destroy its own flag");
        assertFalse(game.isCarryingFlag(blue));
    }
}
