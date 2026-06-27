package org.esvoice.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.esvoice.voice.VoiceChannel;

import java.util.function.Supplier;

public record VoiceModeAckPacket(VoiceChannel channel) {

    public static void encode(VoiceModeAckPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.channel.getId());
    }

    public static VoiceModeAckPacket decode(FriendlyByteBuf buffer) {
        return new VoiceModeAckPacket(VoiceChannel.fromId(buffer.readUnsignedByte()));
    }

    public static void handle(VoiceModeAckPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            try {
                Class.forName("org.esvoice.client.RadioTransmitController")
                        .getMethod("confirm", VoiceChannel.class)
                        .invoke(null, packet.channel);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to confirm the Esvoice transmit channel", exception);
            }
        });
        context.setPacketHandled(true);
    }
}
