package org.esvoice.mixin;

import de.maxhenkel.voicechat.permission.ForgePermission;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Simple Voice Chat gates microphone packets on {@code voicechat.speak} and delivery on
 * {@code voicechat.listen}. Those nodes default to EVERYONE, but a permission provider
 * (LuckPerms, FTB Ranks, custom handlers) can rewrite the default to OPS / false so only
 * operators or cheats-enabled players can talk. Esvoice is a tactical voice layer for every
 * combatant, so speak/listen must stay open for all connected players.
 *
 * {@code voicechat.groups} and {@code voicechat.admin} are left unchanged.
 */
@Mixin(value = ForgePermission.class, remap = false)
public abstract class ForgePermissionMixin {

    @Shadow
    private PermissionNode<Boolean> node;

    @Inject(method = "hasPermission", at = @At("HEAD"), cancellable = true, remap = false)
    private void esvoice$allowSpeakAndListen(ServerPlayer player, CallbackInfoReturnable<Boolean> callback) {
        if (node == null) {
            return;
        }
        String name = node.getNodeName();
        if ("voicechat.speak".equals(name) || "voicechat.listen".equals(name)) {
            callback.setReturnValue(true);
        }
    }
}
