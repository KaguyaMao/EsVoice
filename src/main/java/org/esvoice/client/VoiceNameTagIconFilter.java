package org.esvoice.client;

import de.maxhenkel.voicechat.api.events.NameTagIconRenderEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import org.espetro.client.gui.ClientGameState;
import org.espetro.team.TeamManager;

import java.util.UUID;

public final class VoiceNameTagIconFilter {

    private static final String ATTACK_TEAM = "ATTACK";
    private static final String DEFEND_TEAM = "DEFEND";

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
        // Lobby / hub: show speak icons for everyone so public mic is not faction-gated.
        if (net.minecraft.world.level.Level.OVERWORLD.equals(minecraft.level.dimension())) {
            return true;
        }
        return isSameEspetroTeam(minecraft.player, target);
    }

    private static boolean isSameEspetroTeam(Player localPlayer, Player targetPlayer) {
        String localScoreboardTeam = teamName(localPlayer.getTeam());
        String targetScoreboardTeam = teamName(targetPlayer.getTeam());

        if (isEspetroScoreboardTeam(localScoreboardTeam)) {
            return localScoreboardTeam.equals(targetScoreboardTeam);
        }

        String localEspetroTeam = ClientGameState.getPlayerTeam();
        if (localEspetroTeam == null || targetScoreboardTeam == null) {
            return false;
        }
        return ATTACK_TEAM.equals(localEspetroTeam) && TeamManager.ATTACK_TEAM_ID.equals(targetScoreboardTeam)
                || DEFEND_TEAM.equals(localEspetroTeam) && TeamManager.DEFEND_TEAM_ID.equals(targetScoreboardTeam);
    }

    private static String teamName(Team team) {
        return team == null ? null : team.getName();
    }

    private static boolean isEspetroScoreboardTeam(String teamName) {
        return TeamManager.ATTACK_TEAM_ID.equals(teamName) || TeamManager.DEFEND_TEAM_ID.equals(teamName);
    }
}
