package org.esvoice.voice;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.espetro.Espetro;
import org.espetro.team.SquadManager;
import org.espetro.team.VoteManager;
import org.esvoice.network.EsvoiceNetwork;
import org.esvoice.network.VoiceModeAckPacket;
import org.esvoice.network.VoiceSpeakingPacket;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceChannelRouter {

    private static final long HUD_REFRESH_MILLIS = 600L;
    private static final long HUD_BURST_TIMEOUT_MILLIS = 1_300L;
    private static final int MAX_PENDING_FRAMES_PER_PLAYER = 10;
    private static final Component NOT_IN_SQUAD_MESSAGE = Component.literal("§c未加入小队");
    private static final Component COMMAND_DENIED_MESSAGE =
            Component.literal("§c只有小队队长和阵营指挥官可以使用指挥频道。");
    private static final String[] KNOWN_TEAMS = {"ATTACK", "DEFEND"};
    private static final Map<UUID, VoiceChannel> REQUESTED_CHANNELS = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingRadioStream> PENDING_RADIO_STREAMS = new ConcurrentHashMap<>();
    private static final Map<UUID, SpeakerHudState> HUD_STATES = new ConcurrentHashMap<>();
    // Accessed only from the Minecraft server thread.
    private static final Map<UUID, PlayerRoute> PLAYER_ROUTES = new HashMap<>();
    private static MinecraftServer routeCacheServer;
    private static int routeCacheTick = Integer.MIN_VALUE;

    private VoiceChannelRouter() {
    }

    public static void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, VoiceChannelRouter::onServerStarted);
        registration.registerEvent(MicrophonePacketEvent.class, VoiceChannelRouter::onMicrophonePacket, 100);
        registration.registerEvent(EntitySoundPacketEvent.class, VoiceChannelRouter::onPublicSoundPacket, -100);
    }

    public static VoiceChannel setRequestedChannel(ServerPlayer player, VoiceChannel channel) {
        VoiceChannel requestedChannel = channel == null ? VoiceChannel.PUBLIC : channel;
        if (requestedChannel == VoiceChannel.PUBLIC) {
            REQUESTED_CHANNELS.remove(player.getUUID());
            return VoiceChannel.PUBLIC;
        }

        if (requestedChannel == VoiceChannel.SQUAD && getPlayerSquadId(player) == SquadManager.NO_SQUAD) {
            denyRadio(player, NOT_IN_SQUAD_MESSAGE);
            return VoiceChannel.PUBLIC;
        }
        if (requestedChannel == VoiceChannel.COMMAND && !isCommandAuthorized(player)) {
            denyRadio(player, COMMAND_DENIED_MESSAGE);
            return VoiceChannel.PUBLIC;
        }

        REQUESTED_CHANNELS.put(player.getUUID(), requestedChannel);
        return requestedChannel;
    }

    public static void clearPlayer(UUID playerId) {
        REQUESTED_CHANNELS.remove(playerId);
        PendingRadioStream stream = PENDING_RADIO_STREAMS.remove(playerId);
        if (stream != null) {
            stream.close();
        }
        HUD_STATES.remove(playerId);
        HUD_STATES.values().forEach(state -> state.removeListener(playerId));
        PLAYER_ROUTES.remove(playerId);
    }

    private static void onServerStarted(VoicechatServerStartedEvent event) {
        PENDING_RADIO_STREAMS.values().forEach(PendingRadioStream::close);
        PENDING_RADIO_STREAMS.clear();
        HUD_STATES.clear();
        PLAYER_ROUTES.clear();
        routeCacheServer = null;
        routeCacheTick = Integer.MIN_VALUE;
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

        MinecraftServer server = sender.getServer();
        if (server != null) {
            UUID senderId = sender.getUUID();
            PendingRadioStream stream = PENDING_RADIO_STREAMS.computeIfAbsent(senderId,
                    ignored -> new PendingRadioStream(server, event.getVoicechat(), senderId));
            stream.enqueue(event.getPacket(), channel, opusAudio);
        }
    }

    private static void routeRadioPacket(MinecraftServer server, VoicechatServerApi api, UUID senderId,
                                         VoiceChannel channel, StaticSoundPacket packet) {
        ServerPlayer sender = server.getPlayerList().getPlayer(senderId);
        if (sender == null) {
            return;
        }

        refreshPlayerRoutes(server, api);
        PlayerRoute senderRoute = PLAYER_ROUTES.get(senderId);
        if (senderRoute == null) {
            return;
        }

        if (channel == VoiceChannel.SQUAD) {
            routeSquadPacket(server, api, sender, senderRoute, packet);
            return;
        }

        String senderTeam = senderRoute.team;
        if (senderTeam == null) {
            return;
        }
        if (!senderRoute.commandAuthorized) {
            stopDeniedRadio(sender, COMMAND_DENIED_MESSAGE);
            return;
        }

        for (ServerPlayer listener : server.getPlayerList().getPlayers()) {
            PlayerRoute listenerRoute = PLAYER_ROUTES.get(listener.getUUID());
            if (listener == sender || listenerRoute == null
                    || !Objects.equals(senderTeam, listenerRoute.team)) {
                continue;
            }

            if (!listenerRoute.commandAuthorized) {
                continue;
            }

            VoicechatConnection connection = listenerRoute.connection;
            if (connection == null || !connection.isConnected() || connection.isDisabled()) {
                continue;
            }

            api.sendStaticSoundPacketTo(connection, packet);
            notifyHud(listener, sender, channel);
        }
    }

    private static void routeSquadPacket(MinecraftServer server, VoicechatServerApi api, ServerPlayer sender,
                                         PlayerRoute senderRoute, StaticSoundPacket packet) {
        int senderSquad = senderRoute.squadId;
        if (senderSquad == SquadManager.NO_SQUAD) {
            stopDeniedRadio(sender, NOT_IN_SQUAD_MESSAGE);
            return;
        }

        for (ServerPlayer listener : server.getPlayerList().getPlayers()) {
            PlayerRoute listenerRoute = PLAYER_ROUTES.get(listener.getUUID());
            if (listener == sender || listenerRoute == null || listenerRoute.squadId != senderSquad) {
                continue;
            }

            VoicechatConnection connection = listenerRoute.connection;
            if (connection == null || !connection.isConnected() || connection.isDisabled()) {
                continue;
            }

            api.sendStaticSoundPacketTo(connection, packet);
            notifyHud(listener, sender, VoiceChannel.SQUAD);
        }
    }

    private static void denyRadio(ServerPlayer player, Component message) {
        REQUESTED_CHANNELS.remove(player.getUUID());
        player.displayClientMessage(message, true);
    }

    private static void stopDeniedRadio(ServerPlayer player, Component message) {
        denyRadio(player, message);
        EsvoiceNetwork.acknowledgeMode(player, new VoiceModeAckPacket(VoiceChannel.PUBLIC));
    }

    private static String getPlayerTeam(ServerPlayer player) {
        return Espetro.getPlayerTeam(player);
    }

    private static int getPlayerSquadId(UUID playerId) {
        return SquadManager.getInstance().getPlayerSquadId(playerId);
    }

    private static int getPlayerSquadId(ServerPlayer player) {
        int squadId = getPlayerSquadId(player.getUUID());
        if (squadId != SquadManager.NO_SQUAD) {
            return squadId;
        }

        String team = getPlayerTeam(player);
        if (team != null) {
            squadId = findPlayerSquadId(player.getUUID(), team);
            if (squadId != SquadManager.NO_SQUAD) {
                return squadId;
            }
        }

        for (String knownTeam : KNOWN_TEAMS) {
            squadId = findPlayerSquadId(player.getUUID(), knownTeam);
            if (squadId != SquadManager.NO_SQUAD) {
                return squadId;
            }
        }
        return SquadManager.NO_SQUAD;
    }

    private static int findPlayerSquadId(UUID playerId, String team) {
        for (SquadManager.SquadSnapshot squad : SquadManager.getInstance().getSquadSnapshots(team)) {
            for (SquadManager.MemberSnapshot member : squad.members) {
                if (playerId.equals(member.uuid)) {
                    return squad.id;
                }
            }
        }
        return SquadManager.NO_SQUAD;
    }

    private static boolean isCommandAuthorized(ServerPlayer player) {
        return isSquadLeader(player) || VoteManager.getInstance().isCommander(player.getUUID());
    }

    private static boolean isSquadLeader(ServerPlayer player) {
        int squadId = getPlayerSquadId(player);
        String team = getPlayerTeam(player);
        return isSquadLeader(player.getUUID(), squadId, team);
    }

    private static boolean isSquadLeader(UUID playerId, int squadId, String team) {
        if (squadId == SquadManager.NO_SQUAD) {
            return false;
        }
        if (team == null) {
            return false;
        }

        for (SquadManager.SquadSnapshot squad : SquadManager.getInstance().getSquadSnapshots(team)) {
            if (squad.id != squadId) {
                continue;
            }
            for (SquadManager.MemberSnapshot member : squad.members) {
                if (playerId.equals(member.uuid) && member.leader) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void refreshPlayerRoutes(MinecraftServer server, VoicechatServerApi api) {
        int currentTick = server.getTickCount();
        if (routeCacheServer == server && routeCacheTick == currentTick) {
            return;
        }

        if (routeCacheServer != server) {
            PLAYER_ROUTES.clear();
            routeCacheServer = server;
        }
        routeCacheTick = currentTick;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            String team = getPlayerTeam(player);
            int squadId = getPlayerSquadId(player);
            boolean commandAuthorized = isSquadLeader(playerId, squadId, team)
                    || VoteManager.getInstance().isCommander(playerId);
            PlayerRoute route = PLAYER_ROUTES.computeIfAbsent(playerId, ignored -> new PlayerRoute());
            route.team = team;
            route.squadId = squadId;
            route.commandAuthorized = commandAuthorized;
            route.connection = api.getConnectionOf(playerId);
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
        UUID speakerId = speaker.getUUID();
        SpeakerHudState speakerState = HUD_STATES.computeIfAbsent(speakerId, ignored -> new SpeakerHudState());
        HudNotification notification = speakerState.notificationFor(listener.getUUID(), channel, now);
        if (notification != HudNotification.NONE) {
            String speakerName = notification == HudNotification.NEW_BURST
                    ? speaker.getName().getString()
                    : null;
            EsvoiceNetwork.sendSpeaking(listener, new VoiceSpeakingPacket(speakerId, speakerName, channel));
        }
    }

    private static UUID channelId(UUID senderId, VoiceChannel channel) {
        return UUID.nameUUIDFromBytes(("esvoice:" + channel.name() + ":" + senderId)
                .getBytes(StandardCharsets.UTF_8));
    }

    private enum HudNotification {
        NONE,
        REFRESH,
        NEW_BURST
    }

    private static final class SpeakerHudState {
        private final Map<UUID, ListenerHudState> listeners = new ConcurrentHashMap<>();

        private HudNotification notificationFor(UUID listenerId, VoiceChannel channel, long now) {
            return listeners.computeIfAbsent(listenerId, ignored -> new ListenerHudState())
                    .notificationFor(channel, now);
        }

        private void removeListener(UUID listenerId) {
            listeners.remove(listenerId);
        }
    }

    private static final class ListenerHudState {
        private final long[] lastAudioMillis = new long[VoiceChannel.values().length];
        private final long[] lastNotificationMillis = new long[VoiceChannel.values().length];

        private synchronized HudNotification notificationFor(VoiceChannel channel, long now) {
            int index = channel.ordinal();
            long previousAudio = lastAudioMillis[index];
            boolean newBurst = previousAudio == 0L || now - previousAudio >= HUD_BURST_TIMEOUT_MILLIS;
            lastAudioMillis[index] = now;

            if (!newBurst && now - lastNotificationMillis[index] < HUD_REFRESH_MILLIS) {
                return HudNotification.NONE;
            }
            lastNotificationMillis[index] = now;
            return newBurst ? HudNotification.NEW_BURST : HudNotification.REFRESH;
        }
    }

    private static final class PlayerRoute {
        private String team;
        private int squadId = SquadManager.NO_SQUAD;
        private boolean commandAuthorized;
        private VoicechatConnection connection;
    }

    private record RadioFrame(VoiceChannel channel, StaticSoundPacket packet) {
    }

    private static final class PendingRadioStream implements Runnable {
        private final MinecraftServer server;
        private final VoicechatServerApi api;
        private final UUID senderId;
        private final UUID squadChannelId;
        private final UUID commandChannelId;
        private final ArrayDeque<RadioFrame> frames = new ArrayDeque<>(MAX_PENDING_FRAMES_PER_PLAYER);
        private boolean scheduled;
        private boolean closed;

        private PendingRadioStream(MinecraftServer server, VoicechatServerApi api, UUID senderId) {
            this.server = server;
            this.api = api;
            this.senderId = senderId;
            this.squadChannelId = channelId(senderId, VoiceChannel.SQUAD);
            this.commandChannelId = channelId(senderId, VoiceChannel.COMMAND);
        }

        private void enqueue(MicrophonePacket microphonePacket, VoiceChannel channel, byte[] opusAudio) {
            StaticSoundPacket radioPacket = microphonePacket.staticSoundPacketBuilder()
                    .channelId(channel == VoiceChannel.SQUAD ? squadChannelId : commandChannelId)
                    // The voice-chat event owns its input array; queued frames need an independent lifetime.
                    .opusEncodedData(opusAudio.clone())
                    .category(channel.getCategoryId())
                    .build();

            synchronized (this) {
                if (closed) {
                    return;
                }
                if (frames.size() >= MAX_PENDING_FRAMES_PER_PLAYER) {
                    // Old audio is no longer useful after this much main-thread delay. Keeping it would
                    // increase both latency and heap pressure while the server is already overloaded.
                    frames.removeFirst();
                }
                frames.addLast(new RadioFrame(channel, radioPacket));
                if (scheduled) {
                    return;
                }
                scheduled = true;
            }
            server.execute(this);
        }

        @Override
        public void run() {
            while (true) {
                RadioFrame frame;
                synchronized (this) {
                    frame = frames.pollFirst();
                    if (frame == null || closed) {
                        frames.clear();
                        scheduled = false;
                        return;
                    }
                }
                routeRadioPacket(server, api, senderId, frame.channel, frame.packet);
            }
        }

        private synchronized void close() {
            closed = true;
            frames.clear();
        }
    }
}
