package com.apeira.main.client.ruby;

import com.apeira.main.client.MainClientConfig;
import com.apeira.main.ruby.RubyChangePayload;

import net.minecraft.util.Util;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Test feature: a Zelda-style ruby ticker for the HUD counter ({@link RubyHud}), switched by
 * {@link MainClientConfig#RUBY_TICKER}. It only hears {@link RubyChangePayload}s, which the server sends for direct balance
 * changes (payments, deposits, rewards), so picking up ruby items never starts it.
 * <p>
 * A change shows a banner ("+ [ruby] 250") over the counter, which keeps the old balance. The banner holds for
 * {@link #HOLD_MS}, and every change in that time adds to its total and restarts the hold. Then the banner counts down
 * to 0 while the counter counts to the new balance in step; the count takes longer for larger totals, but not in
 * proportion ({@link #countMillis}), so big totals tick faster per ruby and small ones tick one by one. A change while
 * counting folds what is left into a new hold. When done, the banner fades out.
 */
public final class RubyTickerAnimation {
    /** How long the banner holds still, collecting changes, before counting. */
    static final long HOLD_MS = 900;
    /** How long the banner takes to fade in and out. */
    static final long FADE_MS = 250;

    private enum Phase { IDLE, HOLD, COUNT, FADE }

    private static Phase phase = Phase.IDLE;
    /** The balance the server last sent; the counter shows {@code target - pending}. */
    private static long target;
    /** Rubies still to count into the counter (negative for a loss). */
    private static double pending;
    /** The sign the banner shows, kept while it fades out at 0. */
    private static int sign;
    /** When the current phase started and, while counting, how many rubies a millisecond. */
    private static long phaseStart;
    private static long shownSince;
    private static long lastFrame;
    private static double rate;

    private RubyTickerAnimation() {}

    public static boolean enabled() {
        return MainClientConfig.SPEC.isLoaded() && MainClientConfig.RUBY_TICKER.getAsBoolean();
    }

    public static void onChange(RubyChangePayload payload, IPayloadContext context) {
        if (!enabled()) {
            reset();
            return;
        }
        long now = Util.getMillis();
        if (phase == Phase.IDLE || phase == Phase.FADE) {
            pending = 0;
            shownSince = now;
        }
        target = payload.balance();
        pending += payload.delta();
        if (Math.round(pending) == 0) {
            // A gain and an equal loss cancel out.
            phase = Phase.FADE;
            phaseStart = now;
            pending = 0;
            return;
        }
        sign = pending > 0 ? 1 : -1;
        phase = Phase.HOLD;
        phaseStart = now;
    }

    /** Moves the animation on to {@code now}; called once a frame by {@link RubyHud}. */
    static void update(long now) {
        long frame = Math.max(0, now - lastFrame);
        lastFrame = now;
        switch (phase) {
            case HOLD -> {
                if (now - phaseStart >= HOLD_MS) {
                    phase = Phase.COUNT;
                    phaseStart = now;
                    rate = Math.abs(pending) / countMillis(Math.abs(pending));
                }
            }
            case COUNT -> {
                double step = Math.min(Math.abs(pending), rate * frame);
                pending -= Math.signum(pending) * step;
                if (Math.abs(pending) < 0.5) {
                    pending = 0;
                    phase = Phase.FADE;
                    phaseStart = now;
                }
            }
            case FADE -> {
                if (now - phaseStart >= FADE_MS) {
                    reset();
                }
            }
            case IDLE -> {}
        }
    }

    /**
     * How long counting {@code amount} rubies takes: 60 ms a ruby up to 8 (each one seen), then growing with the
     * logarithm of the amount, up to 2.5 seconds for millions.
     */
    static double countMillis(double amount) {
        if (amount <= 8) {
            return Math.max(1, amount) * 60;
        }
        return Math.min(2500, 480 + 330 * Math.log10(amount / 8));
    }

    /** Whether the banner is up. */
    static boolean active() {
        return phase != Phase.IDLE;
    }

    /** The balance the counter shows, given the synced {@code balance}. */
    static long displayed(long balance) {
        return active() ? Math.max(0, target - Math.round(pending)) : balance;
    }

    /** The banner's amount, at least 0. */
    static long bannerAmount() {
        return Math.abs(Math.round(pending));
    }

    /** +1 for a gain, -1 for a loss. */
    static int sign() {
        return sign;
    }

    /** The banner's opacity, 0-1: fading in from the first change, out at the end. */
    static float alpha(long now) {
        if (phase == Phase.FADE) {
            return 1 - Math.min(1, (now - phaseStart) / (float) FADE_MS);
        }
        return Math.min(1, (now - shownSince) / (float) FADE_MS);
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    private static void reset() {
        phase = Phase.IDLE;
        pending = 0;
        sign = 0;
    }
}
