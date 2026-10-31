package com.pvparena;

import com.pvparena.arena.ArenaManager;
import com.pvparena.chest.ChestManager;
import com.pvparena.commands.ArenaCommands;
import com.pvparena.config.MessageManager;
import com.pvparena.inventory.InventoryManager;
import com.pvparena.listeners.PlayerDeathListener;
import com.pvparena.listeners.PlayerMoveListener;
import com.pvparena.listeners.FlagListener;
import com.pvparena.scoreboard.SidebarManager;
import com.pvparena.stats.ProfileManager;
import com.pvparena.stats.StatsManager;
import com.pvparena.util.SoundManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PVPArenaPlugin extends JavaPlugin {

    private static PVPArenaPlugin instance;

    private ArenaManager arenaManager;
    private ChestManager chestManager;
    private InventoryManager inventoryManager;
    private StatsManager statsManager;
    private ProfileManager profileManager;
    private SoundManager soundManager;
    private SidebarManager sidebarManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        this.messageManager = new MessageManager(this);
        this.arenaManager = new ArenaManager(this);
        this.chestManager = new ChestManager(this);
        this.inventoryManager = new InventoryManager(this);
        this.statsManager = new StatsManager(this);
        this.profileManager = new ProfileManager(this);
        this.soundManager = new SoundManager();
        this.sidebarManager = new SidebarManager(this);
        
        // Load saved arenas
        arenaManager.loadArenas();
        
        // Register commands
        getCommand("arena").setExecutor(new ArenaCommands(this));
        getCommand("arena").setTabCompleter(new ArenaCommands(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new FlagListener(this), this);
        
        getLogger().info("PVPArena has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all arenas
        if (arenaManager != null) {
            arenaManager.saveArenas();
        }

        // Flush persistent player stats
        if (profileManager != null) {
            profileManager.save();
        }

        // Restore all player inventories (in case of reload/shutdown during game)
        if (inventoryManager != null) {
            inventoryManager.restoreAllInventories();
        }
        
        getLogger().info("PVPArena has been disabled!");
    }

    public static PVPArenaPlugin getInstance() {
        return instance;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public ChestManager getChestManager() {
        return chestManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public SidebarManager getSidebarManager() {
        return sidebarManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}
