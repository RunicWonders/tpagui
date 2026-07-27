# TpaGui

**English** | [简体中文](README_CN.md)

A Paper plugin that adds a graphical interface to your TPA plugin: chest GUI for Java players, native forms for Bedrock, native Dialog request prompts on 1.21.6+, and Velocity cross-server player lists.

[![Modrinth](https://img.shields.io/modrinth/v/tpagui?label=Modrinth&logo=modrinth)](https://modrinth.com/plugin/tpagui)
[![GitHub](https://img.shields.io/github/v/release/RunicWonders/tpagui?label=GitHub&logo=github)](https://github.com/RunicWonders/tpagui/releases)

TpaGui does not implement teleportation itself — it acts as a GUI layer on top of your existing TPA plugin (EssentialsX, HuskHomes, etc.) and works with any of them.

## Quick Start

1. Drop the jar into your `plugins/` folder and restart the server
2. Check `commands.listen-commands` in `plugins/TpaGui/config.yml` matches your TPA plugin (default: `tpa`/`tpahere`)
3. Players run `/tpagui` to open the menu and click a head to send a request

## Features

### Java Edition
- Chest GUI with online player heads: left-click for `/tpa`, right-click for `/tpahere`
- Pagination with prev/next buttons; vanished players are hidden automatically (SuperVanish compatible)
- Configurable back button (custom material, runs any command on click, e.g. `/cd` to return to a main menu)
- On 1.21.6+, incoming requests can show a native accept/deny Dialog (requires a datapack; falls back to chat messages automatically)

### Bedrock Edition (Geyser/Floodgate)
- Bedrock players are detected automatically and served Cumulus forms instead
- Player list with avatars and pagination; teleport requests shown as accept/deny pop-ups
- Back button command can be configured separately from Java (e.g. `/gmenu`)

### Velocity Cross-server
- Syncs the network-wide player list from the Velocity proxy; send teleport requests across servers
- Optional toggle to hide players from other servers

### General
- Multi-language: Simplified Chinese / Traditional Chinese / English; all text customizable
- TPA, accept and deny commands fully configurable to fit any TPA plugin
- Run `/tpagui` from console to list online players
- Startup and periodic update checks (configurable interval)

## Commands
- `/tpagui` - Open the teleport request menu (aliases: `/tpag`, `/tgui`)

## Permissions
- `tpagui.use` - Use the /tpagui command (default: everyone)
- `tpagui.admin` - Receive update notifications (default: OP)

## Configuration

Main sections (see `config.yml` for full comments):

| Section | Description |
|---------|-------------|
| `language` | Interface language (zh_CN / zh_TW / en_US) |
| `velocity` | Cross-server sync toggle, server name, sync interval |
| `java-dialog-gui` | Players per page, avatar display and avatar API |
| `back-button` | Back button toggle, material, separate Java/Bedrock commands |
| `commands` | TPA command names, listened commands, accept/deny commands |
| `update-check` | Update check toggle and interval |

## Dependencies
- Required: Paper 1.21+ (or a fork)
- Optional: Floodgate (Bedrock form support)
- Optional: Velocity (cross-server player sync — the same jar works on the proxy)

## License

MIT, see [LICENSE](LICENSE).
