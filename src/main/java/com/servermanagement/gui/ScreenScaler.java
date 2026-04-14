package com.servermanagement.gui;

/**
 * Utility for computing responsive GUI dimensions that scale with the player's resolution.
 * <p>
 * Screens display at their preferred (designed) size when the GUI workspace is at least
 * as large as the reference dimensions (roughly 1440p at GUI scale 2). On smaller
 * resolutions (e.g. 1080p at GUI scale 2) screens shrink by ~5% so they feel less
 * dominant without breaking text layouts. A minimum floor prevents extreme shrinking.
 */
public final class ScreenScaler {

    /** GUI-unit width at which screens reach their full preferred size (~1440p scale 2). */
    private static final float REF_WIDTH = 1010.0f;
    /** GUI-unit height at which screens reach their full preferred size. */
    private static final float REF_HEIGHT = 570.0f;
    /** Minimum scale factor to prevent text overlap on very small windows. */
    private static final float MIN_SCALE = 0.85f;

    private ScreenScaler() {}

    /**
     * Compute a uniform scale factor for the current screen dimensions.
     * Returns 1.0 at 1440p+, ~0.95 at 1080p, never below MIN_SCALE.
     */
    public static float scaleFactor(int screenWidth, int screenHeight) {
        float raw = Math.min(1.0f, Math.min(screenWidth / REF_WIDTH, screenHeight / REF_HEIGHT));
        return Math.max(raw, MIN_SCALE);
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
