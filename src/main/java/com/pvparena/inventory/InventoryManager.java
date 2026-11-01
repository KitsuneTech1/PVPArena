package com.pvparena.inventory;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.kit.KitConfig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages saving and restoring player inventories when they enter/leave arenas.
 */
public class InventoryManager {

    private final PVPArenaPlugin plugin;
    private final Map<UUID, SavedInventory> savedInventories;

    public InventoryManager(PVPArenaPlugin plugin) {
        this.plugin = plugin;
        this.savedInventories = new HashMap<>();
    }

    /**
     * Saves a player's inventory and clears it for arena use.
     */
    public void saveInventory(Player player) {
        UUID uuid = player.getUniqueId();

        // Don't save if already saved (prevents overwriting)
        if (savedInventories.containsKey(uuid)) {
            return;
        }

        PlayerInventory inv = player.getInventory();

        SavedInventory saved = new SavedInventory();
        saved.contents = inv.getContents().clone();
        saved.armor = inv.getArmorContents().clone();
        saved.offHand = inv.getItemInOffHand().clone();
        saved.exp = player.getTotalExperience();
        saved.level = player.getLevel();
        saved.expProgress = player.getExp();
        saved.health = player.getHealth();
        saved.foodLevel = player.getFoodLevel();
        saved.saturation = player.getSaturation();

        savedInventories.put(uuid, saved);

        // Clear player inventory
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);

        // Reset stats
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    /**
     * Restores a player's saved inventory.
     */
    public void restoreInventory(Player player) {
        UUID uuid = player.getUniqueId();
        SavedInventory saved = savedInventories.remove(uuid);

        if (saved == null) {
            return;
        }

        PlayerInventory inv = player.getInventory();

        // Clear current inventory first
        inv.clear();

        // Restore saved inventory
        inv.setContents(saved.contents);
        inv.setArmorContents(saved.armor);
        inv.setItemInOffHand(saved.offHand);

        // Restore stats. The level, progress, and total are saved together;
        // adding total experience after setting the level would double-count it.
        player.setLevel(saved.level);
        player.setExp(saved.expProgress);
        player.setTotalExperience(saved.exp);
        player.setHealth(Math.min(saved.health, player.getMaxHealth()));
        player.setFoodLevel(saved.foodLevel);
        player.setSaturation(saved.saturation);
    }

    /**
     * Clears a player's inventory for arena entry (without saving).
     */
    public void clearForArena(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);

        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    /**
     * Grants a kit to a player.
     */
    public void grantKit(Player player, KitConfig kit) {
        if (kit == null)
            return;

        PlayerInventory inv = player.getInventory();

        // Armor
        if (kit.getHelmet() != null)
            inv.setHelmet(kit.getHelmet().clone());
        if (kit.getChestplate() != null)
            inv.setChestplate(kit.getChestplate().clone());
        if (kit.getLeggings() != null)
            inv.setLeggings(kit.getLeggings().clone());
        if (kit.getBoots() != null)
            inv.setBoots(kit.getBoots().clone());

        // Hands
        if (kit.getMainHand() != null)
            inv.setItemInMainHand(kit.getMainHand().clone());
        if (kit.getOffHand() != null)
            inv.setItemInOffHand(kit.getOffHand().clone());

        // Inventory items
        for (ItemStack item : kit.getInventory()) {
            if (item != null) {
                inv.addItem(item.clone());
            }
        }
    }

    /**
     * Restores all saved inventories (called on plugin disable).
     */
    public void restoreAllInventories() {
        for (UUID uuid : new java.util.ArrayList<>(savedInventories.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                restoreInventory(player);
            }
        }
        savedInventories.clear();
    }

    /**
     * Checks if a player has a saved inventory.
     */
    public boolean hasSavedInventory(Player player) {
        return savedInventories.containsKey(player.getUniqueId());
    }

    /**
     * Internal class to hold saved inventory data.
     */
    private static class SavedInventory {
        ItemStack[] contents;
        ItemStack[] armor;
        ItemStack offHand;
        int exp;
        int level;
        float expProgress;
        double health;
        int foodLevel;
        float saturation;
    }
}
