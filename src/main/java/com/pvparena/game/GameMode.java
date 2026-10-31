package com.pvparena.game;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import com.pvparena.arena.ArenaState;
import com.pvparena.chest.TeamSupply;
import org.bukkit.entity.Player;

/**
 * Abstract base class for all game modes.
 */
public abstract class GameMode {

    protected final PVPArenaPlugin plugin;
    protected final Arena arena;
    protected TeamSupply teamSupply;
    protected boolean gameEnded;

    public GameMode(PVPArenaPlugin plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.gameEnded = false;
    }

    /**
     * Called when the game starts (after countdown).
     */
    public void startGame() {
        this.teamSupply = new TeamSupply(plugin, arena);
        teamSupply.initialize();

        arena.setState(ArenaState.ACTIVE);

        // Start tracking stats
        plugin.getStatsManager().startTracking(arena);

        // Give initial loadouts
        for (Player player : getActivePlayers()) {
            grantLoadout(player);
        }

        onGameStart();

        // Show the live sidebar and start ticking its timer
        plugin.getSidebarManager().updateAll(arena);
        new com.pvparena.scoreboard.ScoreboardTask(plugin, arena).start();
    }

    /**
     * Called when a player dies.
     */
    public void handlePlayerDeath(Player victim, Player killer) {
        if (gameEnded)
            return;

        // Record stats (per-match display + lifetime profile)
        plugin.getStatsManager().recordDeath(arena, victim);
        plugin.getProfileManager().recordDeath(victim);
        if (killer != null) {
            plugin.getStatsManager().recordKill(arena, killer);
            plugin.getProfileManager().recordKill(killer);
        }

        // Play death sound
        plugin.getSoundManager().playDeath(victim);

        // Send items to trash
        plugin.getChestManager().addToTrashChests(arena, victim.getInventory().getContents());
        plugin.getChestManager().addToTrashChests(arena, victim.getInventory().getArmorContents());

        // Clear inventory
        victim.getInventory().clear();

        onPlayerDeath(victim, killer);

        // Reflect the kill/death in the live sidebar
        plugin.getSidebarManager().updateAll(arena);
    }

    /**
     * Called when a player should respawn.
     */
    public void handlePlayerRespawn(Player player) {
        if (gameEnded)
            return;

        if (teamSupply.tryConsumeLife(player)) {
            // Respawn with loadout
            grantLoadout(player);
            plugin.getSoundManager().playRespawn(player);

            // Teleport to spawn
            player.teleport(arena.getSpawnForPlayer(player));

            onPlayerRespawn(player);
        } else {
            // No lives left - spectator mode
            makeSpectator(player);
            plugin.getSidebarManager().clear(player);
        }

        // Reflect the life change in the live sidebar
        plugin.getSidebarManager().updateAll(arena);

        checkWinCondition();
    }

    /**
     * Grants the arena loadout to a player.
     */
    protected void grantLoadout(Player player) {
        plugin.getInventoryManager().clearForArena(player);
        plugin.getInventoryManager().grantKit(player, arena.getKitConfig());
    }

    /**
     * Makes a player a spectator.
     */
    protected void makeSpectator(Player player) {
        arena.removePlayer(player);
        arena.addSpectator(player);

        player.getInventory().clear();
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);

        if (arena.getSpectatorSpawn() != null) {
            player.teleport(arena.getSpectatorSpawn());
        }

        player.sendMessage("§c§lYou are out of lives! You are now spectating.");
    }

    /**
     * Ends the game.
     */
    public void endGame(String winningTeam) {
        if (gameEnded)
            return;
        gameEnded = true;

        arena.setState(ArenaState.ENDING);

        com.pvparena.config.MessageManager messages = plugin.getMessageManager();

        // Record lifetime participation + wins before rosters are cleared.
        for (Player player : getAllParticipants()) {
            plugin.getProfileManager().recordGamePlayed(player);
            if (winningTeam != null && isWinner(player, winningTeam)) {
                plugin.getProfileManager().recordWin(player);
            }
        }

        // Play victory sounds and messages
        for (Player player : getActivePlayers()) {
            if (winningTeam == null || arena.getTeam(player) == null ||
                    arena.getTeam(player).equalsIgnoreCase(winningTeam)) {
                plugin.getSoundManager().playVictory(player);
                player.sendTitle(messages.get("match.victory-title"),
                        messages.get("match.victory-subtitle"), 10, 70, 20);
            } else {
                player.sendTitle(messages.get("match.defeat-title"),
                        messages.get("match.defeat-subtitle"), 10, 70, 20);
            }
        }

        // Clear the live sidebar for everyone in the match
        plugin.getSidebarManager().clearAll(arena);

        // End stats tracking
        plugin.getStatsManager().endTracking(arena);

        // Restore inventories
        for (Player player : getAllParticipants()) {
            plugin.getInventoryManager().restoreInventory(player);
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);

            // Teleport to lobby
            if (arena.getLobbySpawn() != null) {
                player.teleport(arena.getLobbySpawn());
            }
        }

        // Pay out the wager pot to the winner(s), on top of their restored inventory.
        // Must happen before clearPlayers() while team membership is still known.
        awardBettingPot(winningTeam);

        // Clear arena
        arena.clearPlayers();
        arena.setState(ArenaState.IDLE);

        onGameEnd(winningTeam);
    }

    /**
     * Awards the arena's betting pot to whoever won the match.
     * A player wins if they are on the winning team, or (in FFA, where there are
     * no teams) if the winner name matches theirs.
     */
    protected void awardBettingPot(String winningTeam) {
        if (winningTeam == null || arena.getBettingChest() == null) {
            return;
        }

        java.util.List<Player> winners = new java.util.ArrayList<>();
        for (Player player : getAllParticipants()) {
            if (isWinner(player, winningTeam)) {
                winners.add(player);
            }
        }

        if (winners.isEmpty()) {
            return;
        }

        java.util.List<org.bukkit.inventory.ItemStack> awarded =
                plugin.getChestManager().awardBettingPot(arena, winners);

        if (!awarded.isEmpty()) {
            for (Player winner : winners) {
                winner.sendMessage(plugin.getMessageManager().get("match.wager-won"));
            }
        }
    }

    /**
     * Determines whether a player counts as a winner of this match.
     */
    private boolean isWinner(Player player, String winningTeam) {
        String team = arena.getTeam(player);
        if (team != null) {
            return team.equalsIgnoreCase(winningTeam);
        }
        // FFA: endGame is called with the winning player's name
        return player.getName().equalsIgnoreCase(winningTeam);
    }

    /**
     * Gets all active (non-spectator) players.
     */
    protected java.util.List<Player> getActivePlayers() {
        java.util.List<Player> players = new java.util.ArrayList<>();
        for (java.util.UUID uuid : arena.getAllPlayers()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    /**
     * Gets all participants (including spectators).
     */
    protected java.util.List<Player> getAllParticipants() {
        java.util.List<Player> players = new java.util.ArrayList<>(getActivePlayers());
        for (java.util.UUID uuid : arena.getSpectators()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    /**
     * Gets the team supply tracker.
     */
    public TeamSupply getTeamSupply() {
        return teamSupply;
    }

    /**
     * Checks if the game has ended.
     */
    public boolean isGameEnded() {
        return gameEnded;
    }

    // Abstract methods for subclasses
    protected abstract void onGameStart();

    protected abstract void onPlayerDeath(Player victim, Player killer);

    protected abstract void onPlayerRespawn(Player player);

    protected abstract void onGameEnd(String winningTeam);

    public abstract void checkWinCondition();

    public abstract String getDisplayName();
}
