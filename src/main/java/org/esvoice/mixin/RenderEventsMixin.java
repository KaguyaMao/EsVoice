package org.esvoice.mixin;

import de.maxhenkel.voicechat.voice.client.RenderEvents;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderEvents.class, remap = false)
public abstract class RenderEventsMixin {

    @Inject(method = "onRenderHUD", at = @At("HEAD"), cancellable = true, remap = false)
    private void esvoice$hideVanillaVoiceHud(GuiGraphics graphics, float partialTick, CallbackInfo callback) {
        callback.cancel();
    }
}
