package org.esvoice.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import se.mickelus.mutil.gui.GuiElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * Small retained-mode MUtil widget kit used by every Esvoice screen.
 *
 * Values are read from suppliers while drawing. This keeps the element tree
 * stable while connection state, key bindings and settings change.
 */
final class EsvoiceMutilWidgets {

    static final int BACKDROP = 0xE0121517;
    static final int PANEL = 0xF0191C1E;
    static final int PANEL_SOFT = 0xE0212527;
    static final int BORDER = 0xC05B6260;
    static final int TEXT = 0xFFFFFFFF;
    static final int MUTED = 0xFFD4D8E0;
    static final int DIM = 0xFFAEB4BF;
    static final int GOLD = 0xFFFFC766;
    static final int SQUAD_BLUE = 0xFF67A7FF;
    static final int POSITIVE = 0xFF75D58A;
    static final int WARNING = 0xFFFFB44C;
    static final int NEGATIVE = 0xFFFF6666;

    private static KeybindButton capturingKey;

    private EsvoiceMutilWidgets() {
    }

    static Panel panel(int x, int y, int width, int height) {
        return new Panel(x, y, width, height, PANEL, BORDER);
    }

    static Panel softPanel(int x, int y, int width, int height) {
        return new Panel(x, y, width, height, PANEL_SOFT, BORDER);
    }

    static DynamicText text(int x, int y, String value, int color) {
        return text(x, y, Component.literal(value), color);
    }

    static DynamicText text(int x, int y, Component value, int color) {
        return new DynamicText(x, y, 0, () -> value, () -> color, false);
    }

    static DynamicText dynamicText(int x, int y, int width, Supplier<Component> value,
                                   Supplier<Integer> color, boolean centered) {
        return new DynamicText(x, y, width, value, color, centered);
    }

    static DynamicText centeredText(int x, int y, int width, String value, int color) {
        return centeredText(x, y, width, Component.literal(value), color);
    }

    static DynamicText centeredText(int x, int y, int width, Component value, int color) {
        return new DynamicText(x, y, width, () -> value, () -> color, true);
    }

    static ActionButton button(int x, int y, int width, int height, String label, Runnable action) {
        return button(x, y, width, height, () -> Component.literal(label), action);
    }

    static ActionButton button(int x, int y, int width, int height, Component label, Runnable action) {
        return button(x, y, width, height, () -> label, action);
    }

    static ActionButton button(int x, int y, int width, int height,
                               Supplier<Component> label, Runnable action) {
        return new ActionButton(x, y, width, height, label, action);
    }

    static ToggleButton toggle(int x, int y, int width, int height,
                               Supplier<Component> label, BooleanSupplier value, Runnable action) {
        return new ToggleButton(x, y, width, height, label, value, action);
    }

    static Slider slider(int x, int y, int width, int height, double min, double max,
                         DoubleSupplier value, DoubleConsumer save, Supplier<Component> label) {
        return new Slider(x, y, width, height, min, max, value, save, label);
    }

    static VerticalScroll scroll(int x, int y, int width, int height, int contentHeight) {
        return new VerticalScroll(x, y, width, height, contentHeight);
    }

    static TextField textField(int x, int y, int width, int height, Consumer<String> changed) {
        return new TextField(x, y, width, height, "", Component.translatable("gui.esvoice.search"), changed);
    }

    static TextField textField(int x, int y, int width, int height, String initial,
                               Consumer<String> changed) {
        return new TextField(x, y, width, height, initial,
                Component.translatable("gui.esvoice.search"), changed);
    }

    static TextField textField(int x, int y, int width, int height, String initial,
                               Component placeholder, Consumer<String> changed) {
        return new TextField(x, y, width, height, initial, placeholder, changed);
    }

    static KeybindButton keybind(int x, int y, int width, int height, KeyMapping mapping) {
        return new KeybindButton(x, y, width, height, mapping);
    }

    static boolean captureKey(int keyCode, int scanCode) {
        KeybindButton active = capturingKey;
        if (active == null) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            active.stopCapturing();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            active.assign(InputConstants.UNKNOWN);
            return true;
        }
        active.assign(InputConstants.getKey(keyCode, scanCode));
        return true;
    }

    static boolean captureMouseButton(int button) {
        KeybindButton active = capturingKey;
        if (active == null) {
            return false;
        }
        active.assign(InputConstants.Type.MOUSE.getOrCreate(button));
        return true;
    }

    static void cancelKeyCapture() {
        if (capturingKey != null) {
            capturingKey.capturing = false;
            capturingKey = null;
        }
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

    static class DynamicText extends GuiElement {
        private final Supplier<Component> value;
        private final Supplier<Integer> color;
        private final boolean centered;

        private DynamicText(int x, int y, int width, Supplier<Component> value,
                            Supplier<Integer> color, boolean centered) {
            super(x, y, Math.max(1, width), Minecraft.getInstance().font.lineHeight);
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
            Component current = Objects.requireNonNullElseGet(value.get(), Component::empty);
            int tx = x + getX();
            if (centered) {
                tx += Math.max(0, (getWidth() - Minecraft.getInstance().font.width(current)) / 2);
            }
            graphics.drawString(Minecraft.getInstance().font, current, tx, y + getY(), color.get(), false);
            super.draw(graphics, x, y, width, height, mouseX, mouseY, partialTick);
        }
    }

    static class ActionButton extends GuiElement {
        private final Supplier<Component> label;
        private final Runnable action;
        protected BooleanSupplier enabled = () -> true;
        protected BooleanSupplier selected = () -> false;
        protected Supplier<Integer> textColor = () -> TEXT;
        private Supplier<List<Component>> tooltip = List::of;

        private ActionButton(int x, int y, int width, int height,
                             Supplier<Component> label, Runnable action) {
            super(x, y, width, height);
            this.label = label;
            this.action = action;
        }

        ActionButton enabled(boolean enabled) {
            this.enabled = () -> enabled;
            return this;
        }

        ActionButton enabled(BooleanSupplier enabled) {
            this.enabled = enabled;
            return this;
        }

        ActionButton selected(boolean selected) {
            this.selected = () -> selected;
            return this;
        }

        ActionButton selected(BooleanSupplier selected) {
            this.selected = selected;
            return this;
        }

        ActionButton textColor(int textColor) {
            this.textColor = () -> textColor;
            return this;
        }

        ActionButton textColor(Supplier<Integer> textColor) {
            this.textColor = textColor;
            return this;
        }

        ActionButton tooltip(Supplier<List<Component>> tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        @Override
        public boolean onMouseClick(int mouseX, int mouseY, int button) {
            if (button != 0 || !enabled.getAsBoolean() || !isVisible() || !hasFocus()) {
                return false;
            }
            action.run();
            return true;
        }

        @Override
        public List<Component> getTooltipLines() {
            if (hasFocus()) {
                List<Component> lines = tooltip.get();
                if (lines != null && !lines.isEmpty()) {
                    return lines;
                }
            }
            return super.getTooltipLines();
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y, int width, int height,
                         int mouseX, int mouseY, float partialTick) {
            if (!isVisible()) {
                return;
            }
            boolean isEnabled = enabled.getAsBoolean();
            boolean isSelected = selected.getAsBoolean();
            int bx = x + getX();
            int by = y + getY();
            int fill = !isEnabled ? 0xB0181B1D : isSelected ? 0xE04A4329
                    : hasFocus() ? 0xE0435145 : 0xD01B1E20;
            int outline = isSelected ? 0xFFE8B85C : hasFocus() && isEnabled ? 0xFFC2C8D5 : 0xA06D7482;
            graphics.fill(bx, by, bx + getWidth(), by + getHeight(), fill);
            graphics.renderOutline(bx, by, getWidth(), getHeight(), outline);
            Component current = Objects.requireNonNullElseGet(label.get(), Component::empty);
            String drawn = Minecraft.getInstance().font.plainSubstrByWidth(
                    current.getString(), Math.max(8, getWidth() - 8));
            int tw = Minecraft.getInstance().font.width(drawn);
            graphics.drawString(Minecraft.getInstance().font, Component.literal(drawn),
                    bx + Math.max(4, (getWidth() - tw) / 2),
                    by + Math.max(1, (getHeight() - Minecraft.getInstance().font.lineHeight) / 2),
                    isEnabled ? textColor.get() : DIM, false);
            super.draw(graphics, x, y, width, height, mouseX, mouseY, partialTick);
        }
    }

    static final class ToggleButton extends ActionButton {
        private ToggleButton(int x, int y, int width, int height, Supplier<Component> label,
                             BooleanSupplier value, Runnable action) {
            super(x, y, width, height, label, action);
            selected(value);
            textColor(() -> value.getAsBoolean() ? POSITIVE : DIM);
        }
    }

    static final class Slider extends GuiElement {
        private final double min;
        private final double max;
        private final DoubleSupplier source;
        private final DoubleConsumer save;
        private final Supplier<Component> label;
        private BooleanSupplier enabled = () -> true;
        private boolean dragging;
        private double localValue;
        private int absoluteX;

        private Slider(int x, int y, int width, int height, double min, double max,
                       DoubleSupplier source, DoubleConsumer save, Supplier<Component> label) {
            super(x, y, width, height);
            this.min = min;
            this.max = max;
            this.source = source;
            this.save = save;
            this.label = label;
        }

        Slider enabled(BooleanSupplier enabled) {
            this.enabled = enabled;
            return this;
        }

        @Override
        public boolean onMouseClick(int mouseX, int mouseY, int button) {
            if (button != 0 || !enabled.getAsBoolean() || !hasFocus() || !isVisible()) {
                return false;
            }
            dragging = true;
            localValue = valueAt(mouseX);
            return true;
        }

        @Override
        public void onMouseRelease(int mouseX, int mouseY, int button) {
            if (button == 0 && dragging) {
                localValue = valueAt(mouseX);
                save.accept(localValue);
                dragging = false;
            }
            super.onMouseRelease(mouseX, mouseY, button);
        }

        private double valueAt(int mouseX) {
            double fraction = (mouseX - absoluteX - 3D) / Math.max(1D, getWidth() - 6D);
            return min + Math.max(0D, Math.min(1D, fraction)) * (max - min);
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y, int width, int height,
                         int mouseX, int mouseY, float partialTick) {
            if (!isVisible()) {
                return;
            }
            absoluteX = x + getX();
            int by = y + getY();
            if (dragging) {
                localValue = valueAt(mouseX);
            }
            double current = dragging ? localValue : source.getAsDouble();
            double fraction = (current - min) / (max - min);
            fraction = Math.max(0D, Math.min(1D, fraction));
            int fillWidth = (int) Math.round((getWidth() - 2D) * fraction);
            graphics.fill(absoluteX, by, absoluteX + getWidth(), by + getHeight(), 0xD0181B1D);
            graphics.fill(absoluteX + 1, by + 1, absoluteX + 1 + fillWidth, by + getHeight() - 1,
                    !enabled.getAsBoolean() ? 0xA03A3D3C
                            : hasFocus() || dragging ? 0xE0678A73 : 0xD04D6757);
            graphics.renderOutline(absoluteX, by, getWidth(), getHeight(),
                    enabled.getAsBoolean() && (hasFocus() || dragging) ? 0xFFC2C8D5 : 0xA06D7482);
            Component currentLabel = label.get();
            String drawn = Minecraft.getInstance().font.plainSubstrByWidth(
                    currentLabel.getString(), Math.max(8, getWidth() - 8));
            graphics.drawCenteredString(Minecraft.getInstance().font, drawn,
                    absoluteX + getWidth() / 2,
                    by + Math.max(1, (getHeight() - Minecraft.getInstance().font.lineHeight) / 2),
                    enabled.getAsBoolean() ? TEXT : DIM);
            super.draw(graphics, x, y, width, height, mouseX, mouseY, partialTick);
        }
    }

    static final class VerticalScroll extends GuiElement {
        private int contentHeight;
        private int offset;

        private VerticalScroll(int x, int y, int width, int height, int contentHeight) {
            super(x, y, width, height);
            this.contentHeight = Math.max(height, contentHeight);
        }

        void setContentHeight(int contentHeight) {
            this.contentHeight = Math.max(getHeight(), contentHeight);
            offset = Math.min(offset, maxOffset());
        }

        private int maxOffset() {
            return Math.max(0, contentHeight - getHeight());
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y, int width, int height,
                         int mouseX, int mouseY, float partialTick) {
            if (!isVisible()) {
                return;
            }
            int bx = x + getX();
            int by = y + getY();
            graphics.enableScissor(bx, by, bx + getWidth(), by + getHeight());
            super.drawChildren(graphics, bx, by - offset, getWidth(), contentHeight,
                    mouseX, mouseY, partialTick);
            graphics.disableScissor();
            if (maxOffset() > 0) {
                int handleHeight = Math.max(14, getHeight() * getHeight() / contentHeight);
                int travel = getHeight() - handleHeight;
                int handleY = by + (int) Math.round(travel * (offset / (double) maxOffset()));
                graphics.fill(bx + getWidth() - 3, by, bx + getWidth(), by + getHeight(), 0x90111415);
                graphics.fill(bx + getWidth() - 3, handleY, bx + getWidth(), handleY + handleHeight,
                        hasFocus() ? 0xFFD0A85C : 0xFF68706E);
            }
        }

        @Override
        public void updateFocusState(int parentX, int parentY, int mouseX, int mouseY) {
            int bx = parentX + getX();
            int by = parentY + getY();
            boolean inside = isVisible() && mouseX >= bx && mouseX < bx + getWidth()
                    && mouseY >= by && mouseY < by + getHeight();
            hasFocus = inside;
            for (GuiElement child : elements) {
                if (inside) {
                    child.updateFocusState(bx, by - offset, mouseX, mouseY);
                } else {
                    child.updateFocusState(-100000, -100000, mouseX, mouseY);
                }
            }
        }

        @Override
        public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
            if (super.onMouseScroll(mouseX, mouseY, amount)) {
                return true;
            }
            if (!hasFocus() || maxOffset() <= 0) {
                return false;
            }
            offset = Math.max(0, Math.min(maxOffset(), offset - (int) Math.round(amount * 18D)));
            return true;
        }
    }

    static final class TextField extends GuiElement {
        private final Consumer<String> changed;
        private final Component placeholder;
        private final StringBuilder value = new StringBuilder();
        private boolean editing;

        private TextField(int x, int y, int width, int height, String initial,
                          Component placeholder, Consumer<String> changed) {
            super(x, y, width, height);
            this.changed = changed;
            this.placeholder = placeholder;
            this.value.append(initial == null ? "" : initial);
        }

        @Override
        public boolean onMouseClick(int mouseX, int mouseY, int button) {
            if (button != 0) {
                return false;
            }
            editing = hasFocus();
            return editing;
        }

        @Override
        public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
            if (!editing) {
                return false;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                editing = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !value.isEmpty()) {
                value.deleteCharAt(value.length() - 1);
                changed.accept(value.toString());
                return true;
            }
            return false;
        }

        @Override
        public boolean onCharType(char character, int modifiers) {
            if (!editing || Character.isISOControl(character) || value.length() >= 64) {
                return false;
            }
            value.append(character);
            changed.accept(value.toString());
            return true;
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y, int width, int height,
                         int mouseX, int mouseY, float partialTick) {
            int bx = x + getX();
            int by = y + getY();
            graphics.fill(bx, by, bx + getWidth(), by + getHeight(), 0xE0141719);
            graphics.renderOutline(bx, by, getWidth(), getHeight(),
                    editing ? 0xFFFFC766 : hasFocus() ? 0xFFC2C8D5 : 0xA06D7482);
            Component shown = value.isEmpty() && !editing
                    ? placeholder
                    : Component.literal(value + (editing && System.currentTimeMillis() / 500L % 2L == 0L ? "_" : ""));
            graphics.drawString(Minecraft.getInstance().font, shown, bx + 5,
                    by + Math.max(1, (getHeight() - Minecraft.getInstance().font.lineHeight) / 2),
                    value.isEmpty() ? DIM : TEXT, false);
        }
    }

    static final class KeybindButton extends GuiElement {
        private final KeyMapping mapping;
        private boolean capturing;

        private KeybindButton(int x, int y, int width, int height, KeyMapping mapping) {
            super(x, y, width, height);
            this.mapping = mapping;
        }

        @Override
        public boolean onMouseClick(int mouseX, int mouseY, int button) {
            if (button != 0 || !hasFocus()) {
                return false;
            }
            cancelKeyCapture();
            capturing = true;
            capturingKey = this;
            return true;
        }

        private void stopCapturing() {
            capturing = false;
            if (capturingKey == this) {
                capturingKey = null;
            }
        }

        private void assign(InputConstants.Key key) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.options.setKey(mapping, key);
            KeyMapping.resetMapping();
            minecraft.options.save();
            stopCapturing();
        }

        private boolean conflicts() {
            if (mapping.isUnbound()) {
                return false;
            }
            for (KeyMapping other : EsvoiceClientModEvents.ALL_KEYS) {
                if (other != mapping && mapping.same(other)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<Component> getTooltipLines() {
            if (!hasFocus()) {
                return super.getTooltipLines();
            }
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.esvoice.keybind.assign"));
            lines.add(Component.translatable("gui.esvoice.keybind.clear"));
            if (conflicts()) {
                lines.add(Component.translatable("gui.esvoice.keybind.conflict"));
            }
            return lines;
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y, int width, int height,
                         int mouseX, int mouseY, float partialTick) {
            int bx = x + getX();
            int by = y + getY();
            int outline = capturing ? GOLD : conflicts() ? NEGATIVE : hasFocus() ? 0xFFC2C8D5 : 0xA06D7482;
            graphics.fill(bx, by, bx + getWidth(), by + getHeight(), 0xE0181B1D);
            graphics.renderOutline(bx, by, getWidth(), getHeight(), outline);
            Component label = capturing
                    ? Component.translatable("gui.esvoice.keybind.listening")
                    : mapping.getTranslatedKeyMessage();
            graphics.drawCenteredString(Minecraft.getInstance().font, label,
                    bx + getWidth() / 2,
                    by + Math.max(1, (getHeight() - Minecraft.getInstance().font.lineHeight) / 2),
                    conflicts() ? NEGATIVE : TEXT);
        }
    }
}
