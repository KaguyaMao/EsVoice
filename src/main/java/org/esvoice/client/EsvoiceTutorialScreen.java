package org.esvoice.client;

import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.gui.widgets.MicTestButton;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.client.ClientVoicechat;
import de.maxhenkel.voicechat.voice.client.MicrophoneActivationType;
import de.maxhenkel.voicechat.voice.client.SoundManager;
import de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager;
import de.maxhenkel.voicechat.voice.common.AudioUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import se.mickelus.mutil.gui.GuiElement;

import java.util.ArrayList;
import java.util.List;

/**
 * First-run Esvoice setup. Replaying it never resets existing settings.
 */
public final class EsvoiceTutorialScreen extends EsvoiceMutilScreen {

    private final Screen parent;
    private Step step = Step.INTRODUCTION;
    private MicTestButton micTest;
    private volatile double microphoneLevel = AudioUtils.LOWEST_DB;

    public EsvoiceTutorialScreen(Screen parent, boolean replay) {
        super(Component.translatable("tutorial.esvoice.title"));
        this.parent = parent;
    }

    public static boolean isFinished() {
        return VoicechatClient.CLIENT_CONFIG != null
                && VoicechatClient.CLIENT_CONFIG.onboardingFinished.get();
    }

    @Override
    protected void buildMutilRoot(GuiElement root) {
        int panelW = Math.min(560, Math.max(296, width - 16));
        int panelH = Math.min(350, Math.max(210, height - 16));
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;

        root.addChild(EsvoiceMutilWidgets.panel(panelX, panelY, panelW, panelH));
        root.addChild(EsvoiceMutilWidgets.text(panelX + 12, panelY + 10,
                Component.translatable("tutorial.esvoice.title"), EsvoiceMutilWidgets.GOLD));
        root.addChild(EsvoiceMutilWidgets.dynamicText(panelX + panelW - 100, panelY + 10, 88,
                () -> Component.translatable("tutorial.esvoice.progress",
                        step.ordinal() + 1, Step.values().length),
                () -> EsvoiceMutilWidgets.DIM, true));

        int contentX = panelX + 12;
        int contentY = panelY + 34;
        int contentW = panelW - 24;
        int contentH = panelH - 72;
        root.addChild(EsvoiceMutilWidgets.softPanel(contentX, contentY, contentW, contentH));
        buildStep(root, contentX + 10, contentY + 10, contentW - 20, contentH - 20);

        int buttonsY = panelY + panelH - 28;
        root.addChild(EsvoiceMutilWidgets.button(panelX + 12, buttonsY, 76, 18,
                Component.translatable(step == Step.INTRODUCTION
                        ? "tutorial.esvoice.skip" : "tutorial.esvoice.back"),
                step == Step.INTRODUCTION ? this::finish : this::previous));
        root.addChild(EsvoiceMutilWidgets.button(panelX + panelW - 88, buttonsY, 76, 18,
                Component.translatable(step == Step.FINISH
                        ? "tutorial.esvoice.finish" : "tutorial.esvoice.next"),
                step == Step.FINISH ? this::finish : this::next));
    }

    private void buildStep(GuiElement root, int x, int y, int width, int height) {
        root.addChild(EsvoiceMutilWidgets.centeredText(x, y, width,
                Component.translatable(step.titleKey), EsvoiceMutilWidgets.MUTED));
        switch (step) {
            case INTRODUCTION -> buildIntroduction(root, x, y + 24, width);
            case DEVICES -> buildDevices(root, x, y + 24, width);
            case MICROPHONE -> buildMicrophone(root, x, y + 24, width);
            case KEYS -> buildKeys(root, x, y + 20, width, height - 20);
            case FINISH -> buildFinish(root, x, y + 24, width);
        }
    }

    private void buildIntroduction(GuiElement root, int x, int y, int width) {
        tutorialLine(root, x, y, width, "tutorial.esvoice.intro.1", EsvoiceMutilWidgets.TEXT);
        tutorialLine(root, x, y + 25, width, "tutorial.esvoice.intro.2", EsvoiceMutilWidgets.SQUAD_BLUE);
        tutorialLine(root, x, y + 50, width, "tutorial.esvoice.intro.3", EsvoiceMutilWidgets.GOLD);
        tutorialLine(root, x, y + 82, width, "tutorial.esvoice.intro.tip", EsvoiceMutilWidgets.DIM);
    }

    private void buildDevices(GuiElement root, int x, int y, int width) {
        tutorialLine(root, x, y, width, "tutorial.esvoice.devices.help", EsvoiceMutilWidgets.DIM);
        root.addChild(EsvoiceMutilWidgets.button(x, y + 34, width, 22,
                () -> Component.translatable("tutorial.esvoice.devices.microphone",
                        visibleDevice(VoicechatClient.CLIENT_CONFIG.microphone.get())),
                () -> cycleDevice(true)));
        root.addChild(EsvoiceMutilWidgets.button(x, y + 65, width, 22,
                () -> Component.translatable("tutorial.esvoice.devices.speaker",
                        visibleDevice(VoicechatClient.CLIENT_CONFIG.speaker.get())),
                () -> cycleDevice(false)));
        tutorialLine(root, x, y + 102, width, "tutorial.esvoice.devices.tip", EsvoiceMutilWidgets.DIM);
    }

    private void buildMicrophone(GuiElement root, int x, int y, int width) {
        root.addChild(EsvoiceMutilWidgets.button(x, y, width, 22,
                () -> Component.translatable("tutorial.esvoice.activation",
                        VoicechatClient.CLIENT_CONFIG.microphoneActivationType.get().getText()),
                this::cycleActivation));
        root.addChild(EsvoiceMutilWidgets.button(x, y + 32, width, 22,
                () -> Component.translatable(isMicTestActive()
                                ? "tutorial.esvoice.mic.stop" : "tutorial.esvoice.mic.start",
                        Math.round(microphoneLevel) + " dB"),
                this::toggleMicTest).selected(this::isMicTestActive));
        tutorialLine(root, x, y + 68, width, "tutorial.esvoice.mic.help", EsvoiceMutilWidgets.DIM);
        root.addChild(EsvoiceMutilWidgets.slider(x, y + 103, width, 20, -40D, 24D,
                () -> VoicechatClient.CLIENT_CONFIG.microphoneGain.get(),
                value -> VoicechatClient.CLIENT_CONFIG.microphoneGain.set(Math.rint(value)).save(),
                () -> Component.translatable("gui.esvoice.microphone.gain",
                        String.format(java.util.Locale.ROOT, "%+.0f dB",
                                VoicechatClient.CLIENT_CONFIG.microphoneGain.get()))));
    }

    private void buildKeys(GuiElement root, int x, int y, int width, int height) {
        KeyMapping[] mappings = EsvoiceClientModEvents.ALL_KEYS;
        int columns = width >= 430 ? 2 : 1;
        int columnWidth = (width - (columns - 1) * 8) / columns;
        int rowHeight = 47;
        int rows = (mappings.length + columns - 1) / columns;
        EsvoiceMutilWidgets.VerticalScroll scroll = EsvoiceMutilWidgets.scroll(
                x, y, width, height, rows * rowHeight);
        root.addChild(scroll);
        for (int i = 0; i < mappings.length; i++) {
            KeyMapping mapping = mappings[i];
            int column = i % columns;
            int row = i / columns;
            int bx = column * (columnWidth + 8);
            int by = row * rowHeight;
            scroll.addChild(EsvoiceMutilWidgets.text(bx + 3, by + 3,
                    Component.translatable(mapping.getName()), EsvoiceMutilWidgets.TEXT));
            scroll.addChild(EsvoiceMutilWidgets.keybind(bx, by + 16, columnWidth, 20, mapping));
        }
    }

    private void buildFinish(GuiElement root, int x, int y, int width) {
        boolean connected = ClientManager.getClient() != null
                && ClientManager.getClient().getConnection() != null;
        tutorialLine(root, x, y, width, connected
                ? "tutorial.esvoice.finish.connected" : "tutorial.esvoice.finish.disconnected",
                connected ? EsvoiceMutilWidgets.POSITIVE : EsvoiceMutilWidgets.WARNING);
        tutorialLine(root, x, y + 31, width, "tutorial.esvoice.finish.keys",
                EsvoiceMutilWidgets.TEXT);
        root.addChild(EsvoiceMutilWidgets.dynamicText(x, y + 55, width,
                () -> Component.translatable("tutorial.esvoice.finish.summary",
                        EsvoiceClientModEvents.PUBLIC_VOICE.getTranslatedKeyMessage(),
                        EsvoiceClientModEvents.SQUAD_VOICE.getTranslatedKeyMessage(),
                        EsvoiceClientModEvents.COMMAND_VOICE.getTranslatedKeyMessage(),
                        EsvoiceClientModEvents.VOICE_SETTINGS.getTranslatedKeyMessage()),
                () -> EsvoiceMutilWidgets.GOLD, true));
        tutorialLine(root, x, y + 88, width, "tutorial.esvoice.finish.replay",
                EsvoiceMutilWidgets.DIM);
    }

    private void tutorialLine(GuiElement root, int x, int y, int width, String key, int color) {
        root.addChild(EsvoiceMutilWidgets.centeredText(x, y, width,
                Component.translatable(key), color));
    }

    private void next() {
        stopMicTest();
        step = Step.values()[Math.min(Step.values().length - 1, step.ordinal() + 1)];
        rebuildMutilRoot();
    }

    private void previous() {
        stopMicTest();
        step = Step.values()[Math.max(0, step.ordinal() - 1)];
        rebuildMutilRoot();
    }

    private void finish() {
        stopMicTest();
        VoicechatClient.CLIENT_CONFIG.onboardingFinished.set(true).save();
        ClientManager.getPlayerStateManager().onFinishOnboarding();
        Minecraft.getInstance().setScreen(parent);
    }

    private void cycleActivation() {
        MicrophoneActivationType current = VoicechatClient.CLIENT_CONFIG.microphoneActivationType.get();
        VoicechatClient.CLIENT_CONFIG.microphoneActivationType.set(
                current == MicrophoneActivationType.PTT
                        ? MicrophoneActivationType.VOICE : MicrophoneActivationType.PTT).save();
    }

    private void cycleDevice(boolean microphone) {
        List<String> devices = new ArrayList<>();
        devices.add("");
        try {
            devices.addAll(microphone ? MicrophoneManager.deviceNames() : SoundManager.getAllSpeakers());
        } catch (Throwable ignored) {
            return;
        }
        devices = devices.stream().distinct().toList();
        String current = microphone ? VoicechatClient.CLIENT_CONFIG.microphone.get()
                : VoicechatClient.CLIENT_CONFIG.speaker.get();
        int index = devices.indexOf(current);
        String next = devices.get((Math.max(-1, index) + 1) % devices.size());
        stopMicTest();
        if (microphone) {
            VoicechatClient.CLIENT_CONFIG.microphone.set(next).save();
        } else {
            VoicechatClient.CLIENT_CONFIG.speaker.set(next).save();
        }
        ClientVoicechat client = ClientManager.getClient();
        if (client != null) {
            client.reloadAudio();
        }
    }

    private Component visibleDevice(String device) {
        return device == null || device.isBlank()
                ? Component.translatable("gui.esvoice.device.default")
                : Component.literal(device);
    }

    private void ensureMicTest() {
        if (micTest != null) {
            return;
        }
        micTest = new MicTestButton(0, 0, false, new MicTestButton.MicListener() {
            @Override
            public void onMicValue(double value) {
                microphoneLevel = value;
            }

            @Override
            public void onStop() {
                microphoneLevel = AudioUtils.LOWEST_DB;
            }
        });
    }

    private void toggleMicTest() {
        ensureMicTest();
        micTest.onPress();
    }

    private boolean isMicTestActive() {
        return micTest != null && micTest.isMicActive();
    }

    private void stopMicTest() {
        if (micTest != null) {
            micTest.stop();
            micTest = null;
            microphoneLevel = AudioUtils.LOWEST_DB;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (micTest != null && micTest.isMicActive()) {
            micTest.updateLastRender();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        finish();
    }

    @Override
    public void removed() {
        stopMicTest();
        super.removed();
    }

    private enum Step {
        INTRODUCTION("tutorial.esvoice.step.introduction"),
        DEVICES("tutorial.esvoice.step.devices"),
        MICROPHONE("tutorial.esvoice.step.microphone"),
        KEYS("tutorial.esvoice.step.keys"),
        FINISH("tutorial.esvoice.step.finish");

        private final String titleKey;

        Step(String titleKey) {
            this.titleKey = titleKey;
        }
    }
}
