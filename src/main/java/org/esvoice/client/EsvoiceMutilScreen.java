package org.esvoice.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import se.mickelus.mutil.gui.GuiElement;

abstract class EsvoiceMutilScreen extends Screen {

    protected GuiElement root;

    protected EsvoiceMutilScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        rebuildMutilRoot();
    }

    protected final void rebuildMutilRoot() {
        GuiElement newRoot = new GuiElement(0, 0, width, height);
        buildMutilRoot(newRoot);
        root = newRoot;
    }

    protected abstract void buildMutilRoot(GuiElement root);

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, EsvoiceMutilWidgets.BACKDROP);
        if (root != null) {
            root.updateFocusState(0, 0, mouseX, mouseY);
            root.draw(graphics, 0, 0, width, height, mouseX, mouseY, partialTick);
            var tooltip = root.getTooltipLines();
            if (tooltip != null && !tooltip.isEmpty()) {
                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (EsvoiceMutilWidgets.captureMouseButton(button)) {
            return true;
        }
        if (root != null) {
            root.updateFocusState(0, 0, (int) mouseX, (int) mouseY);
            if (root.onMouseClick((int) mouseX, (int) mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (root != null) {
            root.onMouseRelease((int) mouseX, (int) mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (EsvoiceMutilWidgets.captureKey(keyCode, scanCode)) {
            return true;
        }
        if (root != null && root.onKeyPress(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (root != null && root.onKeyRelease(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (root != null && root.onCharType(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (root != null) {
            root.updateFocusState(0, 0, (int) mouseX, (int) mouseY);
            if (root.onMouseScroll(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        EsvoiceMutilWidgets.cancelKeyCapture();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
