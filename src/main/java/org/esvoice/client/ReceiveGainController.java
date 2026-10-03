package org.esvoice.client;

import org.espetromini.api.MiniClientAPI;

/**
 * 把 espetromini 客户端配置里的接收增益应用到 Simple Voice Chat 解码后的 PCM 帧上。
 *
 * <p>加性放大超过 16bit 范围时按饱和裁剪（而不是回绕），否则会变成刺耳的爆音。
 */
public final class ReceiveGainController {

    private ReceiveGainController() {
    }

    /**
     * @param audio 解码后的单帧 PCM（会被就地修改）
     * @return 同一个数组（方便链式调用）
     */
    public static short[] applyGain(short[] audio) {
        if (audio == null || audio.length == 0) {
            return audio;
        }
        double db = MiniClientAPI.receiveGainDb();
        if (Math.abs(db) < 0.01D) {
            return audio;
        }

        double factor = Math.pow(10.0D, db / 20.0D);
        if (factor <= 0.0D) {
            java.util.Arrays.fill(audio, (short) 0);
            return audio;
        }
        if (factor == 1.0D) {
            return audio;
        }

        for (int i = 0; i < audio.length; i++) {
            int value = (int) Math.round(audio[i] * factor);
            if (value > Short.MAX_VALUE) {
                value = Short.MAX_VALUE;
            } else if (value < Short.MIN_VALUE) {
                value = Short.MIN_VALUE;
            }
            audio[i] = (short) value;
        }
        return audio;
    }
}
