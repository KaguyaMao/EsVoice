package org.esvoice.client;

import org.esvoice.network.EsvoiceNetwork;
import org.esvoice.network.VoiceModePacket;
import org.esvoice.voice.VoiceChannel;

public final class RadioTransmitController {

    private static volatile VoiceChannel activeChannel = VoiceChannel.PUBLIC;
    private static volatile boolean publicKeyDown;
    private static VoiceChannel requestedChannel = VoiceChannel.PUBLIC;

    private RadioTransmitController() {
    }

    public static VoiceChannel getActiveChannel() {
        return activeChannel;
    }

    public static boolean isRadioKeyDown() {
        return activeChannel.isRadio();
    }

    public static boolean shouldActivateMicrophone() {
        return publicKeyDown || isRadioKeyDown();
    }

    public static void update(VoiceChannel channel, boolean isPublicKeyDown, boolean connected) {
        publicKeyDown = connected && isPublicKeyDown;
        VoiceChannel next = channel == null ? VoiceChannel.PUBLIC : channel;
        if (requestedChannel == next) {
            return;
        }

        requestedChannel = next;
        if (!connected) {
            activeChannel = VoiceChannel.PUBLIC;
            return;
        }

        // Stopping a radio transmission is immediate. Starting one waits for
        // the server acknowledgement so no UDP audio can outrun the mode packet.
        if (next == VoiceChannel.PUBLIC) {
            activeChannel = VoiceChannel.PUBLIC;
        }
        EsvoiceNetwork.sendMode(new VoiceModePacket(next));
    }

    public static void confirm(VoiceChannel channel) {
        if (channel == requestedChannel) {
            activeChannel = channel;
        }
    }
}
