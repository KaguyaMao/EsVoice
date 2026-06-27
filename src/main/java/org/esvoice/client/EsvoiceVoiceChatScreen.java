package org.esvoice.client;

import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.gui.audiodevice.SelectMicrophoneScreen;
import de.maxhenkel.voicechat.gui.audiodevice.SelectSpeakerScreen;
import de.maxhenkel.voicechat.gui.volume.AdjustVolumesScreen;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.client.ClientPlayerStateManager;
import de.maxhenkel.voicechat.voice.client.ClientVoicechat;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import se.mickelus.mutil.gui.GuiElement;

import java.util.Locale;

/** Espetro 风格的 Simple Voice Chat 主界面。 */
public final class EsvoiceVoiceChatScreen extends EsvoiceMutilScreen {

    private static final int PANEL_WIDTH = 392;

    public EsvoiceVoiceChatScreen() {
        super(Component.literal("战术语音"));
    }

    @Override
    protected void buildMutilRoot(GuiElement root) {
        ClientPlayerStateManager state = ClientManager.getPlayerStateManager();
        int panelW = Math.min(PANEL_WIDTH, width - 16);
        int panelH = Math.min(226, height - 16);
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;

        root.addChild(EsvoiceMutilWidgets.panel(panelX, panelY, panelW, panelH));
        root.addChild(EsvoiceMutilWidgets.text(panelX + 10, panelY + 9,
                "§6§l战术语音", EsvoiceMutilWidgets.GOLD));
        root.addChild(EsvoiceMutilWidgets.text(panelX + 10, panelY + 22,
                connectionText(state), connectionColor(state)));
        root.addChild(EsvoiceMutilWidgets.button(panelX + panelW - 48, panelY + 8, 38, 15,
                "关闭", this::onClose));

        int contentY = panelY + 38;
        int contentH = panelH - 72;
        int gap = 8;
        int leftW = Math.max(132, (panelW - 28) * 45 / 100);
        int rightW = panelW - 28 - leftW;
        int leftX = panelX + 10;
        int rightX = leftX + leftW + gap;

        buildChannelPanel(root, leftX, contentY, leftW, contentH);
        buildControlPanel(root, rightX, contentY, rightW, contentH, state);

        root.addChild(EsvoiceMutilWidgets.centeredText(panelX + 10, panelY + panelH - 23,
                panelW - 20, "公共麦按住 V · 班组麦按住 B · 指挥麦按住 N · 界面 O",
                EsvoiceMutilWidgets.DIM));
    }

    private void buildChannelPanel(GuiElement root, int x, int y, int width, int height) {
        root.addChild(EsvoiceMutilWidgets.softPanel(x, y, width, height));
        root.addChild(EsvoiceMutilWidgets.text(x + 7, y + 7, "频道按键", EsvoiceMutilWidgets.MUTED));
        channelCard(root, x + 7, y + 22, width - 14, "公共频道", "V", "近距离 · 全体玩家",
                EsvoiceMutilWidgets.TEXT);
        channelCard(root, x + 7, y + 61, width - 14, "班组小队", "B", "无线 · 同班组",
                EsvoiceMutilWidgets.SQUAD_BLUE);
        channelCard(root, x + 7, y + 100, width - 14, "指挥频道", "N", "无线 · 队长与指挥官",
                EsvoiceMutilWidgets.GOLD);
    }

    private void channelCard(GuiElement root, int x, int y, int width, String name, String key,
                             String description, int color) {
        root.addChild(EsvoiceMutilWidgets.softPanel(x, y, width, 33));
        root.addChild(EsvoiceMutilWidgets.text(x + 6, y + 5, name, color));
        root.addChild(EsvoiceMutilWidgets.text(x + width - 18, y + 5, key, EsvoiceMutilWidgets.GOLD));
        root.addChild(EsvoiceMutilWidgets.text(x + 6, y + 18, description, EsvoiceMutilWidgets.DIM));
    }

    private void buildControlPanel(GuiElement root, int x, int y, int width, int height,
                                   ClientPlayerStateManager state) {
        root.addChild(EsvoiceMutilWidgets.softPanel(x, y, width, height));
        root.addChild(EsvoiceMutilWidgets.text(x + 7, y + 7, "语音控制", EsvoiceMutilWidgets.MUTED));

        int innerX = x + 7;
        int innerW = width - 14;
        int halfW = (innerW - 4) / 2;
        boolean disabled = state.isDisabled();
        boolean muted = state.isMuted();

        root.addChild(EsvoiceMutilWidgets.button(innerX, y + 21, halfW, 17,
                disabled ? "语音：已关闭" : "语音：已启用", () -> {
                    state.setDisabled(!state.isDisabled());
                    rebuildMutilRoot();
                }).selected(!disabled).textColor(disabled ? EsvoiceMutilWidgets.NEGATIVE : EsvoiceMutilWidgets.POSITIVE));
        root.addChild(EsvoiceMutilWidgets.button(innerX + halfW + 4, y + 21, halfW, 17,
                muted ? "麦克风：静音" : "麦克风：开启", () -> {
                    state.setMuted(!state.isMuted());
                    rebuildMutilRoot();
                }).selected(!muted).textColor(muted ? EsvoiceMutilWidgets.WARNING : EsvoiceMutilWidgets.POSITIVE));

        valueRow(root, innerX, y + 43, innerW, "接收音量",
                Math.round(VoicechatClient.CLIENT_CONFIG.voiceChatVolume.get() * 100D) + "%",
                () -> changeVolume(-0.1D), () -> changeVolume(0.1D));
        valueRow(root, innerX, y + 66, innerW, "麦克风增益",
                String.format(Locale.ROOT, "%+.0f dB", VoicechatClient.CLIENT_CONFIG.microphoneGain.get()),
                () -> changeGain(-1D), () -> changeGain(1D));

        boolean agc = VoicechatClient.CLIENT_CONFIG.agc.get();
        root.addChild(EsvoiceMutilWidgets.button(innerX, y + 89, innerW, 17,
                agc ? "自动增益控制：开启" : "自动增益控制：关闭", () -> {
                    VoicechatClient.CLIENT_CONFIG.agc.set(!VoicechatClient.CLIENT_CONFIG.agc.get()).save();
                    rebuildMutilRoot();
                }).selected(agc).textColor(agc ? EsvoiceMutilWidgets.POSITIVE : EsvoiceMutilWidgets.DIM));

        root.addChild(EsvoiceMutilWidgets.button(innerX, y + 111, halfW, 17, "输入设备",
                () -> Minecraft.getInstance().setScreen(new SelectMicrophoneScreen(this))));
        root.addChild(EsvoiceMutilWidgets.button(innerX + halfW + 4, y + 111, halfW, 17, "输出设备",
                () -> Minecraft.getInstance().setScreen(new SelectSpeakerScreen(this))));
        root.addChild(EsvoiceMutilWidgets.button(innerX, y + 133, innerW, 17, "玩家与频道音量",
                () -> Minecraft.getInstance().setScreen(new AdjustVolumesScreen())));
    }

    private void valueRow(GuiElement root, int x, int y, int width, String label, String value,
                          Runnable decrease, Runnable increase) {
        root.addChild(EsvoiceMutilWidgets.text(x, y + 4, label, EsvoiceMutilWidgets.TEXT));
        int minusX = x + width - 78;
        root.addChild(EsvoiceMutilWidgets.button(minusX, y, 18, 17, "−", decrease));
        root.addChild(EsvoiceMutilWidgets.centeredText(minusX + 20, y + 4, 38, value, EsvoiceMutilWidgets.MUTED));
        root.addChild(EsvoiceMutilWidgets.button(x + width - 18, y, 18, 17, "+", increase));
    }

    private void changeVolume(double amount) {
        double value = clamp(VoicechatClient.CLIENT_CONFIG.voiceChatVolume.get() + amount, 0D, 3D);
        VoicechatClient.CLIENT_CONFIG.voiceChatVolume.set(value).save();
        rebuildMutilRoot();
    }

    private void changeGain(double amount) {
        double value = clamp(VoicechatClient.CLIENT_CONFIG.microphoneGain.get() + amount, -40D, 24D);
        VoicechatClient.CLIENT_CONFIG.microphoneGain.set(value).save();
        rebuildMutilRoot();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String connectionText(ClientPlayerStateManager state) {
        ClientVoicechat client = ClientManager.getClient();
        if (state.isDisconnected() || client == null || client.getConnection() == null) {
            return "● 语音服务未连接";
        }
        if (state.isDisabled()) {
            return "● 语音服务已关闭";
        }
        return "● 语音服务已连接";
    }

    private static int connectionColor(ClientPlayerStateManager state) {
        ClientVoicechat client = ClientManager.getClient();
        return state.isDisconnected() || client == null || client.getConnection() == null
                ? EsvoiceMutilWidgets.NEGATIVE
                : state.isDisabled() ? EsvoiceMutilWidgets.WARNING : EsvoiceMutilWidgets.POSITIVE;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}
