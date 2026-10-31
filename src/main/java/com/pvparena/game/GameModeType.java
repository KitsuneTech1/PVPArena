package com.pvparena.game;

/**
 * Enum representing available game mode types.
 */
public enum GameModeType {
    CAPTURE_THE_FLAG("Capture The Flag", "CTF"),
    ELIMINATION("Elimination", "ELIM"),
    FREE_FOR_ALL("Free For All", "FFA");

    private final String displayName;
    private final String shortName;

    GameModeType(String displayName, String shortName) {
        this.displayName = displayName;
        this.shortName = shortName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortName() {
        return shortName;
    }

    public static GameModeType fromString(String input) {
        for (GameModeType type : values()) {
            if (type.name().equalsIgnoreCase(input)
                    || type.displayName.equalsIgnoreCase(input)
                    || type.shortName.equalsIgnoreCase(input)) {
                return type;
            }
        }
        return null;
    }
}
