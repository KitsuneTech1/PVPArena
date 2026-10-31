package com.pvparena.util;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Manages sound effects for arena events.
 */
public class SoundManager {

    /**
     * Plays the flag pickup sound (Ender Dragon growl).
     */
    public void playFlagPickup(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);
    }

    /**
     * Plays the flag pickup sound to all nearby players.
     */
    public void playFlagPickup(Location location) {
        location.getWorld().playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);
    }

    /**
     * Plays the flag capture sound (level up).
     */
    public void playFlagCapture(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    /**
     * Plays the flag capture sound to all nearby players.
     */
    public void playFlagCapture(Location location) {
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    /**
     * Plays the death sound (Wither spawn).
     */
    public void playDeath(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.2f);
    }

    /**
     * Plays the death sound at a location.
     */
    public void playDeath(Location location) {
        location.getWorld().playSound(location, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.2f);
    }

    /**
     * Plays the victory sound (Raid victory horn).
     */
    public void playVictory(Player player) {
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    /**
     * Plays the victory sound to all players in a collection.
     */
    public void playVictory(Iterable<Player> players) {
        for (Player player : players) {
            playVictory(player);
        }
    }

    /**
     * Plays the countdown tick sound.
     */
    public void playCountdownTick(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
    }

    /**
     * Plays the game start sound.
     */
    public void playGameStart(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.5f);
    }

    /**
     * Plays a respawn sound.
     */
    public void playRespawn(Player player) {
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.5f, 1.2f);
    }

    /**
     * Plays a flag drop sound.
     */
    public void playFlagDrop(Location location) {
        location.getWorld().playSound(location, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
    }
}
