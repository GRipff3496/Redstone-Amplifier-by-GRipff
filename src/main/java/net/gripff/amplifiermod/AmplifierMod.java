package net.gripff.amplifiermod;

import com.mojang.logging.LogUtils;
import net.gripff.amplifiermod.blocks.AmplifierBlock;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(AmplifierMod.MOD_ID)
public final class AmplifierMod {
    public static final String MOD_ID = "amplifiermod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Block> AMPLIFIER_BLOCK = BLOCKS.register("amplifier",
            () -> new AmplifierBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("amplifier")).noOcclusion()));
    public static final RegistryObject<Item> AMPLIFIER_ITEM = ITEMS.register("amplifier",
            () -> new BlockItem(AMPLIFIER_BLOCK.get(), new Item.Properties().setId(ITEMS.key("amplifier"))));

    public AmplifierMod(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        BLOCKS.register(modBusGroup);
        ITEMS.register(modBusGroup);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        BuildCreativeModeTabContentsEvent.BUS.addListener(AmplifierMod::addCreative);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.getEntries().putAfter(new ItemStack(Items.COMPARATOR),
                    new ItemStack(AMPLIFIER_ITEM.get()),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}