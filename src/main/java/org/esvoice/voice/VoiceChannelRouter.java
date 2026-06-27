package org.esvoice.voice;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.espetro.api.EspetroAPI;
import org.esvoice.network.EsvoiceNetwork;
import org.esvoice.network.VoiceSpeakingPacket;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VoiceChannelRouter {

    private static final long HUD_REFRESH_MILLIS = 600L;
    private static final Map<UUID, VoiceChannel> REQUESTED_CHANNELS = new ConcurrentHashMap<>();
    private static final Map<HudKey, Long> LAST_HUD_NOTIFICATION = new ConcurrentHashMap<>();

    private VoiceChannelRouter() {
    }

    public static void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, VoiceChannelRouter::onServerStarted);
        registration.registerEvent(MicrophonePacketEvent.class, VoiceChannelRouter::onMicrophonePacket, 100);
        registration.registerEvent(EntitySoundPacketEvent.class, VoiceChannelRouter::onPublicSoundPacket, -100);
    }

    public static void setRequestedChannel(ServerPlayer player, VoiceChannel channel) {
        if (channel == null || channel == VoiceChannel.PUBLIC) {
            REQUESTED_CHANNELS.remove(player.getUUID());
            return;
        }

        REQUESTED_CHANNELS.put(player.getUUID(), channel);
        if (channel == VoiceChannel.SQUAD && EspetroAPI.getPlayerSquadId(player.getUUID()) < 0) {
            player.displayClientMessage(Component.literal("§c你尚未加入班组小队，无法使用班组小队麦。"), true);
        } else if (channel == VoiceChannel.COMMAND
                && !EspetroAPI.isSquadLeader(player.getUUID())
                && !EspetroAPI.isCommander(player.getUUID())) {
            player.displayClientMessage(Component.literal("§c只有小队队长和阵营指挥官可以使用指挥频道。"), true);
        }
    }

    public static void clearPlayer(UUID playerId) {
        REQUESTED_CHANNELS.remove(playerId);
        LAST_HUD_NOTIFICATION.keySet().removeIf(key -> key.speakerId.equals(playerId)
                || key.listenerId.equals(playerId));
    }

    private static void onServerStarted(VoicechatServerStartedEvent event) {
        registerCategory(event.getVoicechat(), VoiceChannel.SQUAD,
                "班组小队频道", "同一班组小队的无线语音");
        registerCategory(event.getVoicechat(), VoiceChannel.COMMAND,
                "指挥频道", "本阵营指挥官和小队队长的无线语音");
    }

    private static void registerCategory(VoicechatServerApi api, VoiceChannel channel,
                                         String name, String description) {
        api.registerVolumeCategory(api.volumeCategoryBuilder()
                .setId(channel.getCategoryId())
                .setName(name)
                .setDescription(description)
                .build());
    }

    private static void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection senderConnection = event.getSenderConnection();
        if (senderConnection == null
                || !(senderConnection.getPlayer().getPlayer() instanceof ServerPlayer sender)) {
            return;
        }

        VoiceChannel channel = REQUESTED_CHANNELS.getOrDefault(sender.getUUID(), VoiceChannel.PUBLIC);
        if (!channel.isRadio()) {
            return;
        }

        // Wireless transmissions must never fall through to proximity chat.
        event.cancel();

        byte[] opusAudio = event.getPacket().getOpusEncodedData();
        if (opusAudio.length == 0) {
            return;
        }

        StaticSoundPacket radioPacket = event.getPacket().staticSoundPacketBuilder()
                .channelId(channelId(sender.getUUID(), channel))
                .opusEncodedData(opusAudio.clone())
                .category(channel.getCategoryId())
                .build();

        MinecraftServer server = sender.getServer();
        if (server != null) {
            VoicechatServerApi api = event.getVoicechat();
            UUID senderId = sender.getUUID();
            server.execute(() -> routeRadioPacket(server, api, senderId, channel, radioPacket));
        }
    }

    private static void routeRadioPacket(MinecraftServer server, VoicechatServerApi api, UUID senderId,
                                         VoiceChannel channel, StaticSoundPacket packet) {
        ServerPlayer sender = server.getPlayerList().getPlayer(senderId);
        if (sender == null) {
            return;
        }

        String senderTeam = EspetroAPI.getPlayerTeam(sender);
        if (senderTeam == null) {
            return;
        }

        int senderSquad = EspetroAPI.getPlayerSquadId(senderId);
        boolean senderCommandAuthorized = EspetroAPI.isSquadLeader(senderId)
                || EspetroAPI.isCommander(senderId);
        if ((channel == VoiceChannel.SQUAD && senderSquad < 0)
                || (channel == VoiceChannel.COMMAND && !senderCommandAuthorized)) {
            return;
        }

        for (ServerPlayer listener : server.getPlayerList().getPlayers()) {
            if (listener.getUUID().equals(senderId)
                    || !Objects.equals(senderTeam, EspetroAPI.getPlayerTeam(listener))) {
                continue;
            }

            boolean canReceive = channel == VoiceChannel.SQUAD
                    ? EspetroAPI.getPlayerSquadId(listener.getUUID()) == senderSquad
                    : EspetroAPI.isSquadLeader(listener.getUUID()) || EspetroAPI.isCommander(listener.getUUID());
            if (!canReceive) {
                continue;
            }

            VoicechatConnection connection = api.getConnectionOf(listener.getUUID());
            if (connection == null || !connection.isConnected() || connection.isDisabled()) {
                continue;
            }

            api.sendStaticSoundPacketTo(connection, packet);
            notifyHud(listener, sender, channel);
        }
    }

    private static void onPublicSoundPacket(EntitySoundPacketEvent event) {
        if (event.isCancelled() || !SoundPacketEvent.SOURCE_PROXIMITY.equals(event.getSource())
                || event.getPacket().getOpusEncodedData().length == 0) {
            return;
        }

        VoicechatConnection senderConnection = event.getSenderConnection();
        VoicechatConnection receiverConnection = event.getReceiverConnection();
        if (senderConnection == null || receiverConnection == null
                || !(senderConnection.getPlayer().getPlayer() instanceof ServerPlayer sender)
                || !(receiverConnection.getPlayer().getPlayer() instanceof ServerPlayer listener)) {
            return;
        }
        notifyHud(listener, sender, VoiceChannel.PUBLIC);
    }

    private static void notifyHud(ServerPlayer listener, ServerPlayer speaker, VoiceChannel channel) {
        long now = System.currentTimeMillis();
        HudKey key = new HudKey(listener.getUUID(), speaker.getUUID(), channel);
        AtomicBoolean shouldSend = new AtomicBoolean(false);
        LAST_HUD_NOTIFICATION.compute(key, (ignored, previous) -> {
            if (previous == null || now - previous >= HUD_REFRESH_MILLIS) {
                shouldSend.set(true);
                return now;
            }
            return previous;
        });

        if (shouldSend.get()) {
            EsvoiceNetwork.sendSpeaking(listener,
                    new VoiceSpeakingPacket(speaker.getUUID(), speaker.getName().getString(), channel));
        }
    }

    private static UUID channelId(UUID senderId, VoiceChannel channel) {
        return UUID.nameUUIDFromBytes(("esvoice:" + channel.name() + ":" + senderId)
                .getBytes(StandardCharsets.UTF_8));
    }

    private record HudKey(UUID listenerId, UUID speakerId, VoiceChannel channel) {
    }
}
