package org.esvoice.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.client.ClientPlayerStateManager;
import org.espetro.client.gui.ClientTacticalState;
import org.esvoice.Esvoice;
import org.esvoice.voice.VoiceChannel;

@Mod.EventBusSubscriber(modid = Esvoice.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EsvoiceClientEvents {

    private EsvoiceClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean connected = minecraft.getConnection() != null && minecraft.player != null;
        if (connected && minecraft.screen == null && EsvoiceClientModEvents.VOICE_SETTINGS.consumeClick()) {
            minecraft.setScreen(new EsvoiceVoiceChatScreen());
        }
        VoiceChannel channel = VoiceChannel.PUBLIC;
        if (connected && minecraft.screen == null) {
            if (EsvoiceClientModEvents.COMMAND_VOICE.isDown()) {
                channel = VoiceChannel.COMMAND;
            } else if (EsvoiceClientModEvents.SQUAD_VOICE.isDown()) {
                channel = VoiceChannel.SQUAD;
            }
        }
        boolean publicKeyDown = connected && minecraft.screen == null
                && EsvoiceClientModEvents.PUBLIC_VOICE.isDown();
        RadioTransmitController.update(channel, publicKeyDown, connected);

        if (connected && RadioTransmitController.shouldActivateMicrophone()) {
            VoiceChannel activeChannel = RadioTransmitController.getActiveChannel();
            boolean shouldShowName = activeChannel != VoiceChannel.SQUAD || ClientTacticalState.isInSquad();
            if (shouldShowName) {
                ClientPlayerStateManager state = ClientManager.getPlayerStateManager();
                VoiceSpeakerHud.onLocalTransmit(minecraft.player.getUUID(), minecraft.player.getName().getString(),
                        activeChannel, state.isMuted(), state.isDisabled());
            }
        }
    }

}
