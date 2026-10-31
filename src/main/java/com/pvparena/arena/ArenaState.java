package com.pvparena.arena;

/**
 * Represents the current state of an arena game.
 */
public enum ArenaState {
    /**
     * Arena is idle, waiting for players to join
     */
    IDLE,

    /**
     * Players are in the lobby, waiting for the game to start
     */
    LOBBY,

    /**
     * Countdown before game starts (players frozen)
     */
    COUNTDOWN,

    /**
     * Game is actively running
     */
    ACTIVE,

    /**
     * Game has ended, showing results
     */
    ENDING,

    /**
     * Arena is resetting for the next game
     */
    RESETTING
}
