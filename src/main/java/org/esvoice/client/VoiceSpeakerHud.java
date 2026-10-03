package org.esvoice.client;

import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.espetromini.api.MiniClientAPI;
import org.esvoice.voice.VoiceChannel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VoiceSpeakerHud {

    private static final long DISPLAY_MILLIS = 1_300L;
    /** Compact head + name strip above the chat panel. */
    private static final int AVATAR_SIZE = 8;
    private static final int ROW_HEIGHT = 10;
    private static final int ROW_PAD_X = 3;
    private static final int NAME_GAP = 3;
    /** Overall strip scale (avatar + name). Was 0.72; reduced to 2/3 of that. */
    private static final float SPEAKER_SCALE = 0.72F * 2F / 3F;
    /** Vanilla chat is drawn with a 40px bottom inset (see ChatComponent.render). */
    private static final int CHAT_BOTTOM_INSET = 40;
    private static final int GAP_ABOVE_CHAT = 3;
    private static final int PUBLIC_BLUE = 0xFF55AAFF;
    private static final int SQUAD_GREEN = 0xFF55FF55;
    private static final int COMMAND_PURPLE = 0xFFAA55FF;
    private static final Map<UUID, SpeakerEntry> ACTIVE_SPEAKERS = new LinkedHashMap<>();

    private VoiceSpeakerHud() {
    }

    public static synchronized void onSpeaking(UUID speakerId, String speakerName, VoiceChannel channel) {
        updateSpeaker(speakerId, speakerName, channel, false);
    }

    public static synchronized void onLocalTransmit(UUID speakerId, String speakerName, VoiceChannel channel) {
        updateSpeaker(speakerId, speakerName, channel, true);
    }

    private static void updateSpeaker(UUID speakerId, String speakerName, VoiceChannel channel,
                                      boolean forceVisible) {
        SpeakerEntry entry = ACTIVE_SPEAKERS.get(speakerId);
        if (entry == null) {
            // Name-less packets are refreshes, but resolve a defensive fallback if the initial packet
            // was missed during a client world transition.
            String resolvedName = speakerName != null ? speakerName : resolveName(speakerId);
            entry = new SpeakerEntry(speakerId, resolvedName);
            ACTIVE_SPEAKERS.put(speakerId, entry);
        } else if (speakerName != null) {
            entry.name = speakerName;
        }
        entry.channel = channel;
        entry.forceVisible |= forceVisible;
        entry.expiresAt = System.currentTimeMillis() + DISPLAY_MILLIS;
    }

    public static synchronized void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null
                || !MiniClientAPI.showSpeakerHud()
                || VoicechatClient.CLIENT_CONFIG == null
                || VoicechatClient.CLIENT_CONFIG.hideIcons.get()) {
            return;
        }

        removeExpired(System.currentTimeMillis());
        if (VoicechatClient.CLIENT_CONFIG.showHudIcons.get()) {
            renderVoiceStatus(graphics, minecraft);
        }
        if (!VoicechatClient.CLIENT_CONFIG.showGroupHud.get()) {
            return;
        }
        boolean showSelf = VoicechatClient.CLIENT_CONFIG.showOwnGroupIcon.get();
        List<SpeakerEntry> visible = new ArrayList<>();
        for (SpeakerEntry speaker : ACTIVE_SPEAKERS.values()) {
            if (isVisible(speaker, minecraft.player.getUUID(), showSelf)) {
                visible.add(speaker);
            }
        }
        if (visible.isEmpty()) {
            return;
        }

        int chatHeight = Math.max(20, minecraft.gui.getChat().getHeight());
        // Screen-space bottom of the speaker stack sits just above the chat panel.
        int stackBottom = graphics.guiHeight() - CHAT_BOTTOM_INSET - chatHeight - GAP_ABOVE_CHAT;
        int stackHeight = Math.round(visible.size() * ROW_HEIGHT * SPEAKER_SCALE);
        int originX = 2;
        int originY = Math.max(2, stackBottom - stackHeight);

        graphics.pose().pushPose();
        graphics.pose().translate(originX, originY, 0F);
        graphics.pose().scale(SPEAKER_SCALE, SPEAKER_SCALE, 1F);

        int y = 0;
        for (SpeakerEntry speaker : visible) {
            int textWidth = minecraft.font.width(speaker.name);
            int width = AVATAR_SIZE + NAME_GAP + textWidth;
            graphics.fill(0, y - 1, ROW_PAD_X + width + 2, y + AVATAR_SIZE + 1, 0x99000000);
            drawHead(graphics, skinFor(minecraft, speaker.id), ROW_PAD_X, y);
            graphics.drawString(minecraft.font, speaker.name,
                    ROW_PAD_X + AVATAR_SIZE + NAME_GAP,
                    y + Math.max(0, (AVATAR_SIZE - minecraft.font.lineHeight) / 2),
                    nameColor(speaker), true);
            y += ROW_HEIGHT;
        }
        graphics.pose().popPose();
    }

    private static void renderVoiceStatus(GuiGraphics graphics, Minecraft minecraft) {
        var state = ClientManager.getPlayerStateManager();
        net.minecraft.network.chat.Component status;
        int color;
        if (state.isDisconnected()) {
            status = net.minecraft.network.chat.Component.translatable("hud.esvoice.status.disconnected");
            color = 0xFFFF6666;
        } else if (state.isDisabled()) {
            status = net.minecraft.network.chat.Component.translatable("hud.esvoice.status.disabled");
            color = 0xFFFFB44C;
        } else if (state.isMuted()) {
            status = net.minecraft.network.chat.Component.translatable("hud.esvoice.status.muted");
            color = 0xFFFFB44C;
        } else {
            return;
        }

        float scale = 0.7F;
        int textWidth = minecraft.font.width(status);
        int x = 2;
        int y = 2;

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1F);
        graphics.fill(Math.round(x / scale) - 3, Math.round(y / scale) - 2,
                Math.round(x / scale) + textWidth + 3,
                Math.round(y / scale) + minecraft.font.lineHeight + 2,
                0x99000000);
        graphics.drawString(minecraft.font, status, Math.round(x / scale), Math.round(y / scale), color, true);
        graphics.pose().popPose();
    }

    private static void removeExpired(long now) {
        Iterator<SpeakerEntry> iterator = ACTIVE_SPEAKERS.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expiresAt < now) {
                iterator.remove();
            }
        }
    }

    private static boolean isVisible(SpeakerEntry speaker, UUID localPlayerId, boolean showSelf) {
        return showSelf || speaker.forceVisible || !speaker.id.equals(localPlayerId);
    }

    private static String resolveName(UUID speakerId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(speakerId);
            if (info != null) {
                return info.getProfile().getName();
            }
        }
        return speakerId.toString();
    }

    private static int nameColor(SpeakerEntry speaker) {
        return switch (speaker.channel) {
            case SQUAD -> SQUAD_GREEN;
            case COMMAND -> COMMAND_PURPLE;
            case PUBLIC -> PUBLIC_BLUE;
        };
    }

    private static ResourceLocation skinFor(Minecraft minecraft, UUID playerId) {
        if (minecraft.getConnection() != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(playerId);
            if (info != null) {
                return info.getSkinLocation();
            }
        }
        return DefaultPlayerSkin.getDefaultSkin(playerId);
    }

    private static void drawHead(GuiGraphics graphics, ResourceLocation skin, int x, int y) {
        graphics.blit(skin, x, y, AVATAR_SIZE, AVATAR_SIZE, 8F, 8F, 8, 8, 64, 64);
        graphics.blit(skin, x, y, AVATAR_SIZE, AVATAR_SIZE, 40F, 8F, 8, 8, 64, 64);
    }

    private static final class SpeakerEntry {
        private final UUID id;
        private String name;
        private VoiceChannel channel = VoiceChannel.PUBLIC;
        private boolean forceVisible;
        private long expiresAt;

        private SpeakerEntry(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
