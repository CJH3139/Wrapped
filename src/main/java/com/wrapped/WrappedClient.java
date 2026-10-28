package com.wrapped;

import com.wrapped.compat.Compat;
import com.wrapped.gui.WrappedScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;

public final class WrappedClient implements ClientModInitializer {
    public static final String MOD_ID = "wrapped";

    public static final KeyMapping OPEN = Compat.key("key.wrapped.open", Compat.KEY_K, MOD_ID);

    private static long ticks;

    @Override
    public void onInitializeClient() {
        Compat.registerKey(OPEN);
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
        Compat.onConnection(WrappedState::onJoin, WrappedState::onDisconnect);
    }
}
