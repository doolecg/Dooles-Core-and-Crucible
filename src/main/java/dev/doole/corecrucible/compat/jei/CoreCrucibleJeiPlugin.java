package dev.doole.corecrucible.compat.jei;

import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.compat.ForgingDisplays;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModTiers;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if fabric {
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
//?}

/** JEI integration. Loaded only when JEI is present: NeoForge finds {@code @JeiPlugin}, Fabric the entrypoint. */
@JeiPlugin
//? if fabric {
@Entrypoint("jei_mod_plugin")
//?}
public class CoreCrucibleJeiPlugin implements IModPlugin {
    private static final String INFO = "jei." + CoreCrucible.MOD_ID + ".info.";

    @Override
    public Identifier getPluginUid() {
        return CoreCrucible.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new ForgingSmithingCategory(guiHelper));
    }

    @Override
    // The recipes for the category, plus JEI "info" pages (the text is in the lang file under jei.dooles_core_crucible.info.*)
    // explaining alloy ingots, templates and catalysts.
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ForgingSmithingCategory.TYPE, ForgingDisplays.smithing());

        List<ItemStack> alloys = new ArrayList<>();
        List<ItemStack> templates = new ArrayList<>();
        for (ModTiers tier : ModItems.ALLOY_TIERS) {
            alloys.add(new ItemStack(ModItems.alloyIngot(tier)));
            templates.add(new ItemStack(ModItems.upgradeTemplate(tier)));
        }
        registration.addItemStackInfo(alloys, Component.translatable(INFO + "alloy_ingot"));
        registration.addItemStackInfo(templates, Component.translatable(INFO + "upgrade_template"));
        registration.addItemStackInfo(List.of(new ItemStack(Items.GHAST_TEAR), new ItemStack(Items.WITHER_SKELETON_SKULL), new ItemStack(Items.NETHER_STAR)),
                Component.translatable(INFO + "catalyst"));
    }

    @Override
    // Shows the smithing table as the block these recipes are made in.
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.SMITHING_TABLE), ForgingSmithingCategory.TYPE);
    }
}
