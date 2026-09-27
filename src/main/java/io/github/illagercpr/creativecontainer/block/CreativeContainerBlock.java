package io.github.illagercpr.creativecontainer.block;

import com.mojang.serialization.MapCodec;
import io.github.illagercpr.creativecontainer.menu.CreativeContainerMenu;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The creative container: an endgame block that hands out items from a virtual, never-depleting pool.
 */
public class CreativeContainerBlock extends BaseEntityBlock {

    public static final MapCodec<CreativeContainerBlock> CODEC = simpleCodec(CreativeContainerBlock::new);

    public CreativeContainerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    // BaseEntityBlock defaults to RenderShape.INVISIBLE because it assumes a block entity renderer draws the block.
    // This block is a plain chunk-baked model, so the model must be requested explicitly — without this override the
    // placed block renders nothing at all (only its selection outline), while the item form still renders. The 1-arg
    // signature is what 1.21.1 has; upstream marks it @Deprecated (which makes javac warn on the override even though
    // there is no replacement), so the warning is suppressed deliberately.
    @SuppressWarnings("deprecation")
    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeContainerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CreativeContainerBlockEntity container) {
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("block.creativecontainer.creative_container");
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player menuPlayer) {
                    return new CreativeContainerMenu(containerId, inventory, container);
                }
            }, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CreativeContainerBlockEntity container) {
            container.setReportedAmount(CreativeContainerBlockEntity.defaultReportedAmount());
        }
    }

    // Note: no getCloneItemStack override. Its default already returns a plain copy of this block, which is exactly
    // the wanted behaviour for a container that keeps nothing (and the override is deprecated in 1.21.1).

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.creativecontainer.creative_container.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.creativecontainer.creative_container.2").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Used by the ME storage scan and tests; keeps the block entity lookup in one place. */
    @Nullable
    public static CreativeContainerBlockEntity find(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CreativeContainerBlockEntity container ? container : null;
    }
}
