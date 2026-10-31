package com.pvparena;

import com.pvparena.config.MessageManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feature 3: user-facing strings resolve from messages.yml with color-code
 * translation, and a missing key falls back safely.
 */
class MessagesTest {

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
    void knownKeyResolvesWithColorTranslation() {
        MessageManager messages = plugin.getMessageManager();

        String leave = messages.get("leave.success");
        // '&a' should have been translated to the section-sign form.
        assertEquals("§aYou left the arena.", leave,
                "known key should resolve and translate & color codes");
        assertFalse(leave.contains("&"), "raw ampersand codes must be translated away");
    }

    @Test
    void placeholdersAreSubstituted() {
        MessageManager messages = plugin.getMessageManager();

        String joined = messages.get("join.success", Map.of("arena", "Colosseum"));
        assertTrue(joined.contains("Colosseum"), "placeholder {arena} should be filled in");
        assertFalse(joined.contains("{arena}"), "no unresolved placeholder should remain");

        String formatted = messages.format("stats.line", "stat", "Kills", "value", "7");
        assertTrue(formatted.contains("Kills") && formatted.contains("7"),
                "format() should fill both placeholder pairs");
    }

    @Test
    void missingKeyFallsBackSafely() {
        MessageManager messages = plugin.getMessageManager();

        String missing = messages.get("this.key.does.not.exist");
        assertNotNull(missing, "a missing key must never return null");
        assertEquals("this.key.does.not.exist", missing,
                "a missing key falls back to the key itself, not an exception");
    }

    @Test
    void messagesFileIsWrittenToDisk() {
        // The manager should have materialized a customizable messages.yml.
        java.io.File file = new java.io.File(plugin.getDataFolder(), "messages.yml");
        assertTrue(file.exists(), "messages.yml should be created on enable");
    }
}
