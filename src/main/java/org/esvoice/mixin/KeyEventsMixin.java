package org.esvoice.mixin;

import de.maxhenkel.voicechat.voice.client.KeyEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyEvents.class, remap = false)
public abstract class KeyEventsMixin {

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true, remap = false)
    private void esvoice$disableVoicechatKeyHandling(CallbackInfo callback) {
        callback.cancel();
    }
}
