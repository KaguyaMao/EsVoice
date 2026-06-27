package org.esvoice.mixin;

import de.maxhenkel.voicechat.gui.VoiceChatScreen;
import de.maxhenkel.voicechat.gui.VoiceChatSettingsScreen;
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
        if (screen instanceof VoiceChatScreen || screen instanceof VoiceChatSettingsScreen) {
            return new EsvoiceVoiceChatScreen();
        }
        return screen;
    }
}
