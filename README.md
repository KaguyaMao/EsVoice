# Esvoice

Esvoice 是面向 Minecraft Forge 1.20.1 的战术语音扩展。它在 Simple Voice Chat 的公共近距离语音之外增加班组频道和指挥频道，并使用 Espetro 的阵营、小队、队长及指挥官状态决定无线语音的发送范围。

## 功能

- 公共频道：原版 Simple Voice Chat 近距离语音。
- 班组频道：只发送给同一 Espetro 小队的玩家。
- 指挥频道：只发送给同阵营的指挥官和小队队长。
- 服务端确认频道权限，拒绝无权限的发射请求。
- 独立说话者 HUD、头像、静音和语音禁用状态提示。
- 战术语音设置界面及 Simple Voice Chat 界面替换。
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
| Espetro | 1.0.2-final 或更高 |

客户端和服务器必须安装相同版本的 Esvoice。当前内部网络协议为 `2`。

## 默认按键

| 按键 | 功能 |
| --- | --- |
| `V` | 公共频道按住说话 |
| `B` | 班组频道按住说话 |
| `N` | 指挥频道按住说话 |
| `O` | 打开战术语音设置 |

按键可在 Minecraft 的“控制”页面重新绑定。

## 构建

项目未提交 Unix `gradlew` 启动脚本，需使用兼容 ForgeGradle 6 的 Gradle 8.x：

```bash
cd /home/shushu/IdeaProjects/Esvoice
gradle build
```

若系统 Gradle 为 9.x，请使用本机 Gradle 8.5：

```bash
/home/shushu/.gradle/wrapper/dists/gradle-8.5-bin/5t9huq95ubn472n8rpzujfbqh/gradle-8.5/bin/gradle build
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
