package com.apeira.main.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * apeira-main's client config ({@code config/apeira-client.toml}), registered by {@link ApeiraClient}. Test features
 * that only change what this client draws are switched here, so they can be turned off without touching the economy.
 */
public final class MainClientConfig {
    public static final ModConfigSpec SPEC;
    /** The Zelda-style ruby ticker ({@code client/ruby/RubyTickerAnimation}); off draws the plain balance as before. */
    public static final ModConfigSpec.BooleanValue RUBY_TICKER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("rubies");
        RUBY_TICKER = builder
                .comment("Test feature: show a +/- ruby banner over the HUD counter and tick the counter to the new balance when rubies are paid, deposited or rewarded.")
                .define("rubyTicker", true);
        builder.pop();
        SPEC = builder.build();
    }

    private MainClientConfig() {}
}
