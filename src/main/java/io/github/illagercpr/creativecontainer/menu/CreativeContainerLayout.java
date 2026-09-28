package io.github.illagercpr.creativecontainer.menu;

/**
 * Single source of truth for the GUI geometry.
 *
 * <p>It lives outside the client screen so that the headless GameTests can assert that the shipped textures match the
 * layout the screen draws (a mismatch is invisible at compile time and only shows up as a broken GUI in game).
 *
 * <p>v0.1.1 arrangement: the pool sits in the upper half of the right page and the player inventory in the lower half
 * (the two swapped places), both panels are horizontally centred on the right page so they no longer hug (or overlap)
 * the page's left edge, and the hotbar keeps sharing the browser's bottom edge (y = 202).
 */
public final class CreativeContainerLayout {

    /** Size of {@code textures/gui/creative_container.png}. */
    public static final int IMAGE_WIDTH = 396;
    public static final int IMAGE_HEIGHT = 222;

    /** Size of {@code textures/gui/slot.png} and of one item cell. */
    public static final int CELL = 18;

    /**
     * Left edge of both right-page panels. The right page spans x 200..388 (189px of content between the page
     * borders); a 9-column panel is 162px wide, so centre means (189-162)/2 = 13px of breathing room on each side.
     */
    public static final int PANEL_LEFT = 214;

    // left panel: creative item browser. The grid ends at y = 40 + 9*18 = 202, exactly flush with the hotbar
    // (184 + 18 = 202), so both columns share one bottom edge.
    public static final int BROWSER_LEFT = 16;
    public static final int BROWSER_TOP = 40;
    public static final int BROWSER_COLUMNS = 9;
    public static final int BROWSER_ROWS = 9;

    // right panel, upper half: the pool overview. Header and page share y = 28, the amount field y = 42, the grid
    // y = 56..110.
    public static final int POOL_HEADER_Y = 28;
    public static final int AMOUNT_TOP = 42;
    public static final int AMOUNT_WIDTH = 96;
    public static final int POOL_LEFT = PANEL_LEFT;
    public static final int POOL_TOP = 56;
    public static final int POOL_COLUMNS = 9;
    public static final int POOL_ROWS = 3;

    // right panel, lower half: the player inventory. Slot positions are decided by CreativeContainerMenu, because
    // Slot.x/y are final: three rows at y = 126..180, the hotbar at y = 184..202.
    public static final int PLAYER_PANEL_LEFT = PANEL_LEFT;
    public static final int PLAYER_PANEL_TOP = 126;

    private CreativeContainerLayout() {
    }

    public static int browserPageSize() {
        return BROWSER_COLUMNS * BROWSER_ROWS;
    }

    public static int poolPageSize() {
        return POOL_COLUMNS * POOL_ROWS;
    }

    public static int poolPageCount(int slotCount) {
        return Math.max(1, (slotCount + poolPageSize() - 1) / poolPageSize());
    }
}
