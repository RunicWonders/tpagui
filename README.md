# TpaGui

**English** | [简体中文](README_CN.md)

A Paper plugin that adds a graphical interface to your TPA plugin: a protected chest GUI for Java players, Cumulus forms for Bedrock players, native Dialog support on 1.21.6+, and Velocity cross-server player lists. Folia is supported.

[![Modrinth](https://img.shields.io/modrinth/v/tpagui?label=Modrinth&logo=modrinth)](https://modrinth.com/plugin/tpagui)
[![GitHub](https://img.shields.io/github/v/release/RunicWonders/tpagui?label=GitHub&logo=github)](https://github.com/RunicWonders/tpagui/releases)

TpaGui does not implement teleportation itself. It acts as a GUI layer on top of an existing TPA plugin such as EssentialsX or HuskHomes.

## Quick Start

1. Drop the jar into `plugins/` and restart the server.
2. Check `commands.listen-commands` in `plugins/TpaGui/config.yml` matches your TPA plugin.
3. Players run `/tpagui` to open the player menu and click a head to send a request.

## Features

### Java Edition

- Chest GUI with online player heads: left-click for `/tpa`, right-click for `/tpahere`.
- Pagination, configurable back button, and hidden-player filtering.
- Incoming requests use the Java confirmation GUI, with accept/deny actions and expiry checks.
- Inventory clicks, shift-clicks, hotbar swaps, number-key actions, and drag actions are blocked so player items cannot be moved into or lost in the menu.
- 1.21.6+ native Dialog support remains available when the Java confirmation GUI is disabled.

### Bedrock Edition

- Floodgate players use Cumulus forms for the player menu and incoming request confirmation.
- Incoming Bedrock requests validate the bound requester, request type, expiry, and one-time consumption before running accept/deny commands.
- If a form disappears because the player opens chat or switches to the background, the request is kept pending.
- Configure whether the player is prompted to run `/tpagui` or the form is resent automatically:

```yaml
bedrock-request:
  reopen-mode: "prompt" # prompt or auto
  resend-interval-seconds: 3
```

`auto` resends while the request is still online, current, and unexpired. It stops after accept, deny, replacement, expiry, or disconnect.

### Folia and Velocity

- Folia player and region schedulers are used for player-bound GUI, form, and command operations.
- Velocity can synchronize the network-wide player list and route cross-server requests.

## Commands

- `/tpagui` - Open the teleport request menu or reopen a pending incoming request.
- Aliases: `/tpag`, `/tgui`.

## Permissions

- `tpagui.use` - Use `/tpagui` (default: everyone).
- `tpagui.admin` - Receive update notifications (default: OP).

## Configuration

See `src/main/resources/config.yml` for the complete commented configuration. Main sections:

| Section | Description |
|---------|-------------|
| `language` | Interface language: `zh_CN`, `zh_TW`, or `en_US`. |
| `request-expire-seconds` | Lifetime of an incoming request. |
| `java-request-gui` | Enable the Java incoming-request confirmation GUI. |
| `bedrock-request` | Bedrock form reopen mode and automatic resend interval. |
| `java-dialog-gui` | Native Dialog and player-list settings. |
| `back-button` | Back button and separate Java/Bedrock commands. |
| `velocity` | Cross-server synchronization settings. |
| `commands` | TPA, listen, accept, and deny command names. |
| `update-check` | Update checks and interval. |

## Dependencies

- Required: Paper 1.21+ or a compatible fork.
- Optional: Floodgate for Bedrock form support.
- Optional: Velocity for cross-server player synchronization.

## License

MIT, see [LICENSE](LICENSE).
