package net.revilodev.boundless.quest;

import net.minecraft.world.entity.player.Player;

public final class QuestEvents {
    private QuestEvents() {
    }

    public static void onPlayerTick(Player player) {
        if (player == null) return;
        if (!player.level().isClientSide) return;
        QuestTracker.tickPlayer(player);
    }
}
