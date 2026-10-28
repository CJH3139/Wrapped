package com.wrapped.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.wrapped.cards.Card;
import com.wrapped.compat.Compat;
import com.wrapped.compat.CompatScreen;
import com.wrapped.compat.Gfx;
import com.wrapped.gui.CardTiming.Cue;
import com.wrapped.store.Settings;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.joml.Matrix3x2fStack;

public class WrappedScreen extends CompatScreen {
    private static final String LOADING = "Reading your history...";
    private static final String EMPTY = "Nothing to show yet";
    private static final String HINT = "Click or press space";
    private static final String HINT_GUESS = "Pick one";
    private static final String SKIP = "Skip";
    private static final String MOTION_ON = "Motion: On";
    private static final String MOTION_OFF = "Motion: Off";
    private static final long MAX_STEP_MS = 50;
    private static final float TICK_VOLUME = 0.35f;
    private static final float WHOOSH_PITCH = 1.3f;
    private static final float WHOOSH_VOLUME = 0.4f;
    private static final int[] SCALE_STEPS = {0, 4, 7, 12, 16, 19};
    private static final long AT_REST = Long.MAX_VALUE / 4;

    private enum Control {
        PREVIOUS_YEAR, NEXT_YEAR, INSTANCE, TOTAL
    }

    private final Supplier<Optional<WrappedData>> data;
    private final BooleanSupplier askMerge;
    private final Consumer<Boolean> answerMerge;
    private final Particles particles = new Particles();
    private final Shake shake = new Shake();
    private final List<Cue> queued = new ArrayList<>();
    private boolean mergeAsked;
    private boolean restartTitle;
    private WrappedData loaded;
    private int year;
    private boolean total;
    private StageController controller = new StageController(List.of(), now());
    private Settings settings;
    private long lastFrame = -1;
    private long seed;
    private boolean wiping;
    private Capture capture = Capture.NONE;
    private Rect captureRect;
    private int captureYear;
    private String captureName;

    private enum Capture {
        NONE, DRAW, GRAB
    }

    public WrappedScreen(Supplier<Optional<WrappedData>> data) {
        this(data, () -> false, yes -> {
        });
    }

    public WrappedScreen(Supplier<Optional<WrappedData>> data, BooleanSupplier askMerge,
            Consumer<Boolean> answerMerge) {
        super(Component.literal("Wrapped"));
        this.data = data;
        this.askMerge = askMerge;
        this.answerMerge = answerMerge;
    }

    @Override
    protected void init() {
        if (settings == null) {
            settings = Settings.load(gameDir());
        }
        if (restartTitle) {
            restartTitle = false;
            if (controller.index() == 0) {
                rebuild();
            }
        }
    }

    private static Path gameDir() {
        return Minecraft.getInstance().gameDirectory.toPath();
    }

    int year() {
        return year;
    }

    boolean total() {
        return total;
    }

    Optional<WrappedData> data() {
        return Optional.ofNullable(loaded);
    }

    void selectYear(int year) {
        this.year = year;
        rebuild();
    }

    void setTotal(boolean total) {
        this.total = total;
        rebuild();
    }

    private void rebuild() {
        if (loaded == null) {
            return;
        }
        controller = new StageController(YearPicker.deck(loaded, year, total), now());
        queued.clear();
    }

    private void poll() {
        Optional<WrappedData> next = data.get();
        if (next.isEmpty() || next.get() == loaded) {
            return;
        }
        boolean first = loaded == null;
        boolean noCard = controller.card().isEmpty();
        boolean atStart = noCard || controller.index() == 0;
        loaded = next.get();
        if (first || noCard) {
            year = YearPicker.initial(loaded.years(), LocalDate.now().getYear());
        }
        if (first) {
            total = false;
        }
        if (atStart) {
            rebuild();
        }
    }

    public Screen opening() {
        if (!mergeAsked && askMerge.getAsBoolean()) {
            mergeAsked = true;
            restartTitle = true;
            return new MergePrompt(this, answerMerge);
        }
        return this;
    }

    private boolean mergePending() {
        if (loaded == null || mergeAsked) {
            return false;
        }
        if (askMerge.getAsBoolean()) {
            return true;
        }
        mergeAsked = true;
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        poll();
        if (mergePending()) {
            mergeAsked = true;
            restartTitle = true;
            Compat.setScreen(minecraft, new MergePrompt(this, answerMerge));
        }
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    private boolean hideTopWord() {
        return settings != null && settings.hideTopWord();
    }

    private boolean reduceMotion() {
        return settings != null && settings.reduceMotion();
    }

    private void sync(long now) {
        queued.addAll(controller.update(now));
    }

    private boolean live() {
        return controller.wipe().isEmpty();
    }

    private StageFrame frame(Gfx g, int mx, int my, long now, boolean exporting) {
        boolean hasPrevious = loaded != null && YearPicker.previous(loaded.years(), year).isPresent();
        boolean hasNext = loaded != null && YearPicker.next(loaded.years(), year).isPresent();
        long elapsed = exporting ? AT_REST : controller.elapsed(now);
        long stepElapsed = exporting ? AT_REST : controller.stepElapsed(now);
        StageView view = new StageView(controller.step(), controller.guessPick(), hideTopWord(), total, hasPrevious,
                hasNext, exporting, reduceMotion(), controller.index(), controller.size(), stepElapsed, year);
        return new StageFrame(g, font, width, height, mx, my, elapsed, now, view);
    }

    @Override
    protected void drawBackground(Gfx g, int mouseX, int mouseY, float delta) {
        Compat.extractSubtitles(minecraft);
    }

    @Override
    protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
        poll();
        grabCapture();
        long now = now();
        sync(now);
        long dt = lastFrame < 0 ? 0 : Math.max(0, Math.min(MAX_STEP_MS, now - lastFrame));
        lastFrame = now;
        Optional<Card> current = controller.card();
        if (loaded == null || current.isEmpty() || mergePending()) {
            capture = Capture.NONE;
            queued.clear();
            message(g, now, loaded == null || mergePending() ? LOADING : EMPTY);
            return;
        }
        Card card = current.get();
        if (capture == Capture.DRAW) {
            if (card instanceof Card.Summary) {
                drawCapture(g, card, now);
                capture = Capture.GRAB;
                return;
            }
            capture = Capture.NONE;
        }
        boolean live = live();
        StageFrame f = frame(g, live ? mouseX : -1, live ? mouseY : -1, now, false);
        for (Cue cue : queued) {
            play(f, card, cue, now);
        }
        queued.clear();
        controller.tick(now).ifPresent(p -> Compat.playUi(SoundEvents.EXPERIENCE_ORB_PICKUP, (float) p, TICK_VOLUME));
        boolean wipeNow = controller.wipe().isPresent();
        if (wipeNow && !wiping) {
            Compat.playUi(SoundEvents.PLAYER_ATTACK_SWEEP, WHOOSH_PITCH, WHOOSH_VOLUME);
        }
        wiping = wipeNow;
        boolean motion = !reduceMotion();
        if (motion) {
            particles.step(dt, height);
        } else {
            particles.clear();
        }
        StageBackground.draw(g, World.of(card), width, height, now, motion);
        Shake.Offset offset = motion ? shake.offset(now, Compat.screenEffectScale(minecraft)) : new Shake.Offset(0, 0);
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(offset.x(), offset.y());
        StageRenderer.render(f, card);
        pose.popMatrix();
        if (motion) {
            StageDraw.particles(f, particles);
        }
        chrome(f, card);
        controller.wipe().ifPresent(w -> StageDraw.wipe(f, w));
    }

    private void message(Gfx g, long now, String text) {
        StageBackground.draw(g, World.GRASS, width, height, now, !reduceMotion());
        Rect c = StageLayout.content(width, height);
        int scale = font.width(text) * 2 <= c.w() ? 2 : 1;
        String t = StageLayout.fitText(text, Math.max(1, c.w() / scale), font::width);
        int tw = font.width(t) * scale;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(c.x() + (c.w() - tw) / 2f, c.y() + (c.h() - StageDraw.LINE * scale) / 2f);
        pose.scale(scale, scale);
        g.text(font, t, 0, 0, StageDraw.WHITE, true);
        pose.popMatrix();
    }

    private void chrome(StageFrame f, Card card) {
        StageDraw.progress(f);
        boolean summary = card instanceof Card.Summary;
        if (!summary) {
            StageDraw.hint(f, card instanceof Card.Guess ? HINT_GUESS : HINT);
        }
        CardDraw.button(f.g(), font, reduceMotion() ? MOTION_OFF : MOTION_ON, StageLayout.motion(width, height),
                f.mx(), f.my());
        if (!summary) {
            CardDraw.button(f.g(), font, SKIP, StageLayout.skip(width, height), f.mx(), f.my());
        }
    }

    private void play(StageFrame f, Card card, Cue cue, long now) {
        sound(cue);
        if (reduceMotion()) {
            return;
        }
        int[] colors = World.of(card).particles();
        long s = now * 31 + seed++;
        switch (cue.effect()) {
            case PUFF -> burst(f, card, cue, colors, 22, 0.7f, s);
            case BURST -> burst(f, card, cue, colors, 70, 1f, s);
            case BIG_BURST -> burst(f, card, cue, colors, 120, 1.3f, s);
            case CONFETTI -> particles.rain(width, height, colors, 140, s);
            case NONE -> {
            }
        }
        if (cue.shakeMs() > 0) {
            shake.start(now, cue.shakeMs());
        }
    }

    private void burst(StageFrame f, Card card, Cue cue, int[] colors, int count, float power, long s) {
        float[] at = StageRenderer.anchor(f, card, cue.anchor(), cue.index());
        particles.burst(at[0], at[1], colors, count, power, s);
    }

    private static float note(float base, int index) {
        int i = Math.max(0, Math.min(SCALE_STEPS.length - 1, index));
        return (float) Math.min(2.0, base * Math.pow(2, SCALE_STEPS[i] / 12.0));
    }

    private static void sound(Cue cue) {
        switch (cue.sound()) {
            case THUD -> {
                Compat.playUi(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.8f, 1.0f);
                Compat.playUi(SoundEvents.NOTE_BLOCK_PLING.value(), note(0.7f, cue.index()), 0.7f);
                if (cue.landing()) {
                    Compat.playUi(SoundEvents.FIREWORK_ROCKET_BLAST, 1.0f, 0.6f);
                }
            }
            case CHIME -> {
                Compat.playUi(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
                Compat.playUi(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 0.8f);
                Compat.playUi(SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0f, 0.6f);
                Compat.playUi(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.0f);
            }
            case FANFARE -> {
                Compat.playUi(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                Compat.playUi(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 0.8f);
            }
            case FIREWORK -> {
                Compat.playUi(SoundEvents.FIREWORK_ROCKET_BLAST, 1.0f, 0.8f);
                Compat.playUi(SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.1f, 0.6f);
            }
            case LIGHT -> {
                Compat.playUi(SoundEvents.NOTE_BLOCK_PLING.value(), note(0.9f, cue.index()), 0.8f);
                Compat.playUi(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f, 0.6f);
            }
            case HIT -> {
                Compat.playUi(SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.9f);
                Compat.playUi(SoundEvents.NOTE_BLOCK_PLING.value(), note(0.8f, cue.index()), 0.5f);
            }
            case CLICK -> Compat.playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
        }
    }

    private void drawCapture(Gfx g, Card card, long now) {
        StageFrame f = frame(g, -1, -1, now, true);
        StageBackground.draw(g, World.of(card), width, height, now, false);
        StageRenderer.render(f, card);
        captureRect = StageLayout.shareCard(width, height);
        captureYear = year;
        captureName = ExportName.cardName(card);
    }

    private void grabCapture() {
        if (capture != Capture.GRAB) {
            return;
        }
        capture = Capture.NONE;
        Rect r = captureRect;
        CardExporter.export(minecraft, r.x(), r.y(), r.w(), r.h(), captureYear, captureName);
    }

    private void requestSave() {
        if (capture != Capture.NONE || CardExporter.busy()) {
            return;
        }
        capture = Capture.DRAW;
        click();
    }

    private static void click() {
        Compat.playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
    }

    private boolean control(Card card, double mx, double my, long now) {
        if (StageLayout.motion(width, height).contains(mx, my)) {
            toggleMotion();
            return true;
        }
        boolean summary = card instanceof Card.Summary;
        if (!summary && StageLayout.skip(width, height).contains(mx, my)) {
            controller.skip(now);
            click();
            return true;
        }
        switch (card) {
            case Card.Title c -> {
                Optional<Control> hit = titleHit(c, mx, my);
                hit.ifPresent(this::titleControl);
                return hit.isPresent();
            }
            case Card.Guess c -> {
                if (controller.guessPick().isPresent() || c.options().isEmpty()) {
                    return false;
                }
                List<Rect> slots = StageLayout.guessSlots(width, height, c.options().size());
                for (int i = 0; i < slots.size(); i++) {
                    if (slots.get(i).contains(mx, my)) {
                        controller.pickGuess(c.options().get(i), now);
                        return true;
                    }
                }
                return false;
            }
            case Card.ChatCard c -> {
                if (c.topWord().isPresent() && StageLayout.hideWord(width, height).contains(mx, my)) {
                    toggleHideWord();
                    return true;
                }
                return false;
            }
            case Card.Summary c -> {
                if (StageLayout.shareSave(StageLayout.shareCard(width, height)).contains(mx, my)) {
                    requestSave();
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private Optional<Control> titleHit(Card.Title c, double mx, double my) {
        String yearText = Integer.toString(c.year());
        int yearWidth = font.width(yearText);
        int scale = StageLayout.titleYearScale(width, height, yearWidth);
        boolean hasPrevious = YearPicker.previous(loaded.years(), year).isPresent();
        boolean hasNext = YearPicker.next(loaded.years(), year).isPresent();
        StageLayout.Title layout = StageLayout.title(width, height, yearWidth, scale, hasPrevious, hasNext);
        if (layout.previous().filter(r -> r.contains(mx, my)).isPresent()) {
            return Optional.of(Control.PREVIOUS_YEAR);
        }
        if (layout.next().filter(r -> r.contains(mx, my)).isPresent()) {
            return Optional.of(Control.NEXT_YEAR);
        }
        if (layout.instance().contains(mx, my)) {
            return Optional.of(Control.INSTANCE);
        }
        if (layout.total().contains(mx, my)) {
            return Optional.of(Control.TOTAL);
        }
        return Optional.empty();
    }

    private void titleControl(Control control) {
        OptionalInt target = switch (control) {
            case PREVIOUS_YEAR -> YearPicker.previous(loaded.years(), year);
            case NEXT_YEAR -> YearPicker.next(loaded.years(), year);
            default -> OptionalInt.empty();
        };
        boolean wantTotal = control == Control.TOTAL;
        boolean toggle = (control == Control.TOTAL || control == Control.INSTANCE) && wantTotal != total;
        if (target.isPresent()) {
            selectYear(target.getAsInt());
        } else if (toggle) {
            setTotal(wantTotal);
        } else {
            return;
        }
        click();
    }

    private void toggleHideWord() {
        settings = new Settings(!hideTopWord(), reduceMotion());
        settings.save(gameDir());
        click();
    }

    private void toggleMotion() {
        settings = new Settings(hideTopWord(), !reduceMotion());
        settings.save(gameDir());
        if (settings.reduceMotion()) {
            particles.clear();
        }
        click();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (loaded == null || mergePending() || click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        long now = now();
        sync(now);
        Optional<Card> card = controller.card();
        if (card.isEmpty()) {
            return true;
        }
        if (live() && control(card.get(), click.x(), click.y(), now)) {
            return true;
        }
        if (click.x() < width / 4.0) {
            controller.back(now);
        } else {
            controller.next(now);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (loaded != null && !mergePending()) {
            int key = input.key();
            if (key == InputConstants.KEY_SPACE || key == InputConstants.KEY_RIGHT) {
                long now = now();
                sync(now);
                controller.next(now);
                return true;
            }
            if (key == InputConstants.KEY_LEFT) {
                long now = now();
                sync(now);
                controller.back(now);
                return true;
            }
        }
        return super.keyPressed(input);
    }
}
