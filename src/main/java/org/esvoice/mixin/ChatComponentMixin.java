package org.esvoice.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shrink the vanilla chat panel so the speaker HUD can sit cleanly above it
 * without covering half the left side of the screen.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    private static final double HEIGHT_FACTOR = 0.62D;
    private static final int MIN_HEIGHT = 28;

    @Inject(method = "getHeight()I", at = @At("RETURN"), cancellable = true)
    private void esvoice$shrinkChatHeight(CallbackInfoReturnable<Integer> callback) {
        int original = callback.getReturnValue();
        int shrunk = Math.max(MIN_HEIGHT, (int) Math.round(original * HEIGHT_FACTOR));
        if (shrunk < original) {
            callback.setReturnValue(shrunk);
        }
    }
}
