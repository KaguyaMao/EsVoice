package org.esvoice.mixin;

import de.maxhenkel.voicechat.ForgeVoicechatClientMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.esvoice.client.EsvoiceVoiceChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces the Voice Chat button in Forge's mod list, including Cloth Config. */
@Mixin(value = ForgeVoicechatClientMod.class, remap = false)
public abstract class ForgeVoicechatClientModMixin {

    @Inject(method = "lambda$clientSetup$0", at = @At("HEAD"), cancellable = true, remap = false)
    private static void esvoice$replaceConfigFactory(Minecraft minecraft, Screen parent,
                                                     CallbackInfoReturnable<Screen> callback) {
        callback.setReturnValue(new EsvoiceVoiceChatScreen(parent));
    }
}
