package org.esvoice.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.esvoice.voice.VoiceChannel;
import org.esvoice.voice.VoiceChannelRouter;

import java.util.function.Supplier;

public record VoiceModePacket(VoiceChannel channel) {

    public static void encode(VoiceModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.channel.getId());
    }

    public static VoiceModePacket decode(FriendlyByteBuf buffer) {
        return new VoiceModePacket(VoiceChannel.fromId(buffer.readUnsignedByte()));
    }

    public static void handle(VoiceModePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> {
                VoiceChannelRouter.setRequestedChannel(sender, packet.channel);
                EsvoiceNetwork.acknowledgeMode(sender, new VoiceModeAckPacket(packet.channel));
            });
        }
        context.setPacketHandled(true);
    }
}
