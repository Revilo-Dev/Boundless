
package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

public final class ClientQuestEvents {

    // load quest data when the player joins
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn e) {
        QuestData.loadClient(true);

        // check if the player joined a multiplayer server
        QuestTracker.setClientMultiplayer(
                !Minecraft.getInstance().hasSingleplayerServer()
        );
    }

    // save quest progress when the player leaves
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        QuestTracker.forceSave();
        QuestTracker.setClientMultiplayer(false);
    }

    // save again when the client world closes
    public static void onClientLevelUnload(LevelEvent.Unload e) {
        // ignore server side worlds
        if (!e.getLevel().isClientSide()) return;

        QuestTracker.forceSave();
        QuestTracker.setClientMultiplayer(false);
    }
}

