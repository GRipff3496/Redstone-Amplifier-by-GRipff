package net.gripff.amplifiermod;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MULTIPLIER_POS_0 = BUILDER
            .comment("Multiplier for torch position 0")
            .defineInRange("multiplier_pos0", 2, 1, 15);

    public static final ForgeConfigSpec.IntValue MULTIPLIER_POS_1 = BUILDER
            .comment("Multiplier for torch position 1")
            .defineInRange("multiplier_pos1", 3, 1, 15);

    public static final ForgeConfigSpec.IntValue MULTIPLIER_POS_2 = BUILDER
            .comment("Multiplier for torch position 2")
            .defineInRange("multiplier_pos2", 5, 1, 15);

    public static final ForgeConfigSpec.IntValue MULTIPLIER_POS_3 = BUILDER
            .comment("Multiplier for torch position 3")
            .defineInRange("multiplier_pos3", 10, 1, 15);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}