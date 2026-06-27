package org.esvoice.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.espetro.client.gui.ClientTacticalState;
import org.esvoice.voice.VoiceChannel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VoiceSpeakerHud {

    private static final long DISPLAY_MILLIS = 1_300L;
    private static final int AVATAR_SIZE = 16;
    private static final int ROW_HEIGHT = AVATAR_SIZE + 5;
    // Keep the lowest row above the vanilla hotbar/health HUD at every GUI scale.
    private static final int BOTTOM_MARGIN = 58;
    private static final int COMMANDER_GOLD = 0xFFFFC766;
    private static final Map<UUID, SpeakerEntry> ACTIVE_SPEAKERS = new LinkedHashMap<>();

    private VoiceSpeakerHud() {
    }

    public static synchronized void onSpeaking(UUID speakerId, String speakerName, VoiceChannel channel) {
        onSpeaking(speakerId, speakerName, channel, "");
    }

    public static synchronized void onLocalTransmit(UUID speakerId, String speakerName, VoiceChannel channel,
                                                     boolean muted, boolean disabled) {
        String status = disabled ? " [语音已关闭]" : muted ? " [已静音]" : "";
        onSpeaking(speakerId, speakerName, channel, status);
    }

    private static void onSpeaking(UUID speakerId, String speakerName, VoiceChannel channel, String status) {
        ACTIVE_SPEAKERS.put(speakerId,
                new SpeakerEntry(speakerId, speakerName, channel, status,
                        System.currentTimeMillis() + DISPLAY_MILLIS));
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) {
            return;
        }

        List<SpeakerEntry> speakers = activeSnapshot();
        int y = graphics.guiHeight() - BOTTOM_MARGIN - speakers.size() * ROW_HEIGHT;
        for (SpeakerEntry speaker : speakers) {
            int textWidth = minecraft.font.width(speaker.name) + minecraft.font.width(speaker.status);
            int width = AVATAR_SIZE + 8 + textWidth;

            graphics.fill(4, y - 2, 8 + width, y + AVATAR_SIZE + 2, 0x90000000);
            drawHead(graphics, skinFor(minecraft, speaker.id), 8, y);

            int textX = 8 + AVATAR_SIZE + 4;
            int textY = y + 4;
            graphics.drawString(minecraft.font, speaker.name, textX, textY, nameColor(speaker), true);
            if (!speaker.status.isEmpty()) {
                graphics.drawString(minecraft.font, speaker.status,
                        textX + minecraft.font.width(speaker.name), textY, 0xFFFF6666, true);
            }
            y += ROW_HEIGHT;
        }
    }

    private static synchronized List<SpeakerEntry> activeSnapshot() {
        long now = System.currentTimeMillis();
        ACTIVE_SPEAKERS.values().removeIf(entry -> entry.expiresAt < now);
        return new ArrayList<>(ACTIVE_SPEAKERS.values());
    }

    private static int nameColor(SpeakerEntry speaker) {
        int tacticalColor = 0xFF000000 | (ClientTacticalState.getNameColor(speaker.name) & 0xFFFFFF);
        if (speaker.channel == VoiceChannel.PUBLIC) {
            // Public voice is white except for a commander known by the local
            // faction state. Enemy commanders are not present in that state.
            return tacticalColor == COMMANDER_GOLD ? COMMANDER_GOLD : 0xFFFFFFFF;
        }
        return tacticalColor;
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

    private record SpeakerEntry(UUID id, String name, VoiceChannel channel, String status, long expiresAt) {
    }
}
