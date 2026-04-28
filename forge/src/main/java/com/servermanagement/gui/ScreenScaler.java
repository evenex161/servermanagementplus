package com.servermanagement.gui;

/**
 * Utility for computing responsive GUI dimensions that scale with the player's resolution.
 * <p>
 * Each panel has a preferred (designed) size. The scaler computes a single UNIFORM
 * scale factor that:
 * <ol>
 *   <li>Hard-fits the panel inside the available workspace (screen minus margin).</li>
 *   <li>Applies a mild aesthetic shrink when the workspace is between fit-scale and
 *       the reference size (1010x570 GUI units, roughly 1440p at GUI scale 2).</li>
 *   <li>Never grows above 1.0 (panels are not enlarged past their design size).</li>
 * </ol>
 * The same uniform factor is applied to BOTH width and height so the panel's aspect
 * ratio is always preserved. This is critical because slot positions and internal
 * widget layouts inside menus / screens use absolute design-space offsets that only
 * line up when both axes scale by the same amount.
 */
public final class ScreenScaler {

    /** GUI-unit width at which screens reach their full preferred size (~1440p scale 2). */
    private static final float REF_WIDTH = 1010.0f;
    /** GUI-unit height at which screens reach their full preferred size. */
    private static final float REF_HEIGHT = 570.0f;
    /** Soft aesthetic shrink floor (panels shrink a little at 1080p for visual balance). */
    private static final float AESTHETIC_FLOOR = 0.85f;
    /** Padding (in GUI units) reserved on each side so panels never touch the screen edge. */
    private static final int MARGIN = 20;

    private ScreenScaler() {}

    /**
     * Compute the uniform scale factor for a panel of the given preferred dimensions on
     * the current screen. The result is the most restrictive of:
     * <ul>
     *   <li>Hard fit ratio: largest scale at which the panel still fits inside the workspace.</li>
     *   <li>Aesthetic shrink: ~5% at 1080p, full size at 1440p+, floored at AESTHETIC_FLOOR.</li>
     * </ul>
     * Always capped at 1.0 (never enlarges).
     */
    public static float scaleFactor(int preferredWidth, int preferredHeight,
                                    int screenWidth, int screenHeight) {
        if (preferredWidth <= 0 || preferredHeight <= 0) {
            return 1.0f;
        }
        int availW = Math.max(MARGIN, screenWidth - MARGIN);
        int availH = Math.max(MARGIN, screenHeight - MARGIN);
        float fitW = (float) availW / preferredWidth;
        float fitH = (float) availH / preferredHeight;
        float fit = Math.min(fitW, fitH);
        float aesthetic = Math.min(1.0f, Math.min(screenWidth / REF_WIDTH, screenHeight / REF_HEIGHT));
        aesthetic = Math.max(aesthetic, AESTHETIC_FLOOR);
        return Math.min(1.0f, Math.min(fit, aesthetic));
    }

    /**
     * Legacy compatibility overload. Returns an aesthetic-only scale based on workspace
     * size; does not consider the panel's preferred dimensions, so it cannot guarantee
     * fit. Prefer {@link #scaleFactor(int, int, int, int)}.
     */
    public static float scaleFactor(int screenWidth, int screenHeight) {
        float aesthetic = Math.min(1.0f, Math.min(screenWidth / REF_WIDTH, screenHeight / REF_HEIGHT));
        return Math.max(aesthetic, AESTHETIC_FLOOR);
    }

    /**
     * Scale preferred panel dimensions uniformly to fit the current screen.
     * Both width and height shrink by the same factor, preserving aspect ratio.
     *
     * @return int array {scaledWidth, scaledHeight}
     */
    public static int[] scale(int preferredWidth, int preferredHeight,
                              int screenWidth, int screenHeight) {
        float s = scaleFactor(preferredWidth, preferredHeight, screenWidth, screenHeight);
        return new int[]{Math.round(preferredWidth * s), Math.round(preferredHeight * s)};
    }

    /**
     * Scale a single design-space dimension (such as a slot's Y offset within a menu)
     * by the same uniform factor that {@link #scale} applies to the surrounding panel.
     * <p>
     * Use this for menu slot positions that are written as absolute pixel offsets in
     * a design-space coordinate system: pass the panel's preferred size so the helper
     * computes the matching uniform scale.
     */
    public static int scale1D(int designOffset, int preferredWidth, int preferredHeight,
                              int screenWidth, int screenHeight) {
        float s = scaleFactor(preferredWidth, preferredHeight, screenWidth, screenHeight);
        return Math.round(designOffset * s);
    }
}
