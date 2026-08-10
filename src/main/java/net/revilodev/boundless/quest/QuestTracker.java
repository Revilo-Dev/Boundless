package net.revilodev.boundless.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.revilodev.boundless.BoundlessMod;
import net.revilodev.boundless.BoundlessDebug;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.compat.LevelUpCompat;
import net.revilodev.boundless.network.BoundlessNetwork;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BooleanSupplier;

import static net.revilodev.boundless.network.BoundlessNetwork.sendToastLocal;

public final class QuestTracker {
    public record ExperienceSnapshot(int level, float progress) {}

    public enum Status { INCOMPLETE, COMPLETED, REDEEMED, REJECTED }

    // client and server runtime caches
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private static final Map<String, Map<String, Status>> WORLD_STATES = new HashMap<>();
    private static final Map<String, Integer> CLIENT_KILLS = new HashMap<>();
    private static final Map<String, Boolean> CLIENT_ADV_DONE = new HashMap<>();
    private static final Map<String, Integer> CLIENT_ITEM_PROGRESS = new HashMap<>();
    private static final Map<String, Boolean> CLIENT_EFFECT_PROGRESS = new HashMap<>();
    private static final Map<String, String> CLIENT_INPUT_PROGRESS = new HashMap<>();
    private static final Map<String, Integer> CLIENT_CLAIM_COUNTS = new HashMap<>();
    private static final Map<String, Boolean> CLIENT_SCROLL_REDEEMED = new HashMap<>();
    private static final Map<String, Boolean> CLIENT_SCROLL_CREATED = new HashMap<>();
    private static final Set<String> CLIENT_REPORTED_OBSERVE = new HashSet<>();
    private static final Map<String, ResourceLocation> RL_CACHE = new HashMap<>();
    private static final Map<String, Optional<Item>> ITEM_BY_ID_CACHE = new HashMap<>();
    private static final Map<String, Holder<MobEffect>> EFFECT_BY_ID_CACHE = new HashMap<>();
    private static final Map<UUID, Integer> SERVER_QUEST_SCAN_CURSOR = new HashMap<>();
    private static final Map<UUID, Integer> SERVER_DIRTY_MASKS = new HashMap<>();
    private static final Map<UUID, ServerStateSnapshot> SERVER_STATE_SNAPSHOTS = new HashMap<>();
    private static final int SERVER_QUEST_SCAN_BATCH = 32;
    private static final int DIRTY_INVENTORY = 1;
    private static final int DIRTY_EFFECTS = 1 << 1;
    private static final int DIRTY_XP = 1 << 2;
    private static final int DIRTY_CONTEXT = 1 << 3;
    private static final int DIRTY_ALL = DIRTY_INVENTORY | DIRTY_EFFECTS | DIRTY_XP | DIRTY_CONTEXT;
    private static final ThreadLocal<EvaluationCache> EVALUATION_CACHE = new ThreadLocal<>();

    // current local world key
    private static String ACTIVE_KEY = null;
    private static volatile boolean CLIENT_IN_MULTIPLAYER = false;

    private QuestTracker() {}

    private record ServerStateSnapshot(long inventoryHash, long effectHash, int xpPoints, String biomeId, String dimensionId) {}

    private static final class EvaluationCache {
        private final UUID playerId;
        private final Map<String, Integer> acceptedItemCounts = new HashMap<>();
        private final Map<String, Integer> acceptedKillCounts = new HashMap<>();
        private final Map<String, Boolean> effectResults = new HashMap<>();
        private final Map<String, Boolean> advancementResults = new HashMap<>();
        private String biomeId;
        private String dimensionId;
        private Integer xpPoints;

        private EvaluationCache(UUID playerId) {
            this.playerId = playerId;
        }
    }

    public static void setClientMultiplayer(boolean v) {
        CLIENT_IN_MULTIPLAYER = v;
    }

    // force the next server quest evaluation
    public static void markServerStateDirty(ServerPlayer player) {
        if (player == null) return;
        SERVER_DIRTY_MASKS.merge(player.getUUID(), DIRTY_ALL, (a, b) -> a | b);
    }

    public static void clearServerRuntimeState(ServerPlayer player) {
        if (player == null) return;
        UUID playerId = player.getUUID();
        SERVER_QUEST_SCAN_CURSOR.remove(playerId);
        SERVER_DIRTY_MASKS.remove(playerId);
        SERVER_STATE_SNAPSHOTS.remove(playerId);
    }

    private static boolean isClientMultiplayer() {
        if (FMLEnvironment.dist != Dist.CLIENT) return false;
        return ClientOnly.isClientMultiplayerFlag(CLIENT_IN_MULTIPLAYER);
    }

    public static int getPermanentItemProgress(String key, int current, int required) {
        int req = Math.max(0, required);
        int cur = Math.max(0, current);

        if (req <= 0) return 0;

        int prev = CLIENT_ITEM_PROGRESS.getOrDefault(key, 0);
        int now = Math.max(prev, Math.min(cur, req));

        if (FMLEnvironment.dist == Dist.CLIENT && prev > 0) {
            now = ClientOnly.adjustItemProgress(key, cur, req, prev, now);
        }

        if (now <= 0) {
            CLIENT_ITEM_PROGRESS.remove(key);
            return 0;
        }

        CLIENT_ITEM_PROGRESS.put(key, now);
        return now;
    }

    private static int getPermanentItemProgress(Player player, String key, int current, int required) {
        if (player instanceof ServerPlayer sp) {
            return QuestObjectiveState.get(sp.serverLevel())
                    .updateItemProgress(sp.getUUID(), key, current, required);
        }
        ServerPlayer integrated = resolveIntegratedServerPlayer(player);
        if (integrated != null) {
            return QuestObjectiveState.get(integrated.serverLevel())
                    .updateItemProgress(integrated.getUUID(), key, current, required);
        }
        return getPermanentItemProgress(key, current, required);
    }

    private static boolean getPermanentEffectProgress(String key, boolean hasEffect) {
        boolean prev = CLIENT_EFFECT_PROGRESS.getOrDefault(key, false);
        boolean now = prev || hasEffect;
        if (now) CLIENT_EFFECT_PROGRESS.put(key, true);
        return now;
    }

    private static boolean getPermanentEffectProgress(Player player, String key, boolean hasEffect) {
        if (player instanceof ServerPlayer sp) {
            return QuestObjectiveState.get(sp.serverLevel())
                    .updateFlagDone(sp.getUUID(), key, hasEffect);
        }
        ServerPlayer integrated = resolveIntegratedServerPlayer(player);
        if (integrated != null) {
            return QuestObjectiveState.get(integrated.serverLevel())
                    .updateFlagDone(integrated.getUUID(), key, hasEffect);
        }
        return getPermanentEffectProgress(key, hasEffect);
    }

    private static boolean getPermanentFlagProgress(String key, boolean hasNow) {
        boolean prev = CLIENT_EFFECT_PROGRESS.getOrDefault(key, false);
        boolean now = prev || hasNow;
        if (now) CLIENT_EFFECT_PROGRESS.put(key, true);
        return now;
    }

    private static boolean getPermanentFlagProgress(Player player, String key, boolean hasNow) {
        if (player instanceof ServerPlayer sp) {
            return QuestObjectiveState.get(sp.serverLevel())
                    .updateFlagDone(sp.getUUID(), key, hasNow);
        }
        if (player != null && player.level().isClientSide && isClientMultiplayer()) {
            return CLIENT_EFFECT_PROGRESS.getOrDefault(key, false) || hasNow;
        }
        ServerPlayer integrated = resolveIntegratedServerPlayer(player);
        if (integrated != null) {
            return QuestObjectiveState.get(integrated.serverLevel())
                    .updateFlagDone(integrated.getUUID(), key, hasNow);
        }
        return getPermanentFlagProgress(key, hasNow);
    }

    private static boolean getPermanentFlagProgress(Player player, String key, BooleanSupplier detector) {
        if (key == null || key.isBlank()) return false;
        if (player instanceof ServerPlayer sp) {
            QuestObjectiveState state = QuestObjectiveState.get(sp.serverLevel());
            if (state.getFlagDone(sp.getUUID(), key)) return true;
            return state.updateFlagDone(sp.getUUID(), key, detector.getAsBoolean());
        }
        if (player != null && player.level().isClientSide && isClientMultiplayer()) {
            if (CLIENT_EFFECT_PROGRESS.getOrDefault(key, false)) return true;
            return detector.getAsBoolean();
        }
        ServerPlayer integrated = resolveIntegratedServerPlayer(player);
        if (integrated != null) {
            QuestObjectiveState state = QuestObjectiveState.get(integrated.serverLevel());
            if (state.getFlagDone(integrated.getUUID(), key)) return true;
            return state.updateFlagDone(integrated.getUUID(), key, detector.getAsBoolean());
        }
        if (CLIENT_EFFECT_PROGRESS.getOrDefault(key, false)) return true;
        return getPermanentFlagProgress(key, detector.getAsBoolean());
    }

    public static void setFieldInputProgress(Player player, String key, String value) {
        if (key == null || key.isBlank()) return;
        String normalized = value == null ? "" : value.trim();
        if (player instanceof ServerPlayer sp) {
            QuestObjectiveState.get(sp.serverLevel()).setInputProgress(sp.getUUID(), key, normalized);
        }
        ServerPlayer integrated = resolveIntegratedServerPlayer(player);
        if (integrated != null) {
            QuestObjectiveState.get(integrated.serverLevel()).setInputProgress(integrated.getUUID(), key, normalized);
        }
        if (normalized.isBlank()) CLIENT_INPUT_PROGRESS.remove(key);
        else CLIENT_INPUT_PROGRESS.put(key, normalized);
    }

    public static String getFieldInputProgress(Player player, String key) {
        if (key == null || key.isBlank()) return "";
        if (player instanceof ServerPlayer sp) {
            return QuestObjectiveState.get(sp.serverLevel()).getInputProgress(sp.getUUID(), key);
        }
        ServerPlayer integrated = resolveIntegratedServerPlayer(player);
        if (integrated != null) {
            return QuestObjectiveState.get(integrated.serverLevel()).getInputProgress(integrated.getUUID(), key);
        }
        return CLIENT_INPUT_PROGRESS.getOrDefault(key, "");
    }

    private static ServerPlayer resolveIntegratedServerPlayer(Player player) {
        if (player == null || !player.level().isClientSide) return null;
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("getInstance").invoke(null);
            Object srvObj = mcClass.getMethod("getSingleplayerServer").invoke(mc);
            if (srvObj instanceof net.minecraft.server.MinecraftServer srv) {
                return srv.getPlayerList().getPlayer(player.getUUID());
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String sanitize(String s) {
        if (s == null || s.isBlank()) return "default";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '.'
                    || ch == '_'
                    || ch == '-') {
                out.append(ch);
            } else {
                out.append('_');
            }
        }
        return out.isEmpty() ? "default" : out.toString();
    }

    private static ResourceLocation tryParseCached(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if (RL_CACHE.containsKey(raw)) return RL_CACHE.get(raw);
        ResourceLocation rl = ResourceLocation.tryParse(raw);
        RL_CACHE.put(raw, rl);
        return rl;
    }

    private static Item resolveItemById(String itemId) {
        if (itemId == null || itemId.isBlank()) return null;
        Optional<Item> cached = ITEM_BY_ID_CACHE.get(itemId);
        if (cached != null) return cached.orElse(null);
        ResourceLocation rl = tryParseCached(itemId);
        Optional<Item> resolved = rl == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(rl);
        ITEM_BY_ID_CACHE.put(itemId, resolved);
        return resolved.orElse(null);
    }

    private static String computeClientKey() {
        return ClientOnly.computeClientKey();
    }

    private static Map<String, Status> activeStateMap() {
        String key = ACTIVE_KEY;
        if (key == null) key = "default";
        return WORLD_STATES.computeIfAbsent(key, k -> new LinkedHashMap<>());
    }

    public static void forceSave() {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        try {
            if (ACTIVE_KEY == null) ensureClientStateLoaded(null);
        } catch (Throwable ignored) {}
        if (ACTIVE_KEY != null) ClientOnly.saveClientState(ACTIVE_KEY);
    }

    private static void ensureClientStateLoaded(Player player) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        String key = computeClientKey();
        if (!key.equals(ACTIVE_KEY)) {
            if (ACTIVE_KEY != null) ClientOnly.saveClientState(ACTIVE_KEY);
            ACTIVE_KEY = key;
            ClientOnly.loadClientState(key);
        }
    }

    public static Status decodeStatus(String raw) {
        if (raw == null || raw.isBlank()) return Status.INCOMPLETE;
        try { return Status.valueOf(raw); } catch (Exception ignored) { return Status.INCOMPLETE; }
    }

    private static Status getServerStatus(ServerPlayer player, String questId) {
        String raw = QuestProgressState.get(player.serverLevel()).get(player.getUUID(), questId);
        return decodeStatus(raw);
    }

    public static void setServerStatus(ServerPlayer player, String questId, Status st) {
        QuestProgressState data = QuestProgressState.get(player.serverLevel());
        if (st == null || st == Status.INCOMPLETE) {
            data.set(player.getUUID(), questId, null);
        } else {
            data.set(player.getUUID(), questId, st.name());
        }
    }

    public static Status getStatus(QuestData.Quest q, Player player) {
        if (q == null) return Status.INCOMPLETE;
        if (player instanceof ServerPlayer sp) return getServerStatus(sp, q.id);
        if (player != null && player.level().isClientSide) ensureClientStateLoaded(player);
        return activeStateMap().getOrDefault(q.id, Status.INCOMPLETE);
    }

    public static Status getStatus(String questId, Player player) {
        if (player instanceof ServerPlayer sp) return getServerStatus(sp, questId);
        if (player != null && player.level().isClientSide) ensureClientStateLoaded(player);
        return activeStateMap().getOrDefault(questId, Status.INCOMPLETE);
    }

    public static int getClaimCount(String questId, Player player) {
        if (questId == null || questId.isBlank()) return 0;
        if (player instanceof ServerPlayer sp) {
            return QuestProgressState.get(sp.serverLevel()).getClaimCount(sp.getUUID(), questId);
        }
        if (player != null && player.level().isClientSide) ensureClientStateLoaded(player);
        return Math.max(0, CLIENT_CLAIM_COUNTS.getOrDefault(questId, 0));
    }

    public static boolean hasEverClaimed(QuestData.Quest q, Player player) {
        return q != null && hasEverClaimed(q.id, player);
    }

    public static boolean hasEverClaimed(String questId, Player player) {
        return getClaimCount(questId, player) > 0 || getStatus(questId, player) == Status.REDEEMED;
    }

    public static boolean hasRedeemedScroll(String questId, Player player) {
        if (questId == null || questId.isBlank()) return false;
        if (player instanceof ServerPlayer sp) {
            return QuestProgressState.get(sp.serverLevel()).hasRedeemedScroll(sp.getUUID(), questId);
        }
        if (player != null && player.level().isClientSide) ensureClientStateLoaded(player);
        return Boolean.TRUE.equals(CLIENT_SCROLL_REDEEMED.get(questId));
    }

    public static boolean hasCreatedScroll(String questId, Player player) {
        if (questId == null || questId.isBlank()) return false;
        if (player instanceof ServerPlayer sp) {
            return QuestProgressState.get(sp.serverLevel()).hasCreatedScroll(sp.getUUID(), questId);
        }
        if (player != null && player.level().isClientSide) ensureClientStateLoaded(player);
        return Boolean.TRUE.equals(CLIENT_SCROLL_CREATED.get(questId));
    }

    public static boolean dependenciesMet(QuestData.Quest q, Player player) {
        if (q == null || q.dependencies.isEmpty()) return true;
        if (q.lockAfterDependency) {
            for (String depId : q.dependencies) {
                QuestData.Quest dep = questByIdForPlayer(depId, player);
                if (dep == null) return false;
                if (hasEverClaimed(dep, player)) return false;
            }
            return true;
        }
        for (String depId : q.dependencies) {
            QuestData.Quest dep = questByIdForPlayer(depId, player);
            if (dep == null) return false;
            if (!hasEverClaimed(dep, player)) return false;
        }
        return true;
    }

    private static QuestData.Quest questByIdForPlayer(String questId, Player player) {
        if (player instanceof ServerPlayer sp) {
            return QuestData.byIdServer(sp.server, questId).orElse(null);
        }
        return QuestData.byId(questId).orElse(null);
    }

    private static boolean shouldAutoClaim(QuestData.Quest q) {
        if (q == null) return Config.autoClaimQuestRewards();
        if (q.autoComplete) return true;
        QuestData.Category category = QuestData.categoryById(q.category).orElse(null);
        if (category != null && category.autoComplete) return true;
        return Config.autoClaimQuestRewards();
    }

    public static boolean isVisible(QuestData.Quest q, Player player) {
        if (q == null || player == null) return false;
        if (Config.disabledCategories().contains(q.category)) return false;
        if (q.hiddenUnderDependency && !q.dependencies.isEmpty() && !dependenciesMet(q, player)) return false;
        Status st = getStatus(q, player);
        if (st == Status.REJECTED) return false;
        if (st == Status.REDEEMED && !q.repeatable) return false;
        return true;
    }

    public static boolean hasAnyCompleted(Player player) {
        if (player != null && player.level().isClientSide) ensureClientStateLoaded(player);
        QuestData.loadClient(false);
        for (QuestData.Quest q : QuestData.all()) {
            if (q == null) continue;
            if (getStatus(q, player) == Status.COMPLETED) return true;
        }
        return false;
    }

    public static boolean hasCompleted(Player player, String questId) {
        return hasEverClaimed(questId, player);
    }

    private static boolean isSubmissionQuestType(QuestData.Quest q) {
        if (q == null || q.type == null) return false;
        return "submission".equalsIgnoreCase(q.type) || "submit".equalsIgnoreCase(q.type);
    }

    private static boolean isSubmitTarget(QuestData.Quest q, QuestData.Target t) {
        if (t == null) return false;
        if (t.isSubmit()) return true;
        return isSubmissionQuestType(q) && t.isItem();
    }

    private static boolean hasItemOrSubmitTargets(QuestData.Quest q) {
        if (q == null || q.completion == null || q.completion.targets == null) return false;
        for (QuestData.Target t : q.completion.targets) {
            if (t != null && (t.isItem() || t.isSubmit() || t.isXp())) {
                return true;
            }
        }
        return false;
    }

    public static String flagProgressKey(QuestData.Quest quest, QuestData.Target target) {
        if (quest == null || target == null) return "";
        String id = target.id == null ? "" : target.id.trim();
        return quest.id + ":" + target.kind + ":" + id;
    }

    private static int peekPermanentItemProgress(Player player, String key, int current, int required) {
        int req = Math.max(0, required);
        int cur = Math.max(0, current);
        if (req <= 0) return 0;
        if (player instanceof ServerPlayer sp) {
            int saved = QuestObjectiveState.get(sp.serverLevel()).getItemProgress(sp.getUUID(), key);
            return Math.max(saved, Math.min(cur, req));
        }
        if (player != null && player.level().isClientSide && isClientMultiplayer()) {
            int saved = CLIENT_ITEM_PROGRESS.getOrDefault(key, 0);
            return Math.max(saved, Math.min(cur, req));
        }
        ServerPlayer integrated = resolveIntegratedServerPlayer(player);
        if (integrated != null) {
            int saved = QuestObjectiveState.get(integrated.serverLevel()).getItemProgress(integrated.getUUID(), key);
            return Math.max(saved, Math.min(cur, req));
        }
        int saved = CLIENT_ITEM_PROGRESS.getOrDefault(key, 0);
        return Math.max(saved, Math.min(cur, req));
    }

    public static int getTrackedItemProgress(QuestData.Quest quest, QuestData.Target target, Player player) {
        if (quest == null || target == null || player == null) return 0;
        String key = itemProgressKey(quest, target);
        int current = getAcceptedItemCountInInventory(target, player);
        return peekPermanentItemProgress(player, key, current, target.count);
    }

    private static boolean peekPermanentFlagProgress(Player player, String key, boolean hasNow) {
        if (player instanceof ServerPlayer sp) {
            return QuestObjectiveState.get(sp.serverLevel()).getFlagDone(sp.getUUID(), key) || hasNow;
        }
        if (player != null && player.level().isClientSide && isClientMultiplayer()) {
            return CLIENT_EFFECT_PROGRESS.getOrDefault(key, false) || hasNow;
        }
        ServerPlayer integrated = resolveIntegratedServerPlayer(player);
        if (integrated != null) {
            return QuestObjectiveState.get(integrated.serverLevel()).getFlagDone(integrated.getUUID(), key) || hasNow;
        }
        return CLIENT_EFFECT_PROGRESS.getOrDefault(key, false) || hasNow;
    }

    private static boolean peekPermanentFlagProgress(Player player, String key, BooleanSupplier detector) {
        if (key == null || key.isBlank()) return false;
        if (player instanceof ServerPlayer sp) {
            if (QuestObjectiveState.get(sp.serverLevel()).getFlagDone(sp.getUUID(), key)) return true;
            return detector.getAsBoolean();
        }
        if (player != null && player.level().isClientSide && isClientMultiplayer()) {
            if (CLIENT_EFFECT_PROGRESS.getOrDefault(key, false)) return true;
            return detector.getAsBoolean();
        }
        ServerPlayer integrated = resolveIntegratedServerPlayer(player);
        if (integrated != null) {
            if (QuestObjectiveState.get(integrated.serverLevel()).getFlagDone(integrated.getUUID(), key)) return true;
            return detector.getAsBoolean();
        }
        if (CLIENT_EFFECT_PROGRESS.getOrDefault(key, false)) return true;
        return detector.getAsBoolean();
    }

    private static boolean shouldUseServerObserveDetection(Player player) {
        if (!(player instanceof ServerPlayer sp)) return true;
        return !sp.server.isDedicatedServer();
    }

    private static boolean evaluateTarget(QuestData.Quest q, QuestData.Target t, Player player, boolean trackProgress) {
        if (t == null || player == null) return true;

        if (isSubmitTarget(q, t)) {
            return getAcceptedItemCountInInventory(t, player) >= t.count;
        }

        if (t.isItem()) {
            String key = itemProgressKey(q, t);
            int cur = getAcceptedItemCountInInventory(t, player);
            int prog = trackProgress
                    ? getPermanentItemProgress(player, key, cur, t.count)
                    : peekPermanentItemProgress(player, key, cur, t.count);
            return prog >= t.count;
        }

        if (t.isEntity()) return getAcceptedKillCount(t, player) >= t.count;

        if (t.isEffect()) {
            String key = flagProgressKey(q, t);
            return trackProgress
                    ? getPermanentFlagProgress(player, key, () -> hasEffect(player, t.id))
                    : peekPermanentFlagProgress(player, key, () -> hasEffect(player, t.id));
        }

        if (t.isAdvancement()) return hasAdvancement(player, t.id);
        if (t.isObserve()) {
            String key = flagProgressKey(q, t);
            BooleanSupplier detector = shouldUseServerObserveDetection(player)
                    ? () -> isObservingTarget(player, t.id)
                    : () -> false;
            return trackProgress
                    ? getPermanentFlagProgress(player, key, detector)
                    : peekPermanentFlagProgress(player, key, detector);
        }
        if (t.isBiome()) {
            String key = flagProgressKey(q, t);
            return trackProgress
                    ? getPermanentFlagProgress(player, key, () -> isInBiome(player, t.id))
                    : peekPermanentFlagProgress(player, key, () -> isInBiome(player, t.id));
        }
        if (t.isDimension()) {
            String key = flagProgressKey(q, t);
            return trackProgress
                    ? getPermanentFlagProgress(player, key, () -> isInDimension(player, t.id))
                    : peekPermanentFlagProgress(player, key, () -> isInDimension(player, t.id));
        }
        if (t.isCheck()) {
            String key = flagProgressKey(q, t);
            return trackProgress
                    ? getPermanentFlagProgress(player, key, () -> false)
                    : peekPermanentFlagProgress(player, key, () -> false);
        }
        if (t.isXp()) return getXpAmount(player, t.id) >= t.count;
        if (t.isLevelUpLevel()) return LevelUpCompat.meetsLevelRequirement(player, t.count);
        if (t.isFieldInput()) {
            String key = q.id + ":field:" + t.id;
            String value = getFieldInputProgress(player, key);
            return safeNormalizeFieldInput(value).equals(safeNormalizeFieldInput(t.id));
        }

        return true;
    }

    public static boolean isTargetSatisfied(QuestData.Quest q, QuestData.Target t, Player player) {
        return evaluateTarget(q, t, player, false);
    }

    public static boolean canAcknowledge(QuestData.Quest q, Player player) {
        if (player == null || q == null || q.completion == null) return false;
        if (!dependenciesMet(q, player)) return false;
        Status status = getStatus(q, player);
        if (status == Status.REDEEMED || status == Status.REJECTED) return false;

        for (QuestData.Target t : q.completion.targets) {
            if (t == null) continue;
            if (t.isCheck() && !evaluateTarget(q, t, player, false)) return true;
        }
        return false;
    }

    public static boolean acknowledgeCheckObjectives(QuestData.Quest q, Player player) {
        if (player == null || q == null || q.completion == null) return false;
        boolean changed = false;
        for (QuestData.Target t : q.completion.targets) {
            if (t == null || !t.isCheck()) continue;
            String key = flagProgressKey(q, t);
            if (!getPermanentFlagProgress(player, key, false)) {
                getPermanentFlagProgress(player, key, true);
                changed = true;
            }
        }
        return changed;
    }

    public static boolean isReady(QuestData.Quest q, Player player) {
        if (player == null || q == null || q.completion == null) return false;
        if (getStatus(q, player) == Status.COMPLETED) return true;

        if (!dependenciesMet(q, player)) return false;

        for (QuestData.Target t : q.completion.targets) {
            if (t == null) continue;
            if (!evaluateTarget(q, t, player, false)) return false;
        }

        return true;
    }

    public static boolean updateProgressAndCheckReady(QuestData.Quest q, Player player) {
        if (player == null || q == null || q.completion == null) return false;
        if (getStatus(q, player) == Status.COMPLETED) return true;

        if (!dependenciesMet(q, player)) return false;

        for (QuestData.Target t : q.completion.targets) {
            if (t == null) continue;
            if (!evaluateTarget(q, t, player, true)) return false;
        }

        return true;
    }

    public static int getXpAmount(Player player, String xpType) {
        if (player == null) return 0;
        String mode = normalizeXpType(xpType);
        if ("levels".equals(mode)) {
            return Math.max(0, player.experienceLevel);
        }
        return currentExperiencePoints(player);
    }

    public static String normalizeXpType(String xpType) {
        String mode = xpType == null ? "" : xpType.trim().toLowerCase(Locale.ROOT);
        return "levels".equals(mode) ? "levels" : "points";
    }

    private static String safeNormalizeFieldInput(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean markFlagProgress(ServerPlayer player, String key) {
        if (player == null || key == null || key.isBlank()) return false;
        return !QuestObjectiveState.get(player.serverLevel()).getFlagDone(player.getUUID(), key)
                && QuestObjectiveState.get(player.serverLevel()).updateFlagDone(player.getUUID(), key, true);
    }

    public static boolean refreshPersistentContextTargets(ServerPlayer player) {
        return refreshPersistentContextTargets(player, true, true, true);
    }

    public static boolean refreshPersistentContextTargets(ServerPlayer player, boolean observe, boolean biome, boolean dimension) {
        if (player == null) return false;
        return refreshPersistentContextTargets(player, new ArrayList<>(QuestData.allServer(player.server)), 0, Integer.MAX_VALUE, observe, biome, dimension);
    }

    private static boolean refreshPersistentContextTargets(ServerPlayer player, List<QuestData.Quest> quests, int start, int limit, boolean observe, boolean biome, boolean dimension) {
        if (player == null) return false;
        if (quests == null || quests.isEmpty() || limit <= 0) return false;

        boolean changed = false;
        boolean allowObserveChecks = observe && shouldUseServerObserveDetection(player);
        int total = quests.size();
        int batch = Math.min(total, Math.max(0, limit));
        for (int processed = 0; processed < batch; processed++) {
            QuestData.Quest quest = quests.get((start + processed) % total);
            if (quest == null || quest.completion == null || quest.completion.targets == null) continue;
            if (Config.disabledCategories().contains(quest.category)) continue;

            Status status = getServerStatus(player, quest.id);
            if (status == Status.REDEEMED || status == Status.REJECTED) continue;
            if (!dependenciesMet(quest, player)) continue;

            for (QuestData.Target target : quest.completion.targets) {
                if (target == null) continue;

                boolean matches = (allowObserveChecks && target.isObserve() && isObservingTarget(player, target.id))
                        || (biome && target.isBiome() && isInBiome(player, target.id))
                        || (dimension && target.isDimension() && isInDimension(player, target.id));
                if (!matches) continue;

                changed |= markFlagProgress(player, flagProgressKey(quest, target));
            }
        }

        return changed;
    }

    public static int getCountInInventory(String id, Player player) {
        if (player == null || id == null || id.isBlank()) return 0;
        QuestItemSpec spec = QuestItemSpec.parse(id);
        String key = spec.id;

        ResourceLocation rl = tryParseCached(key);
        if (rl == null) return 0;

        Item direct = resolveItemById(key);
        int found = 0;
        var registries = player.registryAccess();
        var inventory = player.getInventory();
        int containerSize = inventory.getContainerSize();

        if (spec.tag || direct == null) {
            var itemTag = net.minecraft.tags.TagKey.create(Registries.ITEM, rl);
            for (int i = 0; i < containerSize; i++) {
                ItemStack s = inventory.getItem(i);
                if (!s.isEmpty() && s.is(itemTag) && spec.matches(s, registries)) found += s.getCount();
            }
            if (found == 0) {
                var blockTag = net.minecraft.tags.TagKey.create(Registries.BLOCK, rl);
                for (int i = 0; i < containerSize; i++) {
                    ItemStack s = inventory.getItem(i);
                    if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock().builtInRegistryHolder().is(blockTag) && spec.componentsMatch(s, registries)) {
                        found += s.getCount();
                    }
                }
            }
        } else {
            for (int i = 0; i < containerSize; i++) {
                ItemStack s = inventory.getItem(i);
                if (!s.isEmpty() && s.is(direct) && spec.matches(s, registries)) found += s.getCount();
            }
        }

        return found;
    }

    public static int getAcceptedItemCountInInventory(QuestData.Target target, Player player) {
        if (target == null) return 0;
        EvaluationCache cache = evaluationCacheFor(player);
        String cacheKey = acceptedIdsCacheKey(target.acceptedIdsOrLegacy());
        if (cache != null && cache.acceptedItemCounts.containsKey(cacheKey)) {
            return cache.acceptedItemCounts.get(cacheKey);
        }
        int total = 0;
        for (String acceptedId : target.acceptedIdsOrLegacy()) {
            total += getCountInInventory(acceptedId, player);
        }
        total = Math.max(0, total);
        if (cache != null) cache.acceptedItemCounts.put(cacheKey, total);
        return total;
    }

    public static int getKillCount(Player player, String entityId) {
        if (player == null || entityId == null || entityId.isBlank()) return 0;
        if (player instanceof ServerPlayer sp) {
            return KillCounterState.get(sp.serverLevel()).get(player.getUUID(), entityId);
        }
        return CLIENT_KILLS.getOrDefault(entityId, 0);
    }

    public static int getAcceptedKillCount(QuestData.Target target, Player player) {
        if (target == null) return 0;
        EvaluationCache cache = evaluationCacheFor(player);
        String cacheKey = acceptedIdsCacheKey(target.acceptedIdsOrLegacy());
        if (cache != null && cache.acceptedKillCounts.containsKey(cacheKey)) {
            return cache.acceptedKillCounts.get(cacheKey);
        }
        int total = 0;
        for (String acceptedId : target.acceptedIdsOrLegacy()) {
            total += getKillCount(player, acceptedId);
        }
        total = Math.max(0, total);
        if (cache != null) cache.acceptedKillCounts.put(cacheKey, total);
        return total;
    }

    public static boolean isInBiome(Player player, String biomeId) {
        if (player == null || biomeId == null || biomeId.isBlank()) return false;
        ResourceLocation rl = tryParseCached(biomeId);
        if (rl == null) return false;
        String current = currentBiomeId(player);
        return current != null && rl.toString().equals(current);
    }

    public static boolean isInDimension(Player player, String dimensionId) {
        if (player == null || dimensionId == null || dimensionId.isBlank()) return false;
        ResourceLocation rl = tryParseCached(dimensionId);
        if (rl == null) return false;
        String current = currentDimensionId(player);
        return current != null && rl.toString().equals(current);
    }

    public static boolean isObservingTarget(Player player, String targetId) {
        if (player == null || targetId == null || targetId.isBlank()) return false;
        ResourceLocation rl = tryParseCached(targetId);
        if (rl == null) return false;
        if (BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
            return isLookingAtEntity(player, rl);
        }
        if (BuiltInRegistries.BLOCK.containsKey(rl)) {
            return isLookingAtBlock(player, rl);
        }
        if (BuiltInRegistries.ITEM.containsKey(rl)) {
            return isLookingAtItem(player, rl);
        }
        return false;
    }

    private static boolean isLookingAtBlock(Player player, ResourceLocation targetId) {
        HitResult hit = player.pick(24.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return false;
        ResourceLocation looked = BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(blockHit.getBlockPos()).getBlock());
        return targetId.equals(looked);
    }

    private static boolean isLookingAtEntity(Player player, ResourceLocation targetId) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(24.0D));
        AABB bounds = player.getBoundingBox().expandTowards(look.scale(24.0D)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                bounds,
                entity -> isObservedEntityMatch(entity, targetId),
                24.0D * 24.0D
        );
        return hit != null;
    }

    private static boolean isLookingAtItem(Player player, ResourceLocation targetId) {
        HitResult hit = player.pick(24.0D, 0.0F, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            Item blockItem = player.level().getBlockState(blockHit.getBlockPos()).getBlock().asItem();
            ResourceLocation blockItemId = blockItem == null ? null : BuiltInRegistries.ITEM.getKey(blockItem);
            if (targetId.equals(blockItemId)) return true;
        }

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(24.0D));
        AABB bounds = player.getBoundingBox().expandTowards(look.scale(24.0D)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                bounds,
                entity -> isObservedItemMatch(entity, targetId),
                24.0D * 24.0D
        );
        return entityHit != null;
    }

    private static boolean isObservedEntityMatch(Entity entity, ResourceLocation targetId) {
        if (entity == null || !entity.isPickable()) return false;
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return targetId.equals(entityId);
    }

    private static boolean isObservedItemMatch(Entity entity, ResourceLocation targetId) {
        if (entity == null || !entity.isPickable()) return false;
        ItemStack stack = ItemStack.EMPTY;
        if (entity instanceof ItemEntity itemEntity) {
            stack = itemEntity.getItem();
        } else if (entity instanceof ItemFrame itemFrame) {
            stack = itemFrame.getItem();
        }
        if (stack.isEmpty()) return false;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return targetId.equals(itemId);
    }

    public static String itemProgressKey(QuestData.Quest quest, QuestData.Target target) {
        if (quest == null || target == null) return "";
        List<String> accepted = target.acceptedIdsOrLegacy();
        if (accepted.size() <= 1) {
            return quest.id + ":" + (accepted.isEmpty() ? target.id : accepted.get(0));
        }
        List<String> sorted = new ArrayList<>(accepted);
        sorted.sort(String::compareTo);
        return quest.id + ":accepted:" + String.join("|", sorted);
    }

    public static boolean hasEffect(Player player, String effectId) {
        if (player == null || effectId == null || effectId.isBlank()) return false;
        EvaluationCache cache = evaluationCacheFor(player);
        if (cache != null && cache.effectResults.containsKey(effectId)) {
            return cache.effectResults.get(effectId);
        }
        Holder<MobEffect> holder = EFFECT_BY_ID_CACHE.get(effectId);
        if (!EFFECT_BY_ID_CACHE.containsKey(effectId)) {
            ResourceLocation rl = tryParseCached(effectId);
            holder = rl == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
            EFFECT_BY_ID_CACHE.put(effectId, holder);
        }
        boolean result = holder != null && player.hasEffect(holder);
        if (cache != null) cache.effectResults.put(effectId, result);
        return result;
    }

    public static boolean hasAdvancement(Player player, String advId) {
        if (player == null || advId == null || advId.isBlank()) return false;
        EvaluationCache cache = evaluationCacheFor(player);
        if (cache != null && cache.advancementResults.containsKey(advId)) {
            return cache.advancementResults.get(advId);
        }

        final ResourceLocation rl;
        rl = tryParseCached(advId);
        if (rl == null) return false;

        if (player instanceof ServerPlayer sp) {
            boolean result = hasAdvancementServer(sp, rl);
            if (cache != null) cache.advancementResults.put(advId, result);
            return result;
        }

        if (FMLEnvironment.dist == Dist.CLIENT && player.level().isClientSide) {
            try {
                Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
                Object mc = mcClass.getMethod("getInstance").invoke(null);
                Object srvObj = mcClass.getMethod("getSingleplayerServer").invoke(mc);

                if (srvObj instanceof net.minecraft.server.MinecraftServer srv) {
                    ServerPlayer sp = srv.getPlayerList().getPlayer(player.getUUID());
                    if (sp != null) {
                        boolean result = hasAdvancementServer(sp, rl);
                        if (cache != null) cache.advancementResults.put(advId, result);
                        return result;
                    }
                }

            } catch (Throwable ignored) {}

            boolean result = CLIENT_ADV_DONE.getOrDefault(progressCacheKey(player, rl.toString()), false);
            if (cache != null) cache.advancementResults.put(advId, result);
            return result;
        }

        return false;
    }

    private static boolean hasAdvancementServer(ServerPlayer sp, ResourceLocation rl) {
        AdvancementHolder holder = sp.server.getAdvancements().get(rl);
        if (holder == null) return false;

        AdvancementProgress prog = sp.getAdvancements().getOrStartProgress(holder);
        boolean done = prog.isDone();

        CLIENT_ADV_DONE.put(progressCacheKey(sp, rl.toString()), done);
        return done;
    }

    private static String progressCacheKey(Player player, String id) {
        String playerKey = player == null ? "none" : player.getUUID().toString();
        String value = id == null ? "" : id;
        return playerKey + "|" + value;
    }

    public static boolean canCreateScroll(QuestData.Quest q, Player player) {
        return Config.enableQuestScrolls()
                && q != null
                && player != null
                && hasEverClaimed(q, player)
                && !hasCreatedScroll(q.id, player);
    }

    public static boolean canRestartRepeatable(QuestData.Quest q, Player player) {
        return q != null && q.repeatable && player != null && getStatus(q, player) == Status.REDEEMED;
    }

    public static boolean consumeXpTarget(ServerPlayer player, QuestData.Target target) {
        if (player == null || target == null || !target.isXp()) return false;
        int amount = Math.max(0, target.count);
        if (amount <= 0) return true;
        String mode = normalizeXpType(target.id);
        if ("levels".equals(mode)) {
            ExperienceSnapshot next = consumeExperience(new ExperienceSnapshot(player.experienceLevel, player.experienceProgress), mode, amount);
            if (next == null) return false;
            setExperienceSnapshot(player, next);
            return true;
        }

        int current = currentExperiencePoints(player);
        if (current < amount) return false;
        setTotalExperience(player, current - amount);
        return true;
    }

    public static int currentExperiencePoints(Player player) {
        if (player == null) return 0;
        EvaluationCache cache = evaluationCacheFor(player);
        if (cache != null && cache.xpPoints != null) return cache.xpPoints;
        int level = Math.max(0, player.experienceLevel);
        int intoLevel = (int) Math.floor(Math.max(0.0F, player.experienceProgress) * xpNeededForNextLevel(level));
        int total = Math.max(0, experienceForLevel(level) + intoLevel);
        if (cache != null) cache.xpPoints = total;
        return total;
    }

    public static ExperienceSnapshot consumeExperience(ExperienceSnapshot snapshot, String xpType, int amount) {
        if (snapshot == null) return null;
        int sanitizedAmount = Math.max(0, amount);
        int level = Math.max(0, snapshot.level());
        float progress = Math.max(0.0F, Math.min(1.0F, snapshot.progress()));
        if (sanitizedAmount <= 0) return new ExperienceSnapshot(level, progress);

        if ("levels".equals(normalizeXpType(xpType))) {
            if (level < sanitizedAmount) return null;
            return new ExperienceSnapshot(level - sanitizedAmount, progress);
        }

        int total = experienceForLevel(level) + (int) Math.floor(progress * xpNeededForNextLevel(level));
        if (total < sanitizedAmount) return null;
        return snapshotFromTotalExperience(total - sanitizedAmount);
    }

    public static ExperienceSnapshot snapshotFromTotalExperience(int totalExperience) {
        int total = Math.max(0, totalExperience);
        int level = 0;
        while (experienceForLevel(level + 1) <= total) {
            level++;
        }
        int base = experienceForLevel(level);
        int needed = xpNeededForNextLevel(level);
        float progress = needed <= 0 ? 0.0F : (float) (total - base) / (float) needed;
        return new ExperienceSnapshot(level, Math.max(0.0F, Math.min(1.0F, progress)));
    }

    public static void setExperienceSnapshot(ServerPlayer player, ExperienceSnapshot snapshot) {
        if (player == null || snapshot == null) return;
        int level = Math.max(0, snapshot.level());
        int total = experienceForLevel(level)
                + (int) Math.floor(Math.max(0.0F, Math.min(1.0F, snapshot.progress())) * xpNeededForNextLevel(level));
        setTotalExperience(player, total);
    }

    private static int experienceForLevel(int level) {
        int sanitized = Math.max(0, level);
        if (sanitized <= 16) return sanitized * sanitized + 6 * sanitized;
        if (sanitized <= 31) return (int) Math.floor(2.5D * sanitized * sanitized - 40.5D * sanitized + 360.0D);
        return (int) Math.floor(4.5D * sanitized * sanitized - 162.5D * sanitized + 2220.0D);
    }

    private static int xpNeededForNextLevel(int level) {
        int sanitized = Math.max(0, level);
        if (sanitized >= 30) return 112 + (sanitized - 30) * 9;
        return sanitized >= 15 ? 37 + (sanitized - 15) * 5 : 7 + sanitized * 2;
    }

    private static void setTotalExperience(ServerPlayer player, int totalExperience) {
        int clamped = Math.max(0, totalExperience);
        player.totalExperience = 0;
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
        if (clamped > 0) {
            player.giveExperiencePoints(clamped);
        }
    }

    private static void giveExpReward(ServerPlayer player, QuestData.Rewards rewards) {
        if (player == null || rewards == null || !rewards.hasExp()) return;
        int amount = Math.max(0, rewards.expAmount);
        if (amount <= 0) return;
        try {
            if ("levelup".equalsIgnoreCase(rewards.expType)) {
                LevelUpCompat.awardXp(player, amount);
                return;
            }
            if ("levelup_levels".equalsIgnoreCase(rewards.expType)) {
                LevelUpCompat.awardLevels(player, amount);
                return;
            }
            if ("levels".equalsIgnoreCase(rewards.expType)) {
                player.giveExperienceLevels(amount);
            } else {
                player.giveExperiencePoints(amount);
            }
        } catch (Throwable t) {
            BoundlessMod.LOGGER.error("Failed to grant exp reward for player {} quest rewards type={} amount={}",
                    player.getGameProfile().getName(), rewards.expType, amount, t);
        }
    }

    private static void giveLootRewards(ServerPlayer player, QuestData.Rewards rewards) {
        if (player == null || rewards == null || !rewards.hasLootTables()) return;
        for (QuestData.LootTableReward reward : rewards.lootTables) {
            if (reward == null || reward.lootTable == null || reward.lootTable.isBlank()) continue;
            ResourceLocation rl = ResourceLocation.tryParse(reward.lootTable);
            if (rl == null) continue;
            LootTable table;
            try {
                table = player.server.reloadableRegistries()
                        .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, rl));
            } catch (Throwable ignored) {
                table = null;
            }
            if (table == null || table == LootTable.EMPTY) continue;

            LootParams params = new LootParams.Builder(player.serverLevel())
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .create(LootContextParamSets.GIFT);
            ObjectArrayList<ItemStack> generated = table.getRandomItems(params, player.getRandom());
            for (ItemStack stack : generated) {
                if (stack == null || stack.isEmpty()) continue;
                try {
                    ItemStack copy = stack.copy();
                    if (!player.getInventory().add(copy) && !copy.isEmpty()) {
                        player.drop(copy, false);
                    }
                } catch (Throwable t) {
                    BoundlessMod.LOGGER.error("Failed to grant loot-table reward {} to player {}",
                            reward.lootTable, player.getGameProfile().getName(), t);
                }
            }
        }
    }

    private static void giveItemRewards(ServerPlayer player, QuestData.Quest q) {
        if (player == null || q == null || q.rewards == null || q.rewards.items == null) return;
        for (QuestData.RewardEntry r : q.rewards.items) {
            if (r == null || r.acceptedItemsOrLegacy().isEmpty()) continue;
            try {
                List<String> acceptedItems = r.acceptedItemsOrLegacy();
                Map<String, Integer> grantedCounts = new LinkedHashMap<>();
                for (int i = 0; i < Math.max(1, r.count); i++) {
                    String rewardId = acceptedItems.get(player.getRandom().nextInt(acceptedItems.size()));
                    grantedCounts.merge(rewardId, 1, Integer::sum);
                }
                for (Map.Entry<String, Integer> granted : grantedCounts.entrySet()) {
                    String rewardId = granted.getKey();
                    int amount = granted.getValue() == null ? 0 : granted.getValue();
                    if (amount <= 0) continue;
                    QuestItemSpec spec = QuestItemSpec.parse(rewardId);
                    ResourceLocation rl = tryParseCached(spec.id);
                    if (rl == null) {
                        BoundlessMod.LOGGER.warn("Skipping invalid item reward '{}' for quest {}", rewardId, q.id);
                        continue;
                    }
                    Item item = spec.item();
                    if (item == null) {
                        BoundlessMod.LOGGER.warn("Skipping missing item reward '{}' for quest {}", rewardId, q.id);
                        continue;
                    }
                    ItemStack stack = createRewardStack(player, spec, item, Math.max(1, amount));
                    if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                        player.drop(stack, false);
                    }
                }
            } catch (Throwable t) {
                BoundlessMod.LOGGER.error("Failed to grant item reward {} x{} for quest {} to player {}",
                        r.acceptedItemsOrLegacy(), Math.max(1, r.count), q.id, player.getGameProfile().getName(), t);
            }
        }
    }

    private static void giveAdvancementRewards(ServerPlayer player, QuestData.Rewards rewards) {
        if (player == null || rewards == null || !rewards.hasAdvancements()) return;
        for (QuestData.AdvancementReward reward : rewards.advancements) {
            if (reward == null || reward.advancement == null || reward.advancement.isBlank()) continue;
            try {
                ResourceLocation rl = ResourceLocation.tryParse(reward.advancement);
                if (rl == null) continue;
                AdvancementHolder advancement = player.server.getAdvancements().get(rl);
                if (advancement == null) continue;
                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(advancement, criterion);
                }
            } catch (Throwable t) {
                BoundlessMod.LOGGER.error("Failed to grant advancement reward {} to player {}",
                        reward.advancement, player.getGameProfile().getName(), t);
            }
        }
    }

    private static void giveToastRewards(ServerPlayer player, QuestData.Rewards rewards) {
        if (player == null || rewards == null || !rewards.hasToasts()) return;
        for (QuestData.ToastReward reward : rewards.toasts) {
            if (reward == null) continue;
            BoundlessNetwork.sendRewardToast(player, reward.title, reward.description, reward.icon);
        }
    }

    private static ItemStack createRewardStack(ServerPlayer player, QuestItemSpec spec, Item item, int count) {
        if (player != null && spec != null && !spec.components.isBlank()) {
            try {
                ItemParser.ItemResult parsed = new ItemParser(player.registryAccess()).parse(new StringReader(spec.commandSyntax()));
                return new ItemInput(parsed.item(), parsed.components()).createItemStack(count, false);
            } catch (CommandSyntaxException e) {
                BoundlessMod.LOGGER.warn("Could not parse item reward components '{}'; granting base item", spec.serialized(), e);
            } catch (Throwable t) {
                BoundlessMod.LOGGER.warn("Could not apply item reward components '{}'; granting base item", spec.serialized(), t);
            }
        }
        return new ItemStack(item, count);
    }

    private static void runCommandRewards(ServerPlayer player, QuestData.Quest q) {
        if (player == null || q == null || q.rewards == null) return;
        CommandSourceStack css = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        LinkedHashSet<String> toRun = new LinkedHashSet<>();

        if (q.rewards.commands != null) {
            for (QuestData.CommandReward cr : q.rewards.commands) {
                if (cr == null || cr.command == null) continue;
                String cmd = cr.command.trim();
                if (cmd.isBlank()) continue;
                if (cmd.startsWith("/")) cmd = cmd.substring(1).trim();
                toRun.add(cmd);
            }
        }

        if (q.rewards.functions != null) {
            for (QuestData.FunctionReward fr : q.rewards.functions) {
                if (fr == null || fr.function == null) continue;
                String fn = fr.function.trim();
                if (fn.isBlank()) continue;
                if (fn.startsWith("/")) fn = fn.substring(1).trim();
                if (fn.regionMatches(true, 0, "function", 0, "function".length())) {
                    fn = fn.substring("function".length()).trim();
                }
                toRun.add("function " + fn);
            }
        }

        for (String cmd : toRun) {
            try {
                player.server.getCommands().performPrefixedCommand(css, cmd);
            } catch (Throwable t) {
                BoundlessMod.LOGGER.error("Failed to execute quest reward command '{}' for quest {} player {}",
                        cmd, q.id, player.getGameProfile().getName(), t);
            }
        }
    }

    private static void clearQuestCycle(ServerPlayer player, QuestData.Quest q) {
        if (player == null || q == null) return;
        QuestObjectiveState.get(player.serverLevel()).clearQuest(player.getUUID(), q.id);
    }

    public static void markQuestClaimed(ServerPlayer player, QuestData.Quest q) {
        if (player == null || q == null) return;
        QuestProgressState.get(player.serverLevel()).incrementClaimCount(player.getUUID(), q.id);
    }

    public static boolean serverRedeem(QuestData.Quest q, ServerPlayer player) {
        if (q == null || player == null) return false;

        Status current = getServerStatus(player, q.id);
        if (current == Status.REDEEMED || current == Status.REJECTED) return false;
        markQuestClaimed(player, q);
        clearQuestCycle(player, q);
        setServerStatus(player, q.id, Status.REDEEMED);
        giveItemRewards(player, q);
        runCommandRewards(player, q);
        giveLootRewards(player, q.rewards);
        giveAdvancementRewards(player, q.rewards);
        giveExpReward(player, q.rewards);
        giveToastRewards(player, q.rewards);
        return true;
    }

    public static void forceCompleteWithoutRewards(QuestData.Quest q, ServerPlayer player) {
        if (q == null || player == null) return;
        Status current = getServerStatus(player, q.id);
        if (current == Status.REDEEMED || current == Status.REJECTED) return;
        setServerStatus(player, q.id, Status.COMPLETED);
        BoundlessNetwork.sendStatus(player, q.id, Status.COMPLETED.name());
    }

    public static boolean restartRepeatable(QuestData.Quest q, ServerPlayer player) {
        if (q == null || player == null || !q.repeatable) return false;
        if (getServerStatus(player, q.id) != Status.REDEEMED) return false;
        clearQuestCycle(player, q);
        setServerStatus(player, q.id, Status.INCOMPLETE);
        return true;
    }

    public static boolean serverReject(QuestData.Quest q, ServerPlayer player) {
        if (q == null || player == null) return false;
        if (!q.optional) return false;
        setServerStatus(player, q.id, Status.REJECTED);
        return true;
    }

    public static boolean serverUndoReject(QuestData.Quest q, ServerPlayer player) {
        if (q == null || player == null) return false;
        if (!q.optional) return false;
        if (getServerStatus(player, q.id) != Status.REJECTED) return false;
        setServerStatus(player, q.id, Status.INCOMPLETE);
        return true;
    }

    public static void reset(Player player) {
        if (player instanceof ServerPlayer sp) {
            QuestProgressState.get(sp.serverLevel()).clear(sp.getUUID());
            QuestObjectiveState.get(sp.serverLevel()).clearPlayer(sp.getUUID());
            clearServerRuntimeState(sp);
            BoundlessNetwork.syncPlayer(sp);
            CLIENT_EFFECT_PROGRESS.clear();
            if (FMLEnvironment.dist == Dist.CLIENT) clientClearAll();
            return;
        }
        if (player != null && player.level().isClientSide) clientClearAll();
    }

    public static void clientSetStatus(String questId, Status st) {
        if (questId == null || st == null) return;
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try { ensureClientStateLoaded(null); } catch (Throwable ignored) {}
        }
        if (st == Status.INCOMPLETE) {
            activeStateMap().remove(questId);
            clearClientObjectiveForQuest(questId);
        } else {
            activeStateMap().put(questId, st);
            if (st == Status.REDEEMED) clearClientObjectiveForQuest(questId);
        }
        if (FMLEnvironment.dist == Dist.CLIENT && ACTIVE_KEY != null) ClientOnly.saveClientState(ACTIVE_KEY);
    }

    private static void clearClientObjectiveForQuest(String questId) {
        if (questId == null || questId.isBlank()) return;
        CLIENT_ITEM_PROGRESS.entrySet().removeIf(e -> e.getKey() != null && e.getKey().startsWith(questId + ":"));
        CLIENT_EFFECT_PROGRESS.entrySet().removeIf(e -> e.getKey() != null && e.getKey().startsWith(questId + ":"));
        CLIENT_INPUT_PROGRESS.entrySet().removeIf(e -> e.getKey() != null && e.getKey().startsWith(questId + ":"));
        CLIENT_REPORTED_OBSERVE.removeIf(key -> key != null && key.startsWith(questId + ":observe:"));
    }

    public static void clientSetItemProgress(String key, int count) {
        if (key == null || key.isBlank()) return;
        int sanitized = Math.max(0, count);
        if (sanitized <= 0) CLIENT_ITEM_PROGRESS.remove(key);
        else CLIENT_ITEM_PROGRESS.put(key, sanitized);
    }

    public static void clientSetFlagProgress(String key, boolean done) {
        if (key == null || key.isBlank()) return;
        if (done) CLIENT_EFFECT_PROGRESS.put(key, true);
        else {
            CLIENT_EFFECT_PROGRESS.remove(key);
            CLIENT_REPORTED_OBSERVE.remove(key);
        }
    }

    public static void clientSetInputProgress(String key, String value) {
        if (key == null || key.isBlank()) return;
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) CLIENT_INPUT_PROGRESS.remove(key);
        else CLIENT_INPUT_PROGRESS.put(key, normalized);
    }

    public static void clientSetClaimCount(String questId, int count) {
        if (questId == null || questId.isBlank()) return;
        int sanitized = Math.max(0, count);
        if (sanitized <= 0) CLIENT_CLAIM_COUNTS.remove(questId);
        else CLIENT_CLAIM_COUNTS.put(questId, sanitized);
    }

    public static void clientSetScrollRedeemed(String questId, boolean redeemed) {
        if (questId == null || questId.isBlank()) return;
        if (redeemed) CLIENT_SCROLL_REDEEMED.put(questId, true);
        else CLIENT_SCROLL_REDEEMED.remove(questId);
    }

    public static void clientSetScrollCreated(String questId, boolean created) {
        if (questId == null || questId.isBlank()) return;
        if (created) CLIENT_SCROLL_CREATED.put(questId, true);
        else CLIENT_SCROLL_CREATED.remove(questId);
    }

    public static void clientSetKill(String entityId, int count) {
        CLIENT_KILLS.put(entityId, Math.max(0, count));
    }

    public static void clientClearAll() {
        CLIENT_KILLS.clear();
        CLIENT_ADV_DONE.clear();
        CLIENT_ITEM_PROGRESS.clear();
        CLIENT_EFFECT_PROGRESS.clear();
        CLIENT_INPUT_PROGRESS.clear();
        CLIENT_CLAIM_COUNTS.clear();
        CLIENT_SCROLL_REDEEMED.clear();
        CLIENT_SCROLL_CREATED.clear();
        CLIENT_REPORTED_OBSERVE.clear();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try { ensureClientStateLoaded(null); } catch (Throwable ignored) {}
            activeStateMap().clear();
            if (ACTIVE_KEY != null) ClientOnly.saveClientState(ACTIVE_KEY);
        }
    }

    public static void tickPlayer(Player player) {
        if (player == null || !player.level().isClientSide) return;
        if (player.tickCount % 10 == 0) {
            clientReportObservedTargets(player);
        }
        if (isClientMultiplayer()) return;

        ensureClientStateLoaded(player);
        QuestData.loadClient(false);

        for (QuestData.Quest q : QuestData.all()) {
            if (q == null) continue;

            Status cur = getStatus(q, player);
            if (cur == Status.REDEEMED || cur == Status.REJECTED) continue;

            boolean ready = updateProgressAndCheckReady(q, player);
            boolean hasItemTargets = hasItemOrSubmitTargets(q);

            if (ready && cur == Status.INCOMPLETE) {
                clientSetStatus(q.id, Status.COMPLETED);
                sendToastLocal(q.id);
                continue;
            }

            if (hasItemTargets && cur == Status.COMPLETED) continue;

            if (!ready && cur == Status.COMPLETED) clientSetStatus(q.id, Status.INCOMPLETE);
        }
    }

    private static void clientReportObservedTargets(Player player) {
        if (player == null || !player.level().isClientSide || !isClientMultiplayer()) return;
        QuestData.loadClient(false);
        for (QuestData.Quest quest : QuestData.all()) {
            if (quest == null || quest.completion == null || quest.completion.targets == null) continue;
            for (QuestData.Target target : quest.completion.targets) {
                if (target == null || !target.isObserve() || target.id == null || target.id.isBlank()) continue;
                String key = flagProgressKey(quest, target);
                if (CLIENT_EFFECT_PROGRESS.getOrDefault(key, false) || CLIENT_REPORTED_OBSERVE.contains(key)) continue;
                if (!isObservingTarget(player, target.id)) continue;
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        new net.revilodev.boundless.network.BoundlessNetwork.ReportObserve(quest.id, target.id)
                );
                CLIENT_REPORTED_OBSERVE.add(key);
            }
        }
    }

    public static void serverTickPlayer(ServerPlayer sp) {
        if (sp == null) return;
        long startedAt = BoundlessDebug.enabled() ? System.nanoTime() : 0L;
        int dirtyMask = consumeServerDirtyMask(sp);
        if (dirtyMask == 0) return;
        List<QuestData.Quest> quests = new ArrayList<>(QuestData.allServer(sp.server));
        int total = quests.size();
        if (total <= 0) {
            SERVER_QUEST_SCAN_CURSOR.remove(sp.getUUID());
            return;
        }

        int start = Math.floorMod(SERVER_QUEST_SCAN_CURSOR.getOrDefault(sp.getUUID(), 0), total);
        int batch = Math.min(SERVER_QUEST_SCAN_BATCH, total);

        EvaluationCache cache = new EvaluationCache(sp.getUUID());
        EVALUATION_CACHE.set(cache);
        try {
            // Observe targets are reported by multiplayer clients. Avoid rescanning every
            // context target when only inventory/xp/effect state changed.
            boolean contextChanged = (dirtyMask & DIRTY_CONTEXT) != 0;
            if (contextChanged || shouldUseServerObserveDetection(sp)) {
                refreshPersistentContextTargets(sp, quests, start, batch,
                        shouldUseServerObserveDetection(sp), contextChanged, contextChanged);
            }
            for (int processed = 0; processed < batch; processed++) {
                QuestData.Quest q = quests.get((start + processed) % total);
                if (q == null || !questNeedsEvaluationForMask(q, dirtyMask)) continue;
                if (Config.disabledCategories().contains(q.category)) continue;

                Status cur = getServerStatus(sp, q.id);
                if (cur == Status.REDEEMED || cur == Status.REJECTED) continue;

                boolean ready = updateProgressAndCheckReady(q, sp);
                boolean hasItemTargets = hasItemOrSubmitTargets(q);

                if (ready && cur == Status.INCOMPLETE) {
                    if (shouldAutoClaim(q)) {
                        BoundlessNetwork.claimQuest(sp, q);
                    } else {
                        setServerStatus(sp, q.id, Status.COMPLETED);
                        BoundlessNetwork.sendStatus(sp, q.id, Status.COMPLETED.name());
                    }
                    continue;
                }

                if (hasItemTargets && cur == Status.COMPLETED) continue;

                if (!ready && cur == Status.COMPLETED) {
                    setServerStatus(sp, q.id, Status.INCOMPLETE);
                    BoundlessNetwork.sendStatus(sp, q.id, Status.INCOMPLETE.name());
                }
            }
        } finally {
            EVALUATION_CACHE.remove();
        }
        int next = (start + batch) % total;
        if (batch < total) {
            // Keep the dirty state until every quest has been checked. The previous code
            // defined this batch limit but accidentally scanned the full quest list anyway.
            SERVER_QUEST_SCAN_CURSOR.put(sp.getUUID(), next);
            SERVER_DIRTY_MASKS.merge(sp.getUUID(), dirtyMask, (a, b) -> a | b);
        } else {
            SERVER_QUEST_SCAN_CURSOR.remove(sp.getUUID());
        }
        BoundlessNetwork.sendObjectiveProgress(sp);
        if (startedAt != 0L) {
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
            BoundlessDebug.rateLimited("quest-evaluation:" + sp.getUUID(), 5_000L,
                    "player={}, quests={}/{}, dirtyMask={}, elapsed={}ms, pending={}",
                    sp.getGameProfile().getName(), batch, total, dirtyMask, elapsedMillis, batch < total);
        }
    }

    private static int consumeServerDirtyMask(ServerPlayer sp) {
        int detected = detectServerDirtyMask(sp);
        int pending = SERVER_DIRTY_MASKS.getOrDefault(sp.getUUID(), 0);
        SERVER_DIRTY_MASKS.remove(sp.getUUID());
        return detected | pending;
    }

    private static int detectServerDirtyMask(ServerPlayer sp) {
        ServerStateSnapshot current = captureServerState(sp);
        ServerStateSnapshot previous = SERVER_STATE_SNAPSHOTS.put(sp.getUUID(), current);
        if (previous == null) return DIRTY_ALL;
        int mask = 0;
        if (previous.inventoryHash != current.inventoryHash) mask |= DIRTY_INVENTORY;
        if (previous.effectHash != current.effectHash) mask |= DIRTY_EFFECTS;
        if (previous.xpPoints != current.xpPoints) mask |= DIRTY_XP;
        if (!Objects.equals(previous.biomeId, current.biomeId) || !Objects.equals(previous.dimensionId, current.dimensionId)) {
            mask |= DIRTY_CONTEXT;
        }
        return mask;
    }

    private static ServerStateSnapshot captureServerState(ServerPlayer sp) {
        return new ServerStateSnapshot(
                inventoryHash(sp),
                effectHash(sp),
                currentExperiencePoints(sp),
                currentBiomeId(sp),
                currentDimensionId(sp)
        );
    }

    private static long inventoryHash(Player player) {
        if (player == null) return 0L;
        long hash = 1L;
        var inventory = player.getInventory();
        int size = inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inventory.getItem(i);
            hash = 31L * hash + stack.getCount();
            if (stack.isEmpty()) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            hash = 31L * hash + (itemId == null ? 0 : itemId.hashCode());
            hash = 31L * hash + stack.getComponentsPatch().hashCode();
        }
        return hash;
    }

    private static long effectHash(Player player) {
        if (player == null) return 0L;
        long hash = 1L;
        for (var effect : player.getActiveEffects()) {
            if (effect == null) continue;
            ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
            hash = 31L * hash + (effectId == null ? 0 : effectId.hashCode());
            hash = 31L * hash + effect.getAmplifier();
        }
        return hash;
    }

    private static String currentBiomeId(Player player) {
        if (player == null) return null;
        EvaluationCache cache = evaluationCacheFor(player);
        if (cache != null && cache.biomeId != null) return cache.biomeId;
        String biomeId = player.level().getBiome(player.blockPosition())
                .unwrapKey()
                .map(key -> key.location().toString())
                .orElse("");
        if (cache != null) cache.biomeId = biomeId;
        return biomeId;
    }

    private static String currentDimensionId(Player player) {
        if (player == null) return null;
        EvaluationCache cache = evaluationCacheFor(player);
        if (cache != null && cache.dimensionId != null) return cache.dimensionId;
        String dimensionId = player.level().dimension().location().toString();
        if (cache != null) cache.dimensionId = dimensionId;
        return dimensionId;
    }

    private static EvaluationCache evaluationCacheFor(Player player) {
        if (player == null) return null;
        EvaluationCache cache = EVALUATION_CACHE.get();
        if (cache == null || !player.getUUID().equals(cache.playerId)) return null;
        return cache;
    }

    private static String acceptedIdsCacheKey(List<String> acceptedIds) {
        if (acceptedIds == null || acceptedIds.isEmpty()) return "";
        if (acceptedIds.size() == 1) return acceptedIds.get(0);
        List<String> copy = new ArrayList<>(acceptedIds);
        copy.sort(String::compareTo);
        return String.join("|", copy);
    }

    private static boolean questNeedsEvaluationForMask(QuestData.Quest q, int dirtyMask) {
        if (q == null) return false;
        if ((dirtyMask & DIRTY_ALL) == DIRTY_ALL) return true;
        if (q.completion == null || q.completion.targets == null) return false;
        for (QuestData.Target target : q.completion.targets) {
            if (target == null) continue;
            if ((dirtyMask & DIRTY_INVENTORY) != 0 && (target.isItem() || target.isSubmit())) return true;
            if ((dirtyMask & DIRTY_EFFECTS) != 0 && target.isEffect()) return true;
            if ((dirtyMask & DIRTY_XP) != 0 && (target.isXp() || target.isLevelUpLevel())) return true;
            if ((dirtyMask & DIRTY_CONTEXT) != 0 && (target.isBiome() || target.isDimension())) return true;
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientOnly {

        private static boolean isClientMultiplayerFlag(boolean fallback) {
            try {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc == null) return fallback;
                if (mc.getConnection() == null) return false;
                return !mc.hasSingleplayerServer();
            } catch (Throwable ignored) {}
            return fallback;
        }

        private static int adjustItemProgress(String key, int current, int required, int prev, int now) {
            try {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc != null && mc.player != null) {
                    String questId = resolveQuestIdFromProgressKey(key);
                    if (!questId.isBlank()) {
                        if (getStatus(questId, mc.player) == Status.INCOMPLETE) return Math.min(current, required);
                    }
                }
            } catch (Throwable ignored) {}
            return now;
        }

        private static String resolveQuestIdFromProgressKey(String key) {
            if (key == null || key.isBlank()) return "";
            try {
                QuestData.loadClient(false);
                String best = "";
                for (QuestData.Quest quest : QuestData.all()) {
                    if (quest == null || quest.id == null || quest.id.isBlank()) continue;
                    String prefix = quest.id + ":";
                    if (!key.startsWith(prefix)) continue;
                    if (quest.id.length() > best.length()) best = quest.id;
                }
                if (!best.isBlank()) return best;
            } catch (Throwable ignored) {}

            int idx = key.indexOf(':');
            if (idx > 0) return key.substring(0, idx);
            return "";
        }

        private static String computeClientKey() {
            try {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc == null) return "default";
                if (mc.getSingleplayerServer() != null) {
                    String name = mc.getSingleplayerServer().getWorldData().getLevelName();
                    if (name == null || name.isBlank()) name = "world";
                    return "sp_" + sanitize(name);
                }
                if (mc.getCurrentServer() != null) {
                    String ip = mc.getCurrentServer().ip;
                    if (ip == null || ip.isBlank()) ip = "multiplayer";
                    return "mp_" + sanitize(ip);
                }
            } catch (Throwable ignored) {}
            return "default";
        }

        private static Path clientSavePath(String key) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            File dir = new File(mc.gameDirectory, "config/boundless/quest_state");
            return new File(dir, key + ".json").toPath();
        }

        private static void loadClientState(String key) {
            Map<String, Status> map = WORLD_STATES.computeIfAbsent(key, k -> new LinkedHashMap<>());
            map.clear();
            try {
                Path p = clientSavePath(key);
                if (!Files.exists(p)) return;
                try (BufferedReader r = new BufferedReader(new FileReader(p.toFile()))) {
                    JsonObject obj = GSON.fromJson(r, JsonObject.class);
                    if (obj == null) return;
                    for (String qid : obj.keySet()) {
                        try {
                            Status st = decodeStatus(obj.get(qid).getAsString());
                            map.put(qid, st);
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }

        private static void saveClientState(String key) {
            try {
                Path p = clientSavePath(key);
                Files.createDirectories(p.getParent());
                JsonObject obj = new JsonObject();
                Map<String, Status> map = WORLD_STATES.get(key);
                if (map != null) map.forEach((qid, st) -> obj.addProperty(qid, st.name()));
                try (BufferedWriter w = new BufferedWriter(new FileWriter(p.toFile()))) {
                    GSON.toJson(obj, w);
                }
            } catch (Throwable ignored) {}
        }
    }
}
