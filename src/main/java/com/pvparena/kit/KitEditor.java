package com.pvparena.kit;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * GUI for editing arena loadouts/kits.
 */
public class KitEditor implements Listener {

    private final PVPArenaPlugin plugin;
    private final Player player;
    private final Arena arena;
    private final Inventory inventory;

    // Slot assignments
    private static final int HELMET_SLOT = 0;
    private static final int CHESTPLATE_SLOT = 1;
    private static final int LEGGINGS_SLOT = 2;
    private static final int BOOTS_SLOT = 3;
    private static final int WEAPON_SLOT = 9;
    private static final int OFFHAND_SLOT = 10;
    private static final int FOOD_START_SLOT = 18;
    private static final int CONFIRM_SLOT = 35;
    private static final int CLEAR_SLOT = 26;

    public KitEditor(PVPArenaPlugin plugin, Player player, Arena arena) {
        this.plugin = plugin;
        this.player = player;
        this.arena = arena;
        this.inventory = Bukkit.createInventory(null, 36, "§6Kit Editor - " + arena.getName());

        setupInventory();
    }

    /**
     * Sets up the initial inventory display.
     */
    private void setupInventory() {
        // Fill with glass panes for organization
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        // Armor slots (row 1)
        inventory.setItem(HELMET_SLOT, createItem(Material.LEATHER_HELMET, "§eHelmet", "§7Click with item to set"));
        inventory.setItem(CHESTPLATE_SLOT,
                createItem(Material.LEATHER_CHESTPLATE, "§eChestplate", "§7Click with item to set"));
        inventory.setItem(LEGGINGS_SLOT,
                createItem(Material.LEATHER_LEGGINGS, "§eLeggings", "§7Click with item to set"));
        inventory.setItem(BOOTS_SLOT, createItem(Material.LEATHER_BOOTS, "§eBoots", "§7Click with item to set"));

        // Weapon slots (row 2)
        inventory.setItem(WEAPON_SLOT, createItem(Material.WOODEN_SWORD, "§eMain Weapon", "§7Click with item to set"));
        inventory.setItem(OFFHAND_SLOT, createItem(Material.SHIELD, "§eOffhand", "§7Click with item to set"));

        // Food/consumables (row 3)
        for (int i = 0; i < 5; i++) {
            inventory.setItem(FOOD_START_SLOT + i,
                    createItem(Material.BREAD, "§eConsumable " + (i + 1), "§7Click with item to set"));
        }

        // Control buttons
        inventory.setItem(CLEAR_SLOT, createItem(Material.BARRIER, "§c§lClear Kit", "§7Remove all items"));
        inventory.setItem(CONFIRM_SLOT, createItem(Material.LIME_WOOL, "§a§lConfirm Kit", "§7Save and close"));

        // Load existing kit if present
        loadExistingKit();
    }

    /**
     * Loads an existing kit configuration into the editor.
     */
    private void loadExistingKit() {
        KitConfig kit = arena.getKitConfig();
        if (kit == null)
            return;

        if (kit.getHelmet() != null)
            inventory.setItem(HELMET_SLOT, kit.getHelmet().clone());
        if (kit.getChestplate() != null)
            inventory.setItem(CHESTPLATE_SLOT, kit.getChestplate().clone());
        if (kit.getLeggings() != null)
            inventory.setItem(LEGGINGS_SLOT, kit.getLeggings().clone());
        if (kit.getBoots() != null)
            inventory.setItem(BOOTS_SLOT, kit.getBoots().clone());
        if (kit.getMainHand() != null)
            inventory.setItem(WEAPON_SLOT, kit.getMainHand().clone());
        if (kit.getOffHand() != null)
            inventory.setItem(OFFHAND_SLOT, kit.getOffHand().clone());

        List<ItemStack> inv = kit.getInventory();
        for (int i = 0; i < Math.min(5, inv.size()); i++) {
            if (inv.get(i) != null) {
                inventory.setItem(FOOD_START_SLOT + i, inv.get(i).clone());
            }
        }

        updateCostDisplay();
    }

    /**
     * Opens the kit editor GUI.
     */
    public void open() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory))
            return;
        if (!(event.getWhoClicked() instanceof Player clicker))
            return;
        if (!clicker.equals(player))
            return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        ItemStack cursor = event.getCursor();

        // Confirm button
        if (slot == CONFIRM_SLOT) {
            saveKit();
            player.closeInventory();
            player.sendMessage("§aKit saved!");
            return;
        }

        // Clear button
        if (slot == CLEAR_SLOT) {
            clearKit();
            player.sendMessage("§cKit cleared!");
            return;
        }

        // Equipment slots
        if (isEquipmentSlot(slot)) {
            if (cursor != null && cursor.getType() != Material.AIR) {
                // Set item
                inventory.setItem(slot, cursor.clone());
                updateCostDisplay();
            } else {
                // Clear slot
                setPlaceholder(slot);
                updateCostDisplay();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory))
            return;
        if (!event.getPlayer().equals(player))
            return;

        HandlerList.unregisterAll(this);
    }

    /**
     * Checks if a slot is an equipment slot.
     */
    private boolean isEquipmentSlot(int slot) {
        return slot == HELMET_SLOT || slot == CHESTPLATE_SLOT ||
                slot == LEGGINGS_SLOT || slot == BOOTS_SLOT ||
                slot == WEAPON_SLOT || slot == OFFHAND_SLOT ||
                (slot >= FOOD_START_SLOT && slot < FOOD_START_SLOT + 5);
    }

    /**
     * Sets a placeholder item for an equipment slot.
     */
    private void setPlaceholder(int slot) {
        switch (slot) {
            case HELMET_SLOT ->
                inventory.setItem(slot, createItem(Material.LEATHER_HELMET, "§eHelmet", "§7Click with item to set"));
            case CHESTPLATE_SLOT -> inventory.setItem(slot,
                    createItem(Material.LEATHER_CHESTPLATE, "§eChestplate", "§7Click with item to set"));
            case LEGGINGS_SLOT -> inventory.setItem(slot,
                    createItem(Material.LEATHER_LEGGINGS, "§eLeggings", "§7Click with item to set"));
            case BOOTS_SLOT ->
                inventory.setItem(slot, createItem(Material.LEATHER_BOOTS, "§eBoots", "§7Click with item to set"));
            case WEAPON_SLOT ->
                inventory.setItem(slot, createItem(Material.WOODEN_SWORD, "§eMain Weapon", "§7Click with item to set"));
            case OFFHAND_SLOT ->
                inventory.setItem(slot, createItem(Material.SHIELD, "§eOffhand", "§7Click with item to set"));
            default -> {
                if (slot >= FOOD_START_SLOT && slot < FOOD_START_SLOT + 5) {
                    inventory.setItem(slot, createItem(Material.BREAD, "§eConsumable " + (slot - FOOD_START_SLOT + 1),
                            "§7Click with item to set"));
                }
            }
        }
    }

    /**
     * Clears all kit items.
     */
    private void clearKit() {
        for (int slot : new int[] { HELMET_SLOT, CHESTPLATE_SLOT, LEGGINGS_SLOT, BOOTS_SLOT, WEAPON_SLOT,
                OFFHAND_SLOT }) {
            setPlaceholder(slot);
        }
        for (int i = 0; i < 5; i++) {
            setPlaceholder(FOOD_START_SLOT + i);
        }
        updateCostDisplay();
    }

    /**
     * Saves the current kit to the arena.
     */
    private void saveKit() {
        KitConfig kit = new KitConfig();

        ItemStack helmet = inventory.getItem(HELMET_SLOT);
        if (helmet != null && !isPlaceholder(helmet))
            kit.setHelmet(helmet.clone());

        ItemStack chest = inventory.getItem(CHESTPLATE_SLOT);
        if (chest != null && !isPlaceholder(chest))
            kit.setChestplate(chest.clone());

        ItemStack legs = inventory.getItem(LEGGINGS_SLOT);
        if (legs != null && !isPlaceholder(legs))
            kit.setLeggings(legs.clone());

        ItemStack boots = inventory.getItem(BOOTS_SLOT);
        if (boots != null && !isPlaceholder(boots))
            kit.setBoots(boots.clone());

        ItemStack weapon = inventory.getItem(WEAPON_SLOT);
        if (weapon != null && !isPlaceholder(weapon))
            kit.setMainHand(weapon.clone());

        ItemStack offhand = inventory.getItem(OFFHAND_SLOT);
        if (offhand != null && !isPlaceholder(offhand))
            kit.setOffHand(offhand.clone());

        for (int i = 0; i < 5; i++) {
            ItemStack item = inventory.getItem(FOOD_START_SLOT + i);
            if (item != null && !isPlaceholder(item)) {
                kit.addInventoryItem(item.clone());
            }
        }

        arena.setKitConfig(kit);
        plugin.getArenaManager().saveArena(arena);
    }

    /**
     * Checks if an item is a placeholder.
     */
    private boolean isPlaceholder(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore())
            return false;
        List<String> lore = meta.getLore();
        return lore != null && lore.contains("§7Click with item to set");
    }

    /**
     * Updates the cost display on the confirm button.
     */
    private void updateCostDisplay() {
        KitConfig tempKit = new KitConfig();

        // Build temporary kit from current slots
        ItemStack helmet = inventory.getItem(HELMET_SLOT);
        if (helmet != null && !isPlaceholder(helmet))
            tempKit.setHelmet(helmet);

        ItemStack chest = inventory.getItem(CHESTPLATE_SLOT);
        if (chest != null && !isPlaceholder(chest))
            tempKit.setChestplate(chest);

        ItemStack legs = inventory.getItem(LEGGINGS_SLOT);
        if (legs != null && !isPlaceholder(legs))
            tempKit.setLeggings(legs);

        ItemStack boots = inventory.getItem(BOOTS_SLOT);
        if (boots != null && !isPlaceholder(boots))
            tempKit.setBoots(boots);

        ItemStack weapon = inventory.getItem(WEAPON_SLOT);
        if (weapon != null && !isPlaceholder(weapon))
            tempKit.setMainHand(weapon);

        ItemStack offhand = inventory.getItem(OFFHAND_SLOT);
        if (offhand != null && !isPlaceholder(offhand))
            tempKit.setOffHand(offhand);

        for (int i = 0; i < 5; i++) {
            ItemStack item = inventory.getItem(FOOD_START_SLOT + i);
            if (item != null && !isPlaceholder(item)) {
                tempKit.addInventoryItem(item);
            }
        }

        String costStr = tempKit.getCostString();

        ItemStack confirmBtn = createItem(Material.LIME_WOOL, "§a§lConfirm Kit",
                "§7Cost per life:", "§e" + costStr);
        inventory.setItem(CONFIRM_SLOT, confirmBtn);
    }

    /**
     * Creates an item with name and lore.
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }
}
