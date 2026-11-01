package com.pvparena.chest;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import com.pvparena.kit.KitConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Manages chest operations for arenas (Supply, Trash, Betting).
 */
public class ChestManager {

    private final PVPArenaPlugin plugin;

    public ChestManager(PVPArenaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets the inventory from a chest at a location.
     */
    public Inventory getChestInventory(Location location) {
        if (location == null)
            return null;

        Block block = location.getBlock();
        if (block.getState() instanceof Chest chest) {
            return chest.getInventory();
        }
        return null;
    }

    /**
     * Counts how many "lives" (full kit sets) are available in a supply chest.
     */
    public int countAvailableLives(Location chestLocation, KitConfig kit) {
        Inventory inv = getChestInventory(chestLocation);
        Map<Material, Integer> requiredItems = aggregateRequiredItems(kit);
        if (inv == null || requiredItems == null || requiredItems.isEmpty())
            return 0;

        // Count how many complete sets we can make
        long lives = Long.MAX_VALUE;

        for (Map.Entry<Material, Integer> required : requiredItems.entrySet()) {
            long available = countItem(inv, required.getKey());
            long neededPerLife = required.getValue();
            long possibleLives = available / neededPerLife;
            lives = Math.min(lives, possibleLives);
        }

        return lives == Long.MAX_VALUE ? 0 : (int) Math.min(lives, Integer.MAX_VALUE);
    }

    /**
     * Consumes items for one life from the supply chest.
     */
    public boolean consumeLifeFromChest(Location chestLocation, KitConfig kit) {
        Inventory inv = getChestInventory(chestLocation);
        Map<Material, Integer> requiredItems = aggregateRequiredItems(kit);
        if (inv == null || requiredItems == null || requiredItems.isEmpty())
            return false;

        // First verify we have enough
        for (Map.Entry<Material, Integer> required : requiredItems.entrySet()) {
            if (countItem(inv, required.getKey()) < required.getValue()) {
                return false;
            }
        }

        // Consume items
        for (Map.Entry<Material, Integer> required : requiredItems.entrySet()) {
            removeItems(inv, required.getKey(), required.getValue());
        }

        return true;
    }

    /**
     * Adds items to trash chest(s).
     */
    public void addToTrashChests(Arena arena, ItemStack[] items) {
        List<Location> trashChests = arena.getTrashChests();
        if (trashChests.isEmpty())
            return;

        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                remaining.add(item.clone());
            }
        }

        // Distribute items across trash chests
        for (Location chestLoc : trashChests) {
            if (remaining.isEmpty())
                break;

            Inventory inv = getChestInventory(chestLoc);
            if (inv == null)
                continue;

            Iterator<ItemStack> iter = remaining.iterator();
            while (iter.hasNext()) {
                ItemStack item = iter.next();
                HashMap<Integer, ItemStack> overflow = inv.addItem(item);
                if (overflow.isEmpty()) {
                    iter.remove();
                } else {
                    // Update remaining with overflow
                    item.setAmount(overflow.values().iterator().next().getAmount());
                }
            }
        }
    }

    /**
     * Awards the betting pot to the match winner(s) and empties the pot.
     * Pot items are handed out round-robin so multiple winners split the pot.
     * Returns the list of items that were awarded (empty if nothing to award).
     */
    public List<ItemStack> awardBettingPot(Arena arena, Collection<Player> winners) {
        List<ItemStack> awarded = new ArrayList<>();

        Location bettingChest = arena.getBettingChest();
        if (bettingChest == null || winners == null || winners.isEmpty()) {
            return awarded;
        }

        Inventory inv = getChestInventory(bettingChest);
        if (inv == null) {
            return awarded;
        }

        // Snapshot the pot contents
        List<ItemStack> pot = new ArrayList<>();
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                pot.add(item.clone());
            }
        }
        if (pot.isEmpty()) {
            return awarded;
        }

        // Hand out the pot, split round-robin across all winners
        List<Player> winnerList = new ArrayList<>(winners);
        List<ItemStack> leftovers = new ArrayList<>();
        int index = 0;
        for (ItemStack item : pot) {
            Player winner = winnerList.get(index % winnerList.size());
            int originalAmount = item.getAmount();
            Map<Integer, ItemStack> overflow = winner.getInventory().addItem(item.clone());
            int overflowAmount = 0;
            for (ItemStack leftover : overflow.values()) {
                overflowAmount += leftover.getAmount();
                leftovers.add(leftover.clone());
            }
            int deliveredAmount = originalAmount - overflowAmount;
            if (deliveredAmount > 0) {
                ItemStack delivered = item.clone();
                delivered.setAmount(deliveredAmount);
                awarded.add(delivered);
            }
            index++;
        }

        // Remove only the paid-out contents. Any overflow remains in the pot.
        inv.clear();
        for (ItemStack leftover : leftovers) {
            inv.addItem(leftover);
        }

        return awarded;
    }

    /**
     * Gets a formatted string of the betting pot contents.
     */
    public String getBettingPotDisplay(Arena arena) {
        Location bettingChest = arena.getBettingChest();
        if (bettingChest == null) {
            return "§7No betting chest set";
        }

        Inventory inv = getChestInventory(bettingChest);
        if (inv == null) {
            return "§7Betting chest not found";
        }

        Map<Material, Integer> items = new HashMap<>();
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }

        if (items.isEmpty()) {
            return "§7Empty pot";
        }

        StringBuilder sb = new StringBuilder("§6🏆 Winner's Pot: ");
        int count = 0;
        for (Map.Entry<Material, Integer> entry : items.entrySet()) {
            if (count > 0)
                sb.append("§7, ");
            sb.append("§e").append(entry.getValue()).append("x ");
            sb.append("§f").append(formatMaterialName(entry.getKey()));
            count++;
            if (count >= 5) {
                sb.append("§7...");
                break;
            }
        }

        return sb.toString();
    }

    /**
     * Counts total items of a specific type in an inventory.
     */
    private long countItem(Inventory inv, Material material) {
        long count = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Removes a specific amount of items from an inventory.
     */
    private void removeItems(Inventory inv, Material material, int amount) {
        int remaining = amount;

        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    inv.setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
            }
        }
    }

    private Map<Material, Integer> aggregateRequiredItems(KitConfig kit) {
        if (kit == null) {
            return null;
        }

        Map<Material, Integer> required = new EnumMap<>(Material.class);
        for (ItemStack item : kit.getAllItems()) {
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                return null;
            }
            long total = (long) required.getOrDefault(item.getType(), 0) + item.getAmount();
            if (total > Integer.MAX_VALUE) {
                return null;
            }
            required.put(item.getType(), (int) total);
        }
        return required;
    }

    /**
     * Formats a material name for display.
     */
    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }
        return sb.toString().trim();
    }
}
