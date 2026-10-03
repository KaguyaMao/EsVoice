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

    /**
     * 录音开关是 EsVoice 唯一保留的键位。
     *
     * <p>公共麦/小队/指挥/静音麦克风/语音总开关/发言者头像条这 6 个键位已归 espetromini 所有
     * （{@code org.espetromini.client.MiniKeys}，由它注册，可在原版「选项 → 控制」里重绑），
     * 本模组只轮询它们，不再自己注册。
     */
    public static final KeyMapping TOGGLE_RECORDING = new KeyMapping(
            "key.esvoice.toggle_recording", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.esvoice");

    private EsvoiceClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_RECORDING);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("voice_speakers",
                (forgeGui, graphics, partialTick, screenWidth, screenHeight) -> VoiceSpeakerHud.render(graphics));
    }
}
