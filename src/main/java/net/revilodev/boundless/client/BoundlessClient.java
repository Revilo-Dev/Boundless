package net.revilodev.boundless.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

@Environment(EnvType.CLIENT)
public final class BoundlessClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BoundlessNetwork.bootstrapClient();
        KeyBindingHelper.registerKeyBinding(QuestBookKeybinds.openQuestBook());
        ClientTickEvents.END_CLIENT_TICK.register(QuestBookKeybinds::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                QuestTracker.tickPlayer(client.player);
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                QuestPanelClient.onScreenInit(screen));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> QuestData.loadClient(false));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            QuestTracker.clientClearAll();
            QuestData.clearClientNetworkData();
            PinnedQuestHud.resetPinsOnLeave();
        });

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> PinnedQuestHud.onRenderGui(guiGraphics));
    }
}
