package org.esvoice.client;

import org.esvoice.network.EsvoiceNetwork;
import org.esvoice.network.VoiceModePacket;
import org.esvoice.voice.VoiceChannel;

public final class RadioTransmitController {

    private static final long DENIED_RETRY_MILLIS = 1_000L;
    private static volatile VoiceChannel activeChannel = VoiceChannel.PUBLIC;
    private static volatile boolean publicKeyDown;
    private static VoiceChannel requestedChannel = VoiceChannel.PUBLIC;
    private static long nextDeniedRetryMillis;

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
            if (next.isRadio() && activeChannel == VoiceChannel.PUBLIC
                    && System.currentTimeMillis() >= nextDeniedRetryMillis) {
                sendRequest(next);
            }
            return;
        }

        requestedChannel = next;
        if (!connected) {
            activeChannel = VoiceChannel.PUBLIC;
            nextDeniedRetryMillis = 0L;
            return;
        }

        // Stopping a radio transmission is immediate. Starting one waits for
        // the server acknowledgement so no UDP audio can outrun the mode packet.
        if (next == VoiceChannel.PUBLIC) {
            activeChannel = VoiceChannel.PUBLIC;
        }
        sendRequest(next);
    }

    public static void confirm(VoiceChannel channel) {
        if (channel == requestedChannel) {
            activeChannel = channel;
            nextDeniedRetryMillis = 0L;
        } else if (channel == VoiceChannel.PUBLIC) {
            activeChannel = VoiceChannel.PUBLIC;
            nextDeniedRetryMillis = System.currentTimeMillis() + DENIED_RETRY_MILLIS;
        }
    }

    private static void sendRequest(VoiceChannel channel) {
        if (channel.isRadio()) {
            nextDeniedRetryMillis = System.currentTimeMillis() + DENIED_RETRY_MILLIS;
        } else {
            nextDeniedRetryMillis = 0L;
        }
        EsvoiceNetwork.sendMode(new VoiceModePacket(channel));
    }
}
