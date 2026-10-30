# TpaGui

[English](README.md) | **简体中文**

为现有 TPA 插件提供图形界面的 Paper 插件。支持 Java 版受保护箱子菜单、Floodgate 基岩版 Cumulus 表单、被请求确认 GUI、可选的原生 Dialog，以及 Velocity 跨服玩家列表。支持 Folia。

[![Modrinth](https://img.shields.io/modrinth/v/tpagui?label=Modrinth&logo=modrinth)](https://modrinth.com/plugin/tpagui)
[![GitHub](https://img.shields.io/github/v/release/RunicWonders/tpagui?label=GitHub&logo=github)](https://github.com/RunicWonders/tpagui/releases)

TpaGui 本身不实现传送逻辑，而是检测并以玩家身份执行现有 TPA 插件的命令，例如 EssentialsX 或 HuskHomes。

## 运行要求

- Paper 1.21+ 或兼容分支。
- 构建项目需要 Java 21 或更高版本。
- Floodgate 为可选依赖，用于基岩版表单。
- Velocity 为可选依赖，仅在需要跨服同步时使用。

## 安装

1. 下载或构建插件 jar，放入服务器的 `plugins/` 目录。
2. 首次启动服务器，生成 `config.yml` 和语言文件。
3. 打开 `plugins/TpaGui/config.yml`，确认 `commands.listen-commands`、`commands.accept` 和 `commands.deny` 与你的 TPA 插件命令一致。
4. 玩家输入 `/tpagui` 打开玩家菜单。

## 功能

### Java 版

- 受保护的箱子菜单，显示在线玩家头颅。
- 左键执行配置的 `tpa` 命令，右键执行配置的 `tpahere` 命令。
- 支持分页、可配置返回按钮，并过滤当前玩家看不到的隐身玩家。
- 默认使用 Java 被请求确认 GUI。接受和拒绝按钮会以目标玩家身份执行配置的命令。
- 被请求会绑定请求者和请求类型，在 `request-expire-seconds` 到期后失效，并且只能被处理一次。
- 输入 `/tpagui` 时，如果玩家有待处理的被请求，会重新打开确认界面，而不是打开玩家列表。
- 菜单打开期间会取消所有库存操作，包括普通点击、Shift 点击、数字键换位、快捷栏交换、双击收集和拖拽，防止玩家物品被移入菜单或从菜单中丢失。
- 如果关闭 `java-request-gui.enabled`，在支持 1.21.6+ 的服务器上可以使用原生 `/dialog` 流程，不支持时再降级为聊天提示。

### 基岩版（Geyser/Floodgate）

- Floodgate 玩家使用 Cumulus 玩家菜单和被请求确认表单。
- 被请求表单执行接受或拒绝命令前，会校验绑定的请求者、请求类型、过期时间，并保证请求只能消费一次。
- 如果玩家打开聊天栏、切换到后台，或者表单因其他原因消失导致 Floodgate 返回空响应，待处理请求会保留，不会被当作接受或拒绝。
- 输入 `/tpagui` 可以重新打开待处理的基岩版被请求表单。
- 表单消失后的恢复方式可配置：

```yaml
bedrock-request:
  # prompt：提示玩家输入 /tpagui
  # auto：按配置的间隔自动重发表单
  reopen-mode: "prompt"
  resend-interval-seconds: 3
```

`auto` 模式会在玩家在线、请求仍是当前请求且未过期时重新发送表单。接受、拒绝、请求被替换、请求过期或玩家离线后会停止重发。`prompt` 模式会发送本地化的 `request-reopen-hint` 提示。

### Folia 和 Velocity

- 玩家相关的 GUI、表单和命令使用 Folia 玩家调度器。
- 全局操作使用合适的 Folia 区域调度器或异步调度器。
- Velocity 可以同步全服玩家列表，并通过插件消息通道路由跨服请求。
- 配置 Velocity 模式后，参与同步的后端服务器可以使用同一个插件 jar。

## 命令

- `/tpagui` - 打开玩家菜单，或重新打开待处理的被请求。
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
| `bedrock-request` | 基岩版表单恢复模式和自动重发间隔。 |
| `java-dialog-gui` | 原生 Dialog 和玩家列表设置。 |
| `back-button` | 返回按钮材质及 Java/基岩版独立命令。 |
| `velocity` | 跨服同步设置。 |
| `commands` | TPA、监听、接受和拒绝命令名称。 |
| `update-check` | 更新检查及间隔。 |

修改配置后，请重启服务器，或通过你使用的服务器管理方式重新加载插件，使新配置生效。

## 构建

```text
mvn -B -ntp clean verify
```

带依赖的 shaded jar 会生成在 `target/` 目录。

## License

MIT，见 [LICENSE](LICENSE)。
