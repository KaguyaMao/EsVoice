package org.esvoice.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.esvoice.voice.VoiceChannel;

import java.util.UUID;
import java.util.function.Supplier;

public record VoiceSpeakingPacket(UUID speakerId, String speakerName, VoiceChannel channel) {

    public static void encode(VoiceSpeakingPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.speakerId);
        buffer.writeUtf(packet.speakerName, 64);
        buffer.writeByte(packet.channel.getId());
    }

    public static VoiceSpeakingPacket decode(FriendlyByteBuf buffer) {
        return new VoiceSpeakingPacket(buffer.readUUID(), buffer.readUtf(64),
                VoiceChannel.fromId(buffer.readUnsignedByte()));
    }

    public static void handle(VoiceSpeakingPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            try {
                Class.forName("org.esvoice.client.VoiceSpeakerHud")
                        .getMethod("onSpeaking", UUID.class, String.class, VoiceChannel.class)
                        .invoke(null, packet.speakerId, packet.speakerName, packet.channel);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to update the Esvoice speaker HUD", exception);
            }
        });
        context.setPacketHandled(true);
    }
}
