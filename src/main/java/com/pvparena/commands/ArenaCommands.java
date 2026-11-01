package com.pvparena.commands;

import com.pvparena.PVPArenaPlugin;
import com.pvparena.arena.Arena;
import com.pvparena.arena.ArenaState;
import com.pvparena.game.GameModeType;
import com.pvparena.kit.KitEditor;
import com.pvparena.util.CountdownTask;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles all /arena commands.
 */
public class ArenaCommands implements CommandExecutor, TabCompleter {

    private static final String PERM_ADMIN = "pvparena.admin";
    private static final String PERM_HOST = "pvparena.host";

    private final PVPArenaPlugin plugin;

    public ArenaCommands(PVPArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            // Admin commands
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setspawn" -> handleSetSpawn(sender, args);
            case "setcorner1", "setcorner2" -> handleSetCorner(sender, args);
            case "setspectator" -> handleSetSpectator(sender, args);
            case "setlobby" -> handleSetLobby(sender, args);
            case "setsupply" -> handleSetSupply(sender, args);
            case "settrash" -> handleSetTrash(sender, args);
            case "setbet" -> handleSetBet(sender, args);
            case "setflag" -> handleSetFlag(sender, args);
            case "setcapture" -> handleSetCapture(sender, args);
            case "setrespawntime" -> handleSetRespawnTime(sender, args);
            case "setcountdown" -> handleSetCountdown(sender, args);
            case "setstatsign" -> handleSetStatSign(sender, args);
            case "setmode" -> handleSetMode(sender, args);

            // Player commands
            case "list" -> handleList(sender, args);
            case "join" -> handleJoin(sender, args);
            case "leave" -> handleLeave(sender, args);
            case "kit" -> handleKit(sender, args);
            case "ready" -> handleReady(sender, args);
            case "start" -> handleStart(sender, args);
            case "info" -> handleInfo(sender, args);
            case "stats" -> handleStats(sender, args);
            case "leaderboard", "top" -> handleLeaderboard(sender, args);

            default -> sendHelp(sender);
        }

        return true;
    }

    // ==================== ADMIN COMMANDS ====================

    private void handleCreate(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /arena create <name>");
            return;
        }

        String name = args[1];
        Arena arena = plugin.getArenaManager().createArena(name);

        if (arena != null) {
            sender.sendMessage("§aArena '" + name + "' created successfully!");
            sender.sendMessage("§7Use §e/arena setspawn§7, §e/arena setcorner1§7, etc. to configure.");
        } else {
            sender.sendMessage("§cAn arena with that name already exists!");
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /arena delete <name>");
            return;
        }

        String name = args[1];
        if (plugin.getArenaManager().deleteArena(name)) {
            sender.sendMessage("§aArena '" + name + "' deleted!");
        } else {
            sender.sendMessage("§cArena not found!");
        }
    }

    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /arena setspawn <arena> <red|blue|ffa>");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        String team = args[2].toLowerCase();
        Location loc = player.getLocation();

        switch (team) {
            case "red" -> {
                arena.setRedSpawn(loc);
                sender.sendMessage("§aRed spawn set!");
            }
            case "blue" -> {
                arena.setBlueSpawn(loc);
                sender.sendMessage("§aBlue spawn set!");
            }
            case "ffa" -> {
                arena.addFfaSpawn(loc);
                sender.sendMessage("§aFFA spawn added! Total: " + arena.getFfaSpawns().size());
            }
            default -> sender.sendMessage("§cInvalid team! Use: red, blue, or ffa");
        }

        plugin.getArenaManager().saveArena(arena);
    }

    private void handleSetCorner(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /arena " + args[0] + " <arena>");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        Location loc = player.getLocation();

        if (args[0].equalsIgnoreCase("setcorner1")) {
            arena.setCorner1(loc);
            sender.sendMessage("§aCorner 1 set!");
        } else {
            arena.setCorner2(loc);
            sender.sendMessage("§aCorner 2 set!");
        }

        plugin.getArenaManager().saveArena(arena);
    }

    private void handleSetSpectator(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /arena setspectator <arena>");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        arena.setSpectatorSpawn(player.getLocation());
        plugin.getArenaManager().saveArena(arena);
        sender.sendMessage("§aSpectator spawn set!");
    }

    private void handleSetLobby(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /arena setlobby <arena>");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        arena.setLobbySpawn(player.getLocation());
        plugin.getArenaManager().saveArena(arena);
        sender.sendMessage("§aLobby spawn set!");
    }

    private void handleSetSupply(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /arena setsupply <arena> <red|blue>");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        Block block = player.getTargetBlockExact(5);
        if (block == null || !block.getType().name().contains("CHEST")) {
            sender.sendMessage("§cLook at a chest!");
            return;
        }

        String team = args[2].toLowerCase();
        if (team.equals("red")) {
            arena.setRedSupplyChest(block.getLocation());
            sender.sendMessage("§aRed supply chest set!");
        } else if (team.equals("blue")) {
            arena.setBlueSupplyChest(block.getLocation());
            sender.sendMessage("§aBlue supply chest set!");
        } else {
            sender.sendMessage("§cInvalid team! Use: red or blue");
            return;
        }

        plugin.getArenaManager().saveArena(arena);
    }

    private void handleSetTrash(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /arena settrash <arena>");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        Block block = player.getTargetBlockExact(5);
        if (block == null || !block.getType().name().contains("CHEST")) {
            sender.sendMessage("§cLook at a chest!");
            return;
        }

        arena.addTrashChest(block.getLocation());
        plugin.getArenaManager().saveArena(arena);
        sender.sendMessage("§aTrash chest added! Total: " + arena.getTrashChests().size());
    }

    private void handleSetBet(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /arena setbet <arena>");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        Block block = player.getTargetBlockExact(5);
        if (block == null || !block.getType().name().contains("CHEST")) {
            sender.sendMessage("§cLook at a chest!");
            return;
        }

        arena.setBettingChest(block.getLocation());
        plugin.getArenaManager().saveArena(arena);
        sender.sendMessage("§aBetting chest set!");
    }

    private void handleSetFlag(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /arena setflag <arena> <red|blue>");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        String team = args[2].toLowerCase();
        Location loc = player.getLocation().getBlock().getLocation();

        if (team.equals("red")) {
            arena.setRedFlagLocation(loc);
            sender.sendMessage("§aRed flag location set!");
        } else if (team.equals("blue")) {
            arena.setBlueFlagLocation(loc);
            sender.sendMessage("§aBlue flag location set!");
        } else {
            sender.sendMessage("§cInvalid team! Use: red or blue");
            return;
        }

        plugin.getArenaManager().saveArena(arena);
    }

    private void handleSetCapture(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /arena setcapture <arena> <red|blue>");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        String team = args[2].toLowerCase();
        Location loc = player.getLocation();

        if (team.equals("red")) {
            arena.setRedCaptureZone(loc);
            sender.sendMessage("§aRed capture zone set!");
        } else if (team.equals("blue")) {
            arena.setBlueCaptureZone(loc);
            sender.sendMessage("§aBlue capture zone set!");
        } else {
            sender.sendMessage("§cInvalid team! Use: red or blue");
            return;
        }

        plugin.getArenaManager().saveArena(arena);
    }

    private void handleSetRespawnTime(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /arena setrespawntime <arena> <seconds>");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        try {
            int seconds = Integer.parseInt(args[2]);
            arena.setRespawnTime(seconds);
            plugin.getArenaManager().saveArena(arena);
            sender.sendMessage("§aRespawn time set to " + seconds + " seconds!");
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number!");
        }
    }

    private void handleSetCountdown(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /arena setcountdown <arena> <seconds>");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        try {
            int seconds = Integer.parseInt(args[2]);
            arena.setCountdownTime(seconds);
            plugin.getArenaManager().saveArena(arena);
            sender.sendMessage("§aCountdown time set to " + seconds + " seconds!");
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number!");
        }
    }

    private void handleSetStatSign(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /arena setstatsign <arena> <kills|deaths|captures|mvp|items>");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        Block block = player.getTargetBlockExact(5);
        if (block == null || !block.getType().name().contains("SIGN")) {
            sender.sendMessage("§cLook at a sign!");
            return;
        }

        String type = args[2].toLowerCase();
        arena.setStatsSign(type, block.getLocation());
        plugin.getArenaManager().saveArena(arena);
        sender.sendMessage("§aStats sign '" + type + "' set!");
    }

    private void handleSetMode(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN))
            return;
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /arena setmode <arena> <ctf|elimination|ffa>");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        GameModeType type = GameModeType.fromString(args[2]);
        if (type == null) {
            sender.sendMessage("§cInvalid game mode! Use: ctf, elimination, or ffa");
            return;
        }

        arena.setGameModeType(type);
        plugin.getArenaManager().saveArena(arena);
        sender.sendMessage("§aGame mode set to: " + type.getDisplayName());
    }

    // ==================== PLAYER COMMANDS ====================

    private void handleList(CommandSender sender, String[] args) {
        var arenas = plugin.getArenaManager().getArenas();

        if (arenas.isEmpty()) {
            sender.sendMessage("§7No arenas available.");
            return;
        }

        sender.sendMessage("§6§l=== ARENAS ===");
        for (Arena arena : arenas.values()) {
            String status = switch (arena.getState()) {
                case IDLE -> "§7[Idle]";
                case LOBBY -> "§e[Lobby]";
                case COUNTDOWN -> "§c[Starting]";
                case ACTIVE -> "§c[In Progress]";
                case ENDING -> "§6[Ending]";
                case RESETTING -> "§7[Resetting]";
            };
            sender.sendMessage("  §f" + arena.getName() + " " + status + " §7- " +
                    arena.getGameModeType().getDisplayName() + " (" + arena.getPlayerCount() + " players)");
        }
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /arena join <arena> [team]");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        if (arena.getState() != ArenaState.IDLE && arena.getState() != ArenaState.LOBBY) {
            sender.sendMessage("§cThis arena is not accepting players right now!");
            return;
        }

        // Check if already in an arena
        for (Arena a : plugin.getArenaManager().getArenas().values()) {
            if (a.isInArena(player)) {
                sender.sendMessage("§cYou are already in an arena! Use /arena leave first.");
                return;
            }
        }

        if (arena.getPlayerCount() >= arena.getMaxPlayers()) {
            sender.sendMessage("§cThis arena is full!");
            return;
        }

        // Save inventory
        plugin.getInventoryManager().saveInventory(player);

        // Determine team
        String team = args.length >= 3 ? args[2].toLowerCase() : null;

        if (arena.getGameModeType() == GameModeType.FREE_FOR_ALL) {
            arena.getAllPlayers().add(player.getUniqueId());
        } else {
            if ("red".equals(team)) {
                arena.addToRedTeam(player);
            } else if ("blue".equals(team)) {
                arena.addToBlueTeam(player);
            } else {
                // Auto-assign to smaller team
                if (arena.getRedTeam().size() <= arena.getBlueTeam().size()) {
                    arena.addToRedTeam(player);
                    team = "red";
                } else {
                    arena.addToBlueTeam(player);
                    team = "blue";
                }
            }
        }

        // Set arena to lobby state if needed
        if (arena.getState() == ArenaState.IDLE) {
            arena.setState(ArenaState.LOBBY);
            arena.setHost(player.getUniqueId());
        }

        // Teleport to lobby
        if (arena.getLobbySpawn() != null) {
            player.teleport(arena.getLobbySpawn());
        }

        // Show betting pot
        var messages = plugin.getMessageManager();
        String potDisplay = plugin.getChestManager().getBettingPotDisplay(arena);
        player.sendMessage(messages.format("join.success", "arena", arena.getName()));
        if (team != null) {
            String teamColor = team.equals("red") ? "§c" : "§9";
            player.sendMessage(messages.format("join.team", "color", teamColor, "team", team.toUpperCase()));
        }
        player.sendMessage(potDisplay);
    }

    private void handleLeave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        Arena arena = null;
        for (Arena a : plugin.getArenaManager().getArenas().values()) {
            if (a.isInArena(player)) {
                arena = a;
                break;
            }
        }

        if (arena == null) {
            sender.sendMessage(plugin.getMessageManager().get("error.not-in-arena"));
            return;
        }

        // Restore inventory
        plugin.getInventoryManager().restoreInventory(player);

        // Clear the live sidebar
        plugin.getSidebarManager().clear(player);

        // Remove from arena
        arena.removePlayer(player);

        // Reset game mode
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);

        sender.sendMessage(plugin.getMessageManager().get("leave.success"));

        // Check if game should end
        if (arena.getActiveGameMode() != null) {
            arena.getActiveGameMode().checkWinCondition();
        }
    }

    private void handleKit(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_HOST))
            return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        // Find player's arena
        Arena arena = null;
        for (Arena a : plugin.getArenaManager().getArenas().values()) {
            if (a.isHost(player)) {
                arena = a;
                break;
            }
        }

        if (arena == null) {
            sender.sendMessage("§cYou must be hosting an arena to edit the kit!");
            return;
        }

        // Open kit editor
        KitEditor editor = new KitEditor(plugin, player, arena);
        editor.open();
    }

    private void handleReady(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        // Find the arena the player is in
        Arena arena = null;
        for (Arena a : plugin.getArenaManager().getArenas().values()) {
            if (a.isInArena(player)) {
                arena = a;
                break;
            }
        }

        if (arena == null) {
            sender.sendMessage(plugin.getMessageManager().get("error.not-in-arena"));
            return;
        }

        if (arena.getState() != ArenaState.LOBBY) {
            sender.sendMessage("§cYou can only ready up in the lobby!");
            return;
        }

        if (arena.isSpectator(player)) {
            sender.sendMessage("§cSpectators cannot ready up.");
            return;
        }

        var messages = plugin.getMessageManager();
        arena.setReady(player);
        player.sendMessage(messages.format("ready.marked",
                "ready", String.valueOf(arena.getReadyCount()),
                "total", String.valueOf(arena.getPlayerCount())));

        // When everyone is ready and there are enough players, begin the match.
        if (arena.getPlayerCount() >= arena.getMinPlayers() && arena.allPlayersReady()) {
            for (java.util.UUID uuid : arena.getAllPlayers()) {
                Player p = plugin.getServer().getPlayer(uuid);
                if (p != null) {
                    p.sendMessage(messages.get("ready.all"));
                }
            }
            new CountdownTask(plugin, arena).start();
        }
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_HOST))
            return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        // Find player's arena
        Arena arena = null;
        for (Arena a : plugin.getArenaManager().getArenas().values()) {
            if (a.isHost(player)) {
                arena = a;
                break;
            }
        }

        if (arena == null) {
            sender.sendMessage("§cYou must be hosting an arena to start it!");
            return;
        }

        if (arena.getState() != ArenaState.LOBBY) {
            sender.sendMessage("§cArena is not in lobby state!");
            return;
        }

        // Check minimum requirements
        if (arena.getPlayerCount() < arena.getMinPlayers()) {
            sender.sendMessage("§cNeed at least " + arena.getMinPlayers() + " players to start!");
            return;
        }

        // Start countdown
        sender.sendMessage("§aStarting game...");
        new CountdownTask(plugin, arena).start();
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /arena info <arena>");
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage("§cArena not found!");
            return;
        }

        sender.sendMessage("§6§l=== " + arena.getName().toUpperCase() + " ===");
        sender.sendMessage("§7State: §f" + arena.getState());
        sender.sendMessage("§7Mode: §f" + arena.getGameModeType().getDisplayName());
        sender.sendMessage("§7Players: §f" + arena.getPlayerCount() + "/" + arena.getMaxPlayers());
        sender.sendMessage("§7Respawn Time: §f" + arena.getRespawnTime() + "s");
        sender.sendMessage("§7Countdown: §f" + arena.getCountdownTime() + "s");

        sender.sendMessage("§7Red Spawn: " + (arena.getRedSpawn() != null ? "§a✓" : "§c✗"));
        sender.sendMessage("§7Blue Spawn: " + (arena.getBlueSpawn() != null ? "§a✓" : "§c✗"));
        sender.sendMessage("§7Lobby: " + (arena.getLobbySpawn() != null ? "§a✓" : "§c✗"));
        sender.sendMessage(
                "§7Boundaries: " + (arena.getCorner1() != null && arena.getCorner2() != null ? "§a✓" : "§c✗"));
    }

    private void handleStats(CommandSender sender, String[] args) {
        var messages = plugin.getMessageManager();
        var profiles = plugin.getProfileManager();

        String targetName;
        com.pvparena.stats.PlayerProfile profile;

        if (args.length >= 2) {
            targetName = args[1];
            profile = profiles.getProfileByName(targetName);
        } else if (sender instanceof Player player) {
            targetName = player.getName();
            profile = profiles.getProfile(player.getUniqueId());
        } else {
            sender.sendMessage(messages.get("error.players-only"));
            return;
        }

        if (profile == null) {
            sender.sendMessage(messages.format("stats.none", "player", targetName));
            return;
        }

        sender.sendMessage(messages.format("stats.header", "player", profile.getName()));
        sender.sendMessage(messages.format("stats.line", "stat", "Kills", "value", String.valueOf(profile.getKills())));
        sender.sendMessage(messages.format("stats.line", "stat", "Deaths", "value", String.valueOf(profile.getDeaths())));
        sender.sendMessage(messages.format("stats.line", "stat", "K/D", "value",
                String.format("%.2f", profile.getKillDeathRatio())));
        sender.sendMessage(messages.format("stats.line", "stat", "Wins", "value", String.valueOf(profile.getWins())));
        sender.sendMessage(
                messages.format("stats.line", "stat", "Captures", "value", String.valueOf(profile.getCaptures())));
        sender.sendMessage(messages.format("stats.line", "stat", "Games Played", "value",
                String.valueOf(profile.getGamesPlayed())));
    }

    private void handleLeaderboard(CommandSender sender, String[] args) {
        var messages = plugin.getMessageManager();

        String stat = args.length >= 2 ? args[1].toLowerCase() : "kills";
        if (!stat.equals("kills") && !stat.equals("wins") && !stat.equals("captures")) {
            sender.sendMessage(messages.get("leaderboard.usage"));
            return;
        }

        var top = plugin.getProfileManager().getLeaderboard(stat, 10);
        if (top.isEmpty() || top.get(0).getStat(stat) <= 0) {
            sender.sendMessage(messages.get("leaderboard.empty"));
            return;
        }

        sender.sendMessage(messages.format("leaderboard.header", "stat", stat.toUpperCase()));
        int rank = 1;
        for (com.pvparena.stats.PlayerProfile profile : top) {
            int value = profile.getStat(stat);
            if (value <= 0) {
                break;
            }
            sender.sendMessage(messages.format("leaderboard.line",
                    "rank", String.valueOf(rank), "player", profile.getName(), "value", String.valueOf(value)));
            rank++;
        }
    }

    // ==================== UTILITIES ====================

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sender.sendMessage("§cYou don't have permission to do that!");
        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== PVP ARENA HELP ===");
        sender.sendMessage("§e/arena list §7- List all arenas");
        sender.sendMessage("§e/arena join <arena> [team] §7- Join an arena");
        sender.sendMessage("§e/arena ready §7- Mark yourself ready in the lobby");
        sender.sendMessage("§e/arena leave §7- Leave current arena");
        sender.sendMessage("§e/arena info <arena> §7- View arena info");
        sender.sendMessage("§e/arena stats [player] §7- View lifetime stats");
        sender.sendMessage("§e/arena leaderboard [kills|wins|captures] §7- Top 10");

        if (sender.hasPermission(PERM_HOST)) {
            sender.sendMessage("§e/arena kit §7- Open kit editor (host)");
            sender.sendMessage("§e/arena start §7- Start the game (host)");
        }

        if (sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage("§c§lAdmin Commands:");
            sender.sendMessage("§e/arena create <name> §7- Create arena");
            sender.sendMessage("§e/arena delete <name> §7- Delete arena");
            sender.sendMessage("§e/arena setspawn <arena> <team> §7- Set spawn");
            sender.sendMessage("§e/arena setcorner1/2 <arena> §7- Set boundaries");
            sender.sendMessage("§e/arena setmode <arena> <mode> §7- Set game mode");
            sender.sendMessage("§7... and more. See documentation.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList(
                    "list", "join", "leave", "info", "kit", "ready", "start", "stats", "leaderboard"));
            if (sender.hasPermission(PERM_ADMIN)) {
                subCommands.addAll(Arrays.asList(
                        "create", "delete", "setspawn", "setcorner1", "setcorner2",
                        "setspectator", "setlobby", "setsupply", "settrash", "setbet",
                        "setflag", "setcapture", "setrespawntime", "setcountdown",
                        "setstatsign", "setmode"));
            }
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("join", "delete", "info", "setspawn", "setcorner1", "setcorner2",
                    "setspectator", "setlobby", "setsupply", "settrash", "setbet",
                    "setflag", "setcapture", "setrespawntime", "setcountdown",
                    "setstatsign", "setmode").contains(sub)) {
                return plugin.getArenaManager().getArenas().keySet().stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("setspawn", "setsupply", "setflag", "setcapture").contains(sub)) {
                return Arrays.asList("red", "blue", "ffa").stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (sub.equals("setmode")) {
                return Arrays.asList("ctf", "elimination", "ffa").stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (sub.equals("setstatsign")) {
                return Arrays.asList("kills", "deaths", "captures", "mvp", "items").stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (sub.equals("join")) {
                return Arrays.asList("red", "blue").stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("leaderboard")) {
            return Arrays.asList("kills", "wins", "captures").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
