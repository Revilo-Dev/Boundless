package net.revilodev.boundless.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

public final class ServerQuestTicker {
    private ServerQuestTicker() {}

    // check once per second per player
    private static final int CHECK_INTERVAL_TICKS = 20;

    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        if ((sp.tickCount % CHECK_INTERVAL_TICKS) != 0) return;

        // Update statuses server-side and notify client on changes
        QuestTracker.serverTickPlayer(sp);

    }
}
