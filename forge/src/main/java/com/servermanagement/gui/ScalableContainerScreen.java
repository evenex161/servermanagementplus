package com.servermanagement.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * AbstractContainerScreen base class that renders the entire panel through a
 * uniform pose-matrix scale, so every element (slots, buttons, custom text,
 * background panels, widgets) scales together when the GUI workspace is too
 * small for the panel's design size.
 * <p>
 * Subclasses MUST NOT override {@link #render(GuiGraphics, int, int, float)}
 * (it is final). They override {@link #renderContent(GuiGraphics, int, int, float)}
 * instead and write every coordinate in DESIGN SPACE — as if the panel were
 * always at its preferred size. {@code leftPos} and {@code topPos} point to the
 * design-space anchor of the panel; they may be negative when the design panel
 * is larger than the workspace.
 * <p>
 * Mouse coordinates received by widgets, slots, and {@code renderContent} are
 * also in design space. Real screen-pixel mouse coordinates are inverse-
 * transformed via the uniform scale factor so hover and click detection lines
 * up exactly with the visual panel.
 * <p>
 * The dimmed world background is rendered ONCE at full screen size before the
 * pose is pushed, so the dim overlay always covers the entire viewport.
 */
public abstract class ScalableContainerScreen<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {

    private final int designWidth;
    private final int designHeight;
    private float guiScale = 1.0f;
    private boolean suppressBackgroundOnce = false;

    protected ScalableContainerScreen(T menu, Inventory inv, Component title,
                                      int designWidth, int designHeight) {
        super(menu, inv, title);
        this.designWidth = designWidth;
        this.designHeight = designHeight;
        this.imageWidth = designWidth;
        this.imageHeight = designHeight;
    }

    /** Uniform visual-to-design scale factor (1.0 means no scaling). */
    public final float getGuiScale() {
        return guiScale;
    }

    public final int getDesignWidth() {
        return designWidth;
    }

    public final int getDesignHeight() {
        return designHeight;
    }

    @Override
    protected void init() {
        // Always work in design space; pose handles the visual fit.
        this.imageWidth = designWidth;
        this.imageHeight = designHeight;
        this.guiScale = ScreenScaler.scaleFactor(designWidth, designHeight,
                this.width, this.height);
        super.init();
        // super.init() centers leftPos/topPos for the design size, which is
        // exactly what we want — they become the design-space anchor.
    }

    /** Inverse-transform a real X mouse coord into the screen's design space. */
    protected final double inverseMouseX(double mx) {
        double cx = this.width * 0.5;
        return cx + (mx - cx) / guiScale;
    }

    /** Inverse-transform a real Y mouse coord into the screen's design space. */
    protected final double inverseMouseY(double my) {
        double cy = this.height * 0.5;
        return cy + (my - cy) / guiScale;
    }

    @Override
    public final void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 1. Render the dimmed world background ONCE at full screen size.
        renderBackground(g, mouseX, mouseY, partialTick);

        // 2. Suppress the duplicate renderBackground call inside super.render
        //    while the pose is scaled (would shrink the dim overlay).
        suppressBackgroundOnce = true;
        try {
            g.pose().pushPose();
            float cx = this.width * 0.5f;
            float cy = this.height * 0.5f;
            g.pose().translate(cx, cy, 0f);
            g.pose().scale(guiScale, guiScale, 1f);
            g.pose().translate(-cx, -cy, 0f);

            int dmx = (int) inverseMouseX(mouseX);
            int dmy = (int) inverseMouseY(mouseY);
            renderContent(g, dmx, dmy, partialTick);

            g.pose().popPose();
        } finally {
            suppressBackgroundOnce = false;
        }
    }

    /**
     * Render the panel content. The pose matrix is already scaled — use
     * design-space coordinates for everything (text, widgets, custom panels).
     * <p>
     * Default implementation simply calls {@code super.render(...)} (i.e.
     * AbstractContainerScreen.render) which paints the background panel,
     * slots, items, widgets, and tooltip. Subclasses overriding this should
     * typically call {@code super.renderContent(...)} first, then draw any
     * custom design-space decoration on top.
     */
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (suppressBackgroundOnce) {
            return;
        }
        super.renderBackground(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(inverseMouseX(mouseX), inverseMouseY(mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(inverseMouseX(mouseX), inverseMouseY(mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        return super.mouseDragged(inverseMouseX(mouseX), inverseMouseY(mouseY),
                button, dragX / guiScale, dragY / guiScale);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(inverseMouseX(mouseX), inverseMouseY(mouseY),
                scrollX, scrollY);
    }
}
