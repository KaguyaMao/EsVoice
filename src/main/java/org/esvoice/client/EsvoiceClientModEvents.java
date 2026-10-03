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
            "key.esvoice.command_voice", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.esvoice");
    public static final KeyMapping MUTE_MICROPHONE = new KeyMapping(
            "key.esvoice.mute_microphone", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.esvoice");
    public static final KeyMapping DISABLE_VOICE = new KeyMapping(
            "key.esvoice.disable_voice", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.esvoice");
    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
            "key.esvoice.toggle_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.esvoice");
    public static final KeyMapping TOGGLE_RECORDING = new KeyMapping(
            "key.esvoice.toggle_recording", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.esvoice");

    public static final KeyMapping[] ALL_KEYS = {
            PUBLIC_VOICE,
            SQUAD_VOICE,
            COMMAND_VOICE,
            MUTE_MICROPHONE,
            DISABLE_VOICE,
            TOGGLE_HUD,
            TOGGLE_RECORDING
    };

    private EsvoiceClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(PUBLIC_VOICE);
        event.register(SQUAD_VOICE);
        event.register(COMMAND_VOICE);
        event.register(MUTE_MICROPHONE);
        event.register(DISABLE_VOICE);
        event.register(TOGGLE_HUD);
        event.register(TOGGLE_RECORDING);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("voice_speakers",
                (forgeGui, graphics, partialTick, screenWidth, screenHeight) -> VoiceSpeakerHud.render(graphics));
    }
}
