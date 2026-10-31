package com.pvparena.kit;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a kit configuration for an arena match.
 * This defines what items players spawn with and thus defines the "cost per
 * life".
 */
public class KitConfig {

    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack mainHand;
    private ItemStack offHand;
    private final List<ItemStack> inventory;

    public KitConfig() {
        this.inventory = new ArrayList<>();
    }

    // Armor
    public ItemStack getHelmet() {
        return helmet;
    }

    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet;
    }

    public ItemStack getChestplate() {
        return chestplate;
    }

    public void setChestplate(ItemStack chestplate) {
        this.chestplate = chestplate;
    }

    public ItemStack getLeggings() {
        return leggings;
    }

    public void setLeggings(ItemStack leggings) {
        this.leggings = leggings;
    }

    public ItemStack getBoots() {
        return boots;
    }

    public void setBoots(ItemStack boots) {
        this.boots = boots;
    }

    // Hands
    public ItemStack getMainHand() {
        return mainHand;
    }

    public void setMainHand(ItemStack mainHand) {
        this.mainHand = mainHand;
    }

    public ItemStack getOffHand() {
        return offHand;
    }

    public void setOffHand(ItemStack offHand) {
        this.offHand = offHand;
    }

    // Inventory items (food, potions, etc.)
    public List<ItemStack> getInventory() {
        return inventory;
    }

    public void addInventoryItem(ItemStack item) {
        if (item != null) {
            inventory.add(item.clone());
        }
    }

    public void clearInventory() {
        inventory.clear();
    }

    /**
     * Gets all items in this kit as a single list (for cost calculation).
     */
    public List<ItemStack> getAllItems() {
        List<ItemStack> items = new ArrayList<>();
        if (helmet != null)
            items.add(helmet.clone());
        if (chestplate != null)
            items.add(chestplate.clone());
        if (leggings != null)
            items.add(leggings.clone());
        if (boots != null)
            items.add(boots.clone());
        if (mainHand != null)
            items.add(mainHand.clone());
        if (offHand != null)
            items.add(offHand.clone());
        for (ItemStack item : inventory) {
            if (item != null)
                items.add(item.clone());
        }
        return items;
    }

    /**
     * Generates a human-readable cost string for display.
     */
    public String getCostString() {
        List<ItemStack> items = getAllItems();
        if (items.isEmpty()) {
            return "No cost defined";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            sb.append(item.getAmount()).append("x ").append(formatItemName(item.getType().name()));
            if (i < items.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private String formatItemName(String name) {
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Creates a copy of this kit config.
     */
    public KitConfig clone() {
        KitConfig copy = new KitConfig();
        if (helmet != null)
            copy.setHelmet(helmet.clone());
        if (chestplate != null)
            copy.setChestplate(chestplate.clone());
        if (leggings != null)
            copy.setLeggings(leggings.clone());
        if (boots != null)
            copy.setBoots(boots.clone());
        if (mainHand != null)
            copy.setMainHand(mainHand.clone());
        if (offHand != null)
            copy.setOffHand(offHand.clone());
        for (ItemStack item : inventory) {
            copy.addInventoryItem(item);
        }
        return copy;
    }
}
