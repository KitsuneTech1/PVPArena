package com.pvparena.game;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Capture The Flag game mode implementation.
 */
public class CaptureTheFlag extends GameMode {

    private static final int CAPTURES_TO_WIN = 3;

    private int redScore;
    private int blueScore;

    // Flag carriers (player UUID -> which flag they carry: "red" or "blue")
    private final Map<UUID, String> flagCarriers;

    // Flag status
    private boolean redFlagAtBase;
    private boolean blueFlagAtBase;
    private Location droppedRedFlag;
    private Location droppedBlueFlag;

    public CaptureTheFlag(PVPArenaPlugin plugin, Arena arena) {
        super(plugin, arena);
        this.flagCarriers = new HashMap<>();
        this.redScore = 0;
        this.blueScore = 0;
        this.redFlagAtBase = true;
        this.blueFlagAtBase = true;
    }

    @Override
    protected void onGameStart() {
        // Place flags at their initial positions
        placeFlag(arena.getRedFlagLocation(), "red");
        placeFlag(arena.getBlueFlagLocation(), "blue");

        // Announce game start
        for (Player player : getActivePlayers()) {
            player.sendMessage("§6§l=== CAPTURE THE FLAG ===");
            player.sendMessage("§7Capture the enemy flag " + CAPTURES_TO_WIN + " times to win!");
            player.sendMessage("§7Flag carriers get §cSlowness §7and §eGlowing§7!");
        }
    }

    @Override
    protected void onPlayerDeath(Player victim, Player killer) {
        // Check if victim was carrying a flag
        String carriedFlag = flagCarriers.remove(victim.getUniqueId());
        if (carriedFlag != null) {
            // Drop the flag at death location
            dropFlag(carriedFlag, victim.getLocation());

            // Remove carrier effects
            victim.removePotionEffect(PotionEffectType.SLOWNESS);
            victim.removePotionEffect(PotionEffectType.GLOWING);

            // Announce
            String color = carriedFlag.equals("red") ? "§c" : "§9";
            for (Player p : getActivePlayers()) {
                p.sendMessage(
                        color + victim.getName() + " §7dropped the " + color + carriedFlag.toUpperCase() + " §7flag!");
            }
        }
    }

    @Override
    protected void onPlayerRespawn(Player player) {
        // Nothing special needed
    }

    @Override
    protected void onGameEnd(String winningTeam) {
        // Remove any placed flags
        removeFlag(arena.getRedFlagLocation());
        removeFlag(arena.getBlueFlagLocation());

        if (droppedRedFlag != null)
            removeFlag(droppedRedFlag);
        if (droppedBlueFlag != null)
            removeFlag(droppedBlueFlag);
    }

    @Override
    public void checkWinCondition() {
        if (redScore >= CAPTURES_TO_WIN) {
            endGame("red");
        } else if (blueScore >= CAPTURES_TO_WIN) {
            endGame("blue");
        }

        // Also check if a team has no lives and no players
        if (!teamSupply.hasLivesRemaining("red") && getTeamPlayerCount("red") == 0) {
            endGame("blue");
        } else if (!teamSupply.hasLivesRemaining("blue") && getTeamPlayerCount("blue") == 0) {
            endGame("red");
        }
    }

    @Override
    public String getDisplayName() {
        return "Capture The Flag";
    }

    /**
     * Called when a player tries to pick up a flag.
     */
    public void tryPickupFlag(Player player, String flagTeam) {
        String playerTeam = arena.getTeam(player);
        if (playerTeam == null)
            return;

        // Can only pick up enemy flag
        if (playerTeam.equals(flagTeam)) {
            player.sendMessage("§cYou can't pick up your own flag!");
            return;
        }

        // Already carrying a flag?
        if (flagCarriers.containsKey(player.getUniqueId())) {
            return;
        }

        // Pick up the flag
        flagCarriers.put(player.getUniqueId(), flagTeam);

        if (flagTeam.equals("red")) {
            redFlagAtBase = false;
            droppedRedFlag = null;
        } else {
            blueFlagAtBase = false;
            droppedBlueFlag = null;
        }

        // Apply carrier effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));

        // Give flag item (banner on head)
        ItemStack flagItem = createFlagItem(flagTeam);
        player.getInventory().setHelmet(flagItem);

        // Play sound and announce
        plugin.getSoundManager().playFlagPickup(player);

        String color = flagTeam.equals("red") ? "§c" : "§9";
        for (Player p : getActivePlayers()) {
            p.sendMessage(color + player.getName() + " §7picked up the " + color + flagTeam.toUpperCase() + " §7flag!");
        }
    }

    /**
     * Called when a player enters their capture zone while carrying a flag.
     */
    public void tryCapture(Player player) {
        String carriedFlag = flagCarriers.get(player.getUniqueId());
        if (carriedFlag == null)
            return;

        String playerTeam = arena.getTeam(player);
        if (playerTeam == null)
            return;

        // Check if player's own flag is at base (needed to capture in most CTF rules)
        boolean canCapture = (playerTeam.equals("red") && redFlagAtBase)
                || (playerTeam.equals("blue") && blueFlagAtBase);

        if (!canCapture) {
            player.sendMessage("§cYour flag must be at your base to capture!");
            return;
        }

        // Capture!
        flagCarriers.remove(player.getUniqueId());

        // Remove effects
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.GLOWING);

        // Remove banner from head
        player.getInventory().setHelmet(null);

        // Update score
        if (playerTeam.equals("red")) {
            redScore++;
        } else {
            blueScore++;
        }

        // Return captured flag to its base
        if (carriedFlag.equals("red")) {
            placeFlag(arena.getRedFlagLocation(), "red");
            redFlagAtBase = true;
        } else {
            placeFlag(arena.getBlueFlagLocation(), "blue");
            blueFlagAtBase = true;
        }

        // Record stat (per-match display + lifetime profile)
        plugin.getStatsManager().recordCapture(arena, player);
        plugin.getProfileManager().recordCapture(player);

        // Play sounds and announce
        plugin.getSoundManager().playFlagCapture(player.getLocation());

        com.pvparena.config.MessageManager messages = plugin.getMessageManager();
        String teamColor = playerTeam.equals("red") ? "§c" : "§9";
        String captured = messages.format("ctf.captured", "color", teamColor, "player", player.getName());
        String scoreLine = messages.format("ctf.score",
                "red", String.valueOf(redScore), "blue", String.valueOf(blueScore));
        for (Player p : getActivePlayers()) {
            p.sendMessage(captured);
            p.sendMessage(scoreLine);
        }

        // Reflect the capture in the live sidebar
        plugin.getSidebarManager().updateAll(arena);

        checkWinCondition();
    }

    /**
     * Called when a player returns their team's flag.
     */
    public void tryReturnFlag(Player player, String flagTeam) {
        String playerTeam = arena.getTeam(player);
        if (playerTeam == null || !playerTeam.equals(flagTeam))
            return;

        // Can only return own team's flag
        Location droppedLoc = flagTeam.equals("red") ? droppedRedFlag : droppedBlueFlag;
        if (droppedLoc == null)
            return;

        // Return flag to base
        if (flagTeam.equals("red")) {
            removeFlag(droppedRedFlag);
            placeFlag(arena.getRedFlagLocation(), "red");
            redFlagAtBase = true;
            droppedRedFlag = null;
        } else {
            removeFlag(droppedBlueFlag);
            placeFlag(arena.getBlueFlagLocation(), "blue");
            blueFlagAtBase = true;
            droppedBlueFlag = null;
        }

        String color = flagTeam.equals("red") ? "§c" : "§9";
        for (Player p : getActivePlayers()) {
            p.sendMessage(color + player.getName() + " §7returned the " + color + flagTeam.toUpperCase() + " §7flag!");
        }
    }

    /**
     * Drops a flag at a location.
     */
    private void dropFlag(String team, Location location) {
        placeFlag(location, team);
        plugin.getSoundManager().playFlagDrop(location);

        if (team.equals("red")) {
            droppedRedFlag = location;
        } else {
            droppedBlueFlag = location;
        }
    }

    /**
     * Places a banner flag at a location.
     */
    private void placeFlag(Location location, String team) {
        if (location == null)
            return;

        Block block = location.getBlock();
        Material bannerType = team.equals("red") ? Material.RED_BANNER : Material.BLUE_BANNER;
        block.setType(bannerType);
    }

    /**
     * Removes a banner flag from a location.
     */
    private void removeFlag(Location location) {
        if (location == null)
            return;
        location.getBlock().setType(Material.AIR);
    }

    /**
     * Creates a flag item for a player to wear.
     */
    private ItemStack createFlagItem(String team) {
        Material material = team.equals("red") ? Material.RED_BANNER : Material.BLUE_BANNER;
        ItemStack item = new ItemStack(material);
        BannerMeta meta = (BannerMeta) item.getItemMeta();
        meta.setDisplayName((team.equals("red") ? "§c" : "§9") + team.toUpperCase() + " FLAG");
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Gets player count for a team.
     */
    private int getTeamPlayerCount(String team) {
        int count = 0;
        for (Player player : getActivePlayers()) {
            if (team.equals(arena.getTeam(player))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Checks if a player is carrying a flag.
     */
    public boolean isCarryingFlag(Player player) {
        return flagCarriers.containsKey(player.getUniqueId());
    }

    /**
     * Gets the flag a player is carrying.
     */
    public String getCarriedFlag(Player player) {
        return flagCarriers.get(player.getUniqueId());
    }

    /**
     * Gets the current score for a team.
     */
    public int getScore(String team) {
        return team.equals("red") ? redScore : blueScore;
    }

    /**
     * Checks if a flag is at base.
     */
    public boolean isFlagAtBase(String team) {
        return team.equals("red") ? redFlagAtBase : blueFlagAtBase;
    }

    /**
     * Gets dropped flag location.
     */
    public Location getDroppedFlagLocation(String team) {
        return team.equals("red") ? droppedRedFlag : droppedBlueFlag;
    }
}
