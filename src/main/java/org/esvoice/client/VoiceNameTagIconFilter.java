package org.esvoice.client;

import de.maxhenkel.voicechat.api.events.NameTagIconRenderEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

import java.util.UUID;

/**
 * 语音图标只对本方可见：判定完全基于原版计分板队伍（= espetromini 的阵营）。
 *
 * <p>轻量化后单一维度、没有"大厅"概念，因此这里不再有任何维度特例。
 */
public final class VoiceNameTagIconFilter {

    private VoiceNameTagIconFilter() {
    }

    public static void onNameTagIconRender(NameTagIconRenderEvent event) {
        if (!shouldRenderForLocalPlayer(event.getEntityId())) {
            event.cancel();
        }
    }

    private static boolean shouldRenderForLocalPlayer(UUID targetId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }

        Player target = minecraft.level.getPlayerByUUID(targetId);
        if (target == null) {
            return false;
        }
        if (target == minecraft.player || target.getUUID().equals(minecraft.player.getUUID())) {
            return true;
        }
        return isSameScoreboardTeam(minecraft.player, target);
    }

    /** 只有双方都在同一个原版队伍里才显示；无队伍的玩家不显示（与原行为一致）。 */
    private static boolean isSameScoreboardTeam(Player localPlayer, Player targetPlayer) {
        Team localTeam = localPlayer.getTeam();
        Team targetTeam = targetPlayer.getTeam();
        return localTeam != null && targetTeam != null && localTeam.getName().equals(targetTeam.getName());
    }
}
