package org.esvoice.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import se.mickelus.mutil.gui.GuiElement;

final class EsvoiceMutilWidgets {

    static final int BACKDROP = 0x7A3A3A3A;
    static final int PANEL = 0x88363636;
    static final int PANEL_SOFT = 0x66363636;
    static final int BORDER = 0xA06D7482;
    static final int TEXT = 0xFFFFFFFF;
    static final int MUTED = 0xFFD4D8E0;
    static final int DIM = 0xFFAEB4BF;
    static final int GOLD = 0xFFFFC766;
    static final int SQUAD_BLUE = 0xFF67A7FF;
    static final int POSITIVE = 0xFF75D58A;
    static final int WARNING = 0xFFFFB44C;
    static final int NEGATIVE = 0xFFFF6666;

    private EsvoiceMutilWidgets() {
    }

    static Panel panel(int x, int y, int width, int height) {
        return new Panel(x, y, width, height, PANEL, BORDER);
    }

    static Panel softPanel(int x, int y, int width, int height) {
        return new Panel(x, y, width, height, PANEL_SOFT, BORDER);
    }

    static Text text(int x, int y, String value, int color) {
        return new Text(x, y, 0, value, color, false);
    }

    static Text centeredText(int x, int y, int width, String value, int color) {
        return new Text(x, y, width, value, color, true);
    }

    static ActionButton button(int x, int y, int width, int height, String label, Runnable action) {
        return new ActionButton(x, y, width, height, label, action);
    }

    static final class Panel extends GuiElement {
        private final int color;
        private final int borderColor;

        private Panel(int x, int y, int width, int height, int color, int borderColor) {
            super(x, y, width, height);
            this.color = color;
            this.borderColor = borderColor;
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y, int width, int height,
                         int mouseX, int mouseY, float partialTick) {
            if (!isVisible()) {
                return;
            }
            int bx = x + getX();
            int by = y + getY();
            graphics.fill(bx, by, bx + getWidth(), by + getHeight(), color);
            graphics.renderOutline(bx, by, getWidth(), getHeight(), borderColor);
            super.draw(graphics, x, y, width, height, mouseX, mouseY, partialTick);
        }
    }

    static final class Text extends GuiElement {
        private final String value;
        private final int color;
        private final boolean centered;

        private Text(int x, int y, int width, String value, int color, boolean centered) {
            super(x, y, width > 0 ? width : Minecraft.getInstance().font.width(value),
                    Minecraft.getInstance().font.lineHeight);
            this.value = value;
            this.color = color;
            this.centered = centered;
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y, int width, int height,
                         int mouseX, int mouseY, float partialTick) {
            if (!isVisible()) {
                return;
            }
            int tx = x + getX();
            if (centered) {
                tx += Math.max(0, (getWidth() - Minecraft.getInstance().font.width(value)) / 2);
            }
            graphics.drawString(Minecraft.getInstance().font, Component.literal(value), tx, y + getY(), color, false);
            super.draw(graphics, x, y, width, height, mouseX, mouseY, partialTick);
        }
    }

    static final class ActionButton extends GuiElement {
        private final String label;
        private final Runnable action;
        private boolean enabled = true;
        private boolean selected;
        private int textColor = TEXT;

        private ActionButton(int x, int y, int width, int height, String label, Runnable action) {
            super(x, y, width, height);
            this.label = label;
            this.action = action;
        }

        ActionButton enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        ActionButton selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        ActionButton textColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        @Override
        public boolean onMouseClick(int mouseX, int mouseY, int button) {
            if (button != 0 || !enabled || !isVisible() || !hasFocus()) {
                return false;
            }
            action.run();
            return true;
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y, int width, int height,
                         int mouseX, int mouseY, float partialTick) {
            if (!isVisible()) {
                return;
            }
            int bx = x + getX();
            int by = y + getY();
            int fill = !enabled ? 0x38404040 : selected ? 0x80564022 : hasFocus() ? 0x80585858 : 0x60404040;
            int outline = selected ? 0x90E8B85C : hasFocus() && enabled ? 0x60C2C8D5 : 0x706D7482;
            graphics.fill(bx, by, bx + getWidth(), by + getHeight(), fill);
            graphics.renderOutline(bx, by, getWidth(), getHeight(), outline);
            String drawn = Minecraft.getInstance().font.plainSubstrByWidth(label, Math.max(8, getWidth() - 8));
            int tw = Minecraft.getInstance().font.width(drawn);
            graphics.drawString(Minecraft.getInstance().font, Component.literal(drawn),
                    bx + Math.max(4, (getWidth() - tw) / 2),
                    by + Math.max(1, (getHeight() - Minecraft.getInstance().font.lineHeight) / 2),
                    enabled ? textColor : DIM, false);
            super.draw(graphics, x, y, width, height, mouseX, mouseY, partialTick);
        }
    }
}
