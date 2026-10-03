package org.esvoice.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * 直接读取 GLFW 的物理按键状态。
 *
 * <p>Minecraft 只在没有界面打开时更新 {@link KeyMapping} 的按下状态，
 * 所以投票界面开着的阶段 {@code KeyMapping#isDown()} 恒为 false，
 * 按住说话会失效。这里绕过 KeyMapping 直接问 GLFW。
 */
final class EsvoiceVoiceKeys {

    private EsvoiceVoiceKeys() {
    }

    static boolean isBindingHeld(KeyMapping mapping) {
        if (mapping == null) {
            return false;
        }
        InputConstants.Key key = mapping.getKey();
        if (key == null || key == InputConstants.UNKNOWN) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow() == null) {
            return false;
        }
        long window = minecraft.getWindow().getWindow();
        return switch (key.getType()) {
            case KEYSYM -> GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            default -> false;
        };
    }
}
