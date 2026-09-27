package io.github.illagercpr.creativecontainer.menu;

/**
 * Single source of truth for the GUI geometry.
 *
 * <p>It lives outside the client screen so that the headless GameTests can assert that the shipped textures match the
 * layout the screen draws (a mismatch is invisible at compile time and only shows up as a broken GUI in game).
 */
public final class CreativeContainerLayout {

    /** Size of {@code textures/gui/creative_container.png}. */
    public static final int IMAGE_WIDTH = 396;
    public static final int IMAGE_HEIGHT = 222;

    /** Size of {@code textures/gui/slot.png} and of one item cell. */
    public static final int CELL = 18;

    // left panel: creative item browser. The grid ends at y = 40 + 9*18 = 202, exactly flush with the pool grid
    // (148 + 3*18 = 202), so both columns share one bottom edge and one pagination line (y = 206).
    public static final int BROWSER_LEFT = 16;
    public static final int BROWSER_TOP = 40;
    public static final int BROWSER_COLUMNS = 9;
    public static final int BROWSER_ROWS = 9;

    // right panel: player inventory. Slot positions are decided by CreativeContainerMenu, because Slot.x/y are final.
    public static final int PLAYER_PANEL_LEFT = 200;
    public static final int PLAYER_PANEL_TOP = 36;

    // right panel: pool overview and the reported-amount field
    public static final int POOL_HEADER_Y = 120;
    public static final int AMOUNT_TOP = 132;
    public static final int AMOUNT_WIDTH = 96;
    public static final int POOL_LEFT = 200;
    public static final int POOL_TOP = 148;
    public static final int POOL_COLUMNS = 9;
    public static final int POOL_ROWS = 3;

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
