package io.github.illagercpr.creativecontainer.registry;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import io.github.illagercpr.creativecontainer.block.CreativeContainerBlockEntity;
import io.github.illagercpr.creativecontainer.menu.CreativeContainerMenu;
import io.github.illagercpr.creativecontainer.network.AddToPoolPayload;
import io.github.illagercpr.creativecontainer.network.PickItemPayload;
import io.github.illagercpr.creativecontainer.network.PoolDeltaPayload;
import io.github.illagercpr.creativecontainer.network.PoolEditPayload;
import io.github.illagercpr.creativecontainer.network.SelectPoolSlotPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Mod network payloads. */
public final class CCPayloads {

    private CCPayloads() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CCPayloads::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // "2": PoolDeltaPayload gained the designated pipe outlet field (v0.1.1).
        PayloadRegistrar registrar = event.registrar("2");

        registrar.playToServer(PickItemPayload.TYPE, PickItemPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof CreativeContainerMenu menu) {
                menu.handlePickItem(payload.stack(), payload.slot(), context.player());
            }
        });

        registrar.playToServer(PoolEditPayload.TYPE, PoolEditPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof CreativeContainerMenu menu) {
                if (payload.slot() >= 0) {
                    menu.handleRemoveSlot(payload.slot());
                } else if (payload.amount() > 0) {
                    menu.handleSetAmount(payload.amount());
                }
            }
        });

        registrar.playToServer(AddToPoolPayload.TYPE, AddToPoolPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof CreativeContainerMenu menu) {
                menu.handleAddToPool(payload.stack());
            }
        });

        registrar.playToServer(SelectPoolSlotPayload.TYPE, SelectPoolSlotPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof CreativeContainerMenu menu) {
                menu.handleSelectPoolSlot(payload.slot(), context.player());
            }
        });

        // Server -> client: incremental pool updates for the client-side mirror. Applied on the main thread; with no
        // real client tracking the chunk (headless GameTests) the server never sends anything.
        registrar.playToClient(PoolDeltaPayload.TYPE, PoolDeltaPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player().level().getBlockEntity(payload.pos())
                            instanceof CreativeContainerBlockEntity container) {
                        container.applyClientDelta(payload);
                    }
                }));

        CreativeContainer.LOGGER.debug("Registered creative container payloads");
    }
}
