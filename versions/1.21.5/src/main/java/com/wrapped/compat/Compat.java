package com.wrapped.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;

public final class Compat {
    public static final int KEY_K = InputConstants.KEY_K;
    public static final int KEY_SPACE = InputConstants.KEY_SPACE;
    public static final int KEY_LEFT = InputConstants.KEY_LEFT;
    public static final int KEY_RIGHT = InputConstants.KEY_RIGHT;
    public static final int MOUSE_LEFT = InputConstants.MOUSE_BUTTON_LEFT;

    public record Pixels(int[] argb, int width, int height) {
    }

    private Compat() {
    }

    public static KeyMapping key(String name, int code, String modId) {
        return new KeyMapping(name, code, "key.category." + modId + "." + modId);
    }

    public static void registerKey(KeyMapping key) {
        KeyBindingHelper.registerKeyBinding(key);
    }

    public static void onConnection(Consumer<Minecraft> joined, Runnable left) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> joined.accept(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> left.run());
    }

    public static Screen screen(Minecraft client) {
        return client.screen;
    }

    public static void setScreen(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }

    private static SoundEvent sound(UiSound sound) {
        return switch (sound) {
            case BASEDRUM -> SoundEvents.NOTE_BLOCK_BASEDRUM.value();
            case PLING -> SoundEvents.NOTE_BLOCK_PLING.value();
            case BLAST -> SoundEvents.FIREWORK_ROCKET_BLAST;
            case LARGE_BLAST -> SoundEvents.FIREWORK_ROCKET_LARGE_BLAST;
            case TWINKLE -> SoundEvents.FIREWORK_ROCKET_TWINKLE;
            case LEVELUP -> SoundEvents.PLAYER_LEVELUP;
            case CHIME -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case CHALLENGE -> SoundEvents.UI_TOAST_CHALLENGE_COMPLETE;
            case ORB -> SoundEvents.EXPERIENCE_ORB_PICKUP;
            case SWEEP -> SoundEvents.PLAYER_ATTACK_SWEEP;
            case CRIT -> SoundEvents.PLAYER_ATTACK_CRIT;
            case CLICK -> SoundEvents.UI_BUTTON_CLICK.value();
        };
    }

    public static void playUi(UiSound sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound(sound), pitch, volume));
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
        ResourceLocation id = ResourceLocation.tryParse(key);
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
    }

    public static int guiScale(Minecraft client) {
        return (int) client.getWindow().getGuiScale();
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

    public static void showSaved(Minecraft client, String prefix, Path file) {
        Component link = Component.literal(file.getFileName().toString()).withStyle(ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(file.toAbsolutePath().toFile())));
        show(client, Component.literal(prefix).append(link));
    }

    public static void showMessage(Minecraft client, String text) {
        show(client, Component.literal(text));
    }

    private static void show(Minecraft client, Component message) {
        client.execute(() -> client.gui.getChat().addMessage(message));
    }
}
