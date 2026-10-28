# Wrapped

Your 2026 year in Minecraft in Spotify Wrapped style. Press **K** in game and Wrapped plays back your year as a run of full-screen cards: hours played, top servers, longest session, when you play, streaks, chat, blocks, fights, and a player type, ending on a summary card you can save as an image.

It needs no server plugin and no tracking. Everything comes from history already on your PC: the game logs in `logs/` and your singleplayer world stats. It works the moment you install it, even for a year you played before you had the mod.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Download Wrapped from [Modrinth](https://modrinth.com/mod/wrapped) and pick the file for your game version.
3. Drop the jar into `mods/`.

Supported versions: **26.3**, **26.2**, **26.1.2** and **1.21.11**. Each has its own jar. The 26.x jars need Java 25, and the 1.21.11 jar needs Java 21.

## Use

- **K** opens Wrapped (rebind it in Controls).
- Click, Space or Right arrow goes forward. Left arrow, or clicking the left side of the screen, goes back.
- If you have history from more than one year, the arrows beside the year on the title card switch between them.
- **Save as image** on the summary card writes a 1080x1920 PNG to `screenshots/wrapped/`, ready to post.

## Instance and Total

Each launcher instance (each game folder) keeps its own logs, so on its own Wrapped can only see the instance you opened it in. That is the **Instance** tab on the title card.

**Total** is your whole year across every instance:

1. Install Wrapped in each instance you play on.
2. Open it once in each one. The first time, it asks **Add this instance to your Total?** Say yes.
3. Open Wrapped anywhere and pick the **Total** tab to see everything combined.

Some things to know:

- Total belongs to your Minecraft account and is saved outside your instances (in `%APPDATA%\wrapped` on Windows), so every instance shares the same Total.
- Instances you have added keep feeding new play into Total whenever you open Wrapped in them.
- Total remembers history even after an instance's old logs are deleted, or after the instance itself is gone.
- The same log file is never counted twice, even if it was copied into several instances.
- If you say no, that instance stays out of Total and you will not be asked again.

## Privacy

Nothing leaves your PC. Wrapped only reads files the game already wrote. If you would rather not show your most used chat word, hide it on the chat card. It then stays hidden on the summary card and in saved images too.

## Build

```
./gradlew build collectJars
```

