package net.revilodev.boundless.quest;

import net.minecraft.server.level.ServerPlayer;

public final class ServerQuestEvents {
    private ServerQuestEvents() {
    }

    public static void onLogout(ServerPlayer player) {
        if (player == null) return;
        QuestProgressState state = QuestProgressState.get(player.serverLevel());
        state.setDirty();
        player.server.overworld().getDataStorage().save();
    }
}
