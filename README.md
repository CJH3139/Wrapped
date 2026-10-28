# Wrapped

**Your year in Minecraft, Wrapped.**

The late nights. The server you kept coming back to. That session that went on a little too long. Wrapped turns your Minecraft history into a personal year in review, inspired by Spotify Wrapped.

Press **K** to play through fullscreen cards about your year, then save a summary image to share.

[Download on Modrinth](https://modrinth.com/mod/wrapped) · [Report a bug](https://github.com/CJH3139/Wrapped/issues)

## What's in your recap?

- **Your time:** Hours played, your longest session, and when you play most.
- **Your places:** Your most played servers.
- **Your habits:** Play streaks and chat highlights.
- **Your adventures:** Blocks, fights, and a player type based on your stats.
- **Your year to share:** A portrait summary card you can export as an image.

Wrapped reads the logs and singleplayer world statistics already on your PC. You can look back at play from before you installed the mod, as long as that history is still available. If you have history from multiple years, switch between them on the title card.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft version.
2. Download the matching Wrapped jar from [Modrinth](https://modrinth.com/mod/wrapped).
3. Put the jar in your instance's `mods/` folder.
4. Launch Minecraft and press **K**.

Wrapped is a **client-side mod**. No server plugin is required.

### Supported versions

**26.3, 26.2, 26.1.2, every 1.21.x and 1.20.x release, and 1.16.1.**

**Speedrunners: Minecraft 1.16.1 is supported too.** Install Wrapped in your 1.16.1 instances to revisit the play history they contain, and add them to your Total to bring your year together.

Pick the jar whose version range includes your game version. For example, use the `1.21.9-1.21.11` jar for Minecraft 1.21.10.

| Minecraft version | Required Java version |
| --- | --- |
| Supported 26.x releases | 25 or newer |
| 1.20.5 through 1.21.11 | 21 or newer |
| 1.16.1 and 1.20 through 1.20.4 | 17 or newer |

## Explore and share

| Action | Control |
| --- | --- |
| Open Wrapped | **K**, rebindable in Controls |
| Next card | Click, **Space**, or **Right Arrow** |
| Previous card | **Left Arrow** or click the left side of the screen |
| Change year | Use the arrows beside the year on the title card |
| Export your recap | Choose **Save as image** on the summary card |

Exported summaries are **1080 × 1920 PNGs**, saved in `screenshots/wrapped/` inside your game folder.

## One instance or your whole year

Different launcher instances keep separate histories. Wrapped gives you two views on the title card:

- **Instance:** The history in the game folder you currently have open.
- **Total:** The combined history from the instances you've added, tied to your Minecraft account.

To combine your instances:

1. Install Wrapped in each instance you want to include.
2. Open Wrapped in each one and accept **Add this instance to your Total?**
3. Select **Total** in any of them to see your combined recap.

Added instances update your Total whenever you open Wrapped in them. Identical log files are counted once, even when copied between instances. Imported history stays in Total if the original logs or instance are later deleted.

Total is stored outside your game folders, in `%APPDATA%\wrapped` on Windows. If you decline to add an instance, it stays out of Total and Wrapped will not ask again.

## Privacy and available history

**Nothing leaves your PC.** Wrapped builds your recap locally from files Minecraft already wrote. You don't need to have had Wrapped installed throughout the year.

Your recap depends on what's available. Missing or deleted logs can leave gaps, and world statistics come from local singleplayer saves.

You can hide your most used chat word on the chat card. It will also stay hidden on the summary card and in exported images.

## Build from source

Run this from the project folder:

```sh
./gradlew build collectJars
```

On Windows PowerShell:

```powershell
.\gradlew.bat build collectJars
```

The collected mod jars are written to `build/libs/`.
