package org.esvoice.client;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Esvoice 客户端配置（{@code config/esvoice-client.toml}）。
 *
 * <p>目前只有接收增益：Simple Voice Chat 自己的总音量上限是 3.0（约 +9.5 dB），
 * 想要更响就只能由 Esvoice 在解码后的 PCM 上再放大，所以增益放在这里。
 */
public final class EsvoiceClientConfig {

    /** 默认接收增益（dB）。+20 dB ≈ 音量 ×10。 */
    public static final double DEFAULT_RECEIVE_GAIN_DB = 20.0D;
    public static final double MIN_RECEIVE_GAIN_DB = -40.0D;
    public static final double MAX_RECEIVE_GAIN_DB = 40.0D;

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.DoubleValue RECEIVE_GAIN_DB = BUILDER
            .comment("接收语音的额外增益（dB）。0 = 不额外放大；+20 dB ≈ 放大 10 倍。",
                    "Extra gain applied to received voice in dB. 0 = no extra gain, +20 dB = about 10x.")
            .defineInRange("receive_gain_db", DEFAULT_RECEIVE_GAIN_DB,
                    MIN_RECEIVE_GAIN_DB, MAX_RECEIVE_GAIN_DB);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private EsvoiceClientConfig() {
    }

    /** 当前接收增益；配置尚未加载时退回默认值。 */
    public static double receiveGainDb() {
        try {
            return RECEIVE_GAIN_DB.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_RECEIVE_GAIN_DB;
        }
    }

    public static void setReceiveGainDb(double db) {
        double clamped = Math.max(MIN_RECEIVE_GAIN_DB, Math.min(MAX_RECEIVE_GAIN_DB, db));
        try {
            RECEIVE_GAIN_DB.set(clamped);
            RECEIVE_GAIN_DB.save();
        } catch (IllegalStateException notLoaded) {
            // 配置还没加载，忽略即可
        }
    }
}
