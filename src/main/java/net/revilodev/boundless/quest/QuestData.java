package net.revilodev.boundless.quest;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.BoundlessDebug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class QuestData {
    private QuestData() {}

    // quest pack json loader
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String PATH_QUESTS = "quests";
    private static final String PATH_CATEGORIES = "quests/categories";
    private static final String PATH_SUBCATEGORIES = "quests/subcategories";
    private static final String PATH_SUBCATEGORIES_ALT = "quests/sub_categories";
    private static final String PATH_SUBCATEGORY = "quests/subcategory";
    private static final String PATH_SUBCATEGORY_ALT = "quests/sub-category";
    private static final Path INSTANCE_QUEST_PACKS_ROOT =
            FMLPaths.GAMEDIR.get().resolve("config").resolve("boundless").resolve("questpacks");

    private static final DataStore CLIENT_DATA = new DataStore();
    private static final DataStore SERVER_DATA = new DataStore();

    // this keeps client and server quest data from replacing each other
    private static final class DataStore {
        final Map<String, Quest> quests = new LinkedHashMap<>();
        final Map<String, Category> categories = new LinkedHashMap<>();
        final Map<String, SubCategory> subCategories = new LinkedHashMap<>();
        final Collection<Quest> questView = Collections.unmodifiableCollection(quests.values());
        boolean loaded;
        boolean networkAuthoritative;
        String worldId;

        void clear() {
            quests.clear();
            categories.clear();
            subCategories.clear();
        }
    }

    public record NetworkSnapshot(
            Map<String, Quest> quests,
            Map<String, Category> categories,
            Map<String, SubCategory> subCategories) {}

    // immutable quest definition
    public static final class Quest {
        public final String id;
        public final String name;
        public final String icon;
        public final String description;
        public final String media;
        public final List<String> dependencies;
        public final List<String> incompatibleDependencies;
        public final boolean lockAfterDependency;
        public final boolean optional;
        public final boolean repeatable;
        public final boolean autoComplete;
        public final boolean hiddenUnderDependency;
        public final Rewards rewards;
        public final String type;
        public final Completion completion;
        public final String category;
        public final String subCategory;
        public final String sourcePath;
        public final int treeX;
        public final int treeY;
        public final String treeBackground;
        public final boolean treeOrigin;

        public Quest(String id, String name, String icon, String description,
                     List<String> dependencies, boolean lockAfterDependency, boolean optional, boolean repeatable,
                     boolean autoComplete,
                     boolean hiddenUnderDependency, Rewards rewards,
                     String type, Completion completion, String category,
                     String subCategory, String sourcePath) {
            this(id, name, icon, description, dependencies, lockAfterDependency, optional, repeatable, autoComplete,
                    hiddenUnderDependency, rewards, type, completion, category, subCategory, sourcePath, -1, -1, "square");
        }

        public Quest(String id, String name, String icon, String description,
                     List<String> dependencies, boolean lockAfterDependency, boolean optional, boolean repeatable,
                     boolean autoComplete, boolean hiddenUnderDependency, Rewards rewards,
                     String type, Completion completion, String category, String subCategory, String sourcePath,
                     int treeX, int treeY, String treeBackground) {
            this(id, name, icon, description, dependencies,
                    lockAfterDependency && dependencies != null ? dependencies : List.of(), lockAfterDependency,
                    optional, repeatable, autoComplete, hiddenUnderDependency, rewards, type, completion,
                    category, subCategory, sourcePath, treeX, treeY, treeBackground, false);
        }

        public Quest(String id, String name, String icon, String description,
                     List<String> dependencies, List<String> incompatibleDependencies,
                     boolean lockAfterDependency, boolean optional, boolean repeatable,
                     boolean autoComplete, boolean hiddenUnderDependency, Rewards rewards,
                     String type, Completion completion, String category, String subCategory, String sourcePath,
                     int treeX, int treeY, String treeBackground, boolean treeOrigin) {
            this(id, name, icon, description, dependencies, incompatibleDependencies, lockAfterDependency, optional,
                    repeatable, autoComplete, hiddenUnderDependency, rewards, type, completion, category, subCategory,
                    sourcePath, treeX, treeY, treeBackground, treeOrigin, "");
        }

        public Quest(String id, String name, String icon, String description,
                     List<String> dependencies, List<String> incompatibleDependencies,
                     boolean lockAfterDependency, boolean optional, boolean repeatable,
                     boolean autoComplete, boolean hiddenUnderDependency, Rewards rewards,
                     String type, Completion completion, String category, String subCategory, String sourcePath,
                     int treeX, int treeY, String treeBackground, boolean treeOrigin, String media) {

            this.id = Objects.requireNonNull(id);
            this.name = name == null ? id : name;
            this.icon = icon == null || icon.isBlank() ? "boundless:quest_book" : icon;
            this.description = description == null ? "" : description;
            this.media = media == null ? "" : media;
            this.dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
            this.incompatibleDependencies = lockAfterDependency
                    && (incompatibleDependencies == null || incompatibleDependencies.isEmpty())
                    ? this.dependencies
                    : incompatibleDependencies == null ? List.of() : List.copyOf(incompatibleDependencies);
            this.lockAfterDependency = lockAfterDependency;
            this.optional = optional;
            this.repeatable = repeatable;
            this.autoComplete = autoComplete;
            this.hiddenUnderDependency = hiddenUnderDependency;
            this.rewards = rewards;
            this.type = type == null ? "collection" : type;
            this.completion = completion;
            this.category = (category == null || category.isBlank()) ? "misc" : category;
            this.subCategory = subCategory == null ? "" : subCategory;
            this.sourcePath = sourcePath == null ? "" : sourcePath;
            this.treeX = treeX;
            this.treeY = treeY;
            this.treeBackground = normalizeTreeBackground(treeBackground);
            this.treeOrigin = treeOrigin;
        }

        private static String normalizeTreeBackground(String raw) {
            String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
            return switch (value) {
                case "circle", "diamond", "rounded_square", "square" -> value;
                default -> "square";
            };
        }

        public Optional<Item> iconItem() {
            // Texture paths are not registry ids, so they cannot produce an item icon
            try {
                if (icon == null || icon.isBlank() || icon.contains("textures/")) return Optional.empty();
                ResourceLocation rl = ResourceLocation.parse(icon);
                return BuiltInRegistries.ITEM.getOptional(rl);
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }

        public String sourceSortKey() {
            if (sourcePath != null && !sourcePath.isBlank()) {
                return sourcePath.toLowerCase(Locale.ROOT);
            }
            return id.toLowerCase(Locale.ROOT);
        }

        public String sourceFileName() {
            if (sourcePath == null || sourcePath.isBlank()) return id;
            int lastSlash = sourcePath.lastIndexOf('/');
            return lastSlash >= 0 ? sourcePath.substring(lastSlash + 1) : sourcePath;
        }
    }

    public static final class Rewards {
        public final List<RewardEntry> items;
        public final List<CommandReward> commands;
        public final List<FunctionReward> functions;
        public final List<LootTableReward> lootTables;
        public final List<AdvancementReward> advancements;
        public final List<ToastReward> toasts;
        public final String expType;
        public final int expAmount;

        public Rewards(List<RewardEntry> items,
                       List<CommandReward> commands,
                       List<FunctionReward> functions,
                       List<LootTableReward> lootTables,
                       List<AdvancementReward> advancements,
                       List<ToastReward> toasts,
                       String expType,
                       int expAmount) {
            this.items = items == null ? List.of() : List.copyOf(items);
            this.commands = commands == null ? List.of() : List.copyOf(commands);
            this.functions = functions == null ? List.of() : List.copyOf(functions);
            this.lootTables = lootTables == null ? List.of() : List.copyOf(lootTables);
            this.advancements = advancements == null ? List.of() : List.copyOf(advancements);
            this.toasts = toasts == null ? List.of() : List.copyOf(toasts);
            this.expType = expType == null ? "" : expType;
            this.expAmount = Math.max(0, expAmount);
        }

        public boolean hasExp() { return !expType.isBlank() && expAmount > 0; }
        public boolean hasCommands() { return commands != null && !commands.isEmpty(); }
        public boolean hasFunctions() { return functions != null && !functions.isEmpty(); }
        public boolean hasLootTables() { return lootTables != null && !lootTables.isEmpty(); }
        public boolean hasAdvancements() { return advancements != null && !advancements.isEmpty(); }
        public boolean hasToasts() { return toasts != null && !toasts.isEmpty(); }
        public boolean hasAny() {
            return (items != null && !items.isEmpty())
                    || hasCommands()
                    || hasFunctions()
                    || hasLootTables()
                    || hasAdvancements()
                    || hasToasts()
                    || hasExp();
        }
    }

    public static final class RewardEntry {
        public final String item;
        public final List<String> acceptedItems;
        public final int count;

        public RewardEntry(String item, int count) {
            this(item == null || item.isBlank() ? List.of() : List.of(item), count);
        }

        public RewardEntry(List<String> acceptedItems, int count) {
            List<String> normalized = new ArrayList<>();
            if (acceptedItems != null) {
                for (String acceptedItem : acceptedItems) {
                    if (acceptedItem == null) continue;
                    String trimmed = acceptedItem.trim();
                    if (!trimmed.isBlank() && !normalized.contains(trimmed)) normalized.add(trimmed);
                }
            }
            this.acceptedItems = List.copyOf(normalized);
            this.item = this.acceptedItems.isEmpty() ? "" : this.acceptedItems.get(0);
            this.count = Math.max(1, count);
        }

        public List<String> acceptedItemsOrLegacy() {
            return acceptedItems.isEmpty() && item != null && !item.isBlank() ? List.of(item) : acceptedItems;
        }
    }

    public static final class CommandReward {
        public final String command;
        public final String icon;
        public final String title;

        public CommandReward(String command, String icon, String title) {
            this.command = command == null ? "" : command;
            this.icon = icon == null ? "" : icon;
            this.title = title == null ? "" : title;
        }
    }

    public static final class FunctionReward {
        public final String function;
        public final String icon;
        public final String title;

        public FunctionReward(String function, String icon, String title) {
            this.function = function == null ? "" : function;
            this.icon = icon == null ? "" : icon;
            this.title = title == null ? "" : title;
        }
    }

    public static final class AdvancementReward {
        public final String advancement;

        public AdvancementReward(String advancement) {
            this.advancement = advancement == null ? "" : advancement;
        }
    }

    public static final class ToastReward {
        public final String title;
        public final String description;
        public final String icon;

        public ToastReward(String title, String description, String icon) {
            this.title = title == null ? "" : title;
            this.description = description == null ? "" : description;
            this.icon = icon == null ? "" : icon;
        }
    }

    public static final class Completion {
        public final List<Target> targets;

        public Completion(List<Target> targets) {
            this.targets = targets == null ? List.of() : List.copyOf(targets);
        }
    }

    public static final class Target {
        public final String kind;
        public final String id;
        public final List<String> acceptedIds;
        public final int count;
        public final String hint;

        public Target(String kind, String id, int count) {
            this(kind, id == null || id.isBlank() ? List.of() : List.of(id), count, "");
        }

        public Target(String kind, String id, int count, String hint) {
            this(kind, id == null || id.isBlank() ? List.of() : List.of(id), count, hint);
        }

        public Target(String kind, List<String> acceptedIds, int count) {
            this(kind, acceptedIds, count, "");
        }

        public Target(String kind, List<String> acceptedIds, int count, String hint) {
            this.kind = kind;
            List<String> normalized = new ArrayList<>();
            if (acceptedIds != null) {
                for (String acceptedId : acceptedIds) {
                    if (acceptedId == null) continue;
                    String trimmed = acceptedId.trim();
                    if (!trimmed.isBlank() && !normalized.contains(trimmed)) normalized.add(trimmed);
                }
            }
            this.acceptedIds = List.copyOf(normalized);
            this.id = this.acceptedIds.isEmpty() ? "" : this.acceptedIds.get(0);
            this.count = Math.max(1, count);
            this.hint = hint == null ? "" : hint;
        }

        public boolean isItem() { return "item".equals(kind); }
        public boolean isSubmit() { return "submit".equals(kind); }
        public boolean isEntity() { return "entity".equals(kind); }
        public boolean isEffect() { return "effect".equals(kind); }
        public boolean isAdvancement() { return "advancement".equals(kind); }
        public boolean isXp() { return "xp".equals(kind); }
        public boolean isLevelUpLevel() { return "levelup_level".equals(kind); }
        public boolean isFieldInput() { return "field".equals(kind); }
        public boolean isObserve() { return "observe".equals(kind); }
        public boolean isCheck() { return "check".equals(kind); }
        public boolean isBiome() { return "biome".equals(kind); }
        public boolean isDimension() { return "dimension".equals(kind); }
        public boolean hasMultipleAcceptedIds() { return acceptedIds.size() > 1; }
        public List<String> acceptedIdsOrLegacy() { return acceptedIds.isEmpty() && !id.isBlank() ? List.of(id) : acceptedIds; }
    }

    public static final class Category {
        public final String id;
        public final String icon;
        public final String name;
        public final int order;
        public final boolean excludeFromAll;
        public final String dependency;
        public final boolean autoComplete;
        public final boolean treeLayout;
        public final String background;
        public final boolean backgroundDarkening;
        public final float backgroundDarkeningOpacity;
        public final List<Sticker> stickers;

        public Category(String id, String icon, String name, int order,
                        boolean excludeFromAll, String dependency, boolean autoComplete) {
            this(id, icon, name, order, excludeFromAll, dependency, autoComplete, false, "");
        }

        public Category(String id, String icon, String name, int order,
                        boolean excludeFromAll, String dependency, boolean autoComplete, boolean treeLayout) {
            this(id, icon, name, order, excludeFromAll, dependency, autoComplete, treeLayout, "");
        }

        public Category(String id, String icon, String name, int order,
                        boolean excludeFromAll, String dependency, boolean autoComplete,
                        boolean treeLayout, String background) {
            this(id, icon, name, order, excludeFromAll, dependency, autoComplete, treeLayout, background, false, 0.45f);
        }

        public Category(String id, String icon, String name, int order,
                        boolean excludeFromAll, String dependency, boolean autoComplete,
                        boolean treeLayout, String background, boolean backgroundDarkening,
                        float backgroundDarkeningOpacity) {
            this(id, icon, name, order, excludeFromAll, dependency, autoComplete, treeLayout, background,
                    backgroundDarkening, backgroundDarkeningOpacity, List.of());
        }

        public Category(String id, String icon, String name, int order,
                        boolean excludeFromAll, String dependency, boolean autoComplete,
                        boolean treeLayout, String background, boolean backgroundDarkening,
                        float backgroundDarkeningOpacity, List<Sticker> stickers) {
            this.id = id;
            this.icon = icon == null || icon.isBlank() ? "minecraft:book" : icon;
            this.name = name == null || name.isBlank() ? id : name;
            this.order = order;
            this.excludeFromAll = excludeFromAll;
            this.dependency = dependency == null ? "" : dependency;
            this.autoComplete = autoComplete;
            this.treeLayout = treeLayout;
            this.background = background == null ? "" : background;
            this.backgroundDarkening = backgroundDarkening;
            this.backgroundDarkeningOpacity = Math.max(0f, Math.min(1f, backgroundDarkeningOpacity));
            this.stickers = stickers == null ? List.of() : List.copyOf(stickers);
        }

        public Optional<Item> iconItem() {
            try {
                if (icon == null || icon.isBlank() || icon.contains("textures/")) return Optional.empty();
                ResourceLocation rl = ResourceLocation.parse(icon);
                return BuiltInRegistries.ITEM.getOptional(rl);
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
    }

    public static final class LootTableReward {
        public final String lootTable;
        public final String icon;
        public final String title;

        public LootTableReward(String lootTable, String icon, String title) {
            this.lootTable = lootTable == null ? "" : lootTable;
            this.icon = icon == null ? "" : icon;
            this.title = title == null ? "" : title;
        }
    }

    public static final class SubCategory {
        public final String id;
        public final String category;
        public final String icon;
        public final String name;
        public final int order;
        public final boolean defaultOpen;
        public final List<String> quests;
        public final String sourcePath;

        public SubCategory(String id, String category, String icon, String name,
                           int order, boolean defaultOpen, List<String> quests, String sourcePath) {
            this.id = id == null ? "" : id;
            this.category = category == null ? "" : category;
            this.icon = icon == null || icon.isBlank() ? "minecraft:book" : icon;
            this.name = name == null || name.isBlank() ? this.id : name;
            this.order = order;
            this.defaultOpen = defaultOpen;
            this.quests = quests == null ? List.of() : List.copyOf(quests);
            this.sourcePath = sourcePath == null ? "" : sourcePath;
        }

        public Optional<Item> iconItem() {
            try {
                if (icon == null || icon.isBlank() || icon.contains("textures/")) return Optional.empty();
                ResourceLocation rl = ResourceLocation.parse(icon);
                return BuiltInRegistries.ITEM.getOptional(rl);
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }

        public String sourceSortKey() {
            if (sourcePath != null && !sourcePath.isBlank()) {
                String p = sourcePath;
                int lastSlash = p.lastIndexOf('/');
                String name = lastSlash >= 0 ? p.substring(lastSlash + 1) : p;
                return name.toLowerCase(Locale.ROOT);
            }
            return id.toLowerCase(Locale.ROOT);
        }
    }

    private static boolean isQuestDisabled(Quest q) {
        return Config.isCategoryDisabled(q.category);
    }

    private static boolean isBuiltinQuestNamespace(String namespace) {
        return namespace != null && namespace.equalsIgnoreCase("boundless");
    }

    private static boolean shouldSkipNamespace(String namespace) {
        return !Config.enableBuiltinQuestPack() && isBuiltinQuestNamespace(namespace);
    }

    private static boolean shouldIgnoreQuestJson(ResourceLocation loc) {
        if (loc == null) return false;
        String p = loc.getPath();
        if (p == null || p.isBlank()) return false;

        String pl = p.toLowerCase(Locale.ROOT);

        if (pl.equals("quests/example.json")) return true;
        if (pl.equals("quests/_example.json")) return true;

        if (pl.endsWith("/example.json")) return true;
        if (pl.endsWith("/_example.json")) return true;

        return false;
    }

    private static String subKey(String category, String subId) {
        String c = category == null ? "" : category;
        String s = subId == null ? "" : subId;
        return c + "::" + s;
    }

    private static boolean isSubCategoryPath(String path) {
        if (path == null || path.isBlank()) return false;
        String p = path.toLowerCase(Locale.ROOT);
        return p.contains("/sub-category/")
                || p.contains("/subcategories/")
                || p.contains("/sub_category/")
                || p.contains("/subcategory/");
    }

    private static String prettifyId(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String clean = raw.replace('_', ' ').replace('-', ' ').trim();
        String[] parts = clean.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) out.append(part.substring(1));
        }
        return out.toString();
    }

    public static void forceReloadAll(MinecraftServer server) {
        // Reload both resource-pack data and instance-local quest packs.
        CLIENT_DATA.loaded = false;
        SERVER_DATA.loaded = false;
        CLIENT_DATA.worldId = null;
        SERVER_DATA.worldId = null;
        loadServer(server, true);
    }

    private static synchronized void load(ResourceManager rm, boolean forceReload, DataStore data) {
        // The maps are rebuilt together so categories never point at stale quests.
        data.clear();

        Map<ResourceLocation, List<Resource>> catStacks =
                rm.listResourceStacks(PATH_CATEGORIES, rl -> rl.getPath().endsWith(".json"));

        for (var entry : catStacks.entrySet()) {
            if (shouldSkipNamespace(entry.getKey() == null ? null : entry.getKey().getNamespace())) continue;
            List<Resource> stack = entry.getValue();
            if (stack == null || stack.isEmpty()) continue;

            Resource top = stack.get(stack.size() - 1);
            try (Reader raw = top.openAsReader(); Reader reader = new BufferedReader(raw)) {
                JsonObject obj = safeObject(reader);
                if (obj == null) continue;

                String id = optString(obj, "id");
                if (id == null || id.isBlank()) continue;

                String icon = optString(obj, "icon");
                String cname = optString(obj, "name");
                int order = parseIntFlexible(obj, "order", 0);
                boolean excludeFromAll = parseBoolFlexible(obj, "exclude_from_all", false);
                String dependency = optString(obj, "dependency");
                boolean autoComplete = parseBoolFlexible(obj, "auto_complete",
                        parseBoolFlexible(obj, "autoComplete", false));
                boolean treeLayout = "tree".equalsIgnoreCase(optString(obj, "layout"));
                String background = optString(obj, "background");
                boolean backgroundDarkening = parseBoolFlexible(obj, "background_darkening", false);
                float backgroundDarkeningOpacity = parseFloatFlexible(obj, "background_darkening_opacity", 0.45f);
                List<Sticker> stickers = parseStickers(obj);

                data.categories.put(id, new Category(id, icon, cname, order, excludeFromAll, dependency,
                        autoComplete, treeLayout, background, backgroundDarkening, backgroundDarkeningOpacity, stickers));
            } catch (Exception e) {
                BoundlessDebug.warnRateLimited("category-parse:" + entry.getKey(), 5_000L,
                        "could not parse category {}", entry.getKey(), e);
            }
        }

        loadSubCategoriesFromManager(rm, PATH_SUBCATEGORIES, data);
        loadSubCategoriesFromManager(rm, PATH_SUBCATEGORIES_ALT, data);
        loadSubCategoriesFromManager(rm, PATH_SUBCATEGORY, data);
        loadSubCategoriesFromManager(rm, PATH_SUBCATEGORY_ALT, data);

        Map<ResourceLocation, List<Resource>> questStacks =
                rm.listResourceStacks(PATH_QUESTS, rl -> rl.getPath().endsWith(".json"));

        for (var entry : questStacks.entrySet()) {
            ResourceLocation loc = entry.getKey();
            if (shouldSkipNamespace(loc == null ? null : loc.getNamespace())) continue;
            String path = loc.getPath();
            if (path.contains("/categories/")) continue;
            if (isSubCategoryPath(path)) continue;
            if (shouldIgnoreQuestJson(loc)) continue;

            List<Resource> stack = entry.getValue();
            if (stack == null || stack.isEmpty()) continue;

            Resource top = stack.get(stack.size() - 1);
            try (Reader raw = top.openAsReader(); Reader reader = new BufferedReader(raw)) {
                JsonObject obj = safeObject(reader);
                if (obj == null) continue;

                Quest q = parseQuestObject(obj, loc);
                if (q != null && !isQuestDisabled(q)) data.quests.put(q.id, q);
            } catch (Exception e) {
                BoundlessDebug.warnRateLimited("quest-parse:" + loc, 5_000L,
                        "could not parse quest {}", loc, e);
            }
        }

        loadModQuestPacksFromInstance(data);

        ensureSubCategoriesFromQuests(data);

        if (!data.categories.containsKey("all")) {
            data.categories.put("all", new Category("all", "minecraft:book", "All",
                    Integer.MIN_VALUE, false, "", false));
        }
    }

    private static void loadModQuestPacksFromInstance(DataStore data) {
        // Instance packs are regular folders under the game's config directory.
        if (!Files.isDirectory(INSTANCE_QUEST_PACKS_ROOT)) return;
        QuestPackStorage.cleanupStagedQuestPacks(INSTANCE_QUEST_PACKS_ROOT);

        try (DirectoryStream<Path> packs = Files.newDirectoryStream(INSTANCE_QUEST_PACKS_ROOT)) {
            for (Path packRoot : packs) {
                if (!Files.isDirectory(packRoot)) continue;
                String folderName = packRoot.getFileName() == null ? "" : packRoot.getFileName().toString();
                if (!QuestPackStorage.isQuestPackDirectoryName(folderName)) continue;
                if (!isInstancePackEnabled(packRoot)) continue;
                loadModQuestPackData(packRoot, data);
            }
        } catch (Exception e) {
            BoundlessDebug.warnRateLimited("questpack-load", 5_000L,
                    "could not scan questpacks in {}", INSTANCE_QUEST_PACKS_ROOT, e);
        }
    }

    private static boolean isInstancePackEnabled(Path packRoot) {
        if (packRoot == null) return false;
        String id = packRoot.getFileName() == null ? "" : packRoot.getFileName().toString();
        Boolean enabledFromPackJson = readEnabledFlag(packRoot.resolve("boundless").resolve("pack.json"));
        boolean defaultEnabled;
        if (enabledFromPackJson != null) {
            defaultEnabled = enabledFromPackJson;
        } else {
            Boolean enabledFromMcmeta = readEnabledFlag(packRoot.resolve("pack.mcmeta"));
            defaultEnabled = enabledFromMcmeta == null || enabledFromMcmeta;
        }
        return Config.isQuestPackApplied(id, defaultEnabled);
    }

    private static Boolean readEnabledFlag(Path metaPath) {
        if (metaPath == null || !Files.exists(metaPath)) return null;
        try (Reader reader = Files.newBufferedReader(metaPath, StandardCharsets.UTF_8)) {
            JsonObject root = safeObject(reader);
            if (root == null || !root.has("boundless") || !root.get("boundless").isJsonObject()) return null;
            JsonObject boundless = root.getAsJsonObject("boundless");
            if (!boundless.has("enabled")) return null;
            JsonElement enabled = boundless.get("enabled");
            if (enabled == null || !enabled.isJsonPrimitive()) return null;
            JsonPrimitive p = enabled.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            return Boolean.parseBoolean(p.getAsString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void loadModQuestPackData(Path packRoot, DataStore data) {
        boolean treeLayout = readTreeLayout(packRoot.resolve("boundless").resolve("pack.json"));
        Path dataRoot = packRoot.resolve("data");
        if (!Files.isDirectory(dataRoot)) {
            String namespace = namespaceFromPackRoot(packRoot);
            Path directQuestsRoot = packRoot.resolve("quests");
            if (Files.isDirectory(directQuestsRoot)) {
                loadQuestNamespace(directQuestsRoot, namespace, data, treeLayout);
            }
            try (DirectoryStream<Path> namespaces = Files.newDirectoryStream(packRoot)) {
                for (Path namespaceDir : namespaces) {
                    if (!Files.isDirectory(namespaceDir)) continue;
                    if (!Files.isDirectory(namespaceDir.resolve("quests"))) continue;
                    loadQuestNamespace(namespaceDir.resolve("quests"), normalizeNamespace(namespaceDir.getFileName().toString()), data, treeLayout);
                }
            } catch (Exception e) {
                BoundlessDebug.warnRateLimited("questpack-data:" + packRoot.getFileName(), 5_000L,
                        "could not load questpack data from {}", packRoot, e);
            }
            return;
        }

        try (DirectoryStream<Path> namespaces = Files.newDirectoryStream(dataRoot)) {
            for (Path namespaceDir : namespaces) {
                if (!Files.isDirectory(namespaceDir)) continue;
                String ns = namespaceDir.getFileName() == null ? "" : normalizeNamespace(namespaceDir.getFileName().toString());

                Path questsRoot = namespaceDir.resolve("quests");
                if (!Files.isDirectory(questsRoot)) continue;

                loadQuestNamespace(questsRoot, ns, data, treeLayout);
            }
        } catch (Exception e) {
            BoundlessDebug.warnRateLimited("questpack-data:" + packRoot.getFileName(), 5_000L,
                    "could not load questpack data from {}", packRoot, e);
        }
    }

    private static void loadQuestNamespace(Path questsRoot, String namespace, DataStore data, boolean treeLayout) {
        if (!Files.isDirectory(questsRoot)) return;
        String ns = normalizeNamespace(namespace);
        // instance packs may use the boundless namespace without being built in data
        loadCategoriesFromPath(questsRoot.resolve("categories"), ns, data, treeLayout);
        loadSubCategoriesFromPath(questsRoot.resolve("subcategories"), ns, data);
        loadSubCategoriesFromPath(questsRoot.resolve("sub_category"), ns, data);
        loadSubCategoriesFromPath(questsRoot.resolve("subcategory"), ns, data);
        loadSubCategoriesFromPath(questsRoot.resolve("sub-category"), ns, data);
        loadQuestsFromPath(questsRoot, ns, data);
    }

    private static String namespaceFromPackRoot(Path packRoot) {
        if (packRoot == null || packRoot.getFileName() == null) return "boundless_pack";
        return normalizeNamespace(packRoot.getFileName().toString());
    }

    private static String normalizeNamespace(String raw) {
        String input = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-' || ch == '.') {
                out.append(ch);
            } else {
                out.append('_');
            }
        }
        return out.isEmpty() ? "boundless_pack" : out.toString();
    }

    private static void loadCategoriesFromPath(Path categoriesDir, String namespace, DataStore data, boolean packTreeLayout) {
        if (!Files.isDirectory(categoriesDir)) return;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(categoriesDir, "*.json")) {
            for (Path file : files) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject obj = safeObject(reader);
                    if (obj == null) continue;
                    String id = optString(obj, "id");
                    if (id == null || id.isBlank()) continue;
                    String icon = optString(obj, "icon");
                    String cname = optString(obj, "name");
                    int order = parseIntFlexible(obj, "order", 0);
                    boolean excludeFromAll = parseBoolFlexible(obj, "exclude_from_all", false);
                    String dependency = optString(obj, "dependency");
                    boolean autoComplete = parseBoolFlexible(obj, "auto_complete",
                            parseBoolFlexible(obj, "autoComplete", false));
                    boolean treeLayout = packTreeLayout || "tree".equalsIgnoreCase(optString(obj, "layout"));
                    String background = optString(obj, "background");
                    boolean backgroundDarkening = parseBoolFlexible(obj, "background_darkening", false);
                    float backgroundDarkeningOpacity = parseFloatFlexible(obj, "background_darkening_opacity", 0.45f);
                    List<Sticker> stickers = parseStickers(obj);
                    data.categories.put(id, new Category(id, icon, cname, order, excludeFromAll, dependency,
                            autoComplete, treeLayout, background, backgroundDarkening, backgroundDarkeningOpacity, stickers));
                } catch (Exception e) {
                    BoundlessDebug.warnRateLimited("category-file:" + file, 5_000L,
                            "could not parse category file {}", file, e);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public record Sticker(String type, String value, int x, int y) {
        public Sticker {
            type = "icon".equalsIgnoreCase(type) ? "icon" : "text";
            value = value == null ? "" : value;
        }
    }

    private static boolean readTreeLayout(Path metaPath) {
        if (metaPath == null || !Files.exists(metaPath)) return false;
        try (Reader reader = Files.newBufferedReader(metaPath, StandardCharsets.UTF_8)) {
            JsonObject root = safeObject(reader);
            if (root == null || !root.has("boundless") || !root.get("boundless").isJsonObject()) return false;
            return "tree".equalsIgnoreCase(optString(root.getAsJsonObject("boundless"), "layout"));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void loadSubCategoriesFromPath(Path subDir, String namespace, DataStore data) {
        if (!Files.isDirectory(subDir)) return;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(subDir, "*.json")) {
            for (Path file : files) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject obj = safeObject(reader);
                    if (obj == null) continue;
                    String relPath = "quests/" + subDir.getFileName() + "/" + file.getFileName();
                    readSubCategoryObject(obj, relPath, data);
                } catch (Exception e) {
                    BoundlessDebug.warnRateLimited("subcategory-file:" + file, 5_000L,
                            "could not parse subcategory file {}", file, e);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void loadQuestsFromPath(Path questsRoot, String namespace, DataStore data) {
        if (!Files.isDirectory(questsRoot)) return;
        try (var stream = Files.walk(questsRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String rel = questsRoot.relativize(path).toString().replace('\\', '/');
                            if (rel.startsWith("categories/")) return;
                            if (isSubCategoryPath("quests/" + rel)) return;
                            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(namespace, "quests/" + rel);
                            if (shouldIgnoreQuestJson(loc)) return;
                            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                                JsonObject obj = safeObject(reader);
                                if (obj == null) return;
                                Quest q = parseQuestObject(obj, loc);
                                if (q != null && !isQuestDisabled(q)) data.quests.put(q.id, q);
                            }
                        } catch (Exception e) {
                            BoundlessDebug.warnRateLimited("quest-file:" + path, 5_000L,
                                    "could not parse quest file {}", path, e);
                        }
                    });
        } catch (Exception ignored) {
        }
    }

    private static void loadSubCategoriesFromManager(ResourceManager rm, String path, DataStore data) {
        Map<ResourceLocation, List<Resource>> subStacks =
                rm.listResourceStacks(path, rl -> rl.getPath().endsWith(".json"));

        for (var entry : subStacks.entrySet()) {
            ResourceLocation loc = entry.getKey();
            if (shouldSkipNamespace(loc == null ? null : loc.getNamespace())) continue;
            List<Resource> stack = entry.getValue();
            if (stack == null || stack.isEmpty()) continue;

            Resource top = stack.get(stack.size() - 1);
            try (Reader raw = top.openAsReader(); Reader reader = new BufferedReader(raw)) {
                JsonObject obj = safeObject(reader);
                if (obj == null) continue;
                readSubCategoryObject(obj, loc == null ? "" : loc.getPath(), data);
            } catch (Exception e) {
                BoundlessDebug.warnRateLimited("subcategory-parse:" + loc, 5_000L,
                        "could not parse subcategory {}", loc, e);
            }
        }
    }

    private static boolean readSubCategoryObject(JsonObject obj, String sourcePath, DataStore data) {
        String id = optString(obj, "id");
        if (id == null || id.isBlank()) return false;

        String cat = optString(obj, "category");
        if (cat == null) cat = "";

        String icon = optString(obj, "icon");
        String name = optString(obj, "name");
        int order = parseIntFlexible(obj, "order", 0);
        boolean defaultOpen = parseBoolFlexible(obj, "default_open", true);
        if (obj.has("defaultOpen")) {
            defaultOpen = parseBoolFlexible(obj, "defaultOpen", defaultOpen);
        }

        String key = subKey(cat, id);
        data.subCategories.put(key, new SubCategory(id, cat, icon, name, order, defaultOpen, List.of(), sourcePath));
        return true;
    }

    private static void ensureSubCategoriesFromQuests(DataStore data) {
        for (Quest q : data.quests.values()) {
            if (q == null || q.subCategory == null || q.subCategory.isBlank()) continue;

            String cat = q.category;
            String subId = q.subCategory;
            String key = subKey(cat, subId);

            String wildcardKey = subKey("", subId);
            if (!data.subCategories.containsKey(key) && !data.subCategories.containsKey(wildcardKey)) {
                Category c = data.categories.get(cat);
                String icon = c != null ? c.icon : q.icon;
                String name = prettifyId(subId);
                data.subCategories.put(key, new SubCategory(subId, cat, icon, name, 0, true, List.of(), ""));
            }
        }
    }

    public static synchronized void loadClient(boolean forceReload) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;

        Minecraft mc = Minecraft.getInstance();
        if (CLIENT_DATA.networkAuthoritative && mc.getConnection() != null) return;
        String worldId = null;

        try {
            if (mc.getSingleplayerServer() != null) {
                var swd = mc.getSingleplayerServer().getWorldData();
                worldId = swd.getLevelName() + "@" + swd.worldGenOptions().seed();
            }
        } catch (Throwable ignored) {}

        if (!forceReload && Objects.equals(CLIENT_DATA.worldId, worldId)
                && CLIENT_DATA.loaded) {
            return;
        }

        CLIENT_DATA.worldId = worldId;

        long startedAt = BoundlessDebug.enabled() ? System.nanoTime() : 0L;
        ResourceManager rm = mc.getResourceManager();
        load(rm, forceReload, CLIENT_DATA);
        CLIENT_DATA.loaded = true;
        if (startedAt != 0L) {
            BoundlessDebug.rateLimited("client-quest-load", 2_000L,
                    "forceReload={}, quests={}, categories={}, elapsed={}ms", forceReload, CLIENT_DATA.quests.size(), CLIENT_DATA.categories.size(),
                    (System.nanoTime() - startedAt) / 1_000_000L);
        }
    }

    public static synchronized void loadServer(MinecraftServer server, boolean forceReload) {
        String worldId;
        try {
            worldId = server.getWorldData().getLevelName() + "@" + server.getWorldData().worldGenOptions().seed();
        } catch (Throwable t) {
            worldId = server.getWorldData().getLevelName();
        }

        if (!forceReload && Objects.equals(SERVER_DATA.worldId, worldId)
                && SERVER_DATA.loaded) {
            return;
        }

        SERVER_DATA.worldId = worldId;

        long startedAt = BoundlessDebug.enabled() ? System.nanoTime() : 0L;
        ResourceManager rm = server.getServerResources().resourceManager();
        load(rm, forceReload, SERVER_DATA);
        SERVER_DATA.loaded = true;
        if (startedAt != 0L) {
            BoundlessDebug.rateLimited("server-quest-load", 2_000L,
                    "world={}, forceReload={}, quests={}, categories={}, elapsed={}ms", worldId, forceReload, SERVER_DATA.quests.size(), SERVER_DATA.categories.size(),
                    (System.nanoTime() - startedAt) / 1_000_000L);
        }
    }

    public static synchronized boolean isEmpty() { return CLIENT_DATA.quests.isEmpty(); }

    public static synchronized Collection<Quest> all() {
        if (!CLIENT_DATA.loaded) loadClient(false);
        return CLIENT_DATA.questView;
    }

    public static synchronized Collection<Quest> allServer(MinecraftServer server) {
        loadServer(server, false);
        return SERVER_DATA.questView;
    }

    public static synchronized Optional<Quest> byId(String id) {
        if (!CLIENT_DATA.loaded) loadClient(false);
        return Optional.ofNullable(CLIENT_DATA.quests.get(id));
    }

    public static synchronized Optional<Quest> byIdServer(MinecraftServer server, String id) {
        if (!SERVER_DATA.loaded) loadServer(server, false);
        return Optional.ofNullable(SERVER_DATA.quests.get(id));
    }

    public static synchronized Optional<Category> categoryById(String id) {
        if (!CLIENT_DATA.loaded) loadClient(false);
        return Optional.ofNullable(CLIENT_DATA.categories.get(id));
    }

    public static synchronized Optional<Category> categoryByIdServer(MinecraftServer server, String id) {
        if (!SERVER_DATA.loaded) loadServer(server, false);
        return Optional.ofNullable(SERVER_DATA.categories.get(id));
    }

    public static synchronized boolean isCategoryUnlocked(Category c, net.minecraft.world.entity.player.Player player) {
        if (c == null) return true;
        if (c.dependency == null || c.dependency.isBlank()) return true;

        var q = byId(c.dependency).orElse(null);
        if (q == null) return true;

        return QuestTracker.getStatus(q, player) == QuestTracker.Status.REDEEMED;
    }

    public static synchronized boolean includeQuestInAll(Quest q, net.minecraft.world.entity.player.Player player) {
        if (q == null) return false;

        Category c = CLIENT_DATA.categories.getOrDefault(q.category, null);
        if (c == null) return true;
        if (c.excludeFromAll) return false;

        return isCategoryUnlocked(c, player);
    }

    public static synchronized List<Category> categoriesOrdered() {
        if (!CLIENT_DATA.loaded) loadClient(false);

        if (!CLIENT_DATA.categories.containsKey("all")) {
            CLIENT_DATA.categories.put("all", new Category("all", "minecraft:book", "All",
                    Integer.MIN_VALUE, false, "", false));
        }

        List<Category> list = new ArrayList<>(CLIENT_DATA.categories.values());
        list.sort((a, b) -> {
            if ("all".equals(a.id)) return -1;
            if ("all".equals(b.id)) return 1;

            if (a.order != b.order) return Integer.compare(a.order, b.order);
            return a.name.compareToIgnoreCase(b.name);
        });

        return list;
    }

    public static synchronized List<Category> categoriesOrderedServer(MinecraftServer server) {
        loadServer(server, false);

        if (!SERVER_DATA.categories.containsKey("all")) {
            SERVER_DATA.categories.put("all", new Category("all", "minecraft:book", "All",
                    Integer.MIN_VALUE, false, "", false));
        }

        List<Category> list = new ArrayList<>(SERVER_DATA.categories.values());
        list.sort((a, b) -> {
            if ("all".equals(a.id)) return -1;
            if ("all".equals(b.id)) return 1;

            if (a.order != b.order) return Integer.compare(a.order, b.order);
            return a.name.compareToIgnoreCase(b.name);
        });

        return list;
    }

    public static synchronized List<SubCategory> subCategoriesAllOrdered() {
        if (!CLIENT_DATA.loaded) loadClient(false);
        ensureSubCategoriesFromQuests(CLIENT_DATA);
        return buildSubCategoryList(null, CLIENT_DATA);
    }

    public static synchronized List<SubCategory> subCategoriesAllOrderedServer(MinecraftServer server) {
        loadServer(server, false);
        ensureSubCategoriesFromQuests(SERVER_DATA);
        return buildSubCategoryList(null, SERVER_DATA);
    }

    public static synchronized List<SubCategory> subCategoriesForCategory(String categoryId) {
        if (!CLIENT_DATA.loaded) loadClient(false);
        ensureSubCategoriesFromQuests(CLIENT_DATA);
        return buildSubCategoryList(categoryId, CLIENT_DATA);
    }

    private static List<SubCategory> buildSubCategoryList(String categoryFilter, DataStore data) {
        Map<String, List<Quest>> grouped = new LinkedHashMap<>();
        for (Quest q : data.quests.values()) {
            if (q == null || q.subCategory == null || q.subCategory.isBlank()) continue;
            if (categoryFilter != null && !q.category.equalsIgnoreCase(categoryFilter)) continue;

            String key = subKey(q.category, q.subCategory);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(q);
        }

        List<SubCategory> out = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            String key = entry.getKey();
            List<Quest> qs = entry.getValue();
            if (qs == null || qs.isEmpty()) continue;

            qs.sort(Comparator.comparing(Quest::sourceSortKey));

            SubCategory meta = data.subCategories.get(key);
            if (meta == null) meta = data.subCategories.get(subKey("", qs.get(0).subCategory));
            String cat = qs.get(0).category;
            String id = meta != null ? meta.id : qs.get(0).subCategory;
            String icon = meta != null ? meta.icon : qs.get(0).icon;
            String name = meta != null ? meta.name : prettifyId(id);
            int order = meta != null ? meta.order : 0;
            boolean defaultOpen = meta != null ? meta.defaultOpen : true;
            String sourcePath = meta != null ? meta.sourcePath : "";

            List<String> questIds = new ArrayList<>(qs.size());
            for (Quest q : qs) questIds.add(q.id);

            out.add(new SubCategory(id, cat, icon, name, order, defaultOpen, questIds, sourcePath));
        }

        out.sort((a, b) -> {
            int cat = a.category.compareToIgnoreCase(b.category);
            if (cat != 0) return cat;
            String ak = a.sourceSortKey();
            String bk = b.sourceSortKey();
            int sk = ak.compareToIgnoreCase(bk);
            if (sk != 0) return sk;
            return a.name.compareToIgnoreCase(b.name);
        });

        return out;
    }

    private static Quest parseQuestObject(JsonObject obj, ResourceLocation src) {
        String id = optString(obj, "id");

        if (id == null || id.isBlank()) {
            String p = src.getPath();
            int lastSlash = p.lastIndexOf('/');
            String n = (lastSlash >= 0 ? p.substring(lastSlash + 1) : p);
            id = n.endsWith(".json") ? n.substring(0, n.length() - 5) : n;
        }

        String name = optString(obj, "name");
        String icon = optString(obj, "icon");
        String description = optString(obj, "description");
        String media = optString(obj, "media");

        List<String> deps = parseDependencies(obj);
        List<String> incompatibleDeps = parseStringList(obj, "incompatible_dependencies");
        boolean optional = parseBoolFlexible(obj, "optional", false);
        boolean repeatable = parseBoolFlexible(obj, "repeatable", false);
        boolean autoComplete = parseBoolFlexible(obj, "auto_complete",
                parseBoolFlexible(obj, "autoComplete", false));
        boolean lockAfterDependency = parseBoolFlexible(obj, "lock_after_dependency",
                parseBoolFlexible(obj, "lockAfterDependency", false));
        boolean hiddenUnderDependency = parseBoolFlexible(obj, "hiddenUnderDependency",
                parseBoolFlexible(obj, "hidden_under_dependency", false));

        JsonElement rewardEl = obj.has("reward") ? obj.get("reward") : null;
        Rewards rewards = parseRewards(rewardEl);

        String type = optString(obj, "type");
        Completion completion = parseCompletion(obj.get("completion"), type);

        String category = optString(obj, "category");
        String subCategory = optString(obj, "sub-category");
        if (subCategory == null) subCategory = optString(obj, "sub_category");
        if (subCategory == null) subCategory = optString(obj, "subCategory");

        String sourcePath = src == null ? "" : src.getPath();
        int treeX = parseIntFlexible(obj, "tree_x", -1);
        int treeY = parseIntFlexible(obj, "tree_y", -1);
        String treeBackground = optString(obj, "tree_background");
        boolean treeOrigin = parseBoolFlexible(obj, "tree_origin", false);

        return new Quest(id, name, icon, description, deps, incompatibleDeps, lockAfterDependency, optional, repeatable, autoComplete, hiddenUnderDependency,
                rewards, type, completion,
                category, subCategory, sourcePath, treeX, treeY, treeBackground, treeOrigin, media);
    }

    private static List<String> parseStringList(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonArray()) return List.of();
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (JsonElement element : obj.getAsJsonArray(key)) {
            if (!element.isJsonPrimitive()) continue;
            String value = element.getAsString().trim();
            if (!value.isBlank()) values.add(value);
        }
        return List.copyOf(values);
    }

    private static List<String> parseDependencies(JsonObject obj) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (!obj.has("dependencies")) return List.of();

        JsonElement depEl = obj.get("dependencies");

        if (depEl.isJsonArray()) {
            for (JsonElement d : depEl.getAsJsonArray()) {
                if (d.isJsonPrimitive()) {
                    String s = d.getAsString();
                    if (s != null && !s.isBlank()) set.add(s);
                }
            }
        } else if (depEl.isJsonPrimitive()) {
            String s = depEl.getAsString();
            if (s != null && !s.isBlank()) set.add(s);
        }

        return new ArrayList<>(set);
    }

    private static Rewards parseRewards(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return new Rewards(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "", 0);
        }

        List<RewardEntry> items = new ArrayList<>();
        List<CommandReward> commands = new ArrayList<>();
        List<FunctionReward> functions = new ArrayList<>();
        List<LootTableReward> lootTables = new ArrayList<>();
        List<AdvancementReward> advancements = new ArrayList<>();
        List<ToastReward> toasts = new ArrayList<>();
        String expType = "";
        int expAmount = 0;

        if (el.isJsonPrimitive()) {
            String cmd = el.getAsString();
            if (cmd != null && !cmd.isBlank()) commands.add(new CommandReward(cmd, "", ""));
            return new Rewards(items, commands, functions, lootTables, advancements, toasts, expType, expAmount);
        }

        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                JsonObject r = e.getAsJsonObject();
                String item = optString(r, "item");
                int count = r.has("count") && r.get("count").isJsonPrimitive() && r.getAsJsonPrimitive("count").isNumber()
                        ? r.getAsJsonPrimitive("count").getAsInt() : 1;
                List<String> accepted = readAcceptedIds(r, "acceptedItems", "item");
                if (!accepted.isEmpty()) items.add(new RewardEntry(accepted, count));
                else if (item != null && !item.isBlank()) items.add(new RewardEntry(item, count));
            }
            return new Rewards(items, commands, functions, lootTables, advancements, toasts, expType, expAmount);
        }

        if (!el.isJsonObject()) {
            return new Rewards(items, commands, functions, lootTables, advancements, toasts, expType, expAmount);
        }

        JsonObject obj = el.getAsJsonObject();

        if (obj.has("items") && obj.get("items").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("items")) {
                if (!e.isJsonObject()) continue;
                JsonObject r = e.getAsJsonObject();
                String item = optString(r, "item");
                int count = r.has("count") && r.get("count").isJsonPrimitive() && r.getAsJsonPrimitive("count").isNumber()
                        ? r.getAsJsonPrimitive("count").getAsInt() : 1;
                List<String> accepted = readAcceptedIds(r, "acceptedItems", "item");
                if (!accepted.isEmpty()) items.add(new RewardEntry(accepted, count));
                else if (item != null && !item.isBlank()) items.add(new RewardEntry(item, count));
            }
        } else if (obj.has("item")) {
            String item = optString(obj, "item");
            int count = obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()
                    ? obj.getAsJsonPrimitive("count").getAsInt() : 1;
            List<String> accepted = readAcceptedIds(obj, "acceptedItems", "item");
            if (!accepted.isEmpty()) items.add(new RewardEntry(accepted, count));
            else if (item != null && !item.isBlank()) items.add(new RewardEntry(item, count));
        }

        if (obj.has("command") && obj.get("command").isJsonPrimitive()) {
            String cmd = obj.get("command").getAsString();
            if (cmd != null && !cmd.isBlank()) commands.add(new CommandReward(cmd, "", ""));
        }

        if (obj.has("commands") && obj.get("commands").isJsonArray()) {
            for (JsonElement ce : obj.getAsJsonArray("commands")) {
                if (ce == null) continue;
                if (ce.isJsonPrimitive()) {
                    String cmd = ce.getAsString();
                    if (cmd != null && !cmd.isBlank()) commands.add(new CommandReward(cmd, "", ""));
                    continue;
                }
                if (!ce.isJsonObject()) continue;
                JsonObject co = ce.getAsJsonObject();
                String cmd = optString(co, "command");
                String icon = optString(co, "icon");
                String title = optString(co, "title");
                if (cmd != null && !cmd.isBlank()) commands.add(new CommandReward(cmd, icon, title));
            }
        }

        if (obj.has("functions") && obj.get("functions").isJsonArray()) {
            for (JsonElement fe : obj.getAsJsonArray("functions")) {
                if (fe == null) continue;
                if (fe.isJsonPrimitive()) {
                    String fn = fe.getAsString();
                    if (fn != null && !fn.isBlank()) functions.add(new FunctionReward(fn, "", ""));
                    continue;
                }
                if (!fe.isJsonObject()) continue;
                JsonObject fo = fe.getAsJsonObject();
                String fn = optString(fo, "function");
                String icon = optString(fo, "icon");
                String title = optString(fo, "title");
                if (fn != null && !fn.isBlank()) functions.add(new FunctionReward(fn, icon, title));
            }
        }

        if (obj.has("lootTable") && obj.get("lootTable").isJsonPrimitive()) {
            String lootTable = obj.get("lootTable").getAsString();
            if (lootTable != null && !lootTable.isBlank()) {
                lootTables.add(new LootTableReward(lootTable, "", ""));
            }
        }

        if (obj.has("lootTables") && obj.get("lootTables").isJsonArray()) {
            for (JsonElement le : obj.getAsJsonArray("lootTables")) {
                if (le == null) continue;
                if (le.isJsonPrimitive()) {
                    String lootTable = le.getAsString();
                    if (lootTable != null && !lootTable.isBlank()) {
                        lootTables.add(new LootTableReward(lootTable, "", ""));
                    }
                    continue;
                }
                if (!le.isJsonObject()) continue;
                JsonObject lo = le.getAsJsonObject();
                String lootTable = optString(lo, "lootTable");
                String icon = optString(lo, "icon");
                String title = optString(lo, "title");
                if (lootTable != null && !lootTable.isBlank()) {
                    lootTables.add(new LootTableReward(lootTable, icon, title));
                }
            }
        }

        if (obj.has("advancement") && obj.get("advancement").isJsonPrimitive()) {
            String advancement = obj.get("advancement").getAsString();
            if (advancement != null && !advancement.isBlank()) advancements.add(new AdvancementReward(advancement));
        }

        if (obj.has("advancements") && obj.get("advancements").isJsonArray()) {
            for (JsonElement ae : obj.getAsJsonArray("advancements")) {
                if (ae == null) continue;
                if (ae.isJsonPrimitive()) {
                    String advancement = ae.getAsString();
                    if (advancement != null && !advancement.isBlank()) advancements.add(new AdvancementReward(advancement));
                    continue;
                }
                if (!ae.isJsonObject()) continue;
                String advancement = optString(ae.getAsJsonObject(), "advancement");
                if (advancement != null && !advancement.isBlank()) advancements.add(new AdvancementReward(advancement));
            }
        }

        if (obj.has("toast") && obj.get("toast").isJsonObject()) {
            JsonObject toast = obj.getAsJsonObject("toast");
            String title = optString(toast, "title");
            String description = optString(toast, "description");
            String icon = optString(toast, "icon");
            if ((title != null && !title.isBlank()) || (description != null && !description.isBlank()) || (icon != null && !icon.isBlank())) {
                toasts.add(new ToastReward(title, description, icon));
            }
        }

        if (obj.has("toasts") && obj.get("toasts").isJsonArray()) {
            for (JsonElement te : obj.getAsJsonArray("toasts")) {
                if (!te.isJsonObject()) continue;
                JsonObject toast = te.getAsJsonObject();
                String title = optString(toast, "title");
                String description = optString(toast, "description");
                String icon = optString(toast, "icon");
                if ((title != null && !title.isBlank()) || (description != null && !description.isBlank()) || (icon != null && !icon.isBlank())) {
                    toasts.add(new ToastReward(title, description, icon));
                }
            }
        }

        if (obj.has("exp") && obj.get("exp").isJsonPrimitive()) {
            expType = obj.get("exp").getAsString();
        }
        if (obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()) {
            expAmount = obj.getAsJsonPrimitive("count").getAsInt();
        }

        return new Rewards(items, commands, functions, lootTables, advancements, toasts, expType, expAmount);
    }

    private static Completion parseCompletion(JsonElement el, String type) {
        if (el == null || el.isJsonNull()) return new Completion(List.of());
        List<Target> out = new ArrayList<>();

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("complete") && obj.get("complete").isJsonArray()) {
                for (JsonElement e : obj.getAsJsonArray("complete")) {
                    if (e.isJsonObject()) parseNewFormatTarget(e.getAsJsonObject(), out);
                }
                return new Completion(out);
            }
        }

        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (e.isJsonObject()) parseTargetObject(e.getAsJsonObject(), out);
            }
            return new Completion(out);
        }

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();

            if (obj.has("collect")) {
                List<String> accepted = readAcceptedIds(obj, "acceptedItems", "collect");
                int count = obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()
                        ? obj.getAsJsonPrimitive("count").getAsInt() : 1;
                if (!accepted.isEmpty()) out.add(new Target("item", accepted, count));
                return new Completion(out);
            }

            if (obj.has("targets") && obj.get("targets").isJsonArray()) {
                for (JsonElement e : obj.getAsJsonArray("targets")) {
                    if (!e.isJsonObject()) continue;
                    parseTargetObject(e.getAsJsonObject(), out);
                }
                return new Completion(out);
            }

            if (obj.has("item")) {
                out.add(new Target("item", List.of(optString(obj, "item")),
                        obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()
                                ? obj.getAsJsonPrimitive("count").getAsInt() : 1));
                return new Completion(out);
            }
            if (obj.has("acceptedItems")) {
                List<String> accepted = readAcceptedIds(obj, "acceptedItems", "");
                int count = obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()
                        ? obj.getAsJsonPrimitive("count").getAsInt() : 1;
                if (!accepted.isEmpty()) out.add(new Target("item", accepted, count));
                return new Completion(out);
            }
            if (obj.has("submit")) {
                List<String> accepted = readAcceptedIds(obj, "acceptedItems", "submit");
                out.add(new Target("submit", accepted,
                        obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()
                                ? obj.getAsJsonPrimitive("count").getAsInt() : 1));
                return new Completion(out);
            }
            if (obj.has("entity")) {
                out.add(new Target("entity", List.of(optString(obj, "entity")),
                        obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()
                                ? obj.getAsJsonPrimitive("count").getAsInt() : 1));
                return new Completion(out);
            }
            if (obj.has("kill") || obj.has("acceptedMobs")) {
                List<String> accepted = readAcceptedIds(obj, "acceptedMobs", "kill");
                int count = obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()
                        ? obj.getAsJsonPrimitive("count").getAsInt() : 1;
                if (!accepted.isEmpty()) out.add(new Target("entity", accepted, count));
                return new Completion(out);
            }
            if (obj.has("effect")) {
                out.add(new Target("effect", optString(obj, "effect"), 1));
                return new Completion(out);
            }
            if (obj.has("advancement")) {
                out.add(new Target("advancement", optString(obj, "advancement"), 1));
                return new Completion(out);
            }
            if (obj.has("observe")) {
                out.add(new Target("observe", optString(obj, "observe"), 1));
                return new Completion(out);
            }
            if (obj.has("check")) {
                String text = obj.get("check").isJsonPrimitive() ? obj.get("check").getAsString() : "";
                if (text == null || text.isBlank() || "true".equalsIgnoreCase(text.trim())) text = "Understand";
                out.add(new Target("check", text, 1));
                return new Completion(out);
            }
            if (obj.has("biome")) {
                out.add(new Target("biome", optString(obj, "biome"), 1));
                return new Completion(out);
            }
            if (obj.has("dimension")) {
                out.add(new Target("dimension", optString(obj, "dimension"), 1));
                return new Completion(out);
            }
            if (obj.has("xp")) {
                out.add(new Target("xp", optString(obj, "xp"),
                        obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()
                                ? obj.getAsJsonPrimitive("count").getAsInt() : 1));
                return new Completion(out);
            }
            if (obj.has("field")) {
                String value = optString(obj, "field");
                String hint = optString(obj, "field_text");
                if (hint == null || hint.isBlank()) hint = optString(obj, "fieldText");
                if (value != null && !value.isBlank()) {
                    out.add(new Target("field", value, 1, hint));
                }
                return new Completion(out);
            }
        }

        return new Completion(List.of());
    }

    private static void parseNewFormatTarget(JsonObject o, List<Target> out) {
        if (o.has("collect")) {
            List<String> ids = readAcceptedIds(o, "acceptedItems", "collect");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (!ids.isEmpty()) out.add(new Target("item", ids, count));
            return;
        }

        if (o.has("submit")) {
            List<String> ids = readAcceptedIds(o, "acceptedItems", "submit");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (!ids.isEmpty()) out.add(new Target("submit", ids, count));
            return;
        }

        if (o.has("kill")) {
            List<String> ids = readAcceptedIds(o, "acceptedMobs", "kill");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (!ids.isEmpty()) out.add(new Target("entity", ids, count));
            return;
        }

        if (o.has("achieve") || o.has("advancement")) {
            String adv = o.has("advancement") ? o.get("advancement").getAsString() : o.get("achieve").getAsString();
            out.add(new Target("advancement", adv, 1));
            return;
        }

        if (o.has("effect")) {
            String eff = o.get("effect").getAsString();
            out.add(new Target("effect", eff, 1));
            return;
        }

        if (o.has("observe")) {
            out.add(new Target("observe", o.get("observe").getAsString(), 1));
            return;
        }

        if (o.has("check")) {
            String text = o.get("check").isJsonPrimitive() ? o.get("check").getAsString() : "";
            if (text == null || text.isBlank() || "true".equalsIgnoreCase(text.trim())) text = "Understand";
            out.add(new Target("check", text, 1));
            return;
        }

        if (o.has("biome")) {
            out.add(new Target("biome", o.get("biome").getAsString(), 1));
            return;
        }

        if (o.has("dimension")) {
            out.add(new Target("dimension", o.get("dimension").getAsString(), 1));
            return;
        }

        if (o.has("xp")) {
            String xpType = o.get("xp").getAsString();
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            out.add(new Target("xp", xpType, count));
            return;
        }

        if (o.has("levelup_level")) {
            int count;
            try {
                count = o.get("levelup_level").getAsInt();
            } catch (Exception ignored) {
                count = o.has("count") ? o.get("count").getAsInt() : 1;
            }
            out.add(new Target("levelup_level", "level", count));
            return;
        }

        if (o.has("field")) {
            String fieldValue = o.get("field").getAsString();
            String hint = "";
            if (o.has("field_text") && o.get("field_text").isJsonPrimitive()) {
                hint = o.get("field_text").getAsString();
            } else if (o.has("fieldText") && o.get("fieldText").isJsonPrimitive()) {
                hint = o.get("fieldText").getAsString();
            }
            out.add(new Target("field", fieldValue, 1, hint));
        }
    }

    private static void parseTargetObject(JsonObject o, List<Target> out) {
        if (o.has("submit")) {
            List<String> items = readAcceptedIds(o, "acceptedItems", "submit");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (!items.isEmpty()) out.add(new Target("submit", items, count));
            return;
        }

        if (o.has("item")) {
            List<String> items = readAcceptedIds(o, "acceptedItems", "item");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (!items.isEmpty()) out.add(new Target("item", items, count));
            return;
        }

        if (o.has("entity")) {
            List<String> entities = readAcceptedIds(o, "acceptedMobs", "entity");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (!entities.isEmpty()) out.add(new Target("entity", entities, count));
            return;
        }

        if (o.has("kill") || o.has("acceptedMobs")) {
            List<String> entities = readAcceptedIds(o, "acceptedMobs", "kill");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (!entities.isEmpty()) out.add(new Target("entity", entities, count));
            return;
        }

        if (o.has("effect")) {
            String eff = optString(o, "effect");
            if (eff != null && !eff.isBlank()) out.add(new Target("effect", eff, 1));
            return;
        }

        if (o.has("advancement")) {
            String adv = optString(o, "advancement");
            if (adv != null && !adv.isBlank()) out.add(new Target("advancement", adv, 1));
            return;
        }

        if (o.has("observe")) {
            String observe = optString(o, "observe");
            if (observe != null && !observe.isBlank()) out.add(new Target("observe", observe, 1));
            return;
        }

        if (o.has("check")) {
            String text = optString(o, "check");
            if (text == null || text.isBlank() || "true".equalsIgnoreCase(text.trim())) text = "Understand";
            out.add(new Target("check", text, 1));
            return;
        }

        if (o.has("biome")) {
            String biome = optString(o, "biome");
            if (biome != null && !biome.isBlank()) out.add(new Target("biome", biome, 1));
            return;
        }

        if (o.has("dimension")) {
            String dimension = optString(o, "dimension");
            if (dimension != null && !dimension.isBlank()) out.add(new Target("dimension", dimension, 1));
            return;
        }

        if (o.has("xp")) {
            String xpType = optString(o, "xp");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (xpType != null && !xpType.isBlank()) out.add(new Target("xp", xpType, count));
            return;
        }

        if (o.has("levelup_level")) {
            int count;
            try {
                count = o.get("levelup_level").getAsInt();
            } catch (Exception ignored) {
                count = o.has("count") ? o.get("count").getAsInt() : 1;
            }
            out.add(new Target("levelup_level", "level", count));
            return;
        }

        if (o.has("field")) {
            String fieldValue = optString(o, "field");
            if (fieldValue == null || fieldValue.isBlank()) return;
            String hint = optString(o, "field_text");
            if (hint == null || hint.isBlank()) hint = optString(o, "fieldText");
            out.add(new Target("field", fieldValue, 1, hint));
        }
    }

    private static List<String> readAcceptedIds(JsonObject obj, String listKey, String singleKey) {
        List<String> out = new ArrayList<>();
        if (obj == null) return out;
        if (listKey != null && obj.has(listKey) && obj.get(listKey).isJsonArray()) {
            for (JsonElement element : obj.getAsJsonArray(listKey)) {
                if (element != null && element.isJsonPrimitive()) {
                    String value = element.getAsString();
                    if (value != null && !value.isBlank() && !out.contains(value)) out.add(value);
                }
            }
        }
        if (singleKey != null && obj.has(singleKey)) {
            JsonElement element = obj.get(singleKey);
            if (element != null) {
                if (element.isJsonArray()) {
                    for (JsonElement value : element.getAsJsonArray()) {
                        if (value != null && value.isJsonPrimitive()) {
                            String raw = value.getAsString();
                            if (raw != null && !raw.isBlank() && !out.contains(raw)) out.add(raw);
                        }
                    }
                } else if (element.isJsonPrimitive()) {
                    String value = element.getAsString();
                    if (value != null && !value.isBlank() && !out.contains(value)) out.add(value);
                }
            }
        }
        return out;
    }

    public static NetworkSnapshot parseNetworkJson(String json) {
        // this parses large sync data away from the render thread
        long startedAt = BoundlessDebug.enabled() ? System.nanoTime() : 0L;
        DataStore parsed = new DataStore();

        try {
            JsonElement rootEl = GSON.fromJson(json, JsonElement.class);
            if (rootEl == null || !rootEl.isJsonObject()) {
                throw new IllegalArgumentException("Quest sync root is invalid");
            }

            JsonObject root = rootEl.getAsJsonObject();

            if (root.has("categories") && root.get("categories").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("categories")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();

                    String id = optString(o, "id");
                    if (id == null || id.isBlank()) continue;

                    String icon = optString(o, "icon");
                    String name = optString(o, "name");
                    int order = parseIntFlexible(o, "order", 0);
                    boolean excludeFromAll = parseBoolFlexible(o, "excludeFromAll", false);
                    String dependency = optString(o, "dependency");
                    boolean autoComplete = parseBoolFlexible(o, "auto_complete",
                            parseBoolFlexible(o, "autoComplete", false));
                    boolean treeLayout = parseBoolFlexible(o, "treeLayout", false);
                    String background = optString(o, "background");
                    boolean backgroundDarkening = parseBoolFlexible(o, "backgroundDarkening", false);
                    float backgroundDarkeningOpacity = parseFloatFlexible(o, "backgroundDarkeningOpacity", 0.45f);
                    List<Sticker> stickers = parseStickers(o);

                    parsed.categories.put(id, new Category(id, icon, name, order, excludeFromAll, dependency,
                            autoComplete, treeLayout, background, backgroundDarkening, backgroundDarkeningOpacity, stickers));
                }
            }

            if (root.has("subCategories") && root.get("subCategories").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("subCategories")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();

                    String id = optString(o, "id");
                    if (id == null || id.isBlank()) continue;

                    String cat = optString(o, "category");
                    if (cat == null) cat = "";
                    String icon = optString(o, "icon");
                    String name = optString(o, "name");
                    int order = parseIntFlexible(o, "order", 0);
                    boolean defaultOpen = parseBoolFlexible(o, "defaultOpen", true);
                    String sourcePath = optString(o, "sourcePath");

                    List<String> questIds = new ArrayList<>();
                    if (o.has("quests") && o.get("quests").isJsonArray()) {
                        for (JsonElement qe : o.getAsJsonArray("quests")) {
                            if (qe.isJsonPrimitive()) {
                                String qid = qe.getAsString();
                                if (qid != null && !qid.isBlank()) questIds.add(qid);
                            }
                        }
                    }

                    String key = subKey(cat, id);
                    parsed.subCategories.put(key, new SubCategory(id, cat, icon, name, order, defaultOpen, questIds, sourcePath));
                }
            }

            if (root.has("quests") && root.get("quests").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("quests")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();

                    String id = optString(o, "id");
                    if (id == null || id.isBlank()) continue;

                    String name = optString(o, "name");
                    String icon = optString(o, "icon");
                    String description = optString(o, "description");
                    String media = optString(o, "media");

                    List<String> deps = new ArrayList<>();
                    if (o.has("dependencies") && o.get("dependencies").isJsonArray()) {
                        for (JsonElement d : o.getAsJsonArray("dependencies")) {
                            if (d.isJsonPrimitive()) {
                                String s = d.getAsString();
                                if (!s.isBlank()) deps.add(s);
                            }
                        }
                    }
                    List<String> incompatibleDeps = parseStringList(o, "incompatibleDependencies");

                    boolean optional = parseBoolFlexible(o, "optional", false);
                    boolean repeatable = parseBoolFlexible(o, "repeatable", false);
                    boolean autoComplete = parseBoolFlexible(o, "auto_complete",
                            parseBoolFlexible(o, "autoComplete", false));
                    boolean lockAfterDependency = parseBoolFlexible(o, "lock_after_dependency",
                            parseBoolFlexible(o, "lockAfterDependency", false));
                    boolean hiddenUnderDependency = parseBoolFlexible(o, "hiddenUnderDependency", false);

                    Rewards rewards = new Rewards(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "", 0);
                    if (o.has("rewards") && o.get("rewards").isJsonObject()) {
                        JsonObject ro = o.getAsJsonObject("rewards");

                        List<RewardEntry> rItems = new ArrayList<>();
                        if (ro.has("items") && ro.get("items").isJsonArray()) {
                            for (JsonElement ie : ro.getAsJsonArray("items")) {
                                if (!ie.isJsonObject()) continue;
                                JsonObject io = ie.getAsJsonObject();
                                String item = optString(io, "item");
                                int cnt = io.has("count") ? io.get("count").getAsInt() : 1;
                                if (item != null && !item.isBlank()) rItems.add(new RewardEntry(item, cnt));
                            }
                        }

                        List<CommandReward> cmds = new ArrayList<>();
                        if (ro.has("command") && ro.get("command").isJsonPrimitive()) {
                            String c = ro.get("command").getAsString();
                            if (c != null && !c.isBlank()) cmds.add(new CommandReward(c, "", ""));
                        }
                        if (ro.has("commands") && ro.get("commands").isJsonArray()) {
                            for (JsonElement ce : ro.getAsJsonArray("commands")) {
                                if (ce == null) continue;
                                if (ce.isJsonPrimitive()) {
                                    String c = ce.getAsString();
                                    if (c != null && !c.isBlank()) cmds.add(new CommandReward(c, "", ""));
                                    continue;
                                }
                                if (!ce.isJsonObject()) continue;
                                JsonObject co = ce.getAsJsonObject();
                                String c = optString(co, "command");
                                String ic = optString(co, "icon");
                                String ti = optString(co, "title");
                                if (c != null && !c.isBlank()) cmds.add(new CommandReward(c, ic, ti));
                            }
                        }

                        List<FunctionReward> fns = new ArrayList<>();
                        if (ro.has("functions") && ro.get("functions").isJsonArray()) {
                            for (JsonElement fe : ro.getAsJsonArray("functions")) {
                                if (fe == null) continue;
                                if (fe.isJsonPrimitive()) {
                                    String f = fe.getAsString();
                                    if (f != null && !f.isBlank()) fns.add(new FunctionReward(f, "", ""));
                                    continue;
                                }
                                if (!fe.isJsonObject()) continue;
                                JsonObject fo = fe.getAsJsonObject();
                                String f = optString(fo, "function");
                                String ic = optString(fo, "icon");
                                String ti = optString(fo, "title");
                                if (f != null && !f.isBlank()) fns.add(new FunctionReward(f, ic, ti));
                            }
                        }

                        List<LootTableReward> lootTables = new ArrayList<>();
                        if (ro.has("lootTables") && ro.get("lootTables").isJsonArray()) {
                            for (JsonElement le : ro.getAsJsonArray("lootTables")) {
                                if (!le.isJsonObject()) continue;
                                JsonObject lo = le.getAsJsonObject();
                                String lootTable = optString(lo, "lootTable");
                                String ic = optString(lo, "icon");
                                String ti = optString(lo, "title");
                                if (lootTable != null && !lootTable.isBlank()) {
                                    lootTables.add(new LootTableReward(lootTable, ic, ti));
                                }
                            }
                        }

                        List<AdvancementReward> advancements = new ArrayList<>();
                        if (ro.has("advancements") && ro.get("advancements").isJsonArray()) {
                            for (JsonElement ae : ro.getAsJsonArray("advancements")) {
                                if (ae == null) continue;
                                if (ae.isJsonPrimitive()) {
                                    String advancement = ae.getAsString();
                                    if (advancement != null && !advancement.isBlank()) advancements.add(new AdvancementReward(advancement));
                                    continue;
                                }
                                if (!ae.isJsonObject()) continue;
                                String advancement = optString(ae.getAsJsonObject(), "advancement");
                                if (advancement != null && !advancement.isBlank()) advancements.add(new AdvancementReward(advancement));
                            }
                        }

                        List<ToastReward> toasts = new ArrayList<>();
                        if (ro.has("toasts") && ro.get("toasts").isJsonArray()) {
                            for (JsonElement te : ro.getAsJsonArray("toasts")) {
                                if (!te.isJsonObject()) continue;
                                JsonObject toast = te.getAsJsonObject();
                                String title = optString(toast, "title");
                                String toastDescription = optString(toast, "description");
                                String toastIcon = optString(toast, "icon");
                                if ((title != null && !title.isBlank()) || (toastDescription != null && !toastDescription.isBlank()) || (toastIcon != null && !toastIcon.isBlank())) {
                                    toasts.add(new ToastReward(title, toastDescription, toastIcon));
                                }
                            }
                        }

                        String expType = optString(ro, "expType");
                        int expAmount = ro.has("expAmount") && ro.get("expAmount").isJsonPrimitive() && ro.getAsJsonPrimitive("expAmount").isNumber()
                                ? ro.getAsJsonPrimitive("expAmount").getAsInt() : 0;

                        rewards = new Rewards(rItems, cmds, fns, lootTables, advancements, toasts, expType, expAmount);
                    }

                    String type = optString(o, "type");

                    Completion completion = new Completion(List.of());
                    if (o.has("completion") && o.get("completion").isJsonObject()) {
                        JsonObject co = o.getAsJsonObject("completion");
                        List<Target> targets = new ArrayList<>();
                        if (co.has("targets") && co.get("targets").isJsonArray()) {
                            for (JsonElement te : co.getAsJsonArray("targets")) {
                                if (!te.isJsonObject()) continue;
                                JsonObject to = te.getAsJsonObject();
                                String kind = optString(to, "kind");
                                List<String> acceptedIds = switch (kind == null ? "" : kind) {
                                    case "item", "submit" -> readAcceptedIds(to, "acceptedItems", "id");
                                    case "entity" -> readAcceptedIds(to, "acceptedMobs", "id");
                                    default -> {
                                        String tid = optString(to, "id");
                                        yield tid == null || tid.isBlank() ? List.of() : List.of(tid);
                                    }
                                };
                                int count = to.has("count") ? to.get("count").getAsInt() : 1;
                                String hint = optString(to, "hint");
                                if (kind != null && !kind.isBlank() && !acceptedIds.isEmpty()) {
                                    targets.add(new Target(kind, acceptedIds, count, hint));
                                }
                            }
                        }
                        completion = new Completion(targets);
                    }

                    String category = optString(o, "category");
                    String subCategory = optString(o, "subCategory");
                    String sourcePath = optString(o, "sourcePath");
                    int treeX = parseIntFlexible(o, "treeX", -1);
                    int treeY = parseIntFlexible(o, "treeY", -1);
                    String treeBackground = optString(o, "treeBackground");
                    boolean treeOrigin = parseBoolFlexible(o, "treeOrigin", false);

                    Quest q = new Quest(id, name, icon, description, deps, incompatibleDeps, lockAfterDependency, optional, repeatable, autoComplete, hiddenUnderDependency,
                            rewards, type, completion,
                            category, subCategory, sourcePath, treeX, treeY, treeBackground, treeOrigin, media);
                    if (!isQuestDisabled(q)) parsed.quests.put(q.id, q);
                }
            }

            if (!parsed.categories.containsKey("all")) {
                parsed.categories.put("all", new Category("all", "minecraft:book", "All",
                        Integer.MIN_VALUE, false, "", false));
            }

            ensureSubCategoriesFromQuests(parsed);
            if (startedAt != 0L) {
                BoundlessDebug.rateLimited("client-quest-parse", 2_000L,
                        "bytes={}, quests={}, categories={}, elapsed={}ms", json == null ? 0 : json.length(), parsed.quests.size(), parsed.categories.size(),
                        (System.nanoTime() - startedAt) / 1_000_000L);
            }
            return new NetworkSnapshot(
                    Collections.unmodifiableMap(new LinkedHashMap<>(parsed.quests)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(parsed.categories)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(parsed.subCategories)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse synced quest data", e);
        }
    }

    public static synchronized void applyNetworkSnapshot(NetworkSnapshot snapshot) {
        if (snapshot == null) return;
        CLIENT_DATA.clear();
        CLIENT_DATA.quests.putAll(snapshot.quests());
        CLIENT_DATA.categories.putAll(snapshot.categories());
        CLIENT_DATA.subCategories.putAll(snapshot.subCategories());
        // remote snapshots use the multiplayer cache key
        CLIENT_DATA.worldId = null;
        CLIENT_DATA.networkAuthoritative = true;
        CLIENT_DATA.loaded = true;
    }

    public static void applyNetworkJson(String json) {
        applyNetworkSnapshot(parseNetworkJson(json));
    }

    public static synchronized void clearClientNetworkData() {
        // Leave client loading available for the next server or world.
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        CLIENT_DATA.clear();
        CLIENT_DATA.networkAuthoritative = false;
        CLIENT_DATA.worldId = null;
        CLIENT_DATA.loaded = true;
    }

    private static String optString(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }

    private static int parseIntFlexible(JsonObject o, String key, int def) {
        if (!o.has(key)) return def;
        JsonPrimitive p = o.getAsJsonPrimitive(key);
        if (p == null) return def;
        if (p.isNumber()) return p.getAsInt();
        try { return Integer.parseInt(p.getAsString()); } catch (Exception ignored) { return def; }
    }

    private static boolean parseBoolFlexible(JsonObject o, String key, boolean def) {
        if (!o.has(key)) return def;
        JsonPrimitive p = o.getAsJsonPrimitive(key);
        if (p == null) return def;
        if (p.isBoolean()) return p.getAsBoolean();
        try { return Boolean.parseBoolean(p.getAsString()); } catch (Exception ignored) { return def; }
    }

    private static float parseFloatFlexible(JsonObject o, String key, float def) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return def;
        try {
            return o.get(key).getAsFloat();
        } catch (RuntimeException ignored) {
            return def;
        }
    }

    private static List<Sticker> parseStickers(JsonObject object) {
        if (object == null || !object.has("stickers") || !object.get("stickers").isJsonArray()) return List.of();
        List<Sticker> stickers = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray("stickers")) {
            if (!element.isJsonObject()) continue;
            JsonObject sticker = element.getAsJsonObject();
            String type = optString(sticker, "type");
            String value = optString(sticker, "value");
            if (value == null || value.isBlank()) continue;
            stickers.add(new Sticker(type, value,
                    parseIntFlexible(sticker, "x", 0), parseIntFlexible(sticker, "y", 0)));
        }
        return List.copyOf(stickers);
    }

    private static JsonObject safeObject(Reader reader) {
        JsonElement el = GsonHelper.fromJson(GSON, reader, JsonElement.class);
        if (el == null || !el.isJsonObject()) return null;
        return el.getAsJsonObject();
    }

}
