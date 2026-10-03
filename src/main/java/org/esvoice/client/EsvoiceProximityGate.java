package org.esvoice.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.espetro.client.gui.ClientGameState;
import org.espetro.team.GamePhase;

/**
 * 投票阶段的临近语音开关。
 *
 * <p>投票阶段玩家一直开着投票界面，Minecraft 不会更新按键状态，
 * 于是按住说话（本地/临近语音）完全失效。这里判断"当前是不是投票阶段 + 界面不是聊天框"，
 * 满足时允许用 GLFW 轮询到的按键状态继续发送临近语音。
 */
final class EsvoiceProximityGate {

    private EsvoiceProximityGate() {
    }

    /** 界面打开时是否仍允许使用本地（临近）语音。 */
    static boolean allowsProximityWithScreenOpen(Minecraft minecraft) {
        if (minecraft == null || minecraft.screen == null) {
            return false;
        }
        // 聊天框里打字时不抢按键
        if (minecraft.screen instanceof ChatScreen) {
            return false;
        }
        GamePhase phase = ClientGameState.getCurrentPhase();
        if (phase == null) {
            return false;
        }
        return switch (phase) {
            // 整个投票阶段都要能用临近麦：地图投票 → 地图揭示/加载 → 选边与分配
            // → 指挥官投票（并行/串行）→ 编制投票 → 编制揭示。
            // （编制投票页原本不在白名单里，导致选编制时按住说话失效）
            case MAP_VOTE, MAP_REVEAL, MAP_LOADING, TEAM_SELECT, TEAM_ASSIGN_SHOW,
                 COMMANDER_VOTE, DEFEND_COMMANDER_VOTE, ATTACK_COMMANDER_VOTE,
                 DEFEND_FACTION_SELECT, ATTACK_FACTION_SELECT, FACTION_REVEAL -> true;
            default -> false;
        };
    }
}
