package net.revilodev.boundless.quest;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// forwards gameplay events into quest progress tracking
public final class QuestEvents {
    private QuestEvents() {
    }

    // tick client quest state
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() == null) return;
        if (!e.getEntity().level().isClientSide) return;
        QuestTracker.tickPlayer(e.getEntity());
    }
}
