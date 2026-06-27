package org.esvoice.voice;

public enum VoiceChannel {
    PUBLIC(0, "", "hud.esvoice.channel.public"),
    SQUAD(1, "esvoice_squad", "hud.esvoice.channel.squad"),
    COMMAND(2, "esvoice_command", "hud.esvoice.channel.command");

    private final int id;
    private final String categoryId;
    private final String translationKey;

    VoiceChannel(int id, String categoryId, String translationKey) {
        this.id = id;
        this.categoryId = categoryId;
        this.translationKey = translationKey;
    }

    public int getId() {
        return id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public boolean isRadio() {
        return this != PUBLIC;
    }

    public static VoiceChannel fromId(int id) {
        for (VoiceChannel channel : values()) {
            if (channel.id == id) {
                return channel;
            }
        }
        return PUBLIC;
    }
}
