package org.esvoice.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.client.ClientPlayerStateManager;
import de.maxhenkel.voicechat.voice.client.ClientVoicechat;
import org.espetromini.api.MiniClientAPI;
import org.espetromini.client.MiniKeys;
import org.espetromini.config.MiniClientConfig;
import org.esvoice.Esvoice;
import org.esvoice.voice.VoiceChannel;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Esvoice.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EsvoiceClientEvents {

    private static UUID cachedPlayerId;
    private static String cachedPlayerName;

    /**
     * 上一次推送给 Simple Voice Chat 的状态。
     *
     * <p>**必须**只与我们自己记录的值比较：SVC 的 {@code isDisabled()} 在语音尚未连接/不可用时
     * 会返回 {@code true}，而 {@code setDisabled()} 每次都写配置文件并发送 {@code UpdateStatePacket}。
     * 若拿 SVC 的 getter 做比较，玩家想开语音时会每 tick 重设一次 → 包速率超限被 SVC 踢出
     * （日志：{@code [voicechat] Player xxx exceeded packet rate limit}）。
     */
    private static Boolean lastPushedMuted;
    private static Boolean lastPushedDisabled;

    private EsvoiceClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean connected = minecraft.getConnection() != null && minecraft.player != null;
        ClientPlayerStateManager voiceState = ClientManager.getPlayerStateManager();
        ClientVoicechat voiceClient = ClientManager.getClient();
        boolean voiceConnected = connected
            && !voiceState.isDisconnected()
            && voiceClient != null
            && voiceClient.getConnection() != null;

        // 键位归 espetromini 所有（可在原版控制界面重绑）：这里只轮询点击并翻转它的持久化设置。
        if (connected && minecraft.screen == null && MiniKeys.ready()) {
            if (MiniKeys.MUTE_MIC.consumeClick()) {
                MiniClientConfig.toggleMicMuted();
            }
            if (MiniKeys.VOICE_TOGGLE.consumeClick()) {
                MiniClientConfig.toggleVoiceEnabled();
            }
            if (MiniKeys.SPEAKER_HUD.consumeClick()) {
                MiniClientConfig.toggleSpeakerHud();
            }
            if (EsvoiceClientModEvents.TOGGLE_RECORDING.consumeClick()) {
                toggleRecording(minecraft, voiceClient);
            }
        }

        boolean voiceEnabled = MiniClientAPI.voiceEnabled();
        // 把 espetromini 的语音状态同步给 Simple Voice Chat：
        // 总开关关闭 → 不听也不说（disabled）；麦克风静音 → 只影响"说"（muted）。
        // 仅在「我们自己记录的上次推送值」变化时推送一次（SVC 的 getter 不能用作比较基准，
        // 否则未连接时 isDisabled() 恒为 true 会导致每 tick 发包 + 写盘，被 SVC 限速踢出）。
        if (connected && voiceConnected) {
            boolean muted = MiniClientAPI.micMuted();
            boolean disabled = !voiceEnabled;
            if (lastPushedMuted == null || lastPushedMuted != muted) {
                lastPushedMuted = muted;
                voiceState.setMuted(muted);
            }
            if (lastPushedDisabled == null || lastPushedDisabled != disabled) {
                lastPushedDisabled = disabled;
                voiceState.setDisabled(disabled);
            }
        }

        // 总开关关闭时强制回到公共麦且不发麦克风；不影响其它玩家（状态只存在本地）。
        VoiceChannel channel = VoiceChannel.PUBLIC;
        if (connected && minecraft.screen == null && voiceEnabled && MiniKeys.ready()) {
            if (MiniKeys.COMMAND_VOICE.isDown()) {
                channel = VoiceChannel.COMMAND;
            } else if (MiniKeys.SQUAD_VOICE.isDown()) {
                channel = VoiceChannel.SQUAD;
            }
        }
        // 界面打开时 KeyMapping 不会更新，改用 GLFW 直接轮询（聊天框里打字时不抢按键）。
        boolean publicKeyDown = false;
        if (connected && voiceEnabled && MiniKeys.ready()) {
            if (minecraft.screen == null) {
                publicKeyDown = MiniKeys.PUBLIC_VOICE.isDown();
            } else if (!(minecraft.screen instanceof ChatScreen)) {
                publicKeyDown = EsvoiceVoiceKeys.isBindingHeld(MiniKeys.PUBLIC_VOICE);
            }
        }
        RadioTransmitController.update(channel, publicKeyDown, connected);

        if (voiceConnected && voiceEnabled && RadioTransmitController.shouldActivateMicrophone()
                && !voiceState.isMuted() && !voiceState.isDisabled()) {
            VoiceChannel activeChannel = RadioTransmitController.getActiveChannel();
            UUID playerId = minecraft.player.getUUID();
            if (!playerId.equals(cachedPlayerId)) {
                cachedPlayerId = playerId;
                cachedPlayerName = minecraft.player.getName().getString();
            }
            VoiceSpeakerHud.onLocalTransmit(playerId, cachedPlayerName, activeChannel);
        } else if (!connected) {
            cachedPlayerId = null;
            cachedPlayerName = null;
        }
    }

    private static void toggleRecording(Minecraft minecraft, ClientVoicechat client) {
        if (client == null || client.getConnection() == null) {
            minecraft.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "gui.esvoice.recording.not_connected"), true);
            return;
        }
        if (client.getInitializationData() == null || !client.getInitializationData().allowRecording()) {
            minecraft.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "gui.esvoice.recording.not_allowed"), true);
            return;
        }
        client.toggleRecording();
        minecraft.player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        client.getRecorder() != null
                                ? "gui.esvoice.recording.started"
                                : "gui.esvoice.recording.stopped"), true);
    }
}
