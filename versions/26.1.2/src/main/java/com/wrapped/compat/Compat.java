package com.wrapped.compat;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;

public final class Compat {
    public record Pixels(int[] argb, int width, int height) {
    }

    private Compat() {
    }

    public static void registerKey(KeyMapping key) {
        KeyMappingHelper.registerKeyMapping(key);
    }

    public static Screen screen(Minecraft client) {
        return client.screen;
    }

    public static void setScreen(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }

    public static void playUi(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    public static void playUi(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    public static ItemStack redBed() {
        return new ItemStack(Items.RED_BED);
    }

    public static String accountName(Minecraft client) {
        return client.getUser().getName();
    }

    public static UUID accountId(Minecraft client) {
        return client.getUser().getProfileId();
    }

    public static boolean isBlockItem(String key) {
        Identifier id = Identifier.tryParse(key);
        return id != null && BuiltInRegistries.ITEM.getValue(id) instanceof BlockItem;
    }

    public static Optional<Path> singleplayerWorld(Minecraft client) {
        IntegratedServer server = client.getSingleplayerServer();
        if (server == null) {
            return Optional.empty();
        }
        return Optional.of(server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize());
    }

    public static BooleanSupplier singleplayerStopped(Minecraft client) {
        IntegratedServer server = client.getSingleplayerServer();
        if (server == null) {
            return () -> true;
        }
        return server::isStopped;
    }

    public static double screenEffectScale(Minecraft client) {
        return client.options.screenEffectScale().get();
    }

    public static void extractSubtitles(Minecraft client) {
        client.gui.extractDeferredSubtitles();
    }

    public static int guiScale(Minecraft client) {
        return client.getWindow().getGuiScale();
    }

    public static void grabFrame(Minecraft client, Executor executor, Consumer<Pixels> done) {
        Screenshot.takeScreenshot(client.getMainRenderTarget(), image -> executor.execute(() -> {
            try (image) {
                done.accept(new Pixels(image.getPixels(), image.getWidth(), image.getHeight()));
            }
        }));
    }

    public static void writePng(int[] argb, int width, int height, Path file) throws IOException {
        try (NativeImage image = new NativeImage(width, height, false)) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setPixel(x, y, argb[y * width + x]);
                }
            }
            image.writeToFile(file);
        }
    }

    public static Component fileLink(Path file) {
        return Component.literal(file.getFileName().toString()).withStyle(ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(file.toAbsolutePath().toFile())));
    }

    public static void showChat(Minecraft client, Component message) {
        client.execute(() -> client.gui.getChat().addClientSystemMessage(message));
    }
}
