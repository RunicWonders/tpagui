# TpaGui

**English** | [简体中文](README_CN.md)

A Paper plugin that adds a graphical interface to an existing TPA plugin. It provides a protected chest GUI for Java players, Cumulus forms for Floodgate Bedrock players, incoming-request confirmation GUIs, optional native Dialog support, and Velocity cross-server player lists. Folia is supported.

[![Modrinth](https://img.shields.io/modrinth/v/tpagui?label=Modrinth&logo=modrinth)](https://modrinth.com/plugin/tpagui)
[![GitHub](https://img.shields.io/github/v/release/RunicWonders/tpagui?label=GitHub&logo=github)](https://github.com/RunicWonders/tpagui/releases)

TpaGui does not implement teleportation itself. It detects and executes the commands provided by an existing TPA plugin such as EssentialsX or HuskHomes.

## Requirements

- Paper 1.21+ or a compatible fork.
- Java 21 or newer to build the project.
- Floodgate is optional and required for Bedrock forms.
- Velocity is optional and required only for cross-server synchronization.

## Installation

1. Download or build the plugin jar and place it in the server's `plugins/` folder.
2. Restart the server once so `config.yml` and language files are generated.
3. Check `plugins/TpaGui/config.yml` and make sure `commands.listen-commands`, `commands.accept`, and `commands.deny` match the commands from your TPA plugin.
4. Players can use `/tpagui` to open the player menu.

## Features

### Java Edition

- Protected chest GUI with online player heads.
- Left-click sends the configured `tpa` command; right-click sends the configured `tpahere` command.
- Pagination, configurable back button, and hidden-player filtering.
- Incoming requests use the Java confirmation GUI by default. The accept and deny buttons execute the configured commands as the target player.
- Incoming requests are bound to the requester and request type, expire after `request-expire-seconds`, and can only be consumed once.
- `/tpagui` reopens a pending incoming request instead of opening the player list.
- All click modes are cancelled while the menu is open, including normal clicks, shift-clicks, number-key swaps, hotbar swaps, double-click collection, and drag operations. Player inventory items cannot be moved into or lost in the GUI.
- If `java-request-gui.enabled` is disabled, the plugin can fall back to the native `/dialog` flow on supported 1.21.6+ servers, then to chat notification.

### Bedrock Edition

- Floodgate players use Cumulus forms for the player menu and incoming request confirmation.
- Incoming forms validate the bound requester, request type, expiry, and one-time consumption before running accept or deny commands.
- If Floodgate returns an empty response because the player opened chat, switched to the background, or the form otherwise disappeared, the pending request is kept. It is never treated as an accepted or denied request.
- `/tpagui` reopens the pending Bedrock request in a new form.
- The form recovery behavior is configurable:

```yaml
bedrock-request:
  # prompt: tell the player to run /tpagui
  # auto: resend the form after the configured interval
  reopen-mode: "prompt"
  resend-interval-seconds: 3
```

With `auto`, the form is resent while the player is online, the request is still the current request, and it has not expired. Resending stops after accept, deny, request replacement, expiry, or disconnect. With `prompt`, the player receives the localized `request-reopen-hint` message.

### Folia and Velocity

- Player-bound GUI, form, and command operations use the Folia player scheduler.
- Global operations use the appropriate Folia region or async scheduler.
- Velocity can synchronize the network-wide player list and route cross-server requests through the plugin message channel.
- The same plugin jar can be used on the participating backend servers when Velocity mode is configured.

## Known Limitations

TpaGui observes the player's TPA command because third-party TPA plugins do not expose a common request-created event. It cannot know whether the external plugin ultimately accepted the command. A confirmation GUI may therefore appear even when the player lacks permission, the target has disabled requests, or the TPA plugin rejects the request for another reason.

## Commands

- `/tpagui` - Open the player menu, or reopen a pending incoming request.
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
| `bedrock-request` | Bedrock form recovery mode and automatic resend interval. |
| `java-dialog-gui` | Native Dialog and player-list settings. |
| `back-button` | Back button material and separate Java/Bedrock commands. |
| `velocity` | Cross-server synchronization settings. |
| `commands` | TPA, listen, accept, and deny command names. |
| `update-check` | Update checks and interval. |

Accept and deny commands run exactly as configured; `{player}` is replaced with the requester name when present. If the placeholder is missing and `commands.append-player-if-missing` is `true` (the default, kept for backward compatibility), the requester name is appended to the end of the command. Set it to `false` to execute commands exactly as written:

```yaml
commands:
  accept:
    - "tpaccept {player}"
  deny:
    - "tpdeny {player}"
```

After changing configuration, restart the server or reload the plugin through your normal server management workflow so the new values are loaded.

## Building

```text
mvn -B -ntp clean verify
```

The shaded jar is written to `target/`.

## License

MIT, see [LICENSE](LICENSE).
