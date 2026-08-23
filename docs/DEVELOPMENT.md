# Esvoice 开发文档

## 架构与数据流

```text
客户端按键
  -> RadioTransmitController
  -> VoiceModePacket
  -> 服务端 VoiceChannelRouter 校验 Espetro 权限
  -> VoiceModeAckPacket
  -> MicrophoneProcessorMixin 允许麦克风发射
  -> Simple Voice Chat MicrophonePacketEvent
  -> VoiceChannelRouter 转为 StaticSoundPacket
  -> 同阵营本地、小队或指挥链
  -> VoiceSpeakingPacket
  -> VoiceSpeakerHud
```

无线音频仍使用 Simple Voice Chat 的 Opus 数据和 UDP 通道；Forge `SimpleChannel` 只承载频道请求、确认和 HUD 元数据。

## 入口类

### `Esvoice`

Forge 主入口，负责网络注册、Forge 事件注册和玩家退出清理。战场 `BattlefieldLifecycleEvent.Cleared` 时调用 `VoiceChannelRouter.clearAll()`。当前还包含 Forge 模板生成的示例方块、物品和配置日志；这些内容与语音功能无关。

### `EsvoiceVoicechatPlugin`

Simple Voice Chat 插件入口。`registerEvents` 将服务端语音事件交给 `VoiceChannelRouter`。

```java
registration.registerEvent(
    MicrophonePacketEvent.class,
    VoiceChannelRouter::onMicrophonePacket,
    100
);
```

## 公共类型与示例

### `VoiceChannel`

| 枚举值 | ID | 分类 ID | 含义 |
| --- | ---: | --- | --- |
| `PUBLIC` | 0 | 空 | 同阵营 Simple Voice Chat 近距离语音 |
| `SQUAD` | 1 | `esvoice_squad` | 同阵营、同小队无线语音 |
| `COMMAND` | 2 | `esvoice_command` | 同阵营指挥链无线语音 |

```java
VoiceChannel channel = VoiceChannel.fromId(rawId);
if (channel.isRadio()) {
    String category = channel.getCategoryId();
}
```

未知 ID（含已移除的旧 DIRECT id `3`）会安全回退到 `PUBLIC`。

### `VoiceChannelRouter`

服务端权威路由器。权限查询对齐 Espetro 1.1：

| 需求 | 来源 |
| --- | --- |
| 阵营 | `EspetroAPI.getPlayerTeam(player)` |
| 小队 | `EspetroAPI.getPlayerSquadId(uuid)` / `SquadManager.NO_SQUAD` |
| 队长 | `SquadManager.getInstance().isSquadLeader(uuid)` |
| 指挥官 | `EspetroAPI.isCommander(uuid)` |

本地（`PUBLIC`）近距离包：仅当**双方都有阵营且阵营不同**时取消；任一方未选阵营时不静音。  
**大厅（主世界）**：公共麦全维度广播，不按阵营/距离过滤。  
旁观玩家的本地麦由路由器接管（Espetro MatchHold 强制 SPECTATOR 时 SVC 默认不广播），战场上按 `getBroadcastRange()` 与阵营规则发送 `StaticSoundPacket`。

```java
VoiceChannel confirmed = VoiceChannelRouter.setRequestedChannel(player, VoiceChannel.SQUAD);
if (confirmed == VoiceChannel.PUBLIC) {
    // 玩家无权限，路由器已向玩家显示原因。
}
```

玩家退出时必须清理：

```java
VoiceChannelRouter.clearPlayer(player.getUUID());
```

整局/战场结束：

```java
VoiceChannelRouter.clearAll();
```

内部约束：

- `REQUESTED_CHANNELS` 可被语音线程读取，因此使用并发映射。
- Minecraft 玩家、小队和指挥官状态只在服务器线程访问。
- 每名发射者最多缓存 10 个无线帧；过载时丢弃最旧帧以保持实时性。
- 玩家路由状态每个服务器 tick 最多重建一次；权限不要缓存超过一 tick。
- HUD 每 600 ms 刷新，持续讲话时只在首包携带玩家名。

### `RadioTransmitController`

客户端发射状态机。开始无线发射必须等待服务端确认，停止发射立即生效。

```java
RadioTransmitController.update(
    VoiceChannel.SQUAD,
    false, // 公共按键是否按下
    minecraft.getConnection() != null
);

if (RadioTransmitController.shouldActivateMicrophone()) {
    VoiceChannel active = RadioTransmitController.getActiveChannel();
}
```

无权限请求每秒最多重试一次，防止按住按键时产生网络包风暴。

### `VoiceSpeakerHud`

```java
VoiceSpeakerHud.onSpeaking(playerId, playerName, VoiceChannel.COMMAND);
VoiceSpeakerHud.onLocalTransmit(playerId, playerName, VoiceChannel.PUBLIC, muted, disabled);
```

`speakerName` 对内部刷新包可以为 `null`；普通扩展调用建议始终提供名字。

### `EsvoiceNetwork`

```java
EsvoiceNetwork.sendMode(new VoiceModePacket(VoiceChannel.SQUAD));
EsvoiceNetwork.acknowledgeMode(player, new VoiceModeAckPacket(confirmed));
```

当前协议版本为 `4`（去掉队长直连与 `VoiceModePacket` 目标小队字段）。不要从客户端调用服务端定向发送方法，也不要在网络线程直接修改 Minecraft 世界状态。

## 网络包

| 类 | 方向 | 内容 | 用途 |
| --- | --- | --- | --- |
| `VoiceModePacket` | 客户端 -> 服务端 | 1 字节频道 ID | 请求频道 |
| `VoiceModeAckPacket` | 服务端 -> 客户端 | 1 字节频道 ID | 确认或回退公共频道 |
| `VoiceSpeakingPacket` | 服务端 -> 客户端 | UUID、带标志频道头、可选名字 | 刷新说话者 HUD |

添加包时：

1. 在 `EsvoiceNetwork.register()` 末尾注册，保持 ID 顺序稳定。
2. 明确 `NetworkDirection`。
3. 在 `handle` 中通过 `enqueueWork` 切换到主线程。
4. 修改现有包编码时递增 `PROTOCOL`，并要求客户端和服务器同步更新。

## 客户端类

| 类 | 职责 |
| --- | --- |
| `EsvoiceClientModEvents` | 注册八项 Esvoice 语音按键和 HUD overlay |
| `EsvoiceClientEvents` | 处理全部语音动作、首次教程、发射状态和本地 HUD |
| `EsvoiceVoiceChatScreen` | MUtil 分页设置中心及 Simple Voice Chat 配置适配 |
| `EsvoiceTutorialScreen` | 首次自动、可跳过且可重播的 MUtil 新手教程 |
| `EsvoiceMutilScreen` | MUtil 页面基类和输入分派 |
| `EsvoiceMutilWidgets` | 动态按钮、滑块、滚动、文本输入和真实键位捕获组件 |
| `VoiceSpeakerHud` | 活跃说话者列表、头像与状态渲染 |
| `RadioTransmitController` | 客户端频道请求/确认状态机 |
| `VoiceNameTagIconFilter` | 仅同 Espetro 阵营显示 SVC 名牌图标 |

客户端类不得从公共类的静态初始化器直接引用。当前网络包通过延迟反射/方法句柄访问客户端处理器，避免专用服务器解析客户端类。

## Mixin 类

| 类 | 注入目标 | 作用 |
| --- | --- | --- |
| `ForgePermissionMixin` | Simple Voice Chat `ForgePermission` | 强制 `voicechat.speak` / `voicechat.listen` 恒 true，避免权限模组收成仅 OP |
| `ChatComponentMixin` | 原版 `ChatComponent` | 将聊天面板高度缩为约 62%，为上方说话者 HUD 留空 |
| `MicrophoneProcessorMixin` | Simple Voice Chat `MicrophoneProcessor` | 无线键按下时激活麦克风并标记无线帧 |
| `KeyEventsMixin` | Simple Voice Chat `KeyEvents` | 完全禁用原生键位处理 |
| `VoicechatKeyRegistrationMixin` | Simple Voice Chat Forge 兼容层 | 阻止原生键位进入控制设置 |
| `ForgeVoicechatClientModMixin` | Simple Voice Chat Forge 配置工厂 | 将模组列表配置入口交给 Esvoice |
| `RenderEventsMixin` | Simple Voice Chat `RenderEvents` | 隐藏原语音 HUD |
| `MinecraftScreenMixin` | `Minecraft.setScreen` | 拦截所有原生语音 GUI 并替换为 Esvoice 页面 |

升级 Simple Voice Chat 后应首先验证 Mixin 方法名和签名。

## 完整类索引

| 包 | 类 |
| --- | --- |
| `org.esvoice` | `Esvoice`, `Config`, `EsvoiceVoicechatPlugin` |
| `org.esvoice.voice` | `VoiceChannel`, `VoiceChannelRouter` |
| `org.esvoice.network` | `EsvoiceNetwork`, `VoiceModePacket`, `VoiceModeAckPacket`, `VoiceSpeakingPacket` |
| `org.esvoice.mixin` | `ForgePermissionMixin`, `ChatComponentMixin`, `MicrophoneProcessorMixin`, `KeyEventsMixin`, `VoicechatKeyRegistrationMixin`, `ForgeVoicechatClientModMixin`, `RenderEventsMixin`, `MinecraftScreenMixin` |
| `org.esvoice.client` | `EsvoiceClientEvents`, `EsvoiceClientModEvents`, `RadioTransmitController`, `VoiceSpeakerHud`, `EsvoiceVoiceChatScreen`, `EsvoiceTutorialScreen`, `EsvoiceMutilScreen`, `EsvoiceMutilWidgets`, `VoiceNameTagIconFilter`, `EsvoiceAuraTipNotifications` |
| `org.esvoice.mixin` | `KeyEventsMixin`, `VoicechatKeyRegistrationMixin`, `ForgeVoicechatClientModMixin`, `MinecraftScreenMixin`, `MicrophoneProcessorMixin`, `RenderEventsMixin` |

## 开发与验证

```bash
# 编译
gradle compileJava

# 完整构建与重混淆
gradle build

# 客户端开发运行
gradle runClient

# 专用服务器开发运行
gradle runServer
```

提交前至少验证：

- 公共、班组、指挥三个频道分别可用。
- 无小队玩家无法使用班组频道。
- 普通队员无法使用指挥频道。
- 队长/指挥官变更后一 tick 内路由更新。
- 玩家退出或战场清理后没有残留 HUD 或队列状态。
- 客户端和专用服务器均可启动。
- `gradle build` 生成可重混淆 JAR。
