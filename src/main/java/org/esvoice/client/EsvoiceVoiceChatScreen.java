package org.esvoice.client;

import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.configbuilder.entry.ConfigEntry;
import de.maxhenkel.voicechat.configbuilder.entry.RangedConfigEntry;
import de.maxhenkel.voicechat.gui.widgets.MicTestButton;
import de.maxhenkel.voicechat.integration.freecam.FreecamMode;
import de.maxhenkel.voicechat.natives.RNNoiseManager;
import de.maxhenkel.voicechat.natives.SpeexManager;
import de.maxhenkel.voicechat.plugins.impl.VolumeCategoryImpl;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.client.ClientPlayerStateManager;
import de.maxhenkel.voicechat.voice.client.ClientVoicechat;
import de.maxhenkel.voicechat.voice.client.MicrophoneActivationType;
import de.maxhenkel.voicechat.voice.client.SoundManager;
import de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager;
import de.maxhenkel.voicechat.voice.client.speaker.AudioType;
import de.maxhenkel.voicechat.voice.common.AudioUtils;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import se.mickelus.mutil.gui.GuiElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Stable MUtil settings center for Esvoice and Simple Voice Chat.
 */
public final class EsvoiceVoiceChatScreen extends EsvoiceMutilScreen {

    private static final int MAX_PANEL_WIDTH = 680;
    private static final int MAX_PANEL_HEIGHT = 390;
    private static final int ROW_HEIGHT = 25;

    private final Screen parent;
    private Page page = Page.OVERVIEW;
    private MicTestButton micTest;
    private volatile double microphoneLevel = AudioUtils.LOWEST_DB;
    private String transientMessage;
    private long transientMessageUntil;

    public EsvoiceVoiceChatScreen() {
        this(null);
    }

    public EsvoiceVoiceChatScreen(Screen parent) {
        super(Component.translatable("gui.esvoice.title"));
        this.parent = parent;
    }

    @Override
    protected void buildMutilRoot(GuiElement root) {
        int panelW = Math.min(MAX_PANEL_WIDTH, Math.max(296, width - 16));
        int panelH = Math.min(MAX_PANEL_HEIGHT, Math.max(210, height - 16));
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;
        int padding = 10;

        root.addChild(EsvoiceMutilWidgets.panel(panelX, panelY, panelW, panelH));
        root.addChild(EsvoiceMutilWidgets.text(panelX + padding, panelY + 9,
                Component.translatable("gui.esvoice.title"), EsvoiceMutilWidgets.GOLD));
        root.addChild(EsvoiceMutilWidgets.dynamicText(panelX + padding + 104, panelY + 9, 250,
                this::connectionText, this::connectionColor, false));
        root.addChild(EsvoiceMutilWidgets.button(panelX + panelW - 55, panelY + 6, 45, 17,
                Component.translatable("gui.esvoice.close"), this::onClose));

        int tabsY = panelY + 29;
        int tabsW = panelW - padding * 2;
        int tabW = Math.max(48, tabsW / Page.values().length);
        for (int i = 0; i < Page.values().length; i++) {
            Page tab = Page.values()[i];
            int x = panelX + padding + i * tabW;
            int w = i == Page.values().length - 1 ? tabsW - i * tabW : tabW - 2;
            root.addChild(EsvoiceMutilWidgets.button(x, tabsY, w, 18,
                    Component.translatable(tab.translationKey), () -> selectPage(tab))
                    .selected(() -> page == tab));
        }

        int contentY = tabsY + 23;
        int footerHeight = 23;
        int contentH = Math.max(116, panelY + panelH - footerHeight - contentY);
        int contentW = panelW - padding * 2;
        root.addChild(EsvoiceMutilWidgets.softPanel(panelX + padding, contentY, contentW, contentH));

        if (VoicechatClient.CLIENT_CONFIG == null) {
            root.addChild(EsvoiceMutilWidgets.centeredText(panelX + padding, contentY + 45, contentW,
                    Component.translatable("gui.esvoice.loading"), EsvoiceMutilWidgets.WARNING));
        } else {
            EsvoiceMutilWidgets.VerticalScroll scroll = EsvoiceMutilWidgets.scroll(
                    panelX + padding + 1, contentY + 1, contentW - 2, contentH - 2,
                    contentHeight(page, contentW));
            root.addChild(scroll);
            switch (page) {
                case OVERVIEW -> buildOverview(scroll, contentW - 8);
                case AUDIO -> buildAudio(scroll, contentW - 8);
                case VOLUMES -> buildVolumes(scroll, contentW - 8);
                case KEYS -> buildKeys(scroll, contentW - 8);
                case ADVANCED -> buildAdvanced(scroll, contentW - 8);
            }
        }

        root.addChild(EsvoiceMutilWidgets.dynamicText(panelX + padding, panelY + panelH - 16,
                panelW - padding * 2, this::footerText,
                () -> transientMessage != null && System.currentTimeMillis() < transientMessageUntil
                        ? EsvoiceMutilWidgets.WARNING : EsvoiceMutilWidgets.DIM, true));
    }

    private int contentHeight(Page selected, int contentW) {
        return switch (selected) {
            case OVERVIEW -> 265;
            case AUDIO -> Math.max(560, 390 + audioDeviceCount() * 21);
            case VOLUMES -> Math.max(260, 82 + volumeEntryCount() * 34);
            case KEYS -> 260;
            case ADVANCED -> 895;
        };
    }

    private void selectPage(Page selected) {
        if (page == selected) {
            return;
        }
        stopMicTest();
        page = selected;
        rebuildMutilRoot();
    }

    private void buildOverview(GuiElement content, int width) {
        int x = 7;
        int y = 7;
        int half = (width - 18) / 2;
        ClientPlayerStateManager state = ClientManager.getPlayerStateManager();

        sectionTitle(content, x, y, "gui.esvoice.section.quick");
        y += 16;
        content.addChild(EsvoiceMutilWidgets.toggle(x, y, half, 19,
                () -> Component.translatable(state.isDisabled()
                        ? "gui.esvoice.voice.disabled" : "gui.esvoice.voice.enabled"),
                () -> !state.isDisabled(), () -> state.setDisabled(!state.isDisabled())));
        content.addChild(EsvoiceMutilWidgets.toggle(x + half + 5, y, half, 19,
                () -> Component.translatable(state.isMuted()
                        ? "gui.esvoice.microphone.muted" : "gui.esvoice.microphone.live"),
                () -> !state.isMuted(), () -> state.setMuted(!state.isMuted())));
        y += 24;
        content.addChild(EsvoiceMutilWidgets.toggle(x, y, half, 19,
                () -> Component.translatable(isHudVisible()
                        ? "gui.esvoice.hud.visible" : "gui.esvoice.hud.hidden"),
                this::isHudVisible, this::toggleHud));
        content.addChild(EsvoiceMutilWidgets.toggle(x + half + 5, y, half, 19,
                () -> Component.translatable(isRecording()
                        ? "gui.esvoice.recording.active" : "gui.esvoice.recording.inactive"),
                this::isRecording, this::toggleRecording));

        y += 35;
        sectionTitle(content, x, y, "gui.esvoice.section.channels");
        y += 16;
        channelCard(content, x, y, width - 15, "gui.esvoice.channel.public",
                "gui.esvoice.channel.public.desc", EsvoiceClientModEvents.PUBLIC_VOICE,
                EsvoiceMutilWidgets.TEXT);
        y += 42;
        channelCard(content, x, y, width - 15, "gui.esvoice.channel.squad",
                "gui.esvoice.channel.squad.desc", EsvoiceClientModEvents.SQUAD_VOICE,
                EsvoiceMutilWidgets.SQUAD_BLUE);
        y += 42;
        channelCard(content, x, y, width - 15, "gui.esvoice.channel.command",
                "gui.esvoice.channel.command.desc", EsvoiceClientModEvents.COMMAND_VOICE,
                EsvoiceMutilWidgets.GOLD);
        y += 50;
        content.addChild(EsvoiceMutilWidgets.button(x, y, width - 15, 20,
                Component.translatable("gui.esvoice.tutorial.open"),
                () -> Minecraft.getInstance().setScreen(new EsvoiceTutorialScreen(this, true))));
    }

    private void channelCard(GuiElement content, int x, int y, int width, String nameKey,
                             String descriptionKey, KeyMapping mapping, int color) {
        GuiElement card = EsvoiceMutilWidgets.softPanel(x, y, width, 36);
        content.addChild(card);
        content.addChild(EsvoiceMutilWidgets.text(x + 7, y + 6,
                Component.translatable(nameKey), color));
        content.addChild(EsvoiceMutilWidgets.text(x + 7, y + 20,
                Component.translatable(descriptionKey), EsvoiceMutilWidgets.DIM));
        content.addChild(EsvoiceMutilWidgets.dynamicText(x + width - 100, y + 11, 90,
                mapping::getTranslatedKeyMessage, () -> EsvoiceMutilWidgets.GOLD, true));
    }

    private void buildAudio(GuiElement content, int width) {
        int x = 7;
        int w = width - 15;
        int y = 7;

        sectionTitle(content, x, y, "gui.esvoice.section.output");
        y += 16;
        content.addChild(EsvoiceMutilWidgets.slider(x, y, w, 20,
                rangedMin(VoicechatClient.CLIENT_CONFIG.voiceChatVolume, 0D),
                rangedMax(VoicechatClient.CLIENT_CONFIG.voiceChatVolume, 3D),
                () -> VoicechatClient.CLIENT_CONFIG.voiceChatVolume.get(),
                value -> save(VoicechatClient.CLIENT_CONFIG.voiceChatVolume, round(value, 2)),
                () -> Component.translatable("gui.esvoice.volume.master",
                        Math.round(VoicechatClient.CLIENT_CONFIG.voiceChatVolume.get() * 100D) + "%")));
        y += 25;
        // 接收增益：Simple Voice Chat 总音量上限只有 3.0（≈ +9.5 dB），再响只能由 Esvoice 放大 PCM。
        content.addChild(EsvoiceMutilWidgets.slider(x, y, w, 20,
                EsvoiceClientConfig.MIN_RECEIVE_GAIN_DB,
                EsvoiceClientConfig.MAX_RECEIVE_GAIN_DB,
                EsvoiceClientConfig::receiveGainDb,
                value -> EsvoiceClientConfig.setReceiveGainDb(Math.rint(value)),
                () -> Component.translatable("gui.esvoice.volume.receive_gain",
                        signed(EsvoiceClientConfig.receiveGainDb()) + " dB")));
        y += 25;
        content.addChild(EsvoiceMutilWidgets.button(x, y, w, 19,
                () -> Component.translatable("gui.esvoice.output.mode",
                        VoicechatClient.CLIENT_CONFIG.audioType.get().getText()),
                this::cycleAudioType));

        y += 34;
        sectionTitle(content, x, y, "gui.esvoice.section.microphone");
        y += 16;
        content.addChild(EsvoiceMutilWidgets.slider(x, y, w, 20, -40D, 24D,
                () -> VoicechatClient.CLIENT_CONFIG.microphoneGain.get(),
                value -> save(VoicechatClient.CLIENT_CONFIG.microphoneGain, Math.rint(value)),
                () -> Component.translatable("gui.esvoice.microphone.gain",
                        signed(VoicechatClient.CLIENT_CONFIG.microphoneGain.get()) + " dB")));
        y += 25;

        int third = (w - 10) / 3;
        content.addChild(EsvoiceMutilWidgets.toggle(x, y, third, 19,
                () -> Component.translatable("gui.esvoice.agc"),
                () -> VoicechatClient.CLIENT_CONFIG.agc.get(),
                () -> save(VoicechatClient.CLIENT_CONFIG.agc, !VoicechatClient.CLIENT_CONFIG.agc.get()))
                .enabled(SpeexManager::canUseAgc));
        content.addChild(EsvoiceMutilWidgets.toggle(x + third + 5, y, third, 19,
                () -> Component.translatable("gui.esvoice.denoiser"),
                () -> VoicechatClient.CLIENT_CONFIG.denoiser.get(),
                () -> save(VoicechatClient.CLIENT_CONFIG.denoiser,
                        !VoicechatClient.CLIENT_CONFIG.denoiser.get()))
                .enabled(RNNoiseManager::canUseDenoiser));
        content.addChild(EsvoiceMutilWidgets.toggle(x + (third + 5) * 2, y, third, 19,
                () -> Component.translatable("gui.esvoice.vad"),
                () -> VoicechatClient.CLIENT_CONFIG.vad.get(),
                () -> save(VoicechatClient.CLIENT_CONFIG.vad, !VoicechatClient.CLIENT_CONFIG.vad.get()))
                .enabled(RNNoiseManager::canUseDenoiser));
        y += 25;

        content.addChild(EsvoiceMutilWidgets.button(x, y, w, 19,
                () -> Component.translatable("gui.esvoice.activation",
                        VoicechatClient.CLIENT_CONFIG.microphoneActivationType.get().getText()),
                this::cycleActivation));
        y += 25;
        content.addChild(EsvoiceMutilWidgets.slider(x, y, w, 20, -127D, 0D,
                () -> VoicechatClient.CLIENT_CONFIG.voiceActivationThreshold.get(),
                value -> save(VoicechatClient.CLIENT_CONFIG.voiceActivationThreshold, Math.rint(value)),
                () -> Component.translatable("gui.esvoice.activation.threshold",
                        Math.round(VoicechatClient.CLIENT_CONFIG.voiceActivationThreshold.get()) + " dB"))
                .enabled(() -> VoicechatClient.CLIENT_CONFIG.microphoneActivationType.get()
                        == MicrophoneActivationType.VOICE));
        y += 25;
        content.addChild(EsvoiceMutilWidgets.button(x, y, w, 19,
                () -> Component.translatable(isMicTestActive()
                                ? "gui.esvoice.microphone.test.stop" : "gui.esvoice.microphone.test.start",
                        Math.round(microphoneLevel) + " dB"),
                this::toggleMicTest)
                .selected(this::isMicTestActive));

        y += 34;
        sectionTitle(content, x, y, "gui.esvoice.section.devices");
        y += 16;
        content.addChild(EsvoiceMutilWidgets.text(x, y + 5,
                Component.translatable("gui.esvoice.device.microphone"), EsvoiceMutilWidgets.MUTED));
        y += 18;
        y = buildDeviceRows(content, x, y, w, true);
        y += 8;
        content.addChild(EsvoiceMutilWidgets.text(x, y + 5,
                Component.translatable("gui.esvoice.device.speaker"), EsvoiceMutilWidgets.MUTED));
        y += 18;
        buildDeviceRows(content, x, y, w, false);
    }

    private int buildDeviceRows(GuiElement content, int x, int y, int width, boolean microphone) {
        List<String> devices = new ArrayList<>();
        devices.add("");
        try {
            devices.addAll(microphone ? MicrophoneManager.deviceNames() : SoundManager.getAllSpeakers());
        } catch (Throwable throwable) {
            showMessage("gui.esvoice.device.error");
        }
        String selected = microphone
                ? VoicechatClient.CLIENT_CONFIG.microphone.get()
                : VoicechatClient.CLIENT_CONFIG.speaker.get();
        for (String device : devices.stream().distinct().toList()) {
            String visibleName = device.isBlank()
                    ? Component.translatable("gui.esvoice.device.default").getString()
                    : device;
            content.addChild(EsvoiceMutilWidgets.button(x, y, width, 18,
                    Component.literal(visibleName), () -> selectDevice(microphone, device))
                    .selected(() -> ObjectsEqual(selectedDevice(microphone), device)));
            y += 21;
        }
        return y;
    }

    private void buildVolumes(GuiElement content, int width) {
        int x = 7;
        int w = width - 15;
        int y = 7;
        List<FilterableRow> rows = new ArrayList<>();

        content.addChild(EsvoiceMutilWidgets.textField(x, y, w, 20, filter -> {
            String normalized = filter.toLowerCase(Locale.ROOT);
            rows.forEach(row -> row.element.setVisible(
                    normalized.isBlank() || row.searchText.contains(normalized)));
        }));
        y += 30;

        List<VolumeCategoryImpl> categories = new ArrayList<>(ClientManager.getCategoryManager().getCategories());
        categories.sort(Comparator.comparing(category -> category.getDisplayName().getString()));
        if (!categories.isEmpty()) {
            sectionTitle(content, x, y, "gui.esvoice.volume.categories");
            y += 16;
            for (VolumeCategoryImpl category : categories) {
                GuiElement row = volumeRow(w, y, category.getDisplayName(),
                        () -> VoicechatClient.CATEGORY_VOLUME_CONFIG.getVolume(category.getId()),
                        value -> VoicechatClient.CATEGORY_VOLUME_CONFIG.setVolume(
                                category.getId(), round(value, 2), category.getSearchName()));
                content.addChild(row);
                rows.add(new FilterableRow(row, category.getSearchName().toLowerCase(Locale.ROOT)));
                y += 34;
            }
        }

        sectionTitle(content, x, y, "gui.esvoice.volume.players");
        y += 16;
        Map<UUID, String> players = availablePlayers();
        for (Map.Entry<UUID, String> entry : players.entrySet()) {
            UUID id = entry.getKey();
            String name = entry.getValue();
            GuiElement row = volumeRow(w, y, Component.literal(name),
                    () -> VoicechatClient.PLAYER_VOLUME_CONFIG.getVolume(id),
                    value -> VoicechatClient.PLAYER_VOLUME_CONFIG.setVolume(id, round(value, 2), name));
            content.addChild(row);
            rows.add(new FilterableRow(row, name.toLowerCase(Locale.ROOT)));
            y += 34;
        }
        if (players.isEmpty()) {
            content.addChild(EsvoiceMutilWidgets.text(x, y,
                    Component.translatable("gui.esvoice.volume.no_players"), EsvoiceMutilWidgets.DIM));
        }
    }

    private GuiElement volumeRow(int width, int y, Component name, java.util.function.DoubleSupplier get,
                                 java.util.function.DoubleConsumer set) {
        GuiElement row = new GuiElement(7, y, width - 15, 29);
        row.addChild(EsvoiceMutilWidgets.softPanel(0, 0, row.getWidth(), row.getHeight()));
        row.addChild(EsvoiceMutilWidgets.text(7, 10, name, EsvoiceMutilWidgets.TEXT));
        int sliderW = Math.max(120, row.getWidth() / 2);
        row.addChild(EsvoiceMutilWidgets.slider(row.getWidth() - sliderW - 5, 5, sliderW, 19,
                0D, 4D, get, set,
                () -> {
                    double value = get.getAsDouble();
                    return value <= 0D
                            ? Component.translatable("gui.esvoice.volume.muted")
                            : Component.translatable("gui.esvoice.volume.multiplier",
                            String.format(Locale.ROOT, "%.2fx", value));
                }));
        return row;
    }

    private void buildKeys(GuiElement content, int width) {
        int x = 7;
        int w = width - 15;
        int y = 7;
        sectionTitle(content, x, y, "gui.esvoice.section.keys");
        y += 16;
        for (KeyMapping mapping : EsvoiceClientModEvents.ALL_KEYS) {
            GuiElement row = new GuiElement(x, y, w, 25);
            row.addChild(EsvoiceMutilWidgets.text(5, 8,
                    Component.translatable(mapping.getName()), EsvoiceMutilWidgets.TEXT));
            int keyW = Math.max(100, w / 3);
            row.addChild(EsvoiceMutilWidgets.keybind(w - keyW - 55, 3, keyW, 19, mapping));
            row.addChild(EsvoiceMutilWidgets.button(w - 50, 3, 45, 19,
                    Component.translatable("gui.esvoice.reset"), () -> resetKey(mapping)));
            content.addChild(row);
            y += 27;
        }
        content.addChild(EsvoiceMutilWidgets.text(x, y + 4,
                Component.translatable("gui.esvoice.keybind.help"), EsvoiceMutilWidgets.DIM));
    }

    private void buildAdvanced(GuiElement content, int width) {
        int x = 7;
        int w = width - 15;
        int y = 7;

        sectionTitle(content, x, y, "gui.esvoice.advanced.general");
        y += 16;
        y = addToggleRow(content, x, y, w, "gui.esvoice.advanced.local_server",
                () -> VoicechatClient.CLIENT_CONFIG.runLocalServer.get(),
                () -> toggle(VoicechatClient.CLIENT_CONFIG.runLocalServer));
        y = addToggleRow(content, x, y, w, "gui.esvoice.advanced.offline_players",
                () -> VoicechatClient.CLIENT_CONFIG.offlinePlayerVolumeAdjustment.get(),
                () -> toggle(VoicechatClient.CLIENT_CONFIG.offlinePlayerVolumeAdjustment));
        y = addToggleRow(content, x, y, w, "gui.esvoice.advanced.fake_players",
                () -> VoicechatClient.CLIENT_CONFIG.showFakePlayersDisconnected.get(),
                () -> toggle(VoicechatClient.CLIENT_CONFIG.showFakePlayersDisconnected));
        y = addToggleRow(content, x, y, w, "gui.esvoice.advanced.mute_on_join",
                () -> VoicechatClient.CLIENT_CONFIG.muteOnJoin.get(),
                () -> toggle(VoicechatClient.CLIENT_CONFIG.muteOnJoin));
        content.addChild(EsvoiceMutilWidgets.button(x, y, w, 19,
                () -> Component.translatable("gui.esvoice.advanced.freecam",
                        Component.translatable(VoicechatClient.CLIENT_CONFIG.freecamMode.get() == FreecamMode.CAMERA
                                ? "gui.esvoice.freecam.camera" : "gui.esvoice.freecam.player")),
                this::cycleFreecam));
        y += ROW_HEIGHT;

        sectionTitle(content, x, y + 6, "gui.esvoice.advanced.network_audio");
        y += 22;
        y = addIntSlider(content, x, y, w, "gui.esvoice.advanced.packet_threshold",
                VoicechatClient.CLIENT_CONFIG.audioPacketThreshold);
        y = addIntSlider(content, x, y, w, "gui.esvoice.advanced.voice_delay",
                VoicechatClient.CLIENT_CONFIG.voiceDeactivationDelay);
        y = addIntSlider(content, x, y, w, "gui.esvoice.advanced.ptt_delay",
                VoicechatClient.CLIENT_CONFIG.pttDeactivationDelay);
        y = addIntSlider(content, x, y, w, "gui.esvoice.advanced.output_buffer",
                VoicechatClient.CLIENT_CONFIG.outputBufferSize);

        sectionTitle(content, x, y + 6, "gui.esvoice.advanced.hud");
        y += 22;
        y = addToggleRow(content, x, y, w, "gui.esvoice.advanced.nametag_icons",
                () -> VoicechatClient.CLIENT_CONFIG.showNametagIcons.get(),
                () -> toggle(VoicechatClient.CLIENT_CONFIG.showNametagIcons));
        y = addToggleRow(content, x, y, w, "gui.esvoice.advanced.hud_icons",
                () -> VoicechatClient.CLIENT_CONFIG.showHudIcons.get(),
                () -> toggle(VoicechatClient.CLIENT_CONFIG.showHudIcons));
        y = addToggleRow(content, x, y, w, "gui.esvoice.advanced.group_hud",
                () -> VoicechatClient.CLIENT_CONFIG.showGroupHud.get(),
                () -> toggle(VoicechatClient.CLIENT_CONFIG.showGroupHud));
        y = addToggleRow(content, x, y, w, "gui.esvoice.advanced.show_self",
                () -> VoicechatClient.CLIENT_CONFIG.showOwnGroupIcon.get(),
                () -> toggle(VoicechatClient.CLIENT_CONFIG.showOwnGroupIcon));
        content.addChild(EsvoiceMutilWidgets.text(x, y + 4,
                Component.translatable("gui.esvoice.advanced.status_hud"),
                EsvoiceMutilWidgets.DIM));
        y += 18;
        y = addDoubleSlider(content, x, y, w, "gui.esvoice.advanced.status_scale",
                VoicechatClient.CLIENT_CONFIG.hudIconScale, 0.25D, 3D, 2);
        y = addIntSlider(content, x, y, w, "gui.esvoice.advanced.status_x",
                VoicechatClient.CLIENT_CONFIG.hudIconPosX);
        y = addIntSlider(content, x, y, w, "gui.esvoice.advanced.status_y",
                VoicechatClient.CLIENT_CONFIG.hudIconPosY);
        content.addChild(EsvoiceMutilWidgets.text(x, y + 4,
                Component.translatable("gui.esvoice.advanced.speaker_hud"),
                EsvoiceMutilWidgets.DIM));
        y += 18;
        y = addDoubleSlider(content, x, y, w, "gui.esvoice.advanced.hud_scale",
                VoicechatClient.CLIENT_CONFIG.groupHudIconScale, 0.25D, 3D, 2);
        y = addIntSlider(content, x, y, w, "gui.esvoice.advanced.hud_x",
                VoicechatClient.CLIENT_CONFIG.groupPlayerIconPosX);
        y = addIntSlider(content, x, y, w, "gui.esvoice.advanced.hud_y",
                VoicechatClient.CLIENT_CONFIG.groupPlayerIconPosY);
        content.addChild(EsvoiceMutilWidgets.button(x, y, w, 19,
                () -> Component.translatable("gui.esvoice.advanced.hud_direction",
                        Component.translatable(VoicechatClient.CLIENT_CONFIG.groupPlayerIconOrientation.get()
                                .name().equals("HORIZONTAL")
                                ? "gui.esvoice.direction.horizontal" : "gui.esvoice.direction.vertical")),
                this::cycleHudDirection));
        y += ROW_HEIGHT;

        sectionTitle(content, x, y + 6, "gui.esvoice.advanced.recording");
        y += 22;
        y = addIntSlider(content, x, y, w, "gui.esvoice.advanced.recording_quality",
                VoicechatClient.CLIENT_CONFIG.recordingQuality);
        content.addChild(EsvoiceMutilWidgets.text(x, y,
                Component.translatable("gui.esvoice.advanced.recording_path"),
                EsvoiceMutilWidgets.DIM));
        y += 14;
        content.addChild(EsvoiceMutilWidgets.textField(x, y, w, 20,
                VoicechatClient.CLIENT_CONFIG.recordingDestination.get(),
                Component.translatable("gui.esvoice.device.default"),
                value -> save(VoicechatClient.CLIENT_CONFIG.recordingDestination, value)));
        y += 30;

        sectionTitle(content, x, y + 6, "gui.esvoice.advanced.compatibility");
        y += 22;
        y = addToggleRow(content, x, y, w, "gui.esvoice.advanced.use_natives",
                () -> VoicechatClient.CLIENT_CONFIG.useNatives.get(),
                () -> toggle(VoicechatClient.CLIENT_CONFIG.useNatives));
        y = addToggleRow(content, x, y, w, "gui.esvoice.advanced.java_microphone",
                () -> VoicechatClient.CLIENT_CONFIG.javaMicrophoneImplementation.get(),
                () -> {
                    toggle(VoicechatClient.CLIENT_CONFIG.javaMicrophoneImplementation);
                    reloadAudio();
                });
        addToggleRow(content, x, y, w, "gui.esvoice.advanced.macos_permission",
                () -> VoicechatClient.CLIENT_CONFIG.macosCheckMicrophonePermission.get(),
                () -> toggle(VoicechatClient.CLIENT_CONFIG.macosCheckMicrophonePermission));
    }

    private int addToggleRow(GuiElement content, int x, int y, int width, String key,
                             BooleanSupplier value, Runnable action) {
        content.addChild(EsvoiceMutilWidgets.toggle(x, y, width, 19,
                () -> Component.translatable(key), value, action));
        return y + ROW_HEIGHT;
    }

    private int addIntSlider(GuiElement content, int x, int y, int width, String key,
                             ConfigEntry<Integer> entry) {
        int min = (int) rangedMin(entry, 0D);
        int max = (int) rangedMax(entry, Math.max(100D, entry.get() * 4D));
        content.addChild(EsvoiceMutilWidgets.slider(x, y, width, 19, min, max,
                () -> entry.get(), value -> save(entry, (int) Math.round(value)),
                () -> Component.translatable(key, entry.get())));
        return y + ROW_HEIGHT;
    }

    private int addDoubleSlider(GuiElement content, int x, int y, int width, String key,
                                ConfigEntry<Double> entry, double fallbackMin,
                                double fallbackMax, int decimals) {
        double min = rangedMin(entry, fallbackMin);
        double max = rangedMax(entry, fallbackMax);
        content.addChild(EsvoiceMutilWidgets.slider(x, y, width, 19, min, max,
                entry::get, value -> save(entry, round(value, decimals)),
                () -> Component.translatable(key,
                        String.format(Locale.ROOT, "%." + decimals + "f", entry.get()))));
        return y + ROW_HEIGHT;
    }

    private void sectionTitle(GuiElement content, int x, int y, String key) {
        content.addChild(EsvoiceMutilWidgets.text(x, y,
                Component.translatable(key), EsvoiceMutilWidgets.MUTED));
    }

    private void toggleHud() {
        save(VoicechatClient.CLIENT_CONFIG.hideIcons, !VoicechatClient.CLIENT_CONFIG.hideIcons.get());
    }

    private boolean isHudVisible() {
        return !VoicechatClient.CLIENT_CONFIG.hideIcons.get();
    }

    private void toggleRecording() {
        ClientVoicechat client = ClientManager.getClient();
        if (client == null || client.getConnection() == null) {
            showMessage("gui.esvoice.recording.not_connected");
            return;
        }
        if (client.getInitializationData() == null || !client.getInitializationData().allowRecording()) {
            showMessage("gui.esvoice.recording.not_allowed");
            return;
        }
        client.toggleRecording();
        showMessage(isRecording() ? "gui.esvoice.recording.started" : "gui.esvoice.recording.stopped");
    }

    private boolean isRecording() {
        ClientVoicechat client = ClientManager.getClient();
        return client != null && client.getRecorder() != null;
    }

    private void cycleAudioType() {
        AudioType current = VoicechatClient.CLIENT_CONFIG.audioType.get();
        AudioType[] values = AudioType.values();
        save(VoicechatClient.CLIENT_CONFIG.audioType, values[(current.ordinal() + 1) % values.length]);
        reloadAudio();
    }

    private void cycleActivation() {
        MicrophoneActivationType current = VoicechatClient.CLIENT_CONFIG.microphoneActivationType.get();
        save(VoicechatClient.CLIENT_CONFIG.microphoneActivationType,
                current == MicrophoneActivationType.PTT
                        ? MicrophoneActivationType.VOICE : MicrophoneActivationType.PTT);
    }

    private void cycleFreecam() {
        FreecamMode current = VoicechatClient.CLIENT_CONFIG.freecamMode.get();
        save(VoicechatClient.CLIENT_CONFIG.freecamMode,
                current == FreecamMode.CAMERA ? FreecamMode.PLAYER : FreecamMode.CAMERA);
    }

    private void cycleHudDirection() {
        var entry = VoicechatClient.CLIENT_CONFIG.groupPlayerIconOrientation;
        var values = de.maxhenkel.voicechat.voice.client.GroupPlayerIconOrientation.values();
        save(entry, values[(entry.get().ordinal() + 1) % values.length]);
    }

    private void selectDevice(boolean microphone, String device) {
        stopMicTest();
        if (microphone) {
            save(VoicechatClient.CLIENT_CONFIG.microphone, device);
        } else {
            save(VoicechatClient.CLIENT_CONFIG.speaker, device);
        }
        reloadAudio();
        showMessage("gui.esvoice.device.changed");
    }

    private String selectedDevice(boolean microphone) {
        return microphone ? VoicechatClient.CLIENT_CONFIG.microphone.get()
                : VoicechatClient.CLIENT_CONFIG.speaker.get();
    }

    private static boolean ObjectsEqual(Object first, Object second) {
        return java.util.Objects.equals(first, second);
    }

    private void reloadAudio() {
        ClientVoicechat client = ClientManager.getClient();
        if (client != null) {
            try {
                client.reloadAudio();
            } catch (Throwable throwable) {
                showMessage("gui.esvoice.device.error");
            }
        }
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

    private void resetKey(KeyMapping mapping) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.setKey(mapping, mapping.getDefaultKey());
        KeyMapping.resetMapping();
        minecraft.options.save();
    }

    private Map<UUID, String> availablePlayers() {
        Map<UUID, String> players = new LinkedHashMap<>();
        for (PlayerState state : ClientManager.getPlayerStateManager().getPlayerStates(true)) {
            if (state.getName() != null) {
                players.put(state.getUuid(), state.getName());
            }
        }
        if (VoicechatClient.CLIENT_CONFIG.offlinePlayerVolumeAdjustment.get()) {
            for (UUID id : VoicechatClient.PLAYER_VOLUME_CONFIG.getVolumes().keySet()) {
                if (!players.containsKey(id) && VoicechatClient.USERNAME_CACHE.has(id)) {
                    players.put(id, VoicechatClient.USERNAME_CACHE.getUsername(id));
                }
            }
        }
        return players.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);
    }

    private int volumeEntryCount() {
        if (VoicechatClient.CLIENT_CONFIG == null) {
            return 0;
        }
        return ClientManager.getCategoryManager().getCategories().size() + availablePlayers().size();
    }

    private int audioDeviceCount() {
        int count = 2;
        try {
            count += MicrophoneManager.deviceNames().size();
        } catch (Throwable ignored) {
            // The audio page will show its regular device error state.
        }
        try {
            count += SoundManager.getAllSpeakers().size();
        } catch (Throwable ignored) {
            // The audio page will show its regular device error state.
        }
        return count;
    }

    private Component connectionText() {
        ClientPlayerStateManager state = ClientManager.getPlayerStateManager();
        ClientVoicechat client = ClientManager.getClient();
        if (state.isDisconnected() || client == null || client.getConnection() == null) {
            return Component.translatable("gui.esvoice.connection.disconnected");
        }
        if (state.isDisabled()) {
            return Component.translatable("gui.esvoice.connection.disabled");
        }
        return Component.translatable("gui.esvoice.connection.connected");
    }

    private int connectionColor() {
        ClientPlayerStateManager state = ClientManager.getPlayerStateManager();
        ClientVoicechat client = ClientManager.getClient();
        return state.isDisconnected() || client == null || client.getConnection() == null
                ? EsvoiceMutilWidgets.NEGATIVE
                : state.isDisabled() ? EsvoiceMutilWidgets.WARNING : EsvoiceMutilWidgets.POSITIVE;
    }

    private Component footerText() {
        if (transientMessage != null && System.currentTimeMillis() < transientMessageUntil) {
            return Component.translatable(transientMessage);
        }
        transientMessage = null;
        return Component.translatable("gui.esvoice.footer",
                EsvoiceClientModEvents.PUBLIC_VOICE.getTranslatedKeyMessage(),
                EsvoiceClientModEvents.SQUAD_VOICE.getTranslatedKeyMessage(),
                EsvoiceClientModEvents.COMMAND_VOICE.getTranslatedKeyMessage(),
                EsvoiceClientModEvents.VOICE_SETTINGS.getTranslatedKeyMessage());
    }

    private void showMessage(String translationKey) {
        transientMessage = translationKey;
        transientMessageUntil = System.currentTimeMillis() + 3_000L;
    }

    @SuppressWarnings("unchecked")
    private static double rangedMin(ConfigEntry<? extends Number> entry, double fallback) {
        if (entry instanceof RangedConfigEntry<?> ranged && ranged.getMin() instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private static double rangedMax(ConfigEntry<? extends Number> entry, double fallback) {
        if (entry instanceof RangedConfigEntry<?> ranged && ranged.getMax() instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }

    private static <T> void save(ConfigEntry<T> entry, T value) {
        entry.set(value).save();
    }

    private static void toggle(ConfigEntry<Boolean> entry) {
        save(entry, !entry.get());
    }

    private static double round(double value, int decimals) {
        double scale = Math.pow(10D, decimals);
        return Math.round(value * scale) / scale;
    }

    private static String signed(double value) {
        return String.format(Locale.ROOT, "%+.0f", value);
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
        stopMicTest();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void removed() {
        stopMicTest();
        super.removed();
    }

    private enum Page {
        OVERVIEW("gui.esvoice.tab.overview"),
        AUDIO("gui.esvoice.tab.audio"),
        VOLUMES("gui.esvoice.tab.volumes"),
        KEYS("gui.esvoice.tab.keys"),
        ADVANCED("gui.esvoice.tab.advanced");

        private final String translationKey;

        Page(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    private record FilterableRow(GuiElement element, String searchText) {
    }
}
