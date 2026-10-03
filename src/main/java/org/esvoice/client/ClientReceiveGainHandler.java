package org.esvoice.client;

import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;

/**
 * Simple Voice Chat 客户端"收到声音"事件的入口：在这里应用接收增益。
 *
 * <p>只有客户端会真正收到这些事件，所以由 {@code EsvoiceVoicechatPlugin} 通过
 * 惰性句柄调用，避免专用服务器加载客户端类。
 */
public final class ClientReceiveGainHandler {

    private ClientReceiveGainHandler() {
    }

    public static void apply(ClientReceiveSoundEvent event) {
        if (event == null) {
            return;
        }
        short[] audio = event.getRawAudio();
        if (audio == null || audio.length == 0) {
            return;
        }
        short[] boosted = ReceiveGainController.applyGain(audio);
        if (boosted != audio) {
            event.setRawAudio(boosted);
        }
    }
}
