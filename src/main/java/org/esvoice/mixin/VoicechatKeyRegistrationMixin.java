package org.esvoice.mixin;

import de.maxhenkel.voicechat.intercompatibility.ForgeClientCompatibilityManager;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps Simple Voice Chat's internal key objects alive for binary compatibility,
 * but prevents them from being exposed in Minecraft's controls menu.
 */
@Mixin(value = ForgeClientCompatibilityManager.class, remap = false)
public abstract class VoicechatKeyRegistrationMixin {

    @Redirect(
            method = "onRegisterKeyBinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/client/event/RegisterKeyMappingsEvent;register(Lnet/minecraft/client/KeyMapping;)V"
            ),
            remap = false
    )
    private void esvoice$hideVoicechatKeyMappings(RegisterKeyMappingsEvent event, KeyMapping mapping) {
        if (!"key.categories.voicechat".equals(mapping.getCategory())) {
            event.register(mapping);
        }
    }
}
