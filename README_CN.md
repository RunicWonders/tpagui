# TpaGui

[English](README.md) | **简体中文**

为 TPA 插件提供图形界面的 Paper 插件：Java 版受保护的箱子菜单、基岩版 Cumulus 表单、1.21.6+ 原生 Dialog 请求提示，以及 Velocity 跨服玩家列表。支持 Folia。

[![Modrinth](https://img.shields.io/modrinth/v/tpagui?label=Modrinth&logo=modrinth)](https://modrinth.com/plugin/tpagui)
[![GitHub](https://img.shields.io/github/v/release/RunicWonders/tpagui?label=GitHub&logo=github)](https://github.com/RunicWonders/tpagui/releases)

TpaGui 本身不实现传送逻辑，而是作为 GUI 层调用现有的 TPA 插件，例如 EssentialsX、HuskHomes 等。

## 快速开始

1. 将 jar 放入 `plugins/` 目录并重启服务器。
2. 在 `plugins/TpaGui/config.yml` 中确认 `commands.listen-commands` 与你的 TPA 插件一致。
3. 玩家输入 `/tpagui` 打开玩家菜单，点击头颅发起请求。

## 功能

### Java 版

- 箱子菜单展示在线玩家头颅，左键执行 `/tpa`，右键执行 `/tpahere`。
- 支持分页、可配置返回按钮，并自动过滤玩家看不到的隐身玩家。
- 收到传送请求时使用 Java 确认 GUI，支持接受/拒绝和请求过期校验。
- 禁止普通点击、Shift 点击、数字键换位、快捷栏交换和拖拽，防止玩家物品被移入菜单或丢失。
- 1.21.6+ 仍可在关闭 Java 确认 GUI 时使用原生 Dialog。

### 基岩版（Geyser/Floodgate）

- Floodgate 玩家自动使用 Cumulus 玩家菜单和被请求确认表单。
- 被请求表单会校验绑定的请求者、请求类型、过期时间，并且只允许一次接受/拒绝处理。
- 玩家打开聊天栏或切换到后台导致表单消失时，请求仍会保留，不会被当作已处理。
- 可以配置表单消失后的处理方式：提示输入 `/tpagui`，或自动重新发送表单：

```yaml
bedrock-request:
  reopen-mode: "prompt" # prompt 或 auto
  resend-interval-seconds: 3
```

`auto` 模式会在玩家在线、请求仍为当前请求且未过期时持续重发；接受、拒绝、请求被替换、过期或玩家离线后自动停止。

### Folia 和 Velocity

- 玩家相关的 GUI、表单和命令使用 Folia 玩家调度器；全局操作使用区域调度器。
- Velocity 可以同步全服玩家列表并路由跨服传送请求。

## 命令

- `/tpagui` - 打开传送请求菜单，或重新打开待处理的被请求确认界面。
- 别名：`/tpag`、`/tgui`。

## 权限

- `tpagui.use` - 使用 `/tpagui`（默认所有玩家）。
- `tpagui.admin` - 接收更新通知（默认 OP）。

## 配置

完整注释配置见 `src/main/resources/config.yml`。主要配置项：

| 配置节 | 说明 |
|--------|------|
| `language` | 界面语言：`zh_CN`、`zh_TW` 或 `en_US`。 |
| `request-expire-seconds` | 被请求的有效时间。 |
| `java-request-gui` | Java 版被请求确认 GUI 开关。 |
| `bedrock-request` | 基岩版表单重开模式和自动重发间隔。 |
| `java-dialog-gui` | 原生 Dialog 和玩家列表设置。 |
| `back-button` | 返回按钮及 Java/基岩版独立命令。 |
| `velocity` | 跨服同步设置。 |
| `commands` | TPA、监听、接受和拒绝命令名称。 |
| `update-check` | 更新检查及间隔。 |

## 依赖

- 必需：Paper 1.21+ 或兼容分支。
- 可选：Floodgate，用于基岩版表单支持。
- 可选：Velocity，用于跨服玩家同步。

## License

MIT，见 [LICENSE](LICENSE)。
