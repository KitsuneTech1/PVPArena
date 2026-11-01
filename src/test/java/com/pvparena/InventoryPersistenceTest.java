package com.pvparena;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryPersistenceTest {

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
    void restoringInventoryPreservesExperienceExactly() {
        PlayerMock player = server.addPlayer("ExperiencePlayer");
        player.setLevel(5);
        player.setExp(0.5f);

        int totalExperience = player.getTotalExperience();
        int level = player.getLevel();
        float progress = player.getExp();

        plugin.getInventoryManager().saveInventory(player);
        plugin.getInventoryManager().restoreInventory(player);

        assertEquals(totalExperience, player.getTotalExperience());
        assertEquals(level, player.getLevel());
        assertEquals(progress, player.getExp(), 0.0001f);
    }
}
