package com.pvparena;

import com.pvparena.stats.PlayerProfile;
import com.pvparena.stats.ProfileManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feature 2: lifetime stats are recorded, ranked, persisted, and survive a
 * reload with a fresh manager.
 */
class PersistentStatsTest {

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
    void recordingUpdatesStats() {
        ProfileManager profiles = plugin.getProfileManager();
        PlayerMock player = server.addPlayer("Ace");

        profiles.recordKill(player);
        profiles.recordKill(player);
        profiles.recordCapture(player);
        profiles.recordWin(player);
        profiles.recordDeath(player);
        profiles.recordGamePlayed(player);

        PlayerProfile profile = profiles.getProfile(player.getUniqueId());
        assertNotNull(profile, "profile should exist after recording");
        assertEquals(2, profile.getKills());
        assertEquals(1, profile.getCaptures());
        assertEquals(1, profile.getWins());
        assertEquals(1, profile.getDeaths());
        assertEquals(1, profile.getGamesPlayed());
    }

    @Test
    void leaderboardSortsDescending() {
        ProfileManager profiles = plugin.getProfileManager();
        PlayerMock low = server.addPlayer("Low");
        PlayerMock high = server.addPlayer("High");
        PlayerMock mid = server.addPlayer("Mid");

        for (int i = 0; i < 5; i++) profiles.recordKill(high);
        for (int i = 0; i < 3; i++) profiles.recordKill(mid);
        profiles.recordKill(low);

        List<PlayerProfile> top = profiles.getLeaderboard("kills", 10);
        assertEquals("High", top.get(0).getName(), "most kills should rank first");
        assertEquals("Mid", top.get(1).getName());
        assertEquals("Low", top.get(2).getName());
    }

    @Test
    void statsSurviveReloadWithFreshManager() {
        ProfileManager profiles = plugin.getProfileManager();
        PlayerMock player = server.addPlayer("Persistent");

        profiles.recordKill(player);
        profiles.recordKill(player);
        profiles.recordCapture(player);
        profiles.recordWin(player);
        // recording auto-saves; make the intent explicit as well
        profiles.save();

        // A brand-new manager must read the same numbers back off disk.
        ProfileManager fresh = new ProfileManager(plugin);
        PlayerProfile loaded = fresh.getProfile(player.getUniqueId());

        assertNotNull(loaded, "profile must load from disk with a fresh manager");
        assertEquals("Persistent", loaded.getName());
        assertEquals(2, loaded.getKills(), "kills must persist across reload");
        assertEquals(1, loaded.getCaptures(), "captures must persist across reload");
        assertEquals(1, loaded.getWins(), "wins must persist across reload");

        // Lookup by name works after reload too
        assertNotNull(fresh.getProfileByName("persistent"),
                "name lookup should be case-insensitive after reload");
    }
}
