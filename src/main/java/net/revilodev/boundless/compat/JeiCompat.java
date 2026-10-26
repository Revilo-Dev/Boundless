package net.revilodev.boundless.compat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class JeiCompat {
    // Kept as Object so JEI remains an optional dependency at runtime.
    private static Object runtime;

    private JeiCompat() {}

    public static boolean isJeiInstalled() {
        return ModList.get().isLoaded("jei");
    }

    public static void setRuntime(Object jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static void refreshScreenLayout(Screen screen) {
        if (runtime == null || screen == null) return;
        refreshOverlay(runtime, "getIngredientListOverlay", screen);
        refreshOverlay(runtime, "getBookmarkOverlay", screen);
    }

    private static void refreshOverlay(Object jeiRuntime, String getterName, Screen screen) {
        try {
            Object overlay = jeiRuntime.getClass().getMethod(getterName).invoke(jeiRuntime);
            Object updater = overlay.getClass().getMethod("getScreenPropertiesUpdater").invoke(overlay);
            Object pending = updater.getClass().getMethod("updateScreen", Screen.class).invoke(updater, screen);
            pending.getClass().getMethod("forceUpdate").invoke(pending);
        } catch (Throwable ignored) {
        }
    }

    public static boolean showItem(ItemStack stack, boolean showRecipe) {
        // Reflection avoids loading JEI classes when the mod is not installed.
        if (stack == null || stack.isEmpty() || runtime == null) return false;
        try {
            Class<?> ingredientRoleClass = Class.forName("mezz.jei.api.recipe.RecipeIngredientRole");
            Object role = Enum.valueOf((Class<Enum>) ingredientRoleClass, showRecipe ? "OUTPUT" : "INPUT");
            Object vanillaItemStackType = Class.forName("mezz.jei.api.constants.VanillaTypes")
                    .getField("ITEM_STACK")
                    .get(null);
            Object jeiHelpers = runtime.getClass().getMethod("getJeiHelpers").invoke(runtime);
            Object focusFactory = jeiHelpers.getClass().getMethod("getFocusFactory").invoke(jeiHelpers);
            Object focus = focusFactory.getClass()
                    .getMethod("createFocus", ingredientRoleClass, Class.forName("mezz.jei.api.ingredients.IIngredientType"), Object.class)
                    .invoke(focusFactory, role, vanillaItemStackType, stack.copy());
            Object recipesGui = runtime.getClass().getMethod("getRecipesGui").invoke(runtime);
            recipesGui.getClass()
                    .getMethod("show", Class.forName("mezz.jei.api.recipe.IFocus"))
                    .invoke(recipesGui, focus);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
