package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

public final class ClientQuestEvents {
    private ClientQuestEvents() {
    }

    public static void onClientLogin() {
        QuestData.loadClient(true);
        QuestTracker.setClientMultiplayer(!Minecraft.getInstance().hasSingleplayerServer());
    }

    public static void onClientLogout() {
        QuestTracker.forceSave();
        QuestTracker.setClientMultiplayer(false);
    }

    public static void onClientLevelUnload() {
        QuestTracker.forceSave();
        QuestTracker.setClientMultiplayer(false);
    }
}
