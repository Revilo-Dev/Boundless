package net.revilodev.boundless;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.revilodev.boundless.client.QuestBookKeybinds;
import net.revilodev.boundless.client.ClientQuestEvents;
import net.revilodev.boundless.client.QuestPanelClient;
import net.revilodev.boundless.command.BoundlessCommands;
import net.revilodev.boundless.item.ModItems;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.KillCounterState;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestEvents;
import net.revilodev.boundless.quest.ServerQuestEvents;
import org.slf4j.Logger;

import java.util.List;

@Mod(BoundlessMod.MOD_ID)
public final class BoundlessMod {
    public static final String MOD_ID = "boundless";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BoundlessMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC, MOD_ID + "-common.toml");
        ModItems.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addCreative);
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(this::clientSetup);
            modBus.addListener(QuestBookKeybinds::onRegisterKeyMappings);
            MinecraftForge.EVENT_BUS.addListener(QuestBookKeybinds::onClientTick);
        }
        BoundlessNetwork.bootstrap();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(QuestEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(ServerQuestEvents::onLogout);
        MinecraftForge.EVENT_BUS.addListener(net.revilodev.boundless.quest.ServerQuestTicker::onPlayerTick);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Boundless common setup complete");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener(QuestPanelClient::onScreenInit);
        MinecraftForge.EVENT_BUS.addListener(QuestPanelClient::onScreenClosing);
        MinecraftForge.EVENT_BUS.addListener(QuestPanelClient::onScreenRenderPre);
        MinecraftForge.EVENT_BUS.addListener(QuestPanelClient::onMouseScrolled);
        MinecraftForge.EVENT_BUS.addListener(ClientQuestEvents::onClientLogin);
        MinecraftForge.EVENT_BUS.addListener(ClientQuestEvents::onClientLogout);
        MinecraftForge.EVENT_BUS.addListener(ClientQuestEvents::onClientLevelUnload);

    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            if (!Config.disableQuestBook()) {
                event.accept(ModItems.QUEST_BOOK.get());
            }
            if (Config.enableQuestScrolls()) {
                event.accept(ModItems.QUEST_COMPLETION_SCROLL.get());
            }
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        BoundlessCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Boundless server starting");
        QuestData.loadServer(event.getServer(), true);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (!Config.disableQuestBook() && Config.spawnWithQuestBook() && !hasQuestBook(sp)) {
                sp.getInventory().add(new ItemStack(ModItems.QUEST_BOOK.get()));
            }
            BoundlessNetwork.syncPlayer(sp);
        }
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        QuestData.loadServer(event.getPlayerList().getServer(), true);
        if (event.getPlayer() != null) {
            BoundlessNetwork.syncPlayer(event.getPlayer());
        } else {
            event.getPlayerList().getPlayers().forEach(BoundlessNetwork::syncPlayer);
        }
    }

    private static boolean hasQuestBook(ServerPlayer player) {
        if (player == null) return false;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(ModItems.QUEST_BOOK.get())) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel server)) return;
        ResourceLocation rl = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (rl == null) return;
        KillCounterState.get(server).inc(sp.getUUID(), rl.toString());
        int count = KillCounterState.get(server).get(sp.getUUID(), rl.toString());
        BoundlessNetwork.KillEntry entry = new BoundlessNetwork.KillEntry(rl.toString(), count);
        BoundlessNetwork.SyncKills payload = new BoundlessNetwork.SyncKills(List.of(entry));
        BoundlessNetwork.sendToPlayer(sp, payload);
    }
}
