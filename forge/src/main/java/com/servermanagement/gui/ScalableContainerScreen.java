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

    /**
     * Fill the entire visible viewport with {@code color}, expressed in
     * design-space coordinates so the rectangle survives the scaled pose.
     * Use this for fullscreen overlays drawn from inside
     * {@link #renderContent} (e.g. animation dim layers) — passing
     * {@code (0, 0, width, height)} to {@link GuiGraphics#fill} would shrink
     * with the pose and clip incorrectly.
     */
    protected final void fillScreen(GuiGraphics g, int color) {
        float s = guiScale > 0 ? guiScale : 1f;
        float cxF = this.width * 0.5f;
        float cyF = this.height * 0.5f;
        int x0 = (int) Math.floor(cxF - cxF / s);
        int y0 = (int) Math.floor(cyF - cyF / s);
        int x1 = (int) Math.ceil(cxF + (this.width - cxF) / s);
        int y1 = (int) Math.ceil(cyF + (this.height - cyF) / s);
        g.fill(x0, y0, x1, y1, color);
    }

    @Override
    public final void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 1. Render ONLY the dimmed world overlay at full screen size.
        //    DO NOT call renderBackground() — AbstractContainerScreen overrides it
        //    to also invoke renderBg(), which would paint an unscaled "ghost" panel
        //    at design-space (leftPos, topPos) before our pose scale is applied.
        if (this.minecraft != null && this.minecraft.level == null) {
            this.renderPanorama(g, partialTick);
        }
        this.renderBlurredBackground(partialTick);
        this.renderMenuBackground(g);

        // 2. Suppress every further renderBackground call (the one inside
        //    Screen.render, plus any explicit calls inside subclasses'
        //    renderContent) so renderBg never paints unscaled.
        suppressBackgroundOnce = true;
        try {
            g.pose().pushPose();
            float cx = this.width * 0.5f;
            float cy = this.height * 0.5f;
            g.pose().translate(cx, cy, 0f);
            g.pose().scale(guiScale, guiScale, 1f);
            g.pose().translate(-cx, -cy, 0f);

            // 3. Paint a uniform translucent "frosted panel" backdrop behind
            //    every screen's content. The blurred world stays visible, but
            //    text rendered on top now has guaranteed contrast.
            drawFrostedPanel(g);

            int dmx = (int) inverseMouseX(mouseX);
            int dmy = (int) inverseMouseY(mouseY);
            renderContent(g, dmx, dmy, partialTick);

            g.pose().popPose();
        } finally {
            suppressBackgroundOnce = false;
        }
    }

    /**
     * Paint a soft translucent backdrop + 1px outline at the design-space
     * panel rect. Override to customize, or call manually with a different
     * rect from a subclass.
     */
    protected void drawFrostedPanel(GuiGraphics g) {
        int x0 = this.leftPos;
        int y0 = this.topPos;
        int x1 = x0 + designWidth;
        int y1 = y0 + designHeight;
        // Soft dark frosted fill — keeps the blur visible underneath.
        g.fill(x0, y0, x1, y1, 0x80101015);
        // Thin outline for panel definition.
        g.fill(x0, y0, x1, y0 + 1, 0x60FFFFFF);
        g.fill(x0, y1 - 1, x1, y1, 0x60FFFFFF);
        g.fill(x0, y0, x0 + 1, y1, 0x60FFFFFF);
        g.fill(x1 - 1, y0, x1, y1, 0x60FFFFFF);
    }

    /**
     * Helper for screens that include the player inventory. Paints a slightly
     * darker frosted strip + 1px outline + a hotbar separator + optional
     * "Inventory" label, framing the 9×{rows} grid + hotbar so players can
     * tell where the inventory begins.
     *
     * @param g       graphics
     * @param slotX   design-space X of the top-left inventory slot
     * @param slotY   design-space Y of the top-left inventory slot
     * @param rows    number of grid rows (typically 3)
     * @param hotbarGap pixel gap between the grid bottom and the hotbar top
     *                  (Vanilla = 4)
     */
    protected void drawInventoryPanel(GuiGraphics g, int slotX, int slotY,
                                      int rows, int hotbarGap, boolean drawLabel) {
        int cols = 9;
        int slotSize = 18;
        int padding = 6;
        int gridH = rows * slotSize;
        int hotbarH = slotSize;
        int innerW = cols * slotSize;
        int innerH = gridH + hotbarGap + hotbarH;

        int px0 = slotX - padding;
        int py0 = slotY - padding - (drawLabel ? 10 : 0);
        int px1 = slotX + innerW + padding;
        int py1 = slotY + innerH + padding;

        // Slightly darker frosted strip than the panel backdrop so the
        // inventory area visually separates from the content.
        g.fill(px0, py0, px1, py1, 0x90080810);
        // Outline.
        g.fill(px0, py0, px1, py0 + 1, 0x80FFFFFF);
        g.fill(px0, py1 - 1, px1, py1, 0x80FFFFFF);
        g.fill(px0, py0, px0 + 1, py1, 0x80FFFFFF);
        g.fill(px1 - 1, py0, px1, py1, 0x80FFFFFF);
        // Hotbar separator (thin line just above the hotbar row).
        int sepY = slotY + gridH + (hotbarGap / 2);
        g.fill(px0 + 2, sepY, px1 - 2, sepY + 1, 0x40FFFFFF);

        if (drawLabel) {
            g.drawString(this.font, "Inventory", slotX, slotY - 10, 0xFFE0E0E0, true);
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
