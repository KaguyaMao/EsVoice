package org.esvoice.client;

import cc.sighs.auratip.api.client.TipClientApi;
import cc.sighs.auratip.api.tip.TipBuilder;
import cc.sighs.auratip.client.render.TipOverlay;
import cc.sighs.auratip.data.TipData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Low-frequency AuraTip status notifications for voice connectivity.
 */
public final class EsvoiceAuraTipNotifications {

    private static Boolean lastVoiceConnected;
    private static long sequence;

    private EsvoiceAuraTipNotifications() {
    }

    public static void update(boolean inGame, boolean voiceConnected) {
        if (!inGame) {
            lastVoiceConnected = null;
            return;
        }
        if (lastVoiceConnected == null) {
            lastVoiceConnected = voiceConnected;
            return;
        }
        if (lastVoiceConnected == voiceConnected) {
            return;
        }
        lastVoiceConnected = voiceConnected;
        show(
            Component.translatable(voiceConnected
                ? "notification.esvoice.connected"
                : "notification.esvoice.disconnected"),
            voiceConnected ? "#FF72A77B" : "#FFB65D54");
    }

    private static void show(Component message, String accent) {
        if (TipOverlay.INSTANCE.isActive()) {
            return;
        }
        TipData tip = new TipBuilder(ResourceLocation.fromNamespaceAndPath(
            "esvoice", "connection_" + ++sequence))
            .visual(visual -> visual
                .size(220, 54)
                .positionPreset("RIGHT_CENTER")
                .animationStyle(ResourceLocation.fromNamespaceAndPath(
                    "auratip", "slide_in_right"))
                .animationSpeed(1.4f)
                .themeColor(accent)
                .stripeWidth(3)
                .background(TipData.VisualSettings.BackgroundType.SOLID,
                    List.of("#EE15191B"), 0)
                .backgroundRounded(false))
            .behavior(behavior -> behavior
                .duration(50)
                .pauseOnHover(true)
                .allowPaging(false))
            .page(0, page -> page.content(message))
            .build();
        TipClientApi.enqueue(List.of(tip), Map.of());
    }
}
