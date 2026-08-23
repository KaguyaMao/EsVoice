# Esvoice

Esvoice 是面向 Minecraft Forge 1.20.1 的战术语音扩展。它在 Simple Voice Chat 的近距离语音之外增加小队和指挥频道，并使用 Espetro 的阵营、小队、队长及指挥官状态决定语音路由。

## 功能

- 本地频道：使用 Simple Voice Chat 近距离衰减，只发送给同一 Espetro 阵营。
- 小队频道：只发送给同一阵营、同一 Espetro 小队的玩家。
- 指挥频道：只发送给同阵营的指挥官和小队队长。
- 服务端确认频道权限，拒绝无权限的发射请求。
- 独立说话者 HUD、头像、静音和语音禁用状态提示。
- MUtil 战术语音设置中心，统一管理音频、设备、音量、键位和高级选项。
- 完全禁用 Simple Voice Chat 原生设置界面与键位，由 Esvoice 统一接管。
- 首次连接自动显示可跳过、可重播的新手教程。
- 无线音频队列限流、路由缓存和低带宽 HUD 刷新。

## 环境与依赖

| 项目 | 版本 |
| --- | --- |
| Java | 17 |
| Minecraft | 1.20.1 |
| Forge | 47.4.20 |
| Simple Voice Chat API | 2.6.13 或更高 |
| Simple Voice Chat | 开发环境使用 2.6.18 |
| MUtil | 6.1.1 或更高 |
| Espetro | 1.1.0-alpha 或更高 |

客户端和服务器必须安装相同版本的 Esvoice。当前内部网络协议为 `4`。

## 默认按键

| 按键 | 功能 |
| --- | --- |
| `V` | 本地语音按住说话 |
| `B` | 小队频道按住说话 |
| `G` | 指挥频道按住说话 |
| `O` | 打开战术语音设置 |
| `M` | 切换麦克风静音 |
| `N` | 切换总语音开关 |
| `H` | 切换语音 HUD |
| 未绑定 | 切换语音录音 |

按键可在 Minecraft 的“控制”页面或 Esvoice 设置中心的“键位”页重新绑定；界面始终显示当前实际绑定。

## 构建

项目包含 Gradle Wrapper：

```bash
cd /home/shu/IdeaProjects/Esvoice
./gradlew build
```

产物位于 `build/libs/esvoice-<version>.jar`。

## 文档

- [配置文档](docs/CONFIGURATION.md)
- [开发文档与类参考](docs/DEVELOPMENT.md)

## 项目关系

```text
Espetro ──提供阵营/小队/权限──> Esvoice
Simple Voice Chat ──提供 Opus 与连接──> Esvoice
Esvoice ──路由无线语音与 HUD 元数据──> 客户端
```
