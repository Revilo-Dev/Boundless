package net.revilodev.boundless.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

// runs server-side quest checks at the configured interval
public final class ServerQuestTicker {
    private ServerQuestTicker() {}

    // Check each player once per second, but spread players across the full second.
    private static final int CHECK_INTERVAL_TICKS = 20;

    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        if (QuestTracker.hasPendingServerEvaluation(sp)) {
            QuestTracker.serverTickPlayer(sp);
            return;
        }
        int slot = Math.floorMod(sp.getUUID().hashCode(), CHECK_INTERVAL_TICKS);
        if ((sp.tickCount % CHECK_INTERVAL_TICKS) != slot) return;

        QuestTracker.serverTickPlayer(sp);
    }
}
