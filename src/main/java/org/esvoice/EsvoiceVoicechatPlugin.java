package org.esvoice;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.NameTagIconRenderEvent;
import org.esvoice.voice.VoiceChannelRouter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

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
        registration.registerEvent(NameTagIconRenderEvent.class, EsvoiceVoicechatPlugin::onNameTagIconRender);
        // 接收增益：放大的对象是"收到的语音"，只在客户端触发（专用服务器不会收到这些事件）。
        registration.registerEvent(ClientReceiveSoundEvent.EntitySound.class,
                EsvoiceVoicechatPlugin::onClientReceiveSound);
        registration.registerEvent(ClientReceiveSoundEvent.LocationalSound.class,
                EsvoiceVoicechatPlugin::onClientReceiveSound);
        registration.registerEvent(ClientReceiveSoundEvent.StaticSound.class,
                EsvoiceVoicechatPlugin::onClientReceiveSound);
    }

    public static VoicechatApi getVoicechatApi() {
        return voicechatApi;
    }

    private static void onNameTagIconRender(NameTagIconRenderEvent event) {
        ClientIconFilterHandler.handle(event);
    }

    private static void onClientReceiveSound(ClientReceiveSoundEvent event) {
        ClientReceiveGainHandler.apply(event);
    }

    /** Loaded lazily only when Simple Voice Chat fires the client-only receive events. */
    private static final class ClientReceiveGainHandler {
        private static final MethodHandle APPLY = findHandler();

        private static MethodHandle findHandler() {
            try {
                return MethodHandles.publicLookup().findStatic(
                        Class.forName("org.esvoice.client.ClientReceiveGainHandler"),
                        "apply",
                        MethodType.methodType(void.class, ClientReceiveSoundEvent.class));
            } catch (ReflectiveOperationException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }

        private static void apply(ClientReceiveSoundEvent event) {
            try {
                APPLY.invokeExact(event);
            } catch (Throwable throwable) {
                throw new IllegalStateException("Failed to apply receive gain", throwable);
            }
        }
    }

    /** Loaded lazily only when Simple Voice Chat fires the client-only render event. */
    private static final class ClientIconFilterHandler {
        private static final MethodHandle ON_NAME_TAG_ICON_RENDER = findHandler();

        private static MethodHandle findHandler() {
            try {
                return MethodHandles.publicLookup().findStatic(
                        Class.forName("org.esvoice.client.VoiceNameTagIconFilter"),
                        "onNameTagIconRender",
                        MethodType.methodType(void.class, NameTagIconRenderEvent.class));
            } catch (ReflectiveOperationException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }

        private static void handle(NameTagIconRenderEvent event) {
            try {
                ON_NAME_TAG_ICON_RENDER.invokeExact(event);
            } catch (Throwable throwable) {
                throw new IllegalStateException("Failed to filter voice name tag icons", throwable);
            }
        }
    }
}
