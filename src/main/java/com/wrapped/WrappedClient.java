package com.wrapped;

import com.mojang.blaze3d.platform.InputConstants;
import com.wrapped.compat.Compat;
import com.wrapped.gui.WrappedScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class WrappedClient implements ClientModInitializer {
    public static final String MOD_ID = "wrapped";

    public static final KeyMapping OPEN = new KeyMapping(
            "key.wrapped.open",
            InputConstants.KEY_K,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "wrapped")));

    private static long ticks;

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(OPEN);
        ClientLifecycleEvents.CLIENT_STARTED.register(WrappedState::startLoading);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            WrappedState.onTick(ticks++);
            while (OPEN.consumeClick()) {
                if (Compat.screen(client) == null) {
                    WrappedState.refresh();
                    WrappedScreen screen = new WrappedScreen(WrappedState::data, WrappedState::shouldAskMerge,
                            WrappedState::answerMerge);
                    Compat.setScreen(client, screen.opening());
                }
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> WrappedState.onJoin(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WrappedState.onDisconnect());
    }
}
