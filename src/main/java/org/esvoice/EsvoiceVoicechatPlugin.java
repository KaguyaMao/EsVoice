package org.esvoice;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import org.esvoice.voice.VoiceChannelRouter;

/**
 * Simple Voice Chat discovers this entry point independently from Forge's
 * {@link net.minecraftforge.fml.common.Mod} entry point.
 */
@ForgeVoicechatPlugin
public final class EsvoiceVoicechatPlugin implements VoicechatPlugin {

    private static VoicechatApi voicechatApi;

    @Override
    public String getPluginId() {
        return Esvoice.MODID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        voicechatApi = api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        VoiceChannelRouter.registerEvents(registration);
    }

    public static VoicechatApi getVoicechatApi() {
        return voicechatApi;
    }
}
