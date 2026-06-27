package org.esvoice.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.esvoice.Esvoice;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Esvoice.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EsvoiceClientModEvents {

    public static final KeyMapping PUBLIC_VOICE = new KeyMapping(
            "key.esvoice.public_voice", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.esvoice");
    public static final KeyMapping SQUAD_VOICE = new KeyMapping(
            "key.esvoice.squad_voice", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.esvoice");
    public static final KeyMapping COMMAND_VOICE = new KeyMapping(
            "key.esvoice.command_voice", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.esvoice");
    public static final KeyMapping VOICE_SETTINGS = new KeyMapping(
            "key.esvoice.voice_settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.esvoice");

    private EsvoiceClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(PUBLIC_VOICE);
        event.register(SQUAD_VOICE);
        event.register(COMMAND_VOICE);
        event.register(VOICE_SETTINGS);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("voice_speakers",
                (forgeGui, graphics, partialTick, screenWidth, screenHeight) -> VoiceSpeakerHud.render(graphics));
    }
}
