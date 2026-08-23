package org.esvoice.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.esvoice.Esvoice;

import java.util.Optional;

public final class EsvoiceNetwork {

    // Version 4 drops the DIRECT channel and the VoiceModePacket target-squad field.
    private static final String PROTOCOL = "4";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Esvoice.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int packetId;

    private EsvoiceNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(packetId++, VoiceModePacket.class,
                VoiceModePacket::encode, VoiceModePacket::decode, VoiceModePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(packetId++, VoiceModeAckPacket.class,
                VoiceModeAckPacket::encode, VoiceModeAckPacket::decode, VoiceModeAckPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(packetId++, VoiceSpeakingPacket.class,
                VoiceSpeakingPacket::encode, VoiceSpeakingPacket::decode, VoiceSpeakingPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendMode(VoiceModePacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendSpeaking(ServerPlayer player, VoiceSpeakingPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void acknowledgeMode(ServerPlayer player, VoiceModeAckPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
