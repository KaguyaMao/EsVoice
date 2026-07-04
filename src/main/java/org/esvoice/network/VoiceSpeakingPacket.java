package org.esvoice.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.esvoice.voice.VoiceChannel;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.UUID;
import java.util.function.Supplier;

public record VoiceSpeakingPacket(UUID speakerId, String speakerName, VoiceChannel channel) {

    private static final int NAME_PRESENT_FLAG = 0x80;
    private static final int CHANNEL_MASK = 0x7F;

    public static void encode(VoiceSpeakingPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.speakerId);
        boolean hasName = packet.speakerName != null;
        buffer.writeByte(packet.channel.getId() | (hasName ? NAME_PRESENT_FLAG : 0));
        if (hasName) {
            buffer.writeUtf(packet.speakerName, 64);
        }
    }

    public static VoiceSpeakingPacket decode(FriendlyByteBuf buffer) {
        UUID speakerId = buffer.readUUID();
        int header = buffer.readUnsignedByte();
        String speakerName = (header & NAME_PRESENT_FLAG) != 0 ? buffer.readUtf(64) : null;
        return new VoiceSpeakingPacket(speakerId, speakerName,
                VoiceChannel.fromId(header & CHANNEL_MASK));
    }

    public static void handle(VoiceSpeakingPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientHandler.handle(packet));
        context.setPacketHandled(true);
    }

    /** Loaded lazily only when a client-bound packet is handled. */
    private static final class ClientHandler {
        private static final MethodHandle ON_SPEAKING = findHandler();

        private static MethodHandle findHandler() {
            try {
                return MethodHandles.publicLookup().findStatic(
                        Class.forName("org.esvoice.client.VoiceSpeakerHud"),
                        "onSpeaking",
                        MethodType.methodType(void.class, UUID.class, String.class, VoiceChannel.class));
            } catch (ReflectiveOperationException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }

        private static void handle(VoiceSpeakingPacket packet) {
            try {
                ON_SPEAKING.invokeExact(packet.speakerId, packet.speakerName, packet.channel);
            } catch (Throwable throwable) {
                throw new IllegalStateException("Failed to update the Esvoice speaker HUD", throwable);
            }
        }
    }
}
