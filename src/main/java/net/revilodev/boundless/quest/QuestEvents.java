package net.revilodev.boundless.quest;

import net.minecraftforge.event.TickEvent;

public final class QuestEvents {
    private QuestEvents() {
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (e.player == null) return;
        if (!e.player.level().isClientSide) return;
        QuestTracker.tickPlayer(e.player);
    }
}
