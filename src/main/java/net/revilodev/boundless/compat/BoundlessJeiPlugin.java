package net.revilodev.boundless.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.revilodev.boundless.client.QuestPanelClient;

@JeiPlugin
public final class BoundlessJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("boundless", "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        // Hand the live runtime to the reflection-based compatibility bridge.
        JeiCompat.setRuntime(jeiRuntime);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(InventoryScreen.class, new IGuiContainerHandler<>() {
            @Override
            public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(InventoryScreen screen) {
                return QuestPanelClient.getJeiExtraAreas(screen);
            }
        });
    }
}
