package com.pvparena.arena;

import com.pvparena.game.GameMode;
import com.pvparena.game.GameModeType;
import com.pvparena.kit.KitConfig;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Represents a PVP Arena with all its configuration and state.
 */
public class Arena {

    private final String name;
    private ArenaState state;
    private GameModeType gameModeType;
    private GameMode activeGameMode;

    // Teams
    private final Set<UUID> redTeam;
    private final Set<UUID> blueTeam;
    private final Set<UUID> spectators;
    private final Set<UUID> allPlayers;
    private final Set<UUID> readyPlayers;

    // Spawn points
    private Location redSpawn;
    private Location blueSpawn;
    private final List<Location> ffaSpawns;
    private Location lobbySpawn;
    private Location spectatorSpawn;

    // Boundaries
    private Location corner1;
    private Location corner2;

    // Chests
    private Location redSupplyChest;
    private Location blueSupplyChest;
    private final List<Location> trashChests;
    private Location bettingChest;

    // Configuration
    private KitConfig kitConfig;
    private int respawnTime; // seconds
    private int countdownTime; // seconds
    private int maxPlayers;
    private int minPlayers;

    // Flag locations (for CTF)
    private Location redFlagLocation;
    private Location blueFlagLocation;
    private Location redCaptureZone;
    private Location blueCaptureZone;

    // Stats sign locations
    private final Map<String, Location> statsSigns;

    // Host
    private UUID host;

    public Arena(String name) {
        this.name = name;
        this.state = ArenaState.IDLE;
        this.gameModeType = GameModeType.CAPTURE_THE_FLAG;

        this.redTeam = new HashSet<>();
        this.blueTeam = new HashSet<>();
        this.spectators = new HashSet<>();
        this.allPlayers = new HashSet<>();
        this.readyPlayers = new HashSet<>();

        this.ffaSpawns = new ArrayList<>();
        this.trashChests = new ArrayList<>();
        this.statsSigns = new HashMap<>();

        this.respawnTime = 5;
        this.countdownTime = 10;
        this.maxPlayers = 16;
        this.minPlayers = 2;
    }

    // --- Getters and Setters ---

    public String getName() {
        return name;
    }

    public ArenaState getState() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public GameModeType getGameModeType() {
        return gameModeType;
    }

    public void setGameModeType(GameModeType gameModeType) {
        this.gameModeType = gameModeType;
    }

    public GameMode getActiveGameMode() {
        return activeGameMode;
    }

    public void setActiveGameMode(GameMode activeGameMode) {
        this.activeGameMode = activeGameMode;
    }

    // Team management
    public Set<UUID> getRedTeam() {
        return redTeam;
    }

    public Set<UUID> getBlueTeam() {
        return blueTeam;
    }

    public Set<UUID> getSpectators() {
        return spectators;
    }

    public Set<UUID> getAllPlayers() {
        return allPlayers;
    }

    public void addToRedTeam(Player player) {
        redTeam.add(player.getUniqueId());
        allPlayers.add(player.getUniqueId());
    }

    public void addToBlueTeam(Player player) {
        blueTeam.add(player.getUniqueId());
        allPlayers.add(player.getUniqueId());
    }

    public void addSpectator(Player player) {
        spectators.add(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        redTeam.remove(uuid);
        blueTeam.remove(uuid);
        spectators.remove(uuid);
        allPlayers.remove(uuid);
        readyPlayers.remove(uuid);
    }

    // Ready system
    public void setReady(Player player) {
        readyPlayers.add(player.getUniqueId());
    }

    public void setNotReady(Player player) {
        readyPlayers.remove(player.getUniqueId());
    }

    public boolean isReady(Player player) {
        return readyPlayers.contains(player.getUniqueId());
    }

    public int getReadyCount() {
        return readyPlayers.size();
    }

    /**
     * True when every player currently in the arena has marked ready.
     */
    public boolean allPlayersReady() {
        return !allPlayers.isEmpty() && readyPlayers.containsAll(allPlayers);
    }

    public boolean isInRedTeam(Player player) {
        return redTeam.contains(player.getUniqueId());
    }

    public boolean isInBlueTeam(Player player) {
        return blueTeam.contains(player.getUniqueId());
    }

    public boolean isSpectator(Player player) {
        return spectators.contains(player.getUniqueId());
    }

    public boolean isInArena(Player player) {
        return allPlayers.contains(player.getUniqueId()) || spectators.contains(player.getUniqueId());
    }

    public String getTeam(Player player) {
        if (isInRedTeam(player))
            return "red";
        if (isInBlueTeam(player))
            return "blue";
        return null;
    }

    // Spawns
    public Location getRedSpawn() {
        return redSpawn;
    }

    public void setRedSpawn(Location redSpawn) {
        this.redSpawn = redSpawn;
    }

    public Location getBlueSpawn() {
        return blueSpawn;
    }

    public void setBlueSpawn(Location blueSpawn) {
        this.blueSpawn = blueSpawn;
    }

    public List<Location> getFfaSpawns() {
        return ffaSpawns;
    }

    public void addFfaSpawn(Location spawn) {
        ffaSpawns.add(spawn);
    }

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(Location lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
    }

    public Location getSpectatorSpawn() {
        return spectatorSpawn;
    }

    public void setSpectatorSpawn(Location spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    // Boundaries
    public Location getCorner1() {
        return corner1;
    }

    public void setCorner1(Location corner1) {
        this.corner1 = corner1;
    }

    public Location getCorner2() {
        return corner2;
    }

    public void setCorner2(Location corner2) {
        this.corner2 = corner2;
    }

    public boolean isInsideBoundary(Location location) {
        if (corner1 == null || corner2 == null) {
            return true; // No boundaries set
        }

        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        return location.getX() >= minX && location.getX() <= maxX
                && location.getY() >= minY && location.getY() <= maxY
                && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    // Chests
    public Location getRedSupplyChest() {
        return redSupplyChest;
    }

    public void setRedSupplyChest(Location redSupplyChest) {
        this.redSupplyChest = redSupplyChest;
    }

    public Location getBlueSupplyChest() {
        return blueSupplyChest;
    }

    public void setBlueSupplyChest(Location blueSupplyChest) {
        this.blueSupplyChest = blueSupplyChest;
    }

    public List<Location> getTrashChests() {
        return trashChests;
    }

    public void addTrashChest(Location chest) {
        trashChests.add(chest);
    }

    public Location getBettingChest() {
        return bettingChest;
    }

    public void setBettingChest(Location bettingChest) {
        this.bettingChest = bettingChest;
    }

    // Kit
    public KitConfig getKitConfig() {
        return kitConfig;
    }

    public void setKitConfig(KitConfig kitConfig) {
        this.kitConfig = kitConfig;
    }

    // Configuration
    public int getRespawnTime() {
        return respawnTime;
    }

    public void setRespawnTime(int respawnTime) {
        this.respawnTime = respawnTime;
    }

    public int getCountdownTime() {
        return countdownTime;
    }

    public void setCountdownTime(int countdownTime) {
        this.countdownTime = countdownTime;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    // CTF Flags
    public Location getRedFlagLocation() {
        return redFlagLocation;
    }

    public void setRedFlagLocation(Location redFlagLocation) {
        this.redFlagLocation = redFlagLocation;
    }

    public Location getBlueFlagLocation() {
        return blueFlagLocation;
    }

    public void setBlueFlagLocation(Location blueFlagLocation) {
        this.blueFlagLocation = blueFlagLocation;
    }

    public Location getRedCaptureZone() {
        return redCaptureZone;
    }

    public void setRedCaptureZone(Location redCaptureZone) {
        this.redCaptureZone = redCaptureZone;
    }

    public Location getBlueCaptureZone() {
        return blueCaptureZone;
    }

    public void setBlueCaptureZone(Location blueCaptureZone) {
        this.blueCaptureZone = blueCaptureZone;
    }

    // Stats signs
    public Map<String, Location> getStatsSigns() {
        return statsSigns;
    }

    public void setStatsSign(String type, Location location) {
        statsSigns.put(type, location);
    }

    // Host
    public UUID getHost() {
        return host;
    }

    public void setHost(UUID host) {
        this.host = host;
    }

    public boolean isHost(Player player) {
        return host != null && host.equals(player.getUniqueId());
    }

    // Utility
    public int getPlayerCount() {
        return allPlayers.size();
    }

    public void clearPlayers() {
        redTeam.clear();
        blueTeam.clear();
        spectators.clear();
        allPlayers.clear();
        readyPlayers.clear();
    }

    public Location getSpawnForPlayer(Player player) {
        if (isInRedTeam(player)) {
            return redSpawn;
        } else if (isInBlueTeam(player)) {
            return blueSpawn;
        } else if (!ffaSpawns.isEmpty()) {
            return ffaSpawns.get(new Random().nextInt(ffaSpawns.size()));
        }
        return lobbySpawn;
    }
}
