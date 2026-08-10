package net.revilodev.boundless.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.BoundlessDebug;
import net.revilodev.boundless.client.toast.QuestUnlockedToast;
import net.revilodev.boundless.item.ModItems;
import net.revilodev.boundless.quest.KillCounterState;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestItemSpec;
import net.revilodev.boundless.quest.QuestObjectiveState;
import net.revilodev.boundless.quest.QuestPackStorage;
import net.revilodev.boundless.quest.QuestProgressState;
import net.revilodev.boundless.quest.QuestTracker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class BoundlessNetwork {

    // network channel and shared packet state
    private static final String CHANNEL = "boundless";
    private static final String VERSION = "3";
    private static boolean REGISTERED = false;

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Set<String> REDEEM_IN_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, QuestPackUploadSession> QUESTPACK_UPLOADS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<java.util.UUID, ObjectiveProgressSnapshot> LAST_OBJECTIVE_SYNC = new ConcurrentHashMap<>();

    private static final AtomicInteger SYNC_ID_GEN = new AtomicInteger();
    private static final int QUEST_CHUNK_BYTES = 60000;
    private static final Path INSTANCE_QUEST_PACKS_ROOT = Config.questPacksRoot();

    private BoundlessNetwork() {}

    // last objective sync snapshot per player
    private record ObjectiveProgressSnapshot(
            Map<String, Integer> items,
            Map<String, Boolean> flags,
            Map<String, String> inputs
    ) {
        private static ObjectiveProgressSnapshot empty() {
            return new ObjectiveProgressSnapshot(Map.of(), Map.of(), Map.of());
        }
    }

    // register payload handlers once per game session
    public static void bootstrap(IEventBus bus) {
        bus.addListener(BoundlessNetwork::register);
    }

    // bind every client and server payload handler
    private static void register(RegisterPayloadHandlersEvent event) {
        if (REGISTERED) return;
        REGISTERED = true;

        PayloadRegistrar r = event.registrar(CHANNEL).versioned(VERSION);

        r.playToServer(Redeem.TYPE, Redeem.CODEC, BoundlessNetwork::handleRedeem);
        r.playToServer(Reject.TYPE, Reject.CODEC, BoundlessNetwork::handleReject);
        r.playToServer(UndoReject.TYPE, UndoReject.CODEC, BoundlessNetwork::handleUndoReject);
        r.playToServer(CreateScroll.TYPE, CreateScroll.CODEC, BoundlessNetwork::handleCreateScroll);
        r.playToServer(RestartRepeatable.TYPE, RestartRepeatable.CODEC, BoundlessNetwork::handleRestartRepeatable);
        r.playToServer(UpdateFieldInput.TYPE, UpdateFieldInput.CODEC, BoundlessNetwork::handleUpdateFieldInput);
        r.playToServer(ReportObserve.TYPE, ReportObserve.CODEC, BoundlessNetwork::handleReportObserve);
        r.playToServer(SetQuestPackEnabled.TYPE, SetQuestPackEnabled.CODEC, BoundlessNetwork::handleSetQuestPackEnabled);
        r.playToServer(UpdateServerConfig.TYPE, UpdateServerConfig.CODEC, BoundlessNetwork::handleUpdateServerConfig);
        r.playToServer(UploadQuestPackChunk.TYPE, UploadQuestPackChunk.CODEC, BoundlessNetwork::handleUploadQuestPackChunk);
        r.playToServer(DeleteQuestPack.TYPE, DeleteQuestPack.CODEC, BoundlessNetwork::handleDeleteQuestPack);

        r.playToClient(SyncStatus.TYPE, SyncStatus.CODEC, BoundlessNetwork::handleSyncStatus);
        r.playToClient(SyncStatuses.TYPE, SyncStatuses.CODEC, BoundlessNetwork::handleSyncStatuses);
        r.playToClient(SyncProgressMeta.TYPE, SyncProgressMeta.CODEC, BoundlessNetwork::handleSyncProgressMeta);
        r.playToClient(SyncObjectiveProgress.TYPE, SyncObjectiveProgress.CODEC, BoundlessNetwork::handleSyncObjectiveProgress);
        r.playToClient(SyncKills.TYPE, SyncKills.CODEC, BoundlessNetwork::handleSyncKills);
        r.playToClient(SyncClear.TYPE, SyncClear.CODEC, BoundlessNetwork::handleSyncClear);
        r.playToClient(Toast.TYPE, Toast.CODEC, BoundlessNetwork::handleToast);
        r.playToClient(RewardToast.TYPE, RewardToast.CODEC, BoundlessNetwork::handleRewardToast);
        r.playToClient(OpenQuestBook.TYPE, OpenQuestBook.CODEC, BoundlessNetwork::handleOpenQuestBook);
        r.playToClient(SyncConfig.TYPE, SyncConfig.CODEC, BoundlessNetwork::handleSyncConfig);
        r.playToClient(SyncQuestsChunk.TYPE, SyncQuestsChunk.CODEC, BoundlessNetwork::handleSyncQuestsChunk);
    }

    // client request to redeem one quest
    public record Redeem(String questId) implements CustomPacketPayload {
        public static final Type<Redeem> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "redeem"));
        public static final StreamCodec<FriendlyByteBuf, Redeem> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.questId),
                buf -> new Redeem(buf.readUtf())
        );
        @Override public Type<Redeem> type() { return TYPE; }
    }

    // client request to reject one optional quest
    public record Reject(String questId) implements CustomPacketPayload {
        public static final Type<Reject> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "reject"));
        public static final StreamCodec<FriendlyByteBuf, Reject> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.questId),
                buf -> new Reject(buf.readUtf())
        );
        @Override public Type<Reject> type() { return TYPE; }
    }

    // client request to undo a quest rejection
    public record UndoReject(String questId) implements CustomPacketPayload {
        public static final Type<UndoReject> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "undo_reject"));
        public static final StreamCodec<FriendlyByteBuf, UndoReject> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.questId),
                buf -> new UndoReject(buf.readUtf())
        );
        @Override public Type<UndoReject> type() { return TYPE; }
    }

    // client request to create a quest scroll
    public record CreateScroll(String questId) implements CustomPacketPayload {
        public static final Type<CreateScroll> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "create_scroll"));
        public static final StreamCodec<FriendlyByteBuf, CreateScroll> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.questId),
                buf -> new CreateScroll(buf.readUtf())
        );
        @Override public Type<CreateScroll> type() { return TYPE; }
    }

    // client request to restart a repeatable quest
    public record RestartRepeatable(String questId) implements CustomPacketPayload {
        public static final Type<RestartRepeatable> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "restart_repeatable"));
        public static final StreamCodec<FriendlyByteBuf, RestartRepeatable> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.questId),
                buf -> new RestartRepeatable(buf.readUtf())
        );
        @Override public Type<RestartRepeatable> type() { return TYPE; }
    }

    // client sync for field input objectives
    public record UpdateFieldInput(String questId, String targetId, String value) implements CustomPacketPayload {
        public static final Type<UpdateFieldInput> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "update_field_input"));
        public static final StreamCodec<FriendlyByteBuf, UpdateFieldInput> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.questId);
                    buf.writeUtf(p.targetId);
                    buf.writeUtf(p.value == null ? "" : p.value);
                },
                buf -> new UpdateFieldInput(buf.readUtf(), buf.readUtf(), buf.readUtf())
        );
        @Override public Type<UpdateFieldInput> type() { return TYPE; }
    }

    // client sync for observe objectives
    public record ReportObserve(String questId, String targetId) implements CustomPacketPayload {
        public static final Type<ReportObserve> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "report_observe"));
        public static final StreamCodec<FriendlyByteBuf, ReportObserve> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.questId == null ? "" : p.questId);
                    buf.writeUtf(p.targetId == null ? "" : p.targetId);
                },
                buf -> new ReportObserve(buf.readUtf(), buf.readUtf())
        );
        @Override public Type<ReportObserve> type() { return TYPE; }
    }

    // client request to enable or disable a quest pack
    public record SetQuestPackEnabled(String id, boolean enabled, boolean builtin) implements CustomPacketPayload {
        public static final Type<SetQuestPackEnabled> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "set_questpack_enabled"));
        public static final StreamCodec<FriendlyByteBuf, SetQuestPackEnabled> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.id == null ? "" : p.id);
                    buf.writeBoolean(p.enabled);
                    buf.writeBoolean(p.builtin);
                },
                buf -> new SetQuestPackEnabled(buf.readUtf(), buf.readBoolean(), buf.readBoolean())
        );
        @Override public Type<SetQuestPackEnabled> type() { return TYPE; }
    }

    // client snapshot of synced config values
    public record UpdateServerConfig(
            String pinnedQuestHudPosition,
            boolean hideQuestBookInInventory,
            String questBookInventoryButtonPosition,
            boolean centerInventoryWithQuestPanel,
            boolean hideCategoryHeader,
            String filterDisplayMode,
            boolean disableCategories,
            boolean hideQuestWidgetIcons,
            double questTextScale,
            double questIconScale,
            boolean enableQuestSearchBox,
            boolean enableDescriptionColors,
            String questWidgetTextColor,
            String descriptionTextColor,
            boolean enableDescriptionReadMore,
            boolean enableDescriptionTextWrapping,
            String descriptionTextAlignment,
            boolean enableQuestToasts,
            boolean disableQuestPinning,
            boolean autoClaimQuestRewards,
            boolean enableQuestScrolls,
            boolean disableQuestBook,
            boolean spawnWithQuestBook) implements CustomPacketPayload {
        public static final Type<UpdateServerConfig> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "update_server_config"));
        public static final StreamCodec<FriendlyByteBuf, UpdateServerConfig> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.pinnedQuestHudPosition == null ? "" : p.pinnedQuestHudPosition);
                    buf.writeBoolean(p.hideQuestBookInInventory);
                    buf.writeUtf(p.questBookInventoryButtonPosition == null ? "" : p.questBookInventoryButtonPosition);
                    buf.writeBoolean(p.centerInventoryWithQuestPanel);
                    buf.writeBoolean(p.hideCategoryHeader);
                    buf.writeUtf(p.filterDisplayMode == null ? "" : p.filterDisplayMode);
                    buf.writeBoolean(p.disableCategories);
                    buf.writeBoolean(p.hideQuestWidgetIcons);
                    buf.writeDouble(p.questTextScale);
                    buf.writeDouble(p.questIconScale);
                    buf.writeBoolean(p.enableQuestSearchBox);
                    buf.writeBoolean(p.enableDescriptionColors);
                    buf.writeUtf(p.questWidgetTextColor == null ? "" : p.questWidgetTextColor);
                    buf.writeUtf(p.descriptionTextColor == null ? "" : p.descriptionTextColor);
                    buf.writeBoolean(p.enableDescriptionReadMore);
                    buf.writeBoolean(p.enableDescriptionTextWrapping);
                    buf.writeUtf(p.descriptionTextAlignment == null ? "" : p.descriptionTextAlignment);
                    buf.writeBoolean(p.enableQuestToasts);
                    buf.writeBoolean(p.disableQuestPinning);
                    buf.writeBoolean(p.autoClaimQuestRewards);
                    buf.writeBoolean(p.enableQuestScrolls);
                    buf.writeBoolean(p.disableQuestBook);
                    buf.writeBoolean(p.spawnWithQuestBook);
                },
                buf -> new UpdateServerConfig(
                        buf.readUtf(),
                        buf.readBoolean(),
                        buf.readUtf(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readUtf(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readUtf(),
                        buf.readUtf(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readUtf(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean())
        );
        @Override public Type<UpdateServerConfig> type() { return TYPE; }
    }

    // one quest pack upload chunk from the editor
    public record UploadQuestPackChunk(String id, boolean enabled, int uploadId, int totalParts, int index, byte[] part) implements CustomPacketPayload {
        public static final Type<UploadQuestPackChunk> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "upload_questpack_chunk"));
        public static final StreamCodec<FriendlyByteBuf, UploadQuestPackChunk> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.id == null ? "" : p.id);
                    buf.writeBoolean(p.enabled);
                    buf.writeVarInt(p.uploadId);
                    buf.writeVarInt(p.totalParts);
                    buf.writeVarInt(p.index);
                    byte[] safe = p.part == null ? new byte[0] : p.part;
                    buf.writeVarInt(safe.length);
                    buf.writeBytes(safe);
                },
                buf -> {
                    String id = buf.readUtf();
                    boolean enabled = buf.readBoolean();
                    int uploadId = buf.readVarInt();
                    int totalParts = buf.readVarInt();
                    int index = buf.readVarInt();
                    int len = buf.readVarInt();
                    if (len < 0 || len > 1_200_000) throw new IllegalArgumentException("questpack chunk len " + len);
                    byte[] bytes = new byte[len];
                    buf.readBytes(bytes);
                    return new UploadQuestPackChunk(id, enabled, uploadId, totalParts, index, bytes);
                }
        );
        @Override public Type<UploadQuestPackChunk> type() { return TYPE; }
    }

    // client request to delete one quest pack
    public record DeleteQuestPack(String id) implements CustomPacketPayload {
        public static final Type<DeleteQuestPack> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "delete_questpack"));
        public static final StreamCodec<FriendlyByteBuf, DeleteQuestPack> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.id == null ? "" : p.id),
                buf -> new DeleteQuestPack(buf.readUtf())
        );
        @Override public Type<DeleteQuestPack> type() { return TYPE; }
    }

    // single quest status sync to clients
    public record SyncStatus(String questId, String status) implements CustomPacketPayload {
        public static final Type<SyncStatus> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_status"));
        public static final StreamCodec<FriendlyByteBuf, SyncStatus> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.questId);
                    buf.writeUtf(p.status);
                },
                buf -> new SyncStatus(buf.readUtf(), buf.readUtf())
        );
        @Override public Type<SyncStatus> type() { return TYPE; }
    }

    // entry model for bulk status sync
    public record StatusEntry(String questId, String status) {
        public static final StreamCodec<FriendlyByteBuf, StatusEntry> CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeUtf(e.questId);
                    buf.writeUtf(e.status);
                },
                buf -> new StatusEntry(buf.readUtf(), buf.readUtf())
        );
    }

    // bulk quest status sync to clients
    public record SyncStatuses(List<StatusEntry> entries) implements CustomPacketPayload {
        public static final Type<SyncStatuses> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_statuses"));
        public static final StreamCodec<FriendlyByteBuf, SyncStatuses> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.entries.size());
                    for (StatusEntry e : p.entries) StatusEntry.CODEC.encode(buf, e);
                },
                buf -> {
                    int n = buf.readVarInt();
                    List<StatusEntry> list = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) list.add(StatusEntry.CODEC.decode(buf));
                    return new SyncStatuses(list);
                }
        );
        @Override public Type<SyncStatuses> type() { return TYPE; }
    }

    public record ProgressMetaEntry(String questId, int claimCount, boolean scrollRedeemed, boolean scrollCreated) {
        public static final StreamCodec<FriendlyByteBuf, ProgressMetaEntry> CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeUtf(e.questId);
                    buf.writeVarInt(e.claimCount);
                    buf.writeBoolean(e.scrollRedeemed);
                    buf.writeBoolean(e.scrollCreated);
                },
                buf -> new ProgressMetaEntry(buf.readUtf(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean())
        );
    }

    public record SyncProgressMeta(List<ProgressMetaEntry> entries) implements CustomPacketPayload {
        public static final Type<SyncProgressMeta> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_progress_meta"));
        public static final StreamCodec<FriendlyByteBuf, SyncProgressMeta> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.entries.size());
                    for (ProgressMetaEntry e : p.entries) ProgressMetaEntry.CODEC.encode(buf, e);
                },
                buf -> {
                    int n = buf.readVarInt();
                    List<ProgressMetaEntry> list = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) list.add(ProgressMetaEntry.CODEC.decode(buf));
                    return new SyncProgressMeta(list);
                }
        );
        @Override public Type<SyncProgressMeta> type() { return TYPE; }
    }

    public record ObjectiveItemEntry(String key, int count) {
        public static final StreamCodec<FriendlyByteBuf, ObjectiveItemEntry> CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeUtf(e.key);
                    buf.writeVarInt(e.count);
                },
                buf -> new ObjectiveItemEntry(buf.readUtf(), buf.readVarInt())
        );
    }

    public record ObjectiveFlagEntry(String key, boolean done) {
        public static final StreamCodec<FriendlyByteBuf, ObjectiveFlagEntry> CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeUtf(e.key);
                    buf.writeBoolean(e.done);
                },
                buf -> new ObjectiveFlagEntry(buf.readUtf(), buf.readBoolean())
        );
    }

    public record ObjectiveInputEntry(String key, String value) {
        public static final StreamCodec<FriendlyByteBuf, ObjectiveInputEntry> CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeUtf(e.key);
                    buf.writeUtf(e.value == null ? "" : e.value);
                },
                buf -> new ObjectiveInputEntry(buf.readUtf(), buf.readUtf())
        );
    }

    public record SyncObjectiveProgress(
            List<ObjectiveItemEntry> items,
            List<ObjectiveFlagEntry> flags,
            List<ObjectiveInputEntry> inputs) implements CustomPacketPayload {
        public static final Type<SyncObjectiveProgress> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_objective_progress"));
        public static final StreamCodec<FriendlyByteBuf, SyncObjectiveProgress> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.items.size());
                    for (ObjectiveItemEntry entry : p.items) ObjectiveItemEntry.CODEC.encode(buf, entry);
                    buf.writeVarInt(p.flags.size());
                    for (ObjectiveFlagEntry entry : p.flags) ObjectiveFlagEntry.CODEC.encode(buf, entry);
                    buf.writeVarInt(p.inputs.size());
                    for (ObjectiveInputEntry entry : p.inputs) ObjectiveInputEntry.CODEC.encode(buf, entry);
                },
                buf -> {
                    int itemCount = buf.readVarInt();
                    List<ObjectiveItemEntry> items = new ArrayList<>(itemCount);
                    for (int i = 0; i < itemCount; i++) items.add(ObjectiveItemEntry.CODEC.decode(buf));
                    int flagCount = buf.readVarInt();
                    List<ObjectiveFlagEntry> flags = new ArrayList<>(flagCount);
                    for (int i = 0; i < flagCount; i++) flags.add(ObjectiveFlagEntry.CODEC.decode(buf));
                    int inputCount = buf.readVarInt();
                    List<ObjectiveInputEntry> inputs = new ArrayList<>(inputCount);
                    for (int i = 0; i < inputCount; i++) inputs.add(ObjectiveInputEntry.CODEC.decode(buf));
                    return new SyncObjectiveProgress(items, flags, inputs);
                }
        );
        @Override public Type<SyncObjectiveProgress> type() { return TYPE; }
    }

    public record KillEntry(String entityId, int count) {
        public static final StreamCodec<FriendlyByteBuf, KillEntry> CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeUtf(e.entityId);
                    buf.writeVarInt(e.count);
                },
                buf -> new KillEntry(buf.readUtf(), buf.readVarInt())
        );
    }



    public record SyncKills(List<KillEntry> entries) implements CustomPacketPayload {
        public static final Type<SyncKills> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_kills"));
        public static final StreamCodec<FriendlyByteBuf, SyncKills> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.entries.size());
                    for (KillEntry e : p.entries) KillEntry.CODEC.encode(buf, e);
                },
                buf -> {
                    int n = buf.readVarInt();
                    List<KillEntry> list = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) list.add(KillEntry.CODEC.decode(buf));
                    return new SyncKills(list);
                }
        );
        @Override public Type<SyncKills> type() { return TYPE; }
    }

    public record SyncClear() implements CustomPacketPayload {
        public static final Type<SyncClear> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_clear"));
        public static final StreamCodec<FriendlyByteBuf, SyncClear> CODEC =
                StreamCodec.of((b, p) -> {}, b -> new SyncClear());
        @Override public Type<SyncClear> type() { return TYPE; }
    }

    public record Toast(String questId) implements CustomPacketPayload {
        public static final Type<Toast> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "toast"));
        public static final StreamCodec<FriendlyByteBuf, Toast> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.questId),
                buf -> new Toast(buf.readUtf())
        );
        @Override public Type<Toast> type() { return TYPE; }
    }

    public record RewardToast(String title, String description, String icon) implements CustomPacketPayload {
        public static final Type<RewardToast> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "reward_toast"));
        public static final StreamCodec<FriendlyByteBuf, RewardToast> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.title == null ? "" : p.title);
                    buf.writeUtf(p.description == null ? "" : p.description);
                    buf.writeUtf(p.icon == null ? "" : p.icon);
                },
                buf -> new RewardToast(buf.readUtf(), buf.readUtf(), buf.readUtf())
        );
        @Override public Type<RewardToast> type() { return TYPE; }
    }

    public record OpenQuestBook() implements CustomPacketPayload {
        public static final Type<OpenQuestBook> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "open_quest_book"));
        public static final StreamCodec<FriendlyByteBuf, OpenQuestBook> CODEC =
                StreamCodec.of((buf, p) -> {}, buf -> new OpenQuestBook());
        @Override public Type<OpenQuestBook> type() { return TYPE; }
    }

    public record SyncConfig(
            List<String> disabledCategories,
            List<String> appliedQuestPacks,
            List<String> disabledQuestPacks,
            String pinnedQuestHudPosition,
            boolean hideQuestBookInInventory,
            String questBookInventoryButtonPosition,
            boolean centerInventoryWithQuestPanel,
            boolean hideCategoryHeader,
            String filterDisplayMode,
            boolean disableCategories,
            boolean enableBuiltinQuestPack,
            boolean hideQuestWidgetIcons,
            double questTextScale,
            double questIconScale,
            boolean enableQuestSearchBox,
            boolean enableDescriptionColors,
            String questWidgetTextColor,
            String descriptionTextColor,
            boolean enableDescriptionReadMore,
            boolean enableDescriptionTextWrapping,
            String descriptionTextAlignment,
            boolean enableQuestToasts,
            boolean disableQuestPinning,
            boolean autoClaimQuestRewards,
            boolean enableQuestScrolls,
            boolean disableQuestBook,
            boolean spawnWithQuestBook) implements CustomPacketPayload {
        public static final Type<SyncConfig> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_config"));
        public static final StreamCodec<FriendlyByteBuf, SyncConfig> CODEC = StreamCodec.of(
                (buf, p) -> {
                    writeStringList(buf, p.disabledCategories);
                    writeStringList(buf, p.appliedQuestPacks);
                    writeStringList(buf, p.disabledQuestPacks);
                    buf.writeUtf(p.pinnedQuestHudPosition == null ? "" : p.pinnedQuestHudPosition);
                    buf.writeBoolean(p.hideQuestBookInInventory);
                    buf.writeUtf(p.questBookInventoryButtonPosition == null ? "" : p.questBookInventoryButtonPosition);
                    buf.writeBoolean(p.centerInventoryWithQuestPanel);
                    buf.writeBoolean(p.hideCategoryHeader);
                    buf.writeUtf(p.filterDisplayMode == null ? "" : p.filterDisplayMode);
                    buf.writeBoolean(p.disableCategories);
                    buf.writeBoolean(p.enableBuiltinQuestPack);
                    buf.writeBoolean(p.hideQuestWidgetIcons);
                    buf.writeDouble(p.questTextScale);
                    buf.writeDouble(p.questIconScale);
                    buf.writeBoolean(p.enableQuestSearchBox);
                    buf.writeBoolean(p.enableDescriptionColors);
                    buf.writeUtf(p.questWidgetTextColor == null ? "" : p.questWidgetTextColor);
                    buf.writeUtf(p.descriptionTextColor == null ? "" : p.descriptionTextColor);
                    buf.writeBoolean(p.enableDescriptionReadMore);
                    buf.writeBoolean(p.enableDescriptionTextWrapping);
                    buf.writeUtf(p.descriptionTextAlignment == null ? "" : p.descriptionTextAlignment);
                    buf.writeBoolean(p.enableQuestToasts);
                    buf.writeBoolean(p.disableQuestPinning);
                    buf.writeBoolean(p.autoClaimQuestRewards);
                    buf.writeBoolean(p.enableQuestScrolls);
                    buf.writeBoolean(p.disableQuestBook);
                    buf.writeBoolean(p.spawnWithQuestBook);
                },
                buf -> new SyncConfig(
                        readStringList(buf),
                        readStringList(buf),
                        readStringList(buf),
                        buf.readUtf(),
                        buf.readBoolean(),
                        buf.readUtf(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readUtf(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readUtf(),
                        buf.readUtf(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readUtf(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean())
        );
        @Override public Type<SyncConfig> type() { return TYPE; }
    }

    public record SyncQuestsChunk(int syncId, int totalParts, int index, byte[] part) implements CustomPacketPayload {
        public static final Type<SyncQuestsChunk> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_quests_chunk"));
        public static final StreamCodec<FriendlyByteBuf, SyncQuestsChunk> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.syncId);
                    buf.writeVarInt(p.totalParts);
                    buf.writeVarInt(p.index);
                    buf.writeVarInt(p.part.length);
                    buf.writeBytes(p.part);
                },
                buf -> {
                    int syncId = buf.readVarInt();
                    int total = buf.readVarInt();
                    int idx = buf.readVarInt();
                    int len = buf.readVarInt();
                    if (len < 0 || len > 1_200_000) throw new IllegalArgumentException("chunk len " + len);
                    byte[] bytes = new byte[len];
                    buf.readBytes(bytes);
                    return new SyncQuestsChunk(syncId, total, idx, bytes);
                }
        );
        @Override public Type<SyncQuestsChunk> type() { return TYPE; }
    }

    // push full quest and progress state to one player
    public static void syncPlayer(ServerPlayer p) {
        long startedAt = BoundlessDebug.enabled() ? System.nanoTime() : 0L;
        clearObjectiveProgressCache(p);
        PacketDistributor.sendToPlayer(p, new SyncClear());
        sendConfig(p);
        sendQuestData(p);
        syncPlayerProgress(p);
        if (startedAt != 0L) {
            BoundlessDebug.rateLimited("player-sync:" + p.getUUID(), 2_000L,
                    "player={}, elapsed={}ms", p.getGameProfile().getName(), (System.nanoTime() - startedAt) / 1_000_000L);
        }
    }

    // sync quest statuses progress kills and config
    private static void syncPlayerProgress(ServerPlayer p) {
        if (p == null) return;
        long startedAt = BoundlessDebug.enabled() ? System.nanoTime() : 0L;

        List<KillEntry> killEntries = new ArrayList<>();
        KillCounterState.get(p.serverLevel()).snapshotFor(p.getUUID())
                .forEach((id, ct) -> killEntries.add(new KillEntry(id, ct)));
        if (!killEntries.isEmpty()) {
            PacketDistributor.sendToPlayer(p, new SyncKills(killEntries));
        }

        List<StatusEntry> statuses = new ArrayList<>();
        QuestProgressState.get(p.serverLevel()).snapshotFor(p.getUUID())
                .forEach((questId, status) -> statuses.add(new StatusEntry(questId, status)));
        if (!statuses.isEmpty()) {
            PacketDistributor.sendToPlayer(p, new SyncStatuses(statuses));
        }

        List<ProgressMetaEntry> metaEntries = new ArrayList<>();
        QuestProgressState.get(p.serverLevel()).progressSnapshotFor(p.getUUID())
                .forEach((questId, progress) -> metaEntries.add(new ProgressMetaEntry(
                        questId,
                        progress == null ? 0 : progress.claimCount(),
                        progress != null && progress.scrollRedeemed(),
                        progress != null && progress.scrollCreated()
                )));
        if (!metaEntries.isEmpty()) {
            PacketDistributor.sendToPlayer(p, new SyncProgressMeta(metaEntries));
        }

        sendObjectiveProgress(p);

        if (startedAt != 0L) {
            BoundlessDebug.rateLimited("player-progress-sync:" + p.getUUID(), 2_000L,
                    "player={}, kills={}, statuses={}, meta={}, elapsed={}ms", p.getGameProfile().getName(), killEntries.size(), statuses.size(), metaEntries.size(),
                    (System.nanoTime() - startedAt) / 1_000_000L);
        }
    }

    // sync objective counters only when they changed
    public static void sendObjectiveProgress(ServerPlayer player) {
        if (player == null) return;
        QuestObjectiveState objectiveState = QuestObjectiveState.get(player.serverLevel());
        Map<String, Integer> itemSnapshot = sanitizeObjectiveItems(objectiveState.itemSnapshotFor(player.getUUID()));
        Map<String, Boolean> flagSnapshot = sanitizeObjectiveFlags(objectiveState.flagSnapshotFor(player.getUUID()));
        Map<String, String> inputSnapshot = sanitizeObjectiveInputs(objectiveState.inputSnapshotFor(player.getUUID()));
        ObjectiveProgressSnapshot next = new ObjectiveProgressSnapshot(itemSnapshot, flagSnapshot, inputSnapshot);
        ObjectiveProgressSnapshot previous = LAST_OBJECTIVE_SYNC.put(player.getUUID(), next);
        if (next.equals(previous)) return;

        List<ObjectiveItemEntry> objectiveItems = new ArrayList<>(itemSnapshot.size());
        itemSnapshot.forEach((key, count) -> objectiveItems.add(new ObjectiveItemEntry(key, count)));
        List<ObjectiveFlagEntry> objectiveFlags = new ArrayList<>(flagSnapshot.size());
        flagSnapshot.forEach((key, done) -> objectiveFlags.add(new ObjectiveFlagEntry(key, done)));
        List<ObjectiveInputEntry> objectiveInputs = new ArrayList<>(inputSnapshot.size());
        inputSnapshot.forEach((key, value) -> objectiveInputs.add(new ObjectiveInputEntry(key, value)));
        PacketDistributor.sendToPlayer(player, new SyncObjectiveProgress(objectiveItems, objectiveFlags, objectiveInputs));
        BoundlessDebug.rateLimited("objective-sync:" + player.getUUID(), 2_000L,
                "player={}, itemEntries={}, flagEntries={}, inputEntries={}",
                player.getGameProfile().getName(), objectiveItems.size(), objectiveFlags.size(), objectiveInputs.size());
    }

    // clear cached objective sync state for one player
    public static void clearObjectiveProgressCache(ServerPlayer player) {
        if (player == null) return;
        LAST_OBJECTIVE_SYNC.remove(player.getUUID());
    }

    // normalize synced item objective values
    private static Map<String, Integer> sanitizeObjectiveItems(Map<String, Integer> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) return Map.of();
        Map<String, Integer> sanitized = new HashMap<>();
        snapshot.forEach((key, count) -> {
            if (key == null || key.isBlank()) return;
            int value = Math.max(0, count == null ? 0 : count);
            if (value > 0) sanitized.put(key, value);
        });
        return sanitized.isEmpty() ? Map.of() : Collections.unmodifiableMap(sanitized);
    }

    // normalize synced flag objective values
    private static Map<String, Boolean> sanitizeObjectiveFlags(Map<String, Boolean> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) return Map.of();
        Map<String, Boolean> sanitized = new HashMap<>();
        snapshot.forEach((key, done) -> {
            if (key == null || key.isBlank() || !Boolean.TRUE.equals(done)) return;
            sanitized.put(key, true);
        });
        return sanitized.isEmpty() ? Map.of() : Collections.unmodifiableMap(sanitized);
    }

    // normalize synced input objective values
    private static Map<String, String> sanitizeObjectiveInputs(Map<String, String> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) return Map.of();
        Map<String, String> sanitized = new HashMap<>();
        snapshot.forEach((key, value) -> {
            if (key == null || key.isBlank()) return;
            String normalized = value == null ? "" : value.trim();
            if (!normalized.isBlank()) sanitized.put(key, normalized);
        });
        return sanitized.isEmpty() ? Map.of() : Collections.unmodifiableMap(sanitized);
    }

    // send the authority config snapshot to one player
    private static void sendConfig(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new SyncConfig(
                configStringList(Config.disabledCategories()),
                configStringList(Config.appliedQuestPacks()),
                configStringList(Config.disabledQuestPacks()),
                Config.pinnedQuestHudPosition(),
                Config.hideQuestBookInInventory(),
                Config.questBookInventoryButtonPosition(),
                Config.centerInventoryWithQuestPanel(),
                Config.hideCategoryHeader(),
                Config.filterDisplayMode(),
                Config.disableCategories(),
                Config.enableBuiltinQuestPack(),
                Config.hideQuestWidgetIcons(),
                Config.questTextScale(),
                Config.questIconScale(),
                Config.enableQuestSearchBox(),
                Config.enableDescriptionColors(),
                String.format(java.util.Locale.ROOT, "%06X", Config.questWidgetTextColor()),
                String.format(java.util.Locale.ROOT, "%06X", Config.descriptionTextColor()),
                Config.enableDescriptionReadMore(),
                Config.enableDescriptionTextWrapping(),
                Config.descriptionTextAlignment(),
                Config.enableQuestToasts(),
                Config.disableQuestPinning(),
                Config.autoClaimQuestRewards(),
                Config.enableQuestScrolls(),
                Config.disableQuestBook(),
                Config.spawnWithQuestBook()
        ));
    }

    // copy config string lists into stable packet data
    private static List<String> configStringList(List<? extends String> values) {
        if (values == null || values.isEmpty()) return List.of();
        List<String> out = new ArrayList<>(values.size());
        for (String value : values) {
            if (value != null) out.add(value);
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static void writeStringList(FriendlyByteBuf buf, List<String> values) {
        List<String> safe = values == null ? List.of() : values;
        buf.writeVarInt(safe.size());
        for (String value : safe) {
            buf.writeUtf(value == null ? "" : value);
        }
    }

    private static List<String> readStringList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > 4096) throw new IllegalArgumentException("string list size " + size);
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(buf.readUtf());
        }
        return values;
    }

    // send one quest claim and scroll meta update
    public static void sendProgressMeta(ServerPlayer player, String questId) {
        if (player == null || questId == null || questId.isBlank()) return;
        var progress = QuestProgressState.get(player.serverLevel()).progress(player.getUUID(), questId);
        PacketDistributor.sendToPlayer(player, new SyncProgressMeta(List.of(
                new ProgressMetaEntry(questId, progress.claimCount(), progress.scrollRedeemed(), progress.scrollCreated())
        )));
    }

    // sync full authoritative quest definitions
    private static void sendQuestData(ServerPlayer p) {
        sendQuestData(List.of(p));
    }

    // send the full authoritative quest definition snapshot
    private static void sendQuestData(List<ServerPlayer> players) {
        if (players == null || players.isEmpty()) return;
        long startedAt = BoundlessDebug.enabled() ? System.nanoTime() : 0L;
        ServerPlayer first = players.get(0);
        if (first == null || first.server == null) return;
        String json = buildQuestSyncJson(first.server);
        for (ServerPlayer player : players) {
            if (player != null) sendQuestJsonChunked(player, json);
        }
        if (startedAt != 0L) {
            BoundlessDebug.rateLimited("quest-data-sync", 2_000L,
                    "players={}, questBytes={}, chunksPerPlayer={}, elapsed={}ms",
                    players.size(), json.getBytes(StandardCharsets.UTF_8).length,
                    Math.max(1, (json.getBytes(StandardCharsets.UTF_8).length + QUEST_CHUNK_BYTES - 1) / QUEST_CHUNK_BYTES),
                    (System.nanoTime() - startedAt) / 1_000_000L);
        }
    }

    // build the authoritative quest json snapshot
    private static String buildQuestSyncJson(MinecraftServer server) {
        var quests = QuestData.allServer(server);
        var categories = QuestData.categoriesOrderedServer(server);
        var subCats = QuestData.subCategoriesAllOrderedServer(server);

        JsonObject root = new JsonObject();

        JsonArray cats = new JsonArray();
        for (QuestData.Category c : categories) {
            JsonObject o = new JsonObject();
            o.addProperty("id", c.id);
            o.addProperty("icon", c.icon);
            o.addProperty("name", c.name);
            o.addProperty("order", c.order);
            o.addProperty("excludeFromAll", c.excludeFromAll);
            o.addProperty("dependency", c.dependency);
            o.addProperty("autoComplete", c.autoComplete);
            cats.add(o);
        }
        root.add("categories", cats);

        JsonArray scs = new JsonArray();
        for (QuestData.SubCategory sc : subCats) {
            JsonObject o = new JsonObject();
            o.addProperty("id", sc.id);
            o.addProperty("category", sc.category);
            o.addProperty("icon", sc.icon);
            o.addProperty("name", sc.name);
            o.addProperty("order", sc.order);
            o.addProperty("defaultOpen", sc.defaultOpen);
            if (sc.sourcePath != null && !sc.sourcePath.isBlank()) {
                o.addProperty("sourcePath", sc.sourcePath);
            }

            JsonArray qids = new JsonArray();
            for (String qid : sc.quests) qids.add(qid);
            o.add("quests", qids);

            scs.add(o);
        }
        root.add("subCategories", scs);

        JsonArray qs = new JsonArray();
        for (QuestData.Quest q : quests) {
            JsonObject o = new JsonObject();
            o.addProperty("id", q.id);
            o.addProperty("name", q.name);
            o.addProperty("icon", q.icon);
            o.addProperty("description", q.description);

            JsonArray deps = new JsonArray();
            for (String d : q.dependencies) deps.add(d);
            o.add("dependencies", deps);

            o.addProperty("optional", q.optional);
            o.addProperty("repeatable", q.repeatable);
            o.addProperty("hiddenUnderDependency", q.hiddenUnderDependency);

            if (q.rewards != null) {
                JsonObject ro = new JsonObject();

                JsonArray items = new JsonArray();
                for (QuestData.RewardEntry r : q.rewards.items) {
                    JsonObject io = new JsonObject();
                    io.addProperty("item", r.item);
                    if (r.acceptedItemsOrLegacy().size() > 1) {
                        JsonArray acceptedItems = new JsonArray();
                        for (String acceptedId : r.acceptedItemsOrLegacy()) {
                            acceptedItems.add(acceptedId);
                        }
                        io.add("acceptedItems", acceptedItems);
                    }
                    io.addProperty("count", r.count);
                    items.add(io);
                }
                ro.add("items", items);

                JsonArray cmds = new JsonArray();
                for (QuestData.CommandReward cr : q.rewards.commands) {
                    JsonObject co = new JsonObject();
                    co.addProperty("command", cr.command);
                    co.addProperty("icon", cr.icon);
                    co.addProperty("title", cr.title);
                    cmds.add(co);
                }
                ro.add("commands", cmds);

                JsonArray fns = new JsonArray();
                for (QuestData.FunctionReward fr : q.rewards.functions) {
                    JsonObject fo = new JsonObject();
                    fo.addProperty("function", fr.function);
                    fo.addProperty("icon", fr.icon);
                    fo.addProperty("title", fr.title);
                    fns.add(fo);
                }
                ro.add("functions", fns);

                JsonArray lootTables = new JsonArray();
                for (QuestData.LootTableReward lr : q.rewards.lootTables) {
                    JsonObject lo = new JsonObject();
                    lo.addProperty("lootTable", lr.lootTable);
                    lo.addProperty("icon", lr.icon);
                    lo.addProperty("title", lr.title);
                    lootTables.add(lo);
                }
                ro.add("lootTables", lootTables);

                JsonArray advancements = new JsonArray();
                for (QuestData.AdvancementReward ar : q.rewards.advancements) {
                    JsonObject ao = new JsonObject();
                    ao.addProperty("advancement", ar.advancement);
                    advancements.add(ao);
                }
                ro.add("advancements", advancements);

                JsonArray toasts = new JsonArray();
                for (QuestData.ToastReward tr : q.rewards.toasts) {
                    JsonObject to = new JsonObject();
                    to.addProperty("title", tr.title);
                    to.addProperty("description", tr.description);
                    to.addProperty("icon", tr.icon);
                    toasts.add(to);
                }
                ro.add("toasts", toasts);

                ro.addProperty("expType", q.rewards.expType);
                ro.addProperty("expAmount", q.rewards.expAmount);

                o.add("rewards", ro);
            }

            o.addProperty("type", q.type);

            if (q.completion != null) {
                JsonObject co = new JsonObject();
                JsonArray targets = new JsonArray();
                for (QuestData.Target t : q.completion.targets) {
                    JsonObject to = new JsonObject();
                    to.addProperty("kind", t.kind);
                    to.addProperty("id", t.id);
                    if (t.isItem() || t.isSubmit()) {
                        JsonArray acceptedItems = new JsonArray();
                        for (String acceptedId : t.acceptedIdsOrLegacy()) {
                            acceptedItems.add(acceptedId);
                        }
                        to.add("acceptedItems", acceptedItems);
                    } else if (t.isEntity()) {
                        JsonArray acceptedMobs = new JsonArray();
                        for (String acceptedId : t.acceptedIdsOrLegacy()) {
                            acceptedMobs.add(acceptedId);
                        }
                        to.add("acceptedMobs", acceptedMobs);
                    }
                    to.addProperty("count", t.count);
                    if (t.hint != null && !t.hint.isBlank()) {
                        to.addProperty("hint", t.hint);
                    }
                    targets.add(to);
                }
                co.add("targets", targets);
                o.add("completion", co);
            }

            o.addProperty("category", q.category);

            if (q.subCategory != null && !q.subCategory.isBlank()) {
                o.addProperty("subCategory", q.subCategory);
            }
            if (q.sourcePath != null && !q.sourcePath.isBlank()) {
                o.addProperty("sourcePath", q.sourcePath);
            }

            qs.add(o);
        }

        root.add("quests", qs);

        return GSON.toJson(root);
    }


    // split large quest json into client packet chunks
    private static void sendQuestJsonChunked(ServerPlayer p, String json) {
        if (json == null) json = "";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        int syncId = SYNC_ID_GEN.incrementAndGet();

        int total = (bytes.length + QUEST_CHUNK_BYTES - 1) / QUEST_CHUNK_BYTES;
        if (total <= 0) total = 1;

        for (int i = 0; i < total; i++) {
            int start = i * QUEST_CHUNK_BYTES;
            int end = Math.min(bytes.length, start + QUEST_CHUNK_BYTES);
            byte[] part = start >= end ? new byte[0] : java.util.Arrays.copyOfRange(bytes, start, end);
            PacketDistributor.sendToPlayer(p, new SyncQuestsChunk(syncId, total, i, part));
        }
        BoundlessDebug.rateLimited("quest-chunks:" + p.getUUID(), 2_000L,
                "player={}, syncId={}, bytes={}, chunks={}", p.getGameProfile().getName(), syncId, bytes.length, total);
    }

    // sync one quest status to one player
    public static void sendStatus(ServerPlayer p, String questId, String status) {
        PacketDistributor.sendToPlayer(p, new SyncStatus(questId, status));
    }

    // send a quest unlock toast to one player
    public static void sendToast(ServerPlayer p, String questId) {
        PacketDistributor.sendToPlayer(p, new Toast(questId));
    }

    // send a reward toast to one player
    public static void sendRewardToast(ServerPlayer p, String title, String description, String icon) {
        PacketDistributor.sendToPlayer(p, new RewardToast(title, description, icon));
    }

    // open the standalone quest book for one player
    public static void sendOpenQuestBook(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new OpenQuestBook());
    }

    // show a local client toast without networking
    public static void sendToastLocal(String questId) {
        QuestData.byId(questId).ifPresent(q ->
                QuestUnlockedToast.show(q.name, q.iconItem().orElse(null))
        );
    }

    // redeem quests on the server authority
    private static void handleRedeem(Redeem p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            QuestData.byIdServer(sp.server, p.questId()).ifPresent(q -> {
                if (QuestTracker.canAcknowledge(q, sp)) {
                    if (!QuestTracker.acknowledgeCheckObjectives(q, sp)) return;
                    sendObjectiveProgress(sp);
                    if (!QuestTracker.updateProgressAndCheckReady(q, sp)) return;
                    if (Config.autoClaimQuestRewards()) {
                        claimQuest(sp, q);
                    } else {
                        QuestTracker.setServerStatus(sp, q.id, QuestTracker.Status.COMPLETED);
                        sendStatus(sp, q.id, QuestTracker.Status.COMPLETED.name());
                    }
                    return;
                }
                if (!QuestTracker.updateProgressAndCheckReady(q, sp)) return;
                claimQuest(sp, q);
            });
        });
    }

    // reject optional quests on the server authority
    private static void handleReject(Reject p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            QuestData.byIdServer(sp.server, p.questId()).ifPresent(q -> {
                if (QuestTracker.serverReject(q, sp)) {
                    QuestTracker.setServerStatus(sp, q.id, QuestTracker.Status.REJECTED);
                    sendStatus(sp, q.id, QuestTracker.Status.REJECTED.name());
                }
            });
        });
    }

    // create quest scrolls on the server authority
    private static void handleCreateScroll(CreateScroll p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            if (!Config.enableQuestScrolls()) return;
            QuestData.byIdServer(sp.server, p.questId()).ifPresent(q -> {
                if (!QuestTracker.canCreateScroll(q, sp)) return;
                QuestProgressState.get(sp.serverLevel()).setScrollCreated(sp.getUUID(), q.id, true);
                ItemStack stack = ModItems.createQuestScroll(q.id);
                if (!sp.getInventory().add(stack) && !stack.isEmpty()) {
                    sp.drop(stack, false);
                }
                sendProgressMeta(sp, q.id);
            });
        });
    }

    // undo rejected quests on the server authority
    private static void handleUndoReject(UndoReject p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            QuestData.byIdServer(sp.server, p.questId()).ifPresent(q -> {
                if (QuestTracker.serverUndoReject(q, sp)) {
                    sendStatus(sp, q.id, QuestTracker.Status.INCOMPLETE.name());
                }
            });
        });
    }

    // restart repeatable quests on the server authority
    private static void handleRestartRepeatable(RestartRepeatable p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            QuestData.byIdServer(sp.server, p.questId()).ifPresent(q -> {
                if (QuestTracker.restartRepeatable(q, sp)) {
                    sendStatus(sp, q.id, QuestTracker.Status.INCOMPLETE.name());
                }
            });
        });
    }

    // store field input progress on the server authority
    private static void handleUpdateFieldInput(UpdateFieldInput p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            if (sp == null || p.questId() == null || p.questId().isBlank() || p.targetId() == null || p.targetId().isBlank()) return;
            QuestData.Quest quest = QuestData.byIdServer(sp.server, p.questId()).orElse(null);
            if (quest == null || quest.completion == null || quest.completion.targets == null) return;
            boolean validFieldTarget = false;
            for (QuestData.Target t : quest.completion.targets) {
                if (t == null || !t.isFieldInput()) continue;
                if (!p.targetId().equals(t.id)) continue;
                validFieldTarget = true;
                break;
            }
            if (!validFieldTarget) return;
            String key = p.questId() + ":field:" + p.targetId();
            QuestTracker.setFieldInputProgress(sp, key, p.value());
            QuestTracker.markServerStateDirty(sp);
            QuestTracker.serverTickPlayer(sp);
            sendObjectiveProgress(sp);
        });
    }

    // toggle quest packs on the current authority
    private static void handleSetQuestPackEnabled(SetQuestPackEnabled p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            if (sp == null || !sp.createCommandSourceStack().hasPermission(2)) return;

            if (p.builtin()) {
                Config.ENABLE_BUILTIN_QUEST_PACK.set(p.enabled());
                Config.SPEC.save();
            } else {
                String id = p.id() == null ? "" : p.id().trim();
                if (id.isBlank()) return;
                if (id.contains("/") || id.contains("\\")) return;
                Path packRoot = INSTANCE_QUEST_PACKS_ROOT.resolve(id).normalize();
                if (!packRoot.startsWith(INSTANCE_QUEST_PACKS_ROOT) || !Files.isDirectory(packRoot)) return;
                Config.setQuestPackApplied(id, p.enabled());
            }

            QuestData.loadServer(sp.server, true);
            for (ServerPlayer player : sp.server.getPlayerList().getPlayers()) {
                syncPlayer(player);
            }
        });
    }

    // apply synced config edits on the server authority
    private static void handleUpdateServerConfig(UpdateServerConfig p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            if (sp == null || !sp.createCommandSourceStack().hasPermission(2)) return;

            Config.PINNED_QUEST_HUD_POSITION.set(p.pinnedQuestHudPosition());
            Config.HIDE_QUEST_BOOK_IN_INVENTORY.set(p.hideQuestBookInInventory());
            Config.QUEST_BOOK_INVENTORY_BUTTON_POSITION.set(p.questBookInventoryButtonPosition());
            Config.CENTER_INVENTORY_WITH_QUEST_PANEL.set(p.centerInventoryWithQuestPanel());
            Config.HIDE_CATEGORY_HEADER.set(p.hideCategoryHeader());
            Config.FILTER_DISPLAY_MODE.set(p.filterDisplayMode());
            Config.DISABLE_CATEGORIES.set(p.disableCategories());
            Config.HIDE_QUEST_WIDGET_ICONS.set(p.hideQuestWidgetIcons());
            Config.QUEST_TEXT_SCALE.set(Math.max(0.5D, Math.min(1.0D, p.questTextScale())));
            Config.QUEST_ICON_SCALE.set(Math.max(0.5D, Math.min(1.0D, p.questIconScale())));
            Config.ENABLE_QUEST_SEARCH_BOX.set(p.enableQuestSearchBox());
            Config.ENABLE_DESCRIPTION_COLORS.set(p.enableDescriptionColors());
            Config.QUEST_WIDGET_TEXT_COLOR.set(p.questWidgetTextColor());
            Config.DESCRIPTION_TEXT_COLOR.set(p.descriptionTextColor());
            Config.ENABLE_DESCRIPTION_READ_MORE.set(p.enableDescriptionReadMore());
            Config.ENABLE_DESCRIPTION_TEXT_WRAPPING.set(p.enableDescriptionTextWrapping());
            Config.DESCRIPTION_TEXT_ALIGNMENT.set(p.descriptionTextAlignment());
            Config.ENABLE_QUEST_TOASTS.set(p.enableQuestToasts());
            Config.DISABLE_QUEST_PINNING.set(p.disableQuestPinning());
            Config.AUTO_CLAIM_QUEST_REWARDS.set(p.autoClaimQuestRewards());
            Config.ENABLE_QUEST_SCROLLS.set(p.enableQuestScrolls());
            Config.DISABLE_QUEST_BOOK.set(p.disableQuestBook());
            Config.SPAWN_WITH_QUEST_BOOK.set(p.spawnWithQuestBook());
            Config.SPEC.save();

            for (ServerPlayer player : sp.server.getPlayerList().getPlayers()) {
                sendConfig(player);
            }
        });
    }

    // accept quest pack upload chunks from the editor
    private static void handleUploadQuestPackChunk(UploadQuestPackChunk p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            if (sp == null || !sp.createCommandSourceStack().hasPermission(2)) return;
            String id = normalizeQuestPackFolderName(p.id());
            if (id.isBlank()) return;
            if (p.totalParts() <= 0 || p.totalParts() > 65536) return;
            if (p.index() < 0 || p.index() >= p.totalParts()) return;

            String key = sp.getUUID() + ":" + id + ":" + p.uploadId();
            QuestPackUploadSession session = QUESTPACK_UPLOADS.compute(key, (ignored, existing) -> {
                if (existing == null || existing.totalParts != p.totalParts()) {
                    return new QuestPackUploadSession(id, p.enabled(), p.totalParts());
                }
                return existing;
            });
            if (session == null) return;
            if (session.parts[p.index()] == null) {
                session.parts[p.index()] = p.part() == null ? new byte[0] : p.part();
                session.received++;
            }
            if (session.received < session.totalParts) return;

            QUESTPACK_UPLOADS.remove(key);
            byte[] zipBytes = session.join();
            try {
                writeUploadedQuestPack(id, zipBytes);
                Config.setQuestPackApplied(id, session.enabled);
                reloadAndSyncAll(sp);
            } catch (IOException ignored) {
            }
        });
    }

    // delete quest packs on the server authority
    private static void handleDeleteQuestPack(DeleteQuestPack p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            if (sp == null || !sp.createCommandSourceStack().hasPermission(2)) return;
            String id = normalizeQuestPackFolderName(p.id());
            if (id.isBlank()) return;
            try {
                deleteDirectoryIfExists(INSTANCE_QUEST_PACKS_ROOT.resolve(id).normalize());
                Config.setQuestPackApplied(id, false);
                reloadAndSyncAll(sp);
            } catch (IOException ignored) {
            }
        });
    }

    // mark observe targets on the server authority
    private static void handleReportObserve(ReportObserve p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            if (sp == null || p.questId() == null || p.questId().isBlank() || p.targetId() == null || p.targetId().isBlank()) return;
            QuestData.byIdServer(sp.server, p.questId()).ifPresent(q -> {
                if (q.completion == null || q.completion.targets == null) return;
                for (QuestData.Target target : q.completion.targets) {
                    if (target == null || !target.isObserve()) continue;
                    if (!p.targetId().equals(target.id)) continue;
                    String key = QuestTracker.flagProgressKey(q, target);
                    if (QuestTracker.markFlagProgress(sp, key)) {
                        BoundlessDebug.rateLimited("observe-report", 2_000L,
                                "player={}, quest={}, target={}", sp.getGameProfile().getName(), q.id, target.id);
                        QuestTracker.markServerStateDirty(sp);
                        sendObjectiveProgress(sp);
                        QuestTracker.serverTickPlayer(sp);
                    }
                    break;
                }
            });
        });
    }

    // reload quests and resync every connected player
    private static void reloadAndSyncAll(ServerPlayer sp) {
        QuestData.loadServer(sp.server, true);
        List<ServerPlayer> players = sp.server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            PacketDistributor.sendToPlayer(player, new SyncClear());
            sendConfig(player);
        }
        sendQuestData(players);
        for (ServerPlayer player : players) {
            syncPlayerProgress(player);
        }
    }

    // normalize uploaded pack folder names
    private static String normalizeQuestPackFolderName(String raw) {
        String id = raw == null ? "" : raw.trim();
        if (id.isBlank()) return "";
        if (".".equals(id) || "..".equals(id)) return "";
        if (id.contains("/") || id.contains("\\") || id.matches(".*[<>:\"|?*].*")) return "";
        String lower = id.toLowerCase(java.util.Locale.ROOT);
        if (id.startsWith(".") || lower.endsWith(".upload") || lower.endsWith(".tmp") || lower.endsWith(".temp")) return "";
        return id;
    }

    // unpack uploaded quest packs into authoritative storage
    private static void writeUploadedQuestPack(String id, byte[] zipBytes) throws IOException {
        Files.createDirectories(INSTANCE_QUEST_PACKS_ROOT);
        QuestPackStorage.recoverStagedQuestPacks(INSTANCE_QUEST_PACKS_ROOT);
        Path targetRoot = INSTANCE_QUEST_PACKS_ROOT.resolve(id).normalize();
        if (!targetRoot.startsWith(INSTANCE_QUEST_PACKS_ROOT)) throw new IOException("Invalid questpack path");
        if (targetRoot.equals(INSTANCE_QUEST_PACKS_ROOT)) throw new IOException("Invalid questpack path");

        Path tempRoot = INSTANCE_QUEST_PACKS_ROOT.resolve("." + id + ".upload").normalize();
        deleteDirectoryIfExists(tempRoot);
        Files.createDirectories(tempRoot);

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes == null ? new byte[0] : zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null || name.isBlank()) continue;
                Path dst = tempRoot.resolve(name).normalize();
                if (!dst.startsWith(tempRoot)) throw new IOException("Invalid questpack zip entry");
                if (entry.isDirectory()) {
                    Files.createDirectories(dst);
                } else {
                    Path parent = dst.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    Files.copy(zis, dst, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }

        QuestPackStorage.replaceDirectoryWithArchive(targetRoot, tempRoot, id, "uploaded");
    }

    // delete an extracted quest pack directory tree
    private static void deleteDirectoryIfExists(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        Path normalized = root.normalize();
        if (!normalized.startsWith(INSTANCE_QUEST_PACKS_ROOT)) throw new IOException("Invalid questpack path");
        if (normalized.equals(INSTANCE_QUEST_PACKS_ROOT)) throw new IOException("Invalid questpack path");
        try (var walk = Files.walk(normalized)) {
            List<Path> paths = new ArrayList<>();
            for (Path path : (Iterable<Path>) walk::iterator) {
                paths.add(path);
            }
            paths.sort((a, b) -> b.getNameCount() - a.getNameCount());
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }

    // apply one quest status sync on clients
    private static void handleSyncStatus(SyncStatus p, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                QuestTracker.clientSetStatus(p.questId(), QuestTracker.decodeStatus(p.status()))
        );
    }

    // apply bulk quest status sync on clients
    private static void handleSyncStatuses(SyncStatuses p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            for (StatusEntry e : p.entries()) {
                QuestTracker.clientSetStatus(e.questId(), QuestTracker.decodeStatus(e.status()));
            }
        });
    }

    // apply quest meta sync on clients
    private static void handleSyncProgressMeta(SyncProgressMeta p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            for (ProgressMetaEntry e : p.entries()) {
                QuestTracker.clientSetClaimCount(e.questId(), e.claimCount());
                QuestTracker.clientSetScrollRedeemed(e.questId(), e.scrollRedeemed());
                QuestTracker.clientSetScrollCreated(e.questId(), e.scrollCreated());
            }
        });
    }

    // apply objective progress sync on clients
    private static void handleSyncObjectiveProgress(SyncObjectiveProgress p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            BoundlessDebug.rateLimited("client-objective-apply", 2_000L,
                    "itemEntries={}, flagEntries={}, inputEntries={}", p.items().size(), p.flags().size(), p.inputs().size());
            for (ObjectiveItemEntry entry : p.items()) {
                QuestTracker.clientSetItemProgress(entry.key(), entry.count());
            }
            for (ObjectiveFlagEntry entry : p.flags()) {
                QuestTracker.clientSetFlagProgress(entry.key(), entry.done());
            }
            for (ObjectiveInputEntry entry : p.inputs()) {
                QuestTracker.clientSetInputProgress(entry.key(), entry.value());
            }
        });
    }

    // apply kill counter sync on clients
    private static void handleSyncKills(SyncKills p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            for (KillEntry e : p.entries())
                QuestTracker.clientSetKill(e.entityId(), e.count());
        });
    }

    // clear client progress and remote quest data
    private static void handleSyncClear(SyncClear p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            QuestTracker.clientClearAll();
            QuestData.clearClientNetworkData();
            ClientQuestSync.clear();
        });
    }

    // show quest unlock toasts on clients
    private static void handleToast(Toast p, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                QuestData.byId(p.questId()).ifPresent(q ->
                        QuestUnlockedToast.show(q.name, q.iconItem().orElse(null))
                )
        );
    }

    // show reward toasts on clients
    private static void handleRewardToast(RewardToast p, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                QuestUnlockedToast.showCustom(p.title(), p.description(), p.icon())
        );
    }

    private static void handleOpenQuestBook(OpenQuestBook p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player().level().isClientSide() && !Config.disableQuestBook()) {
                ClientOnly.openQuestBook();
            }
        });
    }

    private static void handleSyncConfig(SyncConfig p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Config.applySyncedFromServer(
                p.disabledCategories(),
                p.appliedQuestPacks(),
                p.disabledQuestPacks(),
                p.pinnedQuestHudPosition(),
                p.hideQuestBookInInventory(),
                p.questBookInventoryButtonPosition(),
                p.centerInventoryWithQuestPanel(),
                p.hideCategoryHeader(),
                p.filterDisplayMode(),
                p.disableCategories(),
                p.enableBuiltinQuestPack(),
                p.hideQuestWidgetIcons(),
                p.questTextScale(),
                p.questIconScale(),
                p.enableQuestSearchBox(),
                p.enableDescriptionColors(),
                p.questWidgetTextColor(),
                p.descriptionTextColor(),
                p.enableDescriptionReadMore(),
                p.enableDescriptionTextWrapping(),
                p.descriptionTextAlignment(),
                p.enableQuestToasts(),
                p.disableQuestPinning(),
                p.autoClaimQuestRewards(),
                p.enableQuestScrolls(),
                p.disableQuestBook(),
                p.spawnWithQuestBook()
            );
            ClientOnly.applyConfigChanges();
        });
    }

    private static void handleSyncQuestsChunk(SyncQuestsChunk p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientQuestSync.accept(p));
    }

    private static boolean questHasSubmit(QuestData.Quest q) {
        if (q == null || q.completion == null) return false;

        if ("submission".equalsIgnoreCase(q.type) || "submit".equalsIgnoreCase(q.type)) return true;

        for (QuestData.Target t : q.completion.targets) {
            if (isSubmitTarget(q, t)) return true;
        }
        return false;
    }

    private static boolean isSubmitTarget(QuestData.Quest q, QuestData.Target t) {
        if (t == null) return false;
        return "submit".equalsIgnoreCase(t.kind)
                || "xp".equalsIgnoreCase(t.kind)
                || (("submission".equalsIgnoreCase(q.type) || "submit".equalsIgnoreCase(q.type)) && t.isItem());
    }

    public static boolean claimQuest(ServerPlayer sp, QuestData.Quest q) {
        if (sp == null || q == null) return false;
        String lockKey = sp.getUUID() + ":" + q.id;
        if (!REDEEM_IN_FLIGHT.add(lockKey)) return false;
        try {
            QuestTracker.Status status = QuestTracker.getStatus(q, sp);
            if (status == QuestTracker.Status.REDEEMED || status == QuestTracker.Status.REJECTED) return false;
            if (!QuestTracker.updateProgressAndCheckReady(q, sp)) return false;
            if (questHasSubmit(q) && !consumeSubmitTargets(sp, q)) return false;
            boolean ok;
            try {
                ok = QuestTracker.serverRedeem(q, sp);
            } catch (Throwable ignored) {
                ok = false;
            }
            if (!ok) return false;
            sendStatus(sp, q.id, QuestTracker.Status.REDEEMED.name());
            sendProgressMeta(sp, q.id);
            return true;
        } finally {
            REDEEM_IN_FLIGHT.remove(lockKey);
        }
    }

    private static boolean consumeSubmitTargets(ServerPlayer sp, QuestData.Quest q) {
        if (sp == null || q == null || q.completion == null) return false;

        Inventory inv = sp.getInventory();
        int size = inv.getContainerSize();
        ItemStack[] sim = new ItemStack[size];
        for (int i = 0; i < size; i++) sim[i] = inv.getItem(i).copy();
        HolderLookup.Provider registries = sp.registryAccess();
        QuestTracker.ExperienceSnapshot simulatedXp =
                new QuestTracker.ExperienceSnapshot(sp.experienceLevel, sp.experienceProgress);
        boolean hasXpSubmitTarget = false;

        for (QuestData.Target t : q.completion.targets) {
            if (t == null) continue;
            boolean submitTarget = isSubmitTarget(q, t);
            if (!submitTarget) continue;

            if (t.isXp()) {
                hasXpSubmitTarget = true;
                simulatedXp = QuestTracker.consumeExperience(simulatedXp, t.id, t.count);
                if (simulatedXp == null) return false;
                continue;
            }

            int need = Math.max(1, t.count);
            if (!canAndTakeAcceptedItems(sim, t.acceptedIdsOrLegacy(), need, registries)) return false;
        }

        for (QuestData.Target t : q.completion.targets) {
            if (t == null) continue;
            boolean submitTarget = isSubmitTarget(q, t);
            if (!submitTarget) continue;

            if (t.isXp()) continue;

            int need = Math.max(1, t.count);
            if (!takeAcceptedItems(inv, t.acceptedIdsOrLegacy(), need, registries)) return false;
        }

        if (hasXpSubmitTarget) {
            QuestTracker.setExperienceSnapshot(sp, simulatedXp);
        }
        inv.setChanged();
        sp.containerMenu.broadcastChanges();
        return true;
    }

    private static boolean canAndTakeAcceptedItems(ItemStack[] stacks, List<String> acceptedIds, int toTake, HolderLookup.Provider registries) {
        int remaining = Math.max(0, toTake);
        if (remaining <= 0) return true;
        List<String> ids = acceptedIds == null ? List.of() : acceptedIds;
        for (String raw : ids) {
            if (remaining <= 0) break;
            QuestItemSpec spec = QuestItemSpec.parse(raw);
            if (spec.id.isBlank()) continue;
            int available = acceptedItemCount(stacks, spec, registries);
            if (available <= 0) continue;
            int consume = Math.min(remaining, available);
            if (!takeAcceptedItem(stacks, spec, consume, registries)) return false;
            remaining -= consume;
        }
        return remaining <= 0;
    }

    private static boolean takeAcceptedItems(Inventory inventory, List<String> acceptedIds, int toTake, HolderLookup.Provider registries) {
        int remaining = Math.max(0, toTake);
        if (remaining <= 0) return true;
        List<String> ids = acceptedIds == null ? List.of() : acceptedIds;
        for (String raw : ids) {
            if (remaining <= 0) break;
            QuestItemSpec spec = QuestItemSpec.parse(raw);
            if (spec.id.isBlank()) continue;
            int available = acceptedItemCount(inventory, spec, registries);
            if (available <= 0) continue;
            int consume = Math.min(remaining, available);
            if (!takeAcceptedItem(inventory, spec, consume, registries)) return false;
            remaining -= consume;
        }
        return remaining <= 0;
    }

    private static int acceptedItemCount(ItemStack[] stacks, QuestItemSpec spec, HolderLookup.Provider registries) {
        if (stacks == null || spec == null || spec.id.isBlank()) return 0;
        if (spec.tag) {
            ResourceLocation tagRl;
            try { tagRl = ResourceLocation.parse(spec.id); }
            catch (Exception ignored) { return 0; }
            return countTag(stacks, TagKey.create(Registries.ITEM, tagRl), spec, registries);
        }
        Item item = spec.item();
        return item == null ? 0 : countItem(stacks, item, spec, registries);
    }

    private static int acceptedItemCount(Inventory inventory, QuestItemSpec spec, HolderLookup.Provider registries) {
        if (inventory == null || spec == null || spec.id.isBlank()) return 0;
        if (spec.tag) {
            ResourceLocation tagRl;
            try { tagRl = ResourceLocation.parse(spec.id); }
            catch (Exception ignored) { return 0; }
            return countTag(inventory, TagKey.create(Registries.ITEM, tagRl), spec, registries);
        }
        Item item = spec.item();
        return item == null ? 0 : countItem(inventory, item, spec, registries);
    }

    private static boolean takeAcceptedItem(ItemStack[] stacks, QuestItemSpec spec, int toTake, HolderLookup.Provider registries) {
        if (spec.tag) {
            ResourceLocation tagRl;
            try { tagRl = ResourceLocation.parse(spec.id); }
            catch (Exception ignored) { return false; }
            return takeTag(stacks, TagKey.create(Registries.ITEM, tagRl), spec, toTake, registries);
        }
        Item item = spec.item();
        return item != null && takeItem(stacks, item, spec, toTake, registries);
    }

    private static boolean takeAcceptedItem(Inventory inventory, QuestItemSpec spec, int toTake, HolderLookup.Provider registries) {
        if (spec.tag) {
            ResourceLocation tagRl;
            try { tagRl = ResourceLocation.parse(spec.id); }
            catch (Exception ignored) { return false; }
            return takeTag(inventory, TagKey.create(Registries.ITEM, tagRl), spec, toTake, registries);
        }
        Item item = spec.item();
        return item != null && takeItem(inventory, item, spec, toTake, registries);
    }

    private static int countItem(ItemStack[] stacks, Item item, QuestItemSpec spec, HolderLookup.Provider registries) {
        int have = 0;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty() || !stack.is(item)) continue;
            if (spec != null && !spec.matches(stack, registries)) continue;
            have += stack.getCount();
        }
        return have;
    }

    private static int countTag(ItemStack[] stacks, TagKey<Item> tag, QuestItemSpec spec, HolderLookup.Provider registries) {
        int have = 0;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty() || !stack.is(tag)) continue;
            if (spec != null && !spec.matches(stack, registries)) continue;
            have += stack.getCount();
        }
        return have;
    }

    private static int countItem(Inventory inventory, Item item, QuestItemSpec spec, HolderLookup.Provider registries) {
        int have = 0;
        int size = inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.is(item)) continue;
            if (spec != null && !spec.matches(stack, registries)) continue;
            have += stack.getCount();
        }
        return have;
    }

    private static int countTag(Inventory inventory, TagKey<Item> tag, QuestItemSpec spec, HolderLookup.Provider registries) {
        int have = 0;
        int size = inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.is(tag)) continue;
            if (spec != null && !spec.matches(stack, registries)) continue;
            have += stack.getCount();
        }
        return have;
    }

    private static boolean canTakeItem(ItemStack[] stacks, Item item, QuestItemSpec spec, int needed, HolderLookup.Provider registries) {
        int have = 0;
        for (ItemStack s : stacks) {
            if (s == null || s.isEmpty()) continue;
            if (!s.is(item)) continue;
            if (spec != null && !spec.matches(s, registries)) continue;
            have += s.getCount();
            if (have >= needed) return true;
        }
        return have >= needed;
    }

    private static boolean canTakeTag(ItemStack[] stacks, TagKey<Item> tag, QuestItemSpec spec, int needed, HolderLookup.Provider registries) {
        int have = 0;
        for (ItemStack s : stacks) {
            if (s == null || s.isEmpty()) continue;
            if (!s.is(tag)) continue;
            if (spec != null && !spec.matches(s, registries)) continue;
            have += s.getCount();
            if (have >= needed) return true;
        }
        return have >= needed;
    }

    private static boolean takeItem(ItemStack[] stacks, Item item, QuestItemSpec spec, int toTake, HolderLookup.Provider registries) {
        int remaining = toTake;
        for (int i = 0; i < stacks.length && remaining > 0; i++) {
            ItemStack s = stacks[i];
            if (s == null || s.isEmpty()) continue;
            if (!s.is(item)) continue;
            if (spec != null && !spec.matches(s, registries)) continue;

            int take = Math.min(remaining, s.getCount());
            s.shrink(take);
            remaining -= take;

            if (s.isEmpty()) stacks[i] = ItemStack.EMPTY;
        }
        return remaining <= 0;
    }

    private static boolean takeTag(ItemStack[] stacks, TagKey<Item> tag, QuestItemSpec spec, int toTake, HolderLookup.Provider registries) {
        int remaining = toTake;
        for (int i = 0; i < stacks.length && remaining > 0; i++) {
            ItemStack s = stacks[i];
            if (s == null || s.isEmpty()) continue;
            if (!s.is(tag)) continue;
            if (spec != null && !spec.matches(s, registries)) continue;

            int take = Math.min(remaining, s.getCount());
            s.shrink(take);
            remaining -= take;

            if (s.isEmpty()) stacks[i] = ItemStack.EMPTY;
        }
        return remaining <= 0;
    }

    private static boolean takeItem(Inventory inv, Item item, QuestItemSpec spec, int toTake, HolderLookup.Provider registries) {
        int remaining = toTake;
        int size = inv.getContainerSize();

        for (int i = 0; i < size && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (!s.is(item)) continue;
            if (spec != null && !spec.matches(s, registries)) continue;

            int take = Math.min(remaining, s.getCount());
            s.shrink(take);
            remaining -= take;

            if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
        }
        return remaining <= 0;
    }

    private static boolean takeTag(Inventory inv, TagKey<Item> tag, QuestItemSpec spec, int toTake, HolderLookup.Provider registries) {
        int remaining = toTake;
        int size = inv.getContainerSize();

        for (int i = 0; i < size && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (!s.is(tag)) continue;
            if (spec != null && !spec.matches(s, registries)) continue;

            int take = Math.min(remaining, s.getCount());
            s.shrink(take);
            remaining -= take;

            if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
        }
        return remaining <= 0;
    }

    private static final class QuestPackUploadSession {
        final String id;
        final boolean enabled;
        final int totalParts;
        final byte[][] parts;
        int received;

        QuestPackUploadSession(String id, boolean enabled, int totalParts) {
            this.id = id;
            this.enabled = enabled;
            this.totalParts = totalParts;
            this.parts = new byte[totalParts][];
        }

        byte[] join() {
            int len = 0;
            for (byte[] part : parts) {
                if (part != null) len += part.length;
            }
            byte[] out = new byte[len];
            int off = 0;
            for (byte[] part : parts) {
                if (part == null) continue;
                System.arraycopy(part, 0, out, off, part.length);
                off += part.length;
            }
            return out;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientOnly {
        private static void openQuestBook() {
            net.minecraft.client.Minecraft.getInstance()
                    .setScreen(new net.revilodev.boundless.client.screen.StandaloneQuestBookScreen());
        }

        private static void applyConfigChanges() {
            net.revilodev.boundless.client.QuestPanelClient.applyConfigChanges();
            if (net.minecraft.client.Minecraft.getInstance().screen
                    instanceof net.revilodev.boundless.client.screen.StandaloneQuestBookScreen screen) {
                screen.refreshSyncedData();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientQuestSync {
        private static int activeSyncId = -1;
        private static int newestSyncIdSeen = -1;
        private static int expected = -1;
        private static byte[][] parts = null;
        private static int received = 0;

        private static void reset() {
            activeSyncId = -1;
            expected = -1;
            parts = null;
            received = 0;
        }

        private static void clear() {
            reset();
            newestSyncIdSeen = -1;
        }

        private static void accept(SyncQuestsChunk p) {
            if (p == null) return;

            int sid = p.syncId();
            int total = p.totalParts();
            int idx = p.index();

            if (total <= 0 || total > 65536) { reset(); return; }
            if (idx < 0 || idx >= total) { reset(); return; }
            if (sid < newestSyncIdSeen) return;

            if (activeSyncId != sid || expected != total || parts == null) {
                activeSyncId = sid;
                newestSyncIdSeen = sid;
                expected = total;
                parts = new byte[total][];
                received = 0;
            }

            if (parts[idx] == null) {
                parts[idx] = p.part() == null ? new byte[0] : p.part();
                received++;
            }

            if (received >= expected) {
                long startedAt = BoundlessDebug.enabled() ? System.nanoTime() : 0L;
                int totalLen = 0;
                for (int i = 0; i < expected; i++) {
                    if (parts[i] == null) { reset(); return; }
                    totalLen += parts[i].length;
                }

                byte[] all = new byte[totalLen];
                int off = 0;
                for (int i = 0; i < expected; i++) {
                    byte[] b = parts[i];
                    System.arraycopy(b, 0, all, off, b.length);
                    off += b.length;
                }

                String json = new String(all, StandardCharsets.UTF_8);
                reset();
                QuestData.applyNetworkJson(json);
                ClientOnly.applyConfigChanges();
                if (startedAt != 0L) {
                    BoundlessDebug.rateLimited("client-quest-sync", 2_000L,
                            "syncId={}, bytes={}, chunks={}, elapsed={}ms", sid, totalLen, total,
                            (System.nanoTime() - startedAt) / 1_000_000L);
                }
            }
        }
    }
}
