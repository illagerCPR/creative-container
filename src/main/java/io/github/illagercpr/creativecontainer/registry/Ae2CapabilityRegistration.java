package io.github.illagercpr.creativecontainer.registry;

import appeng.api.AECapabilities;
import io.github.illagercpr.creativecontainer.block.CreativeContainerBlockEntity;
import io.github.illagercpr.creativecontainer.interop.ae2.CreativeContainerMeStorage;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * AE2-specific capability registration.
 *
 * <p><b>Why {@code ME_STORAGE} and nothing else:</b> decompiling AE2 19.2.17 shows that
 * {@code appeng.parts.storagebus.StorageBusPart} builds its adjacency lookup as
 * {@code new PartAdjacentApi<>(this, AECapabilities.ME_STORAGE)} — a storage bus reads the ME inventory of the block
 * it faces straight from that block capability. Exporting {@code ME_STORAGE} is therefore the direct, first-class way
 * to be a storage bus target, and it needs no grid node, no channel and no power on our side.
 *
 * <p><b>This class must only be loaded when AE2 is present</b>; it is referenced exclusively from
 * {@link CCCapabilities} behind {@code InteropHooks.ae2Available()}.
 */
final class Ae2CapabilityRegistration {

    private Ae2CapabilityRegistration() {
    }

    static void register(RegisterCapabilitiesEvent event) {
        ICapabilityProvider<CreativeContainerBlockEntity, net.minecraft.core.Direction,
                appeng.api.storage.MEStorage> provider =
                (be, side) -> CreativeContainerMeStorage.of(be);
        event.registerBlockEntity(
                AECapabilities.ME_STORAGE,
                CCBlockEntities.CREATIVE_CONTAINER.get(),
                provider
        );
    }
}
