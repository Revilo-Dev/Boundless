package net.revilodev.boundless.quest;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.revilodev.boundless.network.BoundlessNetwork;

public final class ServerQuestEvents {
    private ServerQuestEvents() {
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        QuestProgressState.get(sp.serverLevel()).setDirty();
        QuestObjectiveState.get(sp.serverLevel()).setDirty();
        KillCounterState.get(sp.serverLevel()).setDirty();
        sp.server.overworld().getDataStorage().save();
    }

    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        QuestTracker.refreshPersistentContextTargets(sp, false, true, true);
        QuestTracker.serverTickPlayer(sp);
        BoundlessNetwork.syncPlayer(sp);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        QuestTracker.refreshPersistentContextTargets(sp, false, true, true);
        QuestTracker.serverTickPlayer(sp);
        BoundlessNetwork.syncPlayer(sp);
    }
}
