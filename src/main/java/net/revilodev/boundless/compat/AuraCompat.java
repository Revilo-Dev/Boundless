package net.revilodev.boundless.compat;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.revilodev.boundless.BoundlessDebug;
import net.revilodev.boundless.client.QuestPanelClient;

import java.lang.reflect.Method;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class AuraCompat {
    private static final String AURA_MOD_ID = "aura";
    private static final String PANEL_EVENT_CLASS = "net.revilodev.aura.api.PanelStateChangedEvent";
    private static final ResourceLocation AURA_INVENTORY_PANEL =
            ResourceLocation.fromNamespaceAndPath(AURA_MOD_ID, "inventory_panel");

    private static Method panelIdMethod;
    private static Method openMethod;
    private static boolean registered;

    private AuraCompat() {
    }

    // listen only when aura and its optional api are available
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void register() {
        if (registered || !ModList.get().isLoaded(AURA_MOD_ID)) return;

        try {
            Class eventClass = Class.forName(PANEL_EVENT_CLASS);
            panelIdMethod = eventClass.getMethod("panelId");
            openMethod = eventClass.getMethod("open");
            NeoForge.EVENT_BUS.addListener(eventClass, (Consumer) (Consumer<Object>) AuraCompat::onPanelStateChanged);
            registered = true;
        } catch (ReflectiveOperationException | LinkageError e) {
            BoundlessDebug.warnRateLimited("aura-panel-compat", 5_000L,
                    "could not register Aura panel compatibility", e);
        }
    }

    // close the quest panel before aura draws its inventory panel
    private static void onPanelStateChanged(Object event) {
        try {
            if (!Boolean.TRUE.equals(openMethod.invoke(event))) return;
            Object panelId = panelIdMethod.invoke(event);
            if (!AURA_INVENTORY_PANEL.equals(panelId)) return;
            QuestPanelClient.closeAllOpenPanels();
        } catch (ReflectiveOperationException | RuntimeException e) {
            BoundlessDebug.warnRateLimited("aura-panel-event", 5_000L,
                    "could not handle Aura panel state", e);
        }
    }

    // ask Aura to close its inventory panel before Boundless opens
    public static void closeAuraInventoryPanels() {
        if (!ModList.get().isLoaded(AURA_MOD_ID)) return;
        try {
            Class<?> panelClass = Class.forName("net.revilodev.aura.client.skills.SkillsPanelClient");
            panelClass.getMethod("closeAllOpenPanels").invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            BoundlessDebug.warnRateLimited("aura-panel-close", 5_000L,
                    "could not close Aura inventory panels", e);
        }
    }
}
