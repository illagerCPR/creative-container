package io.github.illagercpr.creativecontainer;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common (server side) configuration. Everything here is a default or a safety knob; the per-block "reported amount"
 * that storage buses see lives in the block entity itself and is edited from the block settings GUI.
 */
public final class CCConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue DEFAULT_REPORTED_AMOUNT;
    public static final ModConfigSpec.BooleanValue ACCEPT_INSERTIONS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Creative Container common settings").push("general");

        DEFAULT_REPORTED_AMOUNT = builder
                .comment("Amount reported to storage buses for every available item (1 .. 2147483647).",
                        "This is only the default for newly placed blocks; every block stores its own value,",
                        "editable from the block settings GUI.")
                .defineInRange("defaultReportedAmount", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);

        ACCEPT_INSERTIONS = builder
                .comment("Whether the container accepts items pushed into it. The creative pool never stores anything,",
                        "so insertions are rejected by default (the safe behaviour for an infinite source).")
                .define("acceptInsertions", false);

        builder.pop();
        SPEC = builder.build();
    }

    private CCConfig() {
    }
}
