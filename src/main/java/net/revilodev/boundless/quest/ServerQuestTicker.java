package net.revilodev.boundless.quest;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.revilodev.boundless.network.BoundlessNetwork;

public final class ServerQuestTicker {
    private ServerQuestTicker() {}

    // Check each player once per second, but spread players across the full second.
    private static final int CHECK_INTERVAL_TICKS = 20;

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        int slot = Math.floorMod(sp.getUUID().hashCode(), CHECK_INTERVAL_TICKS);
        if ((sp.tickCount % CHECK_INTERVAL_TICKS) != slot) return;

        QuestTracker.serverTickPlayer(sp);
    }
}
