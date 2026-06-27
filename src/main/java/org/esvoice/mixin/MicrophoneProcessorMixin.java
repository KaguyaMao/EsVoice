package org.esvoice.mixin;

import de.maxhenkel.voicechat.voice.client.MicrophoneProcessor;
import org.esvoice.client.RadioTransmitController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MicrophoneProcessor.class, remap = false)
public abstract class MicrophoneProcessorMixin {

    @Inject(method = "shouldTransmitAudio", at = @At("RETURN"), cancellable = true, remap = false)
    private void esvoice$activateRadioMicrophone(CallbackInfoReturnable<Boolean> callback) {
        if (RadioTransmitController.shouldActivateMicrophone()) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "isWhispering", at = @At("RETURN"), cancellable = true, remap = false)
    private void esvoice$markRadioPacket(CallbackInfoReturnable<Boolean> callback) {
        if (RadioTransmitController.isRadioKeyDown()) {
            callback.setReturnValue(true);
        }
    }
}
