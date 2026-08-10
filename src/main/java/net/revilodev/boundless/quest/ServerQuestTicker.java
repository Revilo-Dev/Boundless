package net.revilodev.boundless.quest;

import net.minecraft.server.level.ServerPlayer;

public final class ServerQuestTicker {
    private ServerQuestTicker() {}

    private static final int CHECK_INTERVAL_TICKS = 20;

    public static void onPlayerTick(ServerPlayer player) {
        if (player == null) return;
        if (player.level().isClientSide) return;
        if ((player.tickCount % CHECK_INTERVAL_TICKS) != 0) return;
        QuestTracker.serverTickPlayer(player);
    }
}
