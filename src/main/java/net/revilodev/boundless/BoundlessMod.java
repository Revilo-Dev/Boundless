package net.revilodev.boundless;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.revilodev.boundless.command.BoundlessCommands;
import net.revilodev.boundless.item.ModItems;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.KillCounterState;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestProgressState;
import net.revilodev.boundless.quest.QuestTracker;
import org.slf4j.Logger;

import java.util.List;

public final class BoundlessMod implements ModInitializer {
    public static final String MOD_ID = "boundless";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        Config.init();
        ModItems.register();
        BoundlessNetwork.bootstrap();

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            if (!Config.disableQuestBook()) {
                entries.accept(ModItems.QUEST_BOOK);
            }
            if (Config.enableQuestScrolls()) {
                entries.accept(ModItems.QUEST_COMPLETION_SCROLL);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                BoundlessCommands.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("Boundless server starting");
            QuestData.loadServer(server, true);
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (!success) return;
            QuestData.loadServer(server, true);
            server.getPlayerList().getPlayers().forEach(BoundlessNetwork::syncPlayer);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            if (!Config.disableQuestBook() && Config.spawnWithQuestBook() && !hasQuestBook(player)) {
                player.getInventory().add(new ItemStack(ModItems.QUEST_BOOK));
            }
            BoundlessNetwork.syncPlayer(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            QuestProgressState state = QuestProgressState.get(player.serverLevel());
            state.setDirty();
            player.server.overworld().getDataStorage().save();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                QuestTracker.tickPlayer(player);
                if ((player.tickCount % 20) == 0) {
                    QuestTracker.serverTickPlayer(player);
                }
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof LivingEntity victim)) return;
            if (!(damageSource.getEntity() instanceof ServerPlayer player)) return;
            if (!(player.level() instanceof ServerLevel serverLevel)) return;
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
            if (id == null) return;
            KillCounterState.get(serverLevel).inc(player.getUUID(), id.toString());
            int count = KillCounterState.get(serverLevel).get(player.getUUID(), id.toString());
            BoundlessNetwork.sendToPlayer(player, new BoundlessNetwork.SyncKills(List.of(
                    new BoundlessNetwork.KillEntry(id.toString(), count)
            )));
            QuestTracker.serverTickPlayer(player);
        });

        LOGGER.info("Boundless Fabric setup complete");
    }

    private static boolean hasQuestBook(ServerPlayer player) {
        if (player == null) return false;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(ModItems.QUEST_BOOK)) {
                return true;
            }
        }
        return false;
    }
}
