package org.esvoice.mixin;

import de.maxhenkel.voicechat.voice.client.KeyEvents;
import org.esvoice.client.EsvoiceClientModEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyEvents.class, remap = false)
public abstract class KeyEventsMixin {

    @Inject(method = "handleKeybinds", at = @At("HEAD"), remap = false)
    private void esvoice$consumeConflictingVoicechatKeys(CallbackInfo callback) {
        if (EsvoiceClientModEvents.PUBLIC_VOICE.isDown() && KeyEvents.KEY_VOICE_CHAT != null) {
            KeyEvents.KEY_VOICE_CHAT.consumeClick();
        }
        if (EsvoiceClientModEvents.COMMAND_VOICE.isDown() && KeyEvents.KEY_DISABLE != null) {
            KeyEvents.KEY_DISABLE.consumeClick();
        }
    }
}
