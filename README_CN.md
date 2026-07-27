# TpaGui

[English](README.md) | **简体中文**

为 TPA 插件提供图形界面的 Paper 插件：Java 版箱子菜单、基岩版表单、1.21.6+ 原生 Dialog 请求提示，并支持 Velocity 跨服玩家列表。

[![Modrinth](https://img.shields.io/modrinth/v/tpagui?label=Modrinth&logo=modrinth)](https://modrinth.com/plugin/tpagui)
[![GitHub](https://img.shields.io/github/v/release/RunicWonders/tpagui?label=GitHub&logo=github)](https://github.com/RunicWonders/tpagui/releases)

TpaGui 本身不实现传送逻辑，而是作为 GUI 层调用你现有的 TPA 插件（EssentialsX、HuskHomes 等）的命令，可与之任意搭配。

## 快速开始

1. 将 jar 放入 `plugins/` 目录，重启服务器
2. 在 `plugins/TpaGui/config.yml` 的 `commands.listen-commands` 中确认你的 TPA 命令（默认 `tpa`/`tpahere`）
3. 玩家输入 `/tpagui` 打开菜单，点击头颅发起请求

## 功能

### Java 版
- 箱子菜单展示在线玩家头颅，左键 `/tpa`、右键 `/tpahere`
- 多页显示与翻页按钮，自动隐藏隐身玩家（兼容 SuperVanish）
- 可配置返回按钮（自定义材质，点击执行任意命令，如 `/cd` 返回主菜单）
- 1.21.6+ 收到传送请求时可弹出原生 Dialog 接受/拒绝（需自备数据包，未安装时自动降级为聊天提示）

### 基岩版（Geyser/Floodgate）
- 自动识别基岩版玩家，改用 Cumulus 表单
- 玩家列表支持头像、分页，传送请求以弹窗形式接受/拒绝
- 返回按钮命令可与 Java 版分开配置（如 `/gmenu`）

### Velocity 跨服
- 从 Velocity 代理同步全服玩家列表，跨服发起传送请求
- 可配置是否显示其他服务器的玩家

### 通用
- 多语言：简体中文 / 繁体中文 / English，所有文本可自定义
- TPA 命令、接受/拒绝命令全部可配置，适配各类 TPA 插件
- 控制台执行 `/tpagui` 可查看在线玩家列表
- 启动与周期性更新检查（可配置间隔）

## 命令
- `/tpagui` - 打开传送请求菜单（别名: `/tpag`, `/tgui`）

## 权限
- `tpagui.use` - 使用 /tpagui 命令（默认所有玩家）
- `tpagui.admin` - 接收更新通知（默认 OP）

## 配置

主要配置项（完整注释见 `config.yml`）：

| 配置节 | 说明 |
|--------|------|
| `language` | 界面语言（zh_CN / zh_TW / en_US） |
| `velocity` | 跨服同步开关、本服名称、同步间隔 |
| `java-dialog-gui` | 每页玩家数、头像显示与头像 API |
| `back-button` | 返回按钮开关、材质、Java/基岩各自执行的命令 |
| `commands` | TPA 命令名、监听命令列表、接受/拒绝命令 |
| `update-check` | 更新检查开关与间隔 |

## 依赖
- 必需: Paper 1.21+（或其分支）
- 可选: Floodgate（基岩版表单支持）
- 可选: Velocity（跨服玩家同步，代理端放入同一 jar）

## License

MIT，见 [LICENSE](LICENSE)。
