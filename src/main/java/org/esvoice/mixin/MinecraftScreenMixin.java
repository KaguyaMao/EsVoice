package org.esvoice.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.esvoice.client.EsvoiceVoiceChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public abstract class MinecraftScreenMixin {

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen esvoice$replaceVoiceChatScreen(Screen screen) {
        if (screen != null && screen.getClass().getName().startsWith("de.maxhenkel.voicechat.gui.")) {
            Screen current = Minecraft.getInstance().screen;
            Screen parent = current != null
                    && !current.getClass().getName().startsWith("de.maxhenkel.voicechat.gui.")
                    ? current
                    : null;
            return new EsvoiceVoiceChatScreen(parent);
        }
        return screen;
    }
}
