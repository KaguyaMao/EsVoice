# Esvoice 配置文档

## 配置来源概览

| 配置 | 位置 | 作用 | 重载方式 |
| --- | --- | --- | --- |
| Esvoice Forge 配置 | `config/esvoice-common.toml` | 当前仅保留 Forge 示例字段 | 重启游戏/服务器 |
| Simple Voice Chat 服务端配置 | 世界目录 `serverconfig/voicechat-server.toml` 或插件实际生成路径 | 语音端口、距离、编解码和服务端策略 | 按 Simple Voice Chat 规则重载 |
| Simple Voice Chat 客户端配置 | `config/voicechat/voicechat-client.properties` | 输入输出设备、音量、激活方式、缓冲与 HUD | Esvoice 设置中心即时保存 |
| 玩家/分类音量 | `config/voicechat/player-volumes.properties`、`category-volumes.properties` | 玩家和语音分类音量 | Esvoice“音量”页即时保存 |
| 按键配置 | Minecraft `options.txt` | 八项 Esvoice 语音操作 | 控制设置或 Esvoice“键位”页即时生效 |

Esvoice 没有用于语音频道权限或距离的业务 JSON。频道成员由 Espetro 的实时状态决定，音频质量和基础近距离语音由 Simple Voice Chat 决定。
Simple Voice Chat 的原生设置页、首次向导和键位注册均被禁用，但其客户端配置文件继续作为音频后端的持久化来源。
Esvoice“高级”页还覆盖录音质量/目录、群组与状态 HUD、假玩家显示、原生音频库、Java 麦克风实现及 macOS 权限检查；标注需要重启的选项不会热重载。

## 构建配置

`gradle.properties` 中影响兼容性的字段：

| 字段 | 当前值 | 说明 |
| --- | --- | --- |
| `minecraft_version` | `1.20.1` | 目标游戏版本 |
| `forge_version` | `47.4.20` | Forge 开发版本 |
| `voicechat_api_version` | `2.6.13` | 编译 API 下限 |
| `voicechat_version` | `1.20.1-2.6.18` | 开发运行时语音模组 |
| `espetro_version` | `1.1.0-alpha` | Espetro 编译与声明下限 |
| `mod_version` | `1.0.0-final` | 构建产物版本 |

`mods.toml` 还声明 MUtil `>=6.1.1`。升级任何依赖时，应同时修改 `gradle.properties`、`build.gradle` 依赖和 `mods.toml` 版本范围，并完成客户端/专用服务器启动测试。

## `esvoice-common.toml`

该文件由 `org.esvoice.Config` 注册。当前字段来自 Forge 工程模板，只用于启动日志，不参与频道、权限、带宽或音频质量计算。

```toml
logDirtBlock = true
magicNumber = 42
magicNumberIntroduction = "The magic number is... "
items = ["minecraft:iron_ingot"]
```

| 字段 | 类型 | 默认值 | 范围/格式 | 实际用途 |
| --- | --- | --- | --- | --- |
| `logDirtBlock` | Boolean | `true` | `true`/`false` | 是否输出示例方块日志 |
| `magicNumber` | Integer | `42` | `0..2147483647` | 输出示例数字日志 |
| `magicNumberIntroduction` | String | `The magic number is... ` | 任意字符串 | 示例日志前缀 |
| `items` | String[] | `minecraft:iron_ingot` | 必须是已注册物品 ID | 启动时输出物品日志 |

这些字段可以保持默认。调整它们不会改变语音功能。

## 外部语音配置

Esvoice 转发 Simple Voice Chat 已编码的 Opus 帧，不重新编码。因此带宽与音质主要由 Simple Voice Chat 的服务端/客户端配置决定。修改外部配置时应注意：

- 不要让客户端与服务器使用不兼容的 Simple Voice Chat 版本。
- 语音 UDP 端口必须在防火墙和容器端口映射中开放。
- Esvoice 的小队与指挥频道不使用近距离衰减，但仍尊重连接是否在线、是否禁用语音和分类音量。
- 分类 ID 固定为 `esvoice_squad` 与 `esvoice_command`，可分别调整音量。
- 本地频道沿用 Simple Voice Chat 的近距离衰减，但服务端会取消向敌方阵营发送的声音包。

## JSON 资源配置

### `esvoice.mixins.json`

该文件是构建资源，不是服务器运行参数。

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "org.esvoice.mixin",
  "refmap": "esvoice.refmap.json",
  "compatibilityLevel": "JAVA_17",
  "mixins": [
    "ForgePermissionMixin"
  ],
  "client": [
    "KeyEventsMixin",
    "ForgeVoicechatClientModMixin",
    "MinecraftScreenMixin",
    "MicrophoneProcessorMixin",
    "RenderEventsMixin",
    "VoicechatKeyRegistrationMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

| 字段 | 说明 |
| --- | --- |
| `required` | Mixin 失败时阻止模组以不完整状态继续运行 |
| `package` | 所有 Mixin 类的基础包 |
| `refmap` | ForgeGradle 生成的映射文件 |
| `compatibilityLevel` | 必须保持 `JAVA_17` |
| `mixins` | 通用侧 Mixin（含强制放行 `voicechat.speak` / `voicechat.listen` 的 `ForgePermissionMixin`） |
| `client` | 只允许放客户端目标的 Mixin |
| `defaultRequire` | 每个注入点默认至少匹配一次 |

新增 Mixin 示例：

```json
{
  "client": [
    "KeyEventsMixin",
    "MyNewClientMixin"
  ]
}
```

对应类必须位于 `src/main/java/org/esvoice/mixin/MyNewClientMixin.java`，并确保不会在专用服务器加载客户端类。

### 语言 JSON

语言文件位于：

- `assets/esvoice/lang/zh_cn.json`
- `assets/esvoice/lang/en_us.json`

键名必须在两个文件中保持一致：

```json
{
  "key.categories.esvoice": "Esvoice 语音",
  "key.esvoice.public_voice": "本地语音（按住说话）",
  "hud.esvoice.channel.squad": "班组"
}
```

新增翻译时使用 `域.模组.名称` 风格，并在代码中通过 `Component.translatable("...")` 引用。

### `pack.mcmeta`

```json
{
  "pack": {
    "description": "esvoice resources",
    "pack_format": 15
  }
}
```

Minecraft 1.20.1 的资源包格式为 `15`。升级 Minecraft 时需要同步检查该值。

## 配置排错

1. 班组频道被拒绝：确认玩家已加入 Espetro 小队。
2. 指挥频道被拒绝：确认玩家是当前阵营指挥官或小队队长。
3. 能看到 HUD 但听不到声音：检查 Simple Voice Chat 连接状态和分类音量。
4. 完全无法连接：检查 UDP 端口、客户端/服务器模组版本以及网络协议版本（当前协议 `4`）。
5. 修改语言 JSON 无效：重新构建 JAR 或使用资源包覆盖，不要修改 `build/resources` 下的生成副本。
6. 只有管理员/开作弊能说话：Esvoice 会强制放行 `voicechat.speak` 与 `voicechat.listen`（`ForgePermissionMixin`）。仍异常时检查是否旁观（Espetro MatchHold）或双方阵营是否对立。
7. 旁观阶段本地语音：Esvoice 会自行按距离路由旁观者的本地麦，不再依赖 `spectator_interaction`。
8. 大厅（主世界）公共麦：全大厅互听，不受阵营与近距离限制；说话者 HUD 在聊天栏上方以缩小头像+名字显示。
