package io.github.illagercpr.creativecontainer.client;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import io.github.illagercpr.creativecontainer.block.CreativeContainerBlockEntity;
import io.github.illagercpr.creativecontainer.client.CCClient;
import io.github.illagercpr.creativecontainer.container.CreativeItemIndex;
import io.github.illagercpr.creativecontainer.container.CreativeItemPool;
import io.github.illagercpr.creativecontainer.menu.CreativeContainerLayout;
import io.github.illagercpr.creativecontainer.menu.CreativeContainerMenu;
import io.github.illagercpr.creativecontainer.network.AddToPoolPayload;
import io.github.illagercpr.creativecontainer.network.PickItemPayload;
import io.github.illagercpr.creativecontainer.network.PoolEditPayload;
import io.github.illagercpr.creativecontainer.network.SelectPoolSlotPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Creative-inventory-like screen for the creative container.
 *
 * <p><b>Left panel:</b> a search field plus a paged 9x9 grid of every item the creative tabs offer — the same source
 * the vanilla creative inventory uses. Left click takes one, shift + left click takes a full stack, right click adds
 * the item to the container's pool.
 *
 * <p><b>Right panel:</b> the player inventory (the only real slots of the menu) and the pool overview: left click
 * takes one out of the pool, right click removes the template, and the amount field sets how many of every item the
 * pool reports to storage buses.
 */
public class CreativeContainerScreen extends AbstractContainerScreen<CreativeContainerMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(CreativeContainer.MOD_ID, "textures/gui/creative_container.png");
    private static final ResourceLocation SLOT =
            ResourceLocation.fromNamespaceAndPath(CreativeContainer.MOD_ID, "textures/gui/slot.png");

    // left panel: creative item browser
    private static final int TITLE_Y = 10;
    private static final int GRID_LEFT = CreativeContainerLayout.BROWSER_LEFT;
    private static final int GRID_TOP = CreativeContainerLayout.BROWSER_TOP;
    private static final int COLUMNS = CreativeContainerLayout.BROWSER_COLUMNS;
    private static final int ROWS = CreativeContainerLayout.BROWSER_ROWS;
    private static final int CELL = CreativeContainerLayout.CELL;
    private static final int PAGE_SIZE = CreativeContainerLayout.browserPageSize();

    // right panel: player inventory (slot positions are decided by the menu, see CreativeContainerMenu)
    private static final int PLAYER_LEFT = CreativeContainerLayout.PLAYER_PANEL_LEFT;
    private static final int PLAYER_TOP = CreativeContainerLayout.PLAYER_PANEL_TOP;

    // right panel: pool overview
    private static final int POOL_HEADER_Y = CreativeContainerLayout.POOL_HEADER_Y;
    private static final int AMOUNT_TOP = CreativeContainerLayout.AMOUNT_TOP;
    private static final int AMOUNT_WIDTH = CreativeContainerLayout.AMOUNT_WIDTH;
    private static final int POOL_LEFT = CreativeContainerLayout.POOL_LEFT;
    private static final int POOL_TOP = CreativeContainerLayout.POOL_TOP;
    private static final int POOL_COLUMNS = CreativeContainerLayout.POOL_COLUMNS;
    private static final int POOL_ROWS = CreativeContainerLayout.POOL_ROWS;
    private static final int POOL_PAGE_SIZE = CreativeContainerLayout.poolPageSize();

    private EditBox searchBox;
    private EditBox amountBox;
    private String searchQuery = "";
    private int page;
    private int poolPage;
    private List<CreativeItemIndex.Entry> filtered = List.of();

    /** Last known cursor position, needed because the select key fires from {@link #keyPressed} without coordinates. */
    private double hoverX;
    private double hoverY;

    public CreativeContainerScreen(CreativeContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = CreativeContainerLayout.IMAGE_WIDTH;
        this.imageHeight = CreativeContainerLayout.IMAGE_HEIGHT;
        this.titleLabelX = GRID_LEFT;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = PLAYER_LEFT + 1;
        this.inventoryLabelY = PLAYER_TOP - 12;
    }

    @Override
    protected void init() {
        super.init();

        if (CreativeItemIndex.isEmpty()) {
            CreativeItemIndex.rebuild(minecraft.level.registryAccess(), minecraft.level.enabledFeatures());
        }

        searchBox = new EditBox(font, leftPos + GRID_LEFT, topPos + 20, COLUMNS * CELL, 12,
                Component.translatable("gui.creativecontainer.search"));
        searchBox.setHint(Component.translatable("gui.creativecontainer.search.hint").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);

        amountBox = new EditBox(font, leftPos + PLAYER_LEFT, topPos + AMOUNT_TOP, AMOUNT_WIDTH, 12,
                Component.translatable("gui.creativecontainer.amount"));
        amountBox.setValue(Integer.toString(menu.reportedAmount()));
        amountBox.setTooltip(Tooltip.create(Component.translatable("gui.creativecontainer.amount.tooltip")));
        addRenderableWidget(amountBox);

        onSearchChanged(searchQuery);
    }

    private void onSearchChanged(String query) {
        searchQuery = query;
        filtered = CreativeItemIndex.search(query, Integer.MAX_VALUE);
        page = 0;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (amountBox != null && !amountBox.isFocused()) {
            String serverValue = Integer.toString(menu.reportedAmount());
            if (!serverValue.equals(amountBox.getValue())) {
                amountBox.setValue(serverValue);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // The texture size must be passed explicitly: the short overloads sample in a 256x256 space, which tiles
        // (background, 396x222) or smears to a single pixel (slots, 18x18) instead of drawing the image.
        graphics.blit(BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, imageWidth, imageHeight);

        drawCells(graphics, GRID_LEFT, GRID_TOP, COLUMNS, ROWS);
        drawCells(graphics, POOL_LEFT, POOL_TOP, POOL_COLUMNS, POOL_ROWS);

        drawBrowserItems(graphics);
        drawPoolItems(graphics);
        drawDesignatedOutline(graphics);
        drawHover(graphics, mouseX, mouseY);
    }

    private void drawCells(GuiGraphics graphics, int gridLeft, int gridTop, int columns, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                graphics.blit(SLOT, leftPos + gridLeft + col * CELL, topPos + gridTop + row * CELL,
                        0.0F, 0.0F, CELL, CELL, CELL, CELL);
            }
        }
    }

    private void drawBrowserItems(GuiGraphics graphics) {
        int start = page * PAGE_SIZE;
        for (int index = 0; index < PAGE_SIZE; index++) {
            int itemIndex = start + index;
            if (itemIndex >= filtered.size()) {
                break;
            }
            graphics.renderItem(filtered.get(itemIndex).stack(), itemX(index), itemY(index));
        }
    }

    private void drawPoolItems(GuiGraphics graphics) {
        CreativeContainerBlockEntity container = menu.blockEntity();
        if (container == null) {
            return;
        }
        int start = poolPage * POOL_PAGE_SIZE;
        for (int index = 0; index < POOL_PAGE_SIZE; index++) {
            ItemStack template = container.pool().getSlot(start + index);
            if (template.isEmpty()) {
                continue;
            }
            graphics.renderItem(template, poolX(index), poolY(index));
        }
    }

    /**
     * The slot generic logistics pipes pull from gets a permanent golden frame (v0.1.1). Only the currently visible
     * pool page can show it; the designation itself lives server-side.
     */
    private void drawDesignatedOutline(GuiGraphics graphics) {
        CreativeContainerBlockEntity container = menu.blockEntity();
        if (container == null) {
            return;
        }
        int designated = container.designatedSlot();
        if (designated < 0) {
            return;
        }
        int relative = designated - poolPage * POOL_PAGE_SIZE;
        if (relative < 0 || relative >= POOL_PAGE_SIZE) {
            return;
        }
        int x = leftPos + POOL_LEFT + (relative % POOL_COLUMNS) * CELL;
        int y = topPos + POOL_TOP + (relative / POOL_COLUMNS) * CELL;
        graphics.renderOutline(x + 1, y + 1, CELL - 2, CELL - 2, 0xFFFFD040);
        graphics.renderOutline(x + 2, y + 2, CELL - 4, CELL - 4, 0x90FFD040);
    }

    private void drawHover(GuiGraphics graphics, int mouseX, int mouseY) {
        int browserCell = cellAt(mouseX, mouseY, GRID_LEFT, GRID_TOP, COLUMNS, ROWS);
        if (browserCell >= 0 && page * PAGE_SIZE + browserCell < filtered.size()) {
            highlight(graphics, GRID_LEFT, GRID_TOP, browserCell, COLUMNS);
            return;
        }
        int poolCell = cellAt(mouseX, mouseY, POOL_LEFT, POOL_TOP, POOL_COLUMNS, POOL_ROWS);
        if (poolCell >= 0 && !poolStackAt(poolCell).isEmpty()) {
            highlight(graphics, POOL_LEFT, POOL_TOP, poolCell, POOL_COLUMNS);
        }
    }

    private void highlight(GuiGraphics graphics, int gridLeft, int gridTop, int cell, int columns) {
        int x = leftPos + gridLeft + (cell % columns) * CELL + 1;
        int y = topPos + gridTop + (cell / columns) * CELL + 1;
        // the vanilla slot-highlight colour: visible on the light AE2-style background as well
        graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
    }

    private int itemX(int index) {
        return leftPos + GRID_LEFT + (index % COLUMNS) * CELL + 1;
    }

    private int itemY(int index) {
        return topPos + GRID_TOP + (index / COLUMNS) * CELL + 1;
    }

    private int poolX(int index) {
        return leftPos + POOL_LEFT + (index % POOL_COLUMNS) * CELL + 1;
    }

    private int poolY(int index) {
        return topPos + POOL_TOP + (index / POOL_COLUMNS) * CELL + 1;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);

        CreativeContainerBlockEntity container = menu.blockEntity();
        int filled = container == null ? 0 : container.pool().availableItems().size();
        int slots = container == null ? CreativeItemPool.DEFAULT_SLOT_COUNT : container.pool().slotCount();
        graphics.drawString(font, Component.translatable("gui.creativecontainer.pool", filled, slots),
                POOL_LEFT, POOL_HEADER_Y, 0x404040, false);
        // Right of the amount box: the slot above it belongs to the pool header, so a label there would overlap.
        graphics.drawString(font, Component.translatable("gui.creativecontainer.amount.label"),
                POOL_LEFT + AMOUNT_WIDTH + 4, AMOUNT_TOP + 2, 0x404040, false);

        graphics.drawString(font, pageLabel(), GRID_LEFT, GRID_TOP + ROWS * CELL + 4, 0x606060, false);

        String poolLabel = (poolPage + 1) + " / " + poolPageCount();
        graphics.drawString(font, poolLabel, POOL_LEFT + POOL_COLUMNS * CELL - font.width(poolLabel),
                POOL_TOP + POOL_ROWS * CELL + 4, 0x606060, false);
    }

    private String pageLabel() {
        int totalPages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        return (page + 1) + " / " + totalPages + "   (" + filtered.size() + ")";
    }

    private int poolPageCount() {
        CreativeContainerBlockEntity container = menu.blockEntity();
        int slots = container == null ? CreativeItemPool.DEFAULT_SLOT_COUNT : container.pool().slotCount();
        return CreativeContainerLayout.poolPageCount(slots);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        // Pool slots get one extra line teaching the outlet-selection key; everything else keeps vanilla behaviour.
        int poolCell = cellAt(mouseX, mouseY, POOL_LEFT, POOL_TOP, POOL_COLUMNS, POOL_ROWS);
        ItemStack poolStack = poolCell >= 0 ? poolStackAt(poolCell) : ItemStack.EMPTY;
        if (!poolStack.isEmpty()) {
            List<Component> lines = new ArrayList<>(poolStack.getTooltipLines(
                    net.minecraft.world.item.Item.TooltipContext.of(minecraft.level), minecraft.player,
                    TooltipFlag.NORMAL));
            lines.add(Component.translatable("gui.creativecontainer.designate.hint").withStyle(ChatFormatting.GRAY));
            graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
            return;
        }
        ItemStack hovered = hoveredStack(mouseX, mouseY);
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(font, hovered, mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private ItemStack hoveredStack(int mouseX, int mouseY) {
        int browserCell = cellAt(mouseX, mouseY, GRID_LEFT, GRID_TOP, COLUMNS, ROWS);
        if (browserCell >= 0) {
            int itemIndex = page * PAGE_SIZE + browserCell;
            if (itemIndex < filtered.size()) {
                return filtered.get(itemIndex).stack();
            }
        }
        int poolCell = cellAt(mouseX, mouseY, POOL_LEFT, POOL_TOP, POOL_COLUMNS, POOL_ROWS);
        return poolCell >= 0 ? poolStackAt(poolCell) : ItemStack.EMPTY;
    }

    private ItemStack poolStackAt(int cell) {
        CreativeContainerBlockEntity container = menu.blockEntity();
        return container == null ? ItemStack.EMPTY : container.pool().getSlot(poolPage * POOL_PAGE_SIZE + cell);
    }

    /** @return the cell index inside the given grid, or -1 when the cursor is outside of it */
    private int cellAt(double mouseX, double mouseY, int gridLeft, int gridTop, int columns, int rows) {
        double relX = mouseX - (leftPos + gridLeft);
        double relY = mouseY - (topPos + gridTop);
        if (relX < 0 || relY < 0) {
            return -1;
        }
        int col = (int) (relX / CELL);
        int row = (int) (relY / CELL);
        if (col >= columns || row >= rows) {
            return -1;
        }
        return row * columns + col;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // AbstractContainerScreen.mouseClicked handles the whole screen and returns true on every path (it also owns
        // the "clicked outside" behaviour), so the browser and pool regions must be handled BEFORE calling super —
        // after super they would never be reached.
        if (handleBrowserClick(mouseX, mouseY, button) || handlePoolClick(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleBrowserClick(double mouseX, double mouseY, int button) {
        int cell = cellAt(mouseX, mouseY, GRID_LEFT, GRID_TOP, COLUMNS, ROWS);
        if (cell < 0) {
            return false;
        }
        int itemIndex = page * PAGE_SIZE + cell;
        if (itemIndex >= filtered.size()) {
            return false;
        }
        ItemStack stack = filtered.get(itemIndex).stack();
        if (button == 0) {
            int count = hasShiftDown() ? stack.getMaxStackSize() : 1;
            PacketDistributor.sendToServer(new PickItemPayload(stack.copyWithCount(count), -1));
            return true;
        }
        if (button == 1) {
            PacketDistributor.sendToServer(new AddToPoolPayload(stack));
            return true;
        }
        return false;
    }

    private boolean handlePoolClick(double mouseX, double mouseY, int button) {
        int cell = cellAt(mouseX, mouseY, POOL_LEFT, POOL_TOP, POOL_COLUMNS, POOL_ROWS);
        if (cell < 0) {
            return false;
        }
        ItemStack template = poolStackAt(cell);
        if (template.isEmpty()) {
            return false;
        }
        if (button == 0) {
            int count = hasShiftDown() ? template.getMaxStackSize() : 1;
            PacketDistributor.sendToServer(new PickItemPayload(template.copyWithCount(count), -1));
            return true;
        }
        if (button == 1) {
            PacketDistributor.sendToServer(PoolEditPayload.remove(poolPage * POOL_PAGE_SIZE + cell));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int direction = (int) Math.signum(scrollY);
        if (cellAt(mouseX, mouseY, GRID_LEFT, GRID_TOP, COLUMNS, ROWS) >= 0) {
            int totalPages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            page = Mth.clamp(page - direction, 0, totalPages - 1);
            return true;
        }
        if (cellAt(mouseX, mouseY, POOL_LEFT, POOL_TOP, POOL_COLUMNS, POOL_ROWS) >= 0) {
            poolPage = Mth.clamp(poolPage - direction, 0, poolPageCount() - 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        hoverX = mouseX;
        hoverY = mouseY;
        super.mouseMoved(mouseX, mouseY);
    }

    /**
     * While a text field has focus the keyboard belongs to it: {@code E} must type an "e", not close the screen,
     * {@code Q} must not drop items, the hotbar keys must not swap, and (this mod's) {@code R} must not designate.
     * <p>This is needed because {@link AbstractContainerScreen#keyPressed} checks those binds <em>after</em> the
     * {@code super} chain, and {@link EditBox#keyPressed} returns {@code false} for plain letter keys — without an
     * explicit swallow they would fall through to the container binds. {@code charTyped} is a separate event, so
     * swallowing {@code keyPressed} still lets the character land in the field.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Committing the amount field with Enter stays first: EditBox never consumes Enter.
        if (amountBox != null && amountBox.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            commitAmount();
            return true;
        }
        boolean editing = searchBox != null && searchBox.isFocused() || amountBox != null && amountBox.isFocused();
        if (editing) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                return super.keyPressed(keyCode, scanCode, modifiers); // still closes the screen
            }
            // Let the focused box consume the editing keys (arrows, backspace, Ctrl+A/C/V/X, ...), then swallow
            // everything else so no bind — ours or another mod's — fires while the player is typing.
            EditBox box = searchBox != null && searchBox.isFocused() ? searchBox : amountBox;
            if (box != null) {
                box.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        // The outlet-selection key only fires outside of the text fields (otherwise typing "r" would designate).
        if (CCClient.SELECT_POOL_ITEM.matches(keyCode, scanCode)) {
            int cell = cellAt(hoverX, hoverY, POOL_LEFT, POOL_TOP, POOL_COLUMNS, POOL_ROWS);
            if (cell >= 0 && !poolStackAt(cell).isEmpty()) {
                PacketDistributor.sendToServer(SelectPoolSlotPayload.select(poolPage * POOL_PAGE_SIZE + cell));
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        commitAmount();
        super.removed();
    }

    private void commitAmount() {
        if (amountBox == null) {
            return;
        }
        String raw = amountBox.getValue().trim().replace("_", "").replace(",", "").toLowerCase(Locale.ROOT);
        if (raw.isEmpty()) {
            amountBox.setValue(Integer.toString(menu.reportedAmount()));
            return;
        }
        try {
            long parsed = Long.parseLong(raw);
            int clamped = (int) Mth.clamp(parsed, CreativeItemPool.MIN_AMOUNT, CreativeItemPool.MAX_AMOUNT);
            amountBox.setValue(Integer.toString(clamped));
            if (clamped != menu.reportedAmount()) {
                PacketDistributor.sendToServer(PoolEditPayload.setAmount(clamped));
            }
        } catch (NumberFormatException e) {
            // Not a number ("", "-", "1e9", ...): restore what the server has.
            amountBox.setValue(Integer.toString(menu.reportedAmount()));
        }
    }
}
