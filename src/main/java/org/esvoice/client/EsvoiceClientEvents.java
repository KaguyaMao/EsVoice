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
import de.maxhenkel.voicechat.VoicechatClient;
import org.esvoice.Esvoice;
import org.esvoice.voice.VoiceChannel;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Esvoice.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EsvoiceClientEvents {

    private static UUID cachedPlayerId;
    private static String cachedPlayerName;

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

        boolean muteClicked = EsvoiceClientModEvents.MUTE_MICROPHONE.consumeClick();
        boolean disableClicked = EsvoiceClientModEvents.DISABLE_VOICE.consumeClick();
        boolean hudClicked = EsvoiceClientModEvents.TOGGLE_HUD.consumeClick();
        boolean recordingClicked = EsvoiceClientModEvents.TOGGLE_RECORDING.consumeClick();

        if (connected && minecraft.screen == null) {
            if (muteClicked) {
                voiceState.setMuted(!voiceState.isMuted());
            }
            if (disableClicked) {
                voiceState.setDisabled(!voiceState.isDisabled());
            }
            if (hudClicked && VoicechatClient.CLIENT_CONFIG != null) {
                VoicechatClient.CLIENT_CONFIG.hideIcons
                        .set(!VoicechatClient.CLIENT_CONFIG.hideIcons.get()).save();
            }
            if (recordingClicked) {
                toggleRecording(minecraft, voiceClient);
            }
        }
        VoiceChannel channel = VoiceChannel.PUBLIC;
        if (connected && minecraft.screen == null) {
            if (EsvoiceClientModEvents.COMMAND_VOICE.isDown()) {
                channel = VoiceChannel.COMMAND;
            } else if (EsvoiceClientModEvents.SQUAD_VOICE.isDown()) {
                channel = VoiceChannel.SQUAD;
            }
        }
        // 界面打开时 KeyMapping 不会更新，改用 GLFW 直接轮询（聊天框里打字时不抢按键）。
        boolean publicKeyDown;
        if (minecraft.screen == null) {
            publicKeyDown = connected && EsvoiceClientModEvents.PUBLIC_VOICE.isDown();
        } else {
            publicKeyDown = connected
                    && !(minecraft.screen instanceof ChatScreen)
                    && EsvoiceVoiceKeys.isBindingHeld(EsvoiceClientModEvents.PUBLIC_VOICE);
        }
        RadioTransmitController.update(channel, publicKeyDown, connected);

        if (voiceConnected && RadioTransmitController.shouldActivateMicrophone()
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
