package net.revilodev.boundless.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

@OnlyIn(Dist.CLIENT)
public final class BoundlessClient {
    private BoundlessClient() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(QuestPanelClient::onScreenInit);
        MinecraftForge.EVENT_BUS.addListener(QuestPanelClient::onScreenClosing);
        MinecraftForge.EVENT_BUS.addListener(QuestPanelClient::onScreenRenderPre);
        MinecraftForge.EVENT_BUS.addListener(QuestPanelClient::onMouseScrolled);
    }
}
