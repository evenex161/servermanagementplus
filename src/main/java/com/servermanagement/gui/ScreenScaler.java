package com.servermanagement.gui;

/**
 * Utility for computing responsive GUI dimensions that scale with the player's resolution.
 * <p>
 * Screens display at their preferred (designed) size when the GUI workspace is at least
 * as large as the reference dimensions (roughly 1440p at GUI scale 2). On smaller
 * resolutions (e.g. 1080p) screens shrink proportionally so they don't dominate the view.
 */
public final class ScreenScaler {

    /** GUI-unit width at which screens reach their full preferred size. */
    private static final float REF_WIDTH = 1250.0f;
    /** GUI-unit height at which screens reach their full preferred size. */
    private static final float REF_HEIGHT = 700.0f;

    private ScreenScaler() {}

    /**
     * Compute a uniform scale factor for the current screen dimensions.
     * Returns 1.0 at 1440p+, and proportionally less on smaller screens.
     */
    public static float scaleFactor(int screenWidth, int screenHeight) {
        return Math.min(1.0f, Math.min(screenWidth / REF_WIDTH, screenHeight / REF_HEIGHT));
    }

    /**
     * Scale preferred dimensions to fit the current screen, clamped to avoid overflow.
     *
     * @return int array {scaledWidth, scaledHeight}
     */
    public static int[] scale(int preferredWidth, int preferredHeight,
                              int screenWidth, int screenHeight) {
        float s = scaleFactor(screenWidth, screenHeight);
        int w = Math.min((int) (preferredWidth * s), screenWidth - 20);
        int h = Math.min((int) (preferredHeight * s), screenHeight - 20);
        return new int[]{w, h};
    }
}
