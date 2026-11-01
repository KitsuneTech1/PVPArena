<p align="center"><a href="https://kitsunetechnologies.org/work"><img src="https://raw.githubusercontent.com/KitsuneTech1/.github/main/assets/kitsune-banner.svg" alt="Built by Kitsune Technologies" width="760"></a></p>

# PVPArena

Team PVP arenas with Capture the Flag, Elimination, and Free for All, plus a material cost and wager system.

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Paper](https://img.shields.io/badge/paper-26.1.2-brightgreen.svg)
![Java](https://img.shields.io/badge/java-25-orange.svg)

## What it does

PVPArena runs configurable arena matches with three game modes. Instead of pre-made kits, players fund their team's "lives" by depositing real items (armor, weapons, food) into supply chests before the match starts. Death sends a player's gear to a trash chest, and an optional betting chest lets teams wager items on the outcome, winner takes all.

## Features

- Capture the Flag: team-based flag capture with respawns
- Elimination: last team standing wins
- Free for All: last player alive wins
- Material cost system: deposited item sets become team lives, tracked per supply chest
- Trash chest collects gear from eliminated players; betting chest holds winner-takes-all wagers
- Configurable pre-game countdown with player freeze
- Flag carrier gets Slowness I and Glowing in CTF
- Boundary handling: in-game players teleport to spawn, spectators to a separate viewing area
- Survival inventory saved and restored after the match
- Configurable respawn timer with countdown display
- Sound effects for match events
- Post-match stats on signs: kills, deaths, captures, MVP, total items consumed

## Quick start

```
/arena create myarena
/arena setspawn myarena red
/arena setspawn myarena blue
/arena setlobby myarena
/arena setspectator myarena
/arena setcorner1 myarena
/arena setcorner2 myarena
```

Set up chests (look at the chest first):

```
/arena setsupply myarena red
/arena setsupply myarena blue
/arena settrash myarena
/arena setbet myarena
```

For CTF mode:

```
/arena setmode myarena ctf
/arena setflag myarena red
/arena setflag myarena blue
/arena setcapture myarena red
/arena setcapture myarena blue
```

Play:

```
/arena join myarena red
/arena kit
/arena start
```

## Commands

All commands run through `/arena` (aliases `/pvp`, `/pvparena`).

| Command | Description |
|---|---|
| `/arena list` | List all arenas |
| `/arena join <arena> [team]` | Join an arena |
| `/arena leave` | Leave the current arena |
| `/arena info <arena>` | View arena info |
| `/arena kit` | Open the kit editor (host) |
| `/arena start` | Start the game (host) |
| `/arena create <name>` | Create an arena |
| `/arena delete <name>` | Delete an arena |
| `/arena setspawn <arena> <red\|blue\|ffa>` | Set a spawn point |
| `/arena setcorner1 <arena>` / `/arena setcorner2 <arena>` | Set arena boundaries |
| `/arena setspectator <arena>` | Set the spectator area |
| `/arena setlobby <arena>` | Set the lobby |
| `/arena setsupply <arena> <team>` | Set a supply chest |
| `/arena settrash <arena>` | Add a trash chest |
| `/arena setbet <arena>` | Set the betting chest |
| `/arena setflag <arena> <team>` | Set a flag location (CTF) |
| `/arena setcapture <arena> <team>` | Set a capture zone (CTF) |
| `/arena setrespawntime <arena> <seconds>` | Set the respawn delay |
| `/arena setcountdown <arena> <seconds>` | Set the pre-game countdown |
| `/arena setmode <arena> <mode>` | Set the game mode |
| `/arena setstatsign <arena> <type>` | Set a stats sign |

## Permissions

| Permission | Description | Default |
|---|---|---|
| `pvparena.join` | Join public arena games | true |
| `pvparena.host` | Host matches and use the kit editor | op |
| `pvparena.admin` | Full arena management: create, delete, configure | op |

OP players automatically have all permissions.

## Install

### What you need

- A Paper server compatible with API version 26.1.2. Download it from https://papermc.io/downloads.
- Java 25 or newer. Download it from https://adoptium.net. Check what you have with `java -version` in a terminal.
- This plugin will not work on vanilla Minecraft or on Realms. You need a Spigot/Paper server you host yourself.

### Step by step

**Option A: download the jar**

Go to the [latest release](https://github.com/KitsuneTech1/PVPArena/releases/latest) and download `PVPArena-1.0.0.jar` from the Assets list. This is the easiest option and works for most people.

**Option B: build from source**

1. Install a JDK 25+ from https://adoptium.net if you don't have one.
2. Clone the repo (`git clone https://github.com/KitsuneTech1/PVPArena.git`) or click "Code > Download ZIP" on GitHub and unzip it.
3. From the project folder, run `mvn package`.
4. The built jar will be in `target\PVPArena-1.0.0.jar`.

Then, for both options:

- Drop the jar into your server's `plugins` folder.
- Restart the server completely (not `/reload`, a real restart).
- Verify it loaded by running `/arena list` and confirming you get a response instead of "Unknown command."

**It worked if** `/arena list` responds with an arena list (even an empty one) instead of "Unknown command," and your server console shows PVPArena enabling with no errors during startup.

**Troubleshooting:**

- Console says "Unsupported api-version" or the plugin fails to enable: your server is not compatible with Paper API 26.1.2. Update your server jar to match.
- Plugin doesn't show up in `/plugins`: make sure the jar is directly inside the `plugins` folder, not in a subfolder, and that you fully restarted rather than used `/reload`.
- "You do not have permission" when running arena commands: OP players get every permission automatically. Non-OP players need `pvparena.host` to host matches or `pvparena.admin` to manage arenas, granted through your permissions plugin (like LuckPerms).

## Build

```
mvn clean package
```

The built jar is output to `target/PVPArena-1.0.0.jar`.

## License

MIT. See [LICENSE](LICENSE).
