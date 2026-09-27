package io.github.illagercpr.creativecontainer.menu;

import io.github.illagercpr.creativecontainer.block.CreativeContainerBlockEntity;
import io.github.illagercpr.creativecontainer.container.CreativeItemPool;
import io.github.illagercpr.creativecontainer.registry.CCMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Menu of the creative container.
 *
 * <p>The menu deliberately contains the player's 36 inventory slots and nothing else: the pool overview and the
 * scrolling creative item grid are drawn by
 * {@link io.github.illagercpr.creativecontainer.client.CreativeContainerScreen} from the block entity's synced data.
 * No item can therefore be duplicated through slot clicking, quick-move or shift-clicking.
 */
public class CreativeContainerMenu extends AbstractContainerMenu {

    private static final int PLAYER_SLOT_COUNT = 36;

    // Screen-space coordinates of the player inventory panel. Slot.x/y are final in 1.21.1, so the positions are
    // decided here at construction time and the screen only draws the background around them.
    public static final int PLAYER_PANEL_LEFT = CreativeContainerLayout.PLAYER_PANEL_LEFT;
    public static final int PLAYER_PANEL_TOP = CreativeContainerLayout.PLAYER_PANEL_TOP;
    private static final int CELL = CreativeContainerLayout.CELL;

    private final ContainerLevelAccess access;
    private final ContainerData amountData;

    @Nullable
    private final CreativeContainerBlockEntity blockEntity;

    /** Client-side constructor: the block position is appended by {@code player.openMenu(provider, pos)}. */
    public CreativeContainerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, resolveClientContainer(playerInventory, buf.readBlockPos()),
                new SimpleContainerData(1));
    }

    public CreativeContainerMenu(int containerId, Inventory playerInventory, CreativeContainerBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity, reportedAmountData(blockEntity));
    }

    private CreativeContainerMenu(int containerId, Inventory playerInventory, @Nullable CreativeContainerBlockEntity blockEntity,
                                 ContainerData amountData) {
        super(CCMenus.CREATIVE_CONTAINER.get(), containerId);
        this.blockEntity = blockEntity;
        this.amountData = amountData;
        this.access = blockEntity == null || blockEntity.getLevel() == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        addDataSlots(amountData);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        PLAYER_PANEL_LEFT + 1 + col * CELL, PLAYER_PANEL_TOP + 1 + row * CELL));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    PLAYER_PANEL_LEFT + 1 + col * CELL, PLAYER_PANEL_TOP + 1 + 3 * CELL + 4));
        }
    }

    private static ContainerData reportedAmountData(CreativeContainerBlockEntity blockEntity) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return blockEntity.reportedAmount();
            }

            @Override
            public void set(int index, int value) {
                blockEntity.setReportedAmount(value);
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
    }

    @Nullable
    private static CreativeContainerBlockEntity resolveClientContainer(Inventory playerInventory, BlockPos pos) {
        return playerInventory.player.level().getBlockEntity(pos) instanceof CreativeContainerBlockEntity container
                ? container
                : null;
    }

    @Nullable
    public CreativeContainerBlockEntity blockEntity() {
        return blockEntity;
    }

    /** The amount as the server currently has it; on the client this is the synchronised value. */
    public int reportedAmount() {
        return amountData.get(0);
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) {
            return false;
        }
        return stillValid(access, player, blockEntity.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // The creative grid is not a slot container, so there is nothing to quick-move.
        return ItemStack.EMPTY;
    }

    // ---- server-side actions driven by payloads -------------------------------------------------

    /**
     * Puts one item from the creative grid into the player's inventory.
     *
     * @param slot inventory index 0..35, or a negative value to let the game choose
     */
    public void handlePickItem(ItemStack stack, int slot, Player player) {
        if (stack.isEmpty() || !stillValid(player)) {
            return;
        }
        ItemStack picked = stack.copyWithCount(Math.max(1, Math.min(stack.getCount(), stack.getMaxStackSize())));
        if (slot >= 0 && slot < PLAYER_SLOT_COUNT) {
            ItemStack current = slots.get(slot).getItem();
            if (current.isEmpty()) {
                slots.get(slot).set(picked);
                slots.get(slot).setChanged();
            } else if (ItemStack.isSameItemSameComponents(current, picked)
                    && current.getCount() + picked.getCount() <= current.getMaxStackSize()) {
                current.grow(picked.getCount());
                slots.get(slot).setChanged();
            } else {
                giveToPlayer(picked, player);
            }
        } else {
            giveToPlayer(picked, player);
        }
        broadcastChanges();
    }

    private static void giveToPlayer(ItemStack stack, Player player) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public void handleRemoveSlot(int slot) {
        if (blockEntity != null && slot >= 0 && slot < blockEntity.pool().slotCount()) {
            blockEntity.removeSlot(slot);
        }
    }

    public void handleAddToPool(ItemStack stack) {
        if (blockEntity != null && !stack.isEmpty()) {
            blockEntity.addItem(stack);
        }
    }

    public void handleSetAmount(int amount) {
        if (blockEntity != null) {
            blockEntity.setReportedAmount(amount);
        }
    }
}
