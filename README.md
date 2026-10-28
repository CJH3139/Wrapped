# Wrapped

Your 2026 year in Minecraft in Spotify Wrapped style. Press **K** in game and Wrapped plays back your year as a run of full-screen cards: hours played, top servers, longest session, when you play, streaks, chat, blocks, fights, and a player type, ending on a summary card you can save as an image.

It needs no server plugin and no tracking. Everything comes from history already on your PC. The game logs in `logs/` and your singleplayer world stats.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Download Wrapped from [Modrinth](https://modrinth.com/mod/wrapped) and pick the file for your game version.
3. Drop the jar into `mods/`.

Supported versions: **26.3**, **26.2** and **26.1.2**. Each has its own jar. Java 25 is required.

## Use

- **K** opens Wrapped (rebind it in Controls).
- Click, Space or Right arrow goes forward. Left arrow, or clicking the left side of the screen, goes back.
- On the title card, switch between **Instance** (this game folder) and **Total** (every instance you have added). The first time you open Wrapped in a new instance, it asks whether to add it to your Total.
- **Save as image** on the summary card writes a 1080x1920 PNG to `screenshots/wrapped/`.

## Build

```
./gradlew build collectJars
```

