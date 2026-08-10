package net.revilodev.boundless;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("boundless-common.json");
    private static final int QUEST_PACK_BACKUP_LIMIT = 5;
    private static boolean loading;

    public static final ConfigValue<List<? extends String>> DISABLED_CATEGORIES =
            list("disabledQuestCategories", List.of(), o -> o instanceof String);
    public static final ConfigValue<List<? extends String>> APPLIED_QUEST_PACKS =
            list("appliedQuestPacks", List.of(), o -> o instanceof String);
    public static final ConfigValue<List<? extends String>> DISABLED_QUEST_PACKS =
            list("disabledQuestPacks", List.of(), o -> o instanceof String);
    public static final ConfigValue<String> PINNED_QUEST_HUD_POSITION =
            value("pinnedQuestHudPosition", "bottom_left", Config::validHudPosition);
    public static final ConfigValue<Boolean> HIDE_QUEST_BOOK_IN_INVENTORY =
            value("hideQuestBookInInventory", false, Boolean.class::isInstance);
    public static final ConfigValue<String> QUEST_BOOK_INVENTORY_BUTTON_POSITION =
            value("questBookInventoryButtonPosition", "beside_recipe_book", Config::validInventoryButtonPosition);
    public static final ConfigValue<Boolean> CENTER_INVENTORY_WITH_QUEST_PANEL =
            value("centerInventoryWithQuestPanel", true, Boolean.class::isInstance);
    public static final ConfigValue<Boolean> HIDE_CATEGORY_HEADER =
            value("hideCategoryHeader", false, Boolean.class::isInstance);
    public static final ConfigValue<String> FILTER_DISPLAY_MODE =
            value("filterDisplayMode", "tabs", Config::validFilterDisplayMode);
    public static final ConfigValue<Boolean> DISABLE_CATEGORIES =
            value("disableCategories", false, Boolean.class::isInstance);
    public static final ConfigValue<Boolean> ENABLE_BUILTIN_QUEST_PACK =
            value("enableBuiltinQuestPack", true, Boolean.class::isInstance);
    public static final ConfigValue<Boolean> HIDE_QUEST_WIDGET_ICONS =
            value("hideQuestWidgetIcons", false, Boolean.class::isInstance);
    public static final DoubleValue QUEST_TEXT_SCALE =
            doubleValue("questTextScale", 1.0D, 0.5D, 1.0D);
    public static final DoubleValue QUEST_ICON_SCALE =
            doubleValue("questIconScale", 1.0D, 0.5D, 1.0D);
    public static final ConfigValue<Boolean> ENABLE_QUEST_SEARCH_BOX =
            value("enableQuestSearchBox", false, Boolean.class::isInstance);
    public static final ConfigValue<Boolean> ENABLE_DESCRIPTION_COLORS =
            value("enableDescriptionColors", true, Boolean.class::isInstance);
    public static final ConfigValue<Boolean> ENABLE_QUEST_TOASTS =
            value("enableQuestToasts", true, Boolean.class::isInstance);
    public static final ConfigValue<Boolean> DISABLE_QUEST_PINNING =
            value("disableQuestPinning", false, Boolean.class::isInstance);
    public static final ConfigValue<Boolean> AUTO_CLAIM_QUEST_REWARDS =
            value("autoClaimQuestRewards", false, Boolean.class::isInstance);
    public static final ConfigValue<Boolean> ENABLE_QUEST_SCROLLS =
            value("enableQuestScrolls", true, Boolean.class::isInstance);
    public static final ConfigValue<Boolean> DISABLE_QUEST_BOOK =
            value("disableQuestBook", false, Boolean.class::isInstance);
    public static final ConfigValue<Boolean> SPAWN_WITH_QUEST_BOOK =
            value("spawnWithQuestBook", false, Boolean.class::isInstance);

    public static final ConfigSpec SPEC = new ConfigSpec();

    private Config() {
    }

    public static void init() {
        load();
        save();
        BoundlessMod.LOGGER.info("[Boundless] Config loaded");
    }

    public static Path boundlessConfigRoot() {
        return FabricLoader.getInstance().getConfigDir().resolve("boundless").normalize();
    }

    public static Path questPacksRoot() {
        return boundlessConfigRoot().resolve("questpacks").normalize();
    }

    public static Path questPackBackupsRoot() {
        return boundlessConfigRoot().resolve("backups").resolve("questpacks").normalize();
    }

    public static int questPackBackupLimit() {
        return QUEST_PACK_BACKUP_LIMIT;
    }

    public static List<? extends String> disabledCategories() {
        return DISABLED_CATEGORIES.get();
    }

    public static List<? extends String> appliedQuestPacks() {
        return APPLIED_QUEST_PACKS.get();
    }

    public static List<? extends String> disabledQuestPacks() {
        return DISABLED_QUEST_PACKS.get();
    }

    public static boolean isQuestPackApplied(String id, boolean defaultEnabled) {
        String normalized = normalizeQuestPackId(id);
        if (normalized.isBlank()) return false;
        if (containsNormalized(DISABLED_QUEST_PACKS.get(), normalized)) return false;
        if (containsNormalized(APPLIED_QUEST_PACKS.get(), normalized)) return true;
        return defaultEnabled;
    }

    public static void setQuestPackApplied(String id, boolean enabled) {
        String normalized = normalizeQuestPackId(id);
        if (normalized.isBlank()) return;
        List<String> applied = normalizedCopy(APPLIED_QUEST_PACKS.get());
        List<String> disabled = normalizedCopy(DISABLED_QUEST_PACKS.get());
        applied.remove(normalized);
        disabled.remove(normalized);
        if (enabled) applied.add(normalized);
        else disabled.add(normalized);
        APPLIED_QUEST_PACKS.set(applied);
        DISABLED_QUEST_PACKS.set(disabled);
        SPEC.save();
    }

    public static void applySyncedFromServer(
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
            boolean enableQuestToasts,
            boolean disableQuestPinning,
            boolean autoClaimQuestRewards,
            boolean enableQuestScrolls,
            boolean disableQuestBook,
            boolean spawnWithQuestBook) {
        loading = true;
        try {
            DISABLED_CATEGORIES.set(disabledCategories == null ? List.of() : List.copyOf(disabledCategories));
            APPLIED_QUEST_PACKS.set(appliedQuestPacks == null ? List.of() : List.copyOf(appliedQuestPacks));
            DISABLED_QUEST_PACKS.set(disabledQuestPacks == null ? List.of() : List.copyOf(disabledQuestPacks));
            PINNED_QUEST_HUD_POSITION.set(pinnedQuestHudPosition);
            HIDE_QUEST_BOOK_IN_INVENTORY.set(hideQuestBookInInventory);
            QUEST_BOOK_INVENTORY_BUTTON_POSITION.set(questBookInventoryButtonPosition);
            CENTER_INVENTORY_WITH_QUEST_PANEL.set(centerInventoryWithQuestPanel);
            HIDE_CATEGORY_HEADER.set(hideCategoryHeader);
            FILTER_DISPLAY_MODE.set(filterDisplayMode);
            DISABLE_CATEGORIES.set(disableCategories);
            ENABLE_BUILTIN_QUEST_PACK.set(enableBuiltinQuestPack);
            HIDE_QUEST_WIDGET_ICONS.set(hideQuestWidgetIcons);
            QUEST_TEXT_SCALE.set(Math.max(0.5D, Math.min(1.0D, questTextScale)));
            QUEST_ICON_SCALE.set(Math.max(0.5D, Math.min(1.0D, questIconScale)));
            ENABLE_QUEST_SEARCH_BOX.set(enableQuestSearchBox);
            ENABLE_DESCRIPTION_COLORS.set(enableDescriptionColors);
            ENABLE_QUEST_TOASTS.set(enableQuestToasts);
            DISABLE_QUEST_PINNING.set(disableQuestPinning);
            AUTO_CLAIM_QUEST_REWARDS.set(autoClaimQuestRewards);
            ENABLE_QUEST_SCROLLS.set(enableQuestScrolls);
            DISABLE_QUEST_BOOK.set(disableQuestBook);
            SPAWN_WITH_QUEST_BOOK.set(spawnWithQuestBook);
        } finally {
            loading = false;
        }
    }

    public static String pinnedQuestHudPosition() {
        String s = PINNED_QUEST_HUD_POSITION.get();
        if (s == null) return "bottom_left";
        s = s.trim().toLowerCase(Locale.ROOT);
        return s.isBlank() ? "bottom_left" : s;
    }

    public static boolean spawnWithQuestBook() {
        return SPAWN_WITH_QUEST_BOOK.get();
    }

    public static boolean hideQuestBookInInventory() {
        return HIDE_QUEST_BOOK_IN_INVENTORY.get();
    }

    public static String questBookInventoryButtonPosition() {
        String s = QUEST_BOOK_INVENTORY_BUTTON_POSITION.get();
        if (s == null) return "beside_recipe_book";
        s = s.trim().toLowerCase(Locale.ROOT);
        return (s.equals("beside_recipe_book") || s.equals("above_offhand_slot")) ? s : "beside_recipe_book";
    }

    public static boolean centerInventoryWithQuestPanel() {
        return CENTER_INVENTORY_WITH_QUEST_PANEL.get();
    }

    public static boolean hideCategoryHeader() {
        return HIDE_CATEGORY_HEADER.get();
    }

    public static String filterDisplayMode() {
        String s = FILTER_DISPLAY_MODE.get();
        if (s == null) return "tabs";
        s = s.trim().toLowerCase(Locale.ROOT);
        return (s.equals("tabs") || s.equals("buttons") || s.equals("hidden")) ? s : "tabs";
    }

    public static boolean displayFiltersAsTabs() {
        return "tabs".equals(filterDisplayMode());
    }

    public static boolean displayFiltersAsButtons() {
        return "buttons".equals(filterDisplayMode());
    }

    public static boolean hideFilters() {
        return "hidden".equals(filterDisplayMode());
    }

    public static boolean disableCategories() {
        return DISABLE_CATEGORIES.get();
    }

    public static boolean hideQuestWidgetIcons() {
        return HIDE_QUEST_WIDGET_ICONS.get();
    }

    public static float questTextScale() {
        Double value = QUEST_TEXT_SCALE.get();
        if (value == null) return 1.0f;
        return (float) Math.max(0.5D, Math.min(1.0D, value));
    }

    public static float questIconScale() {
        Double value = QUEST_ICON_SCALE.get();
        if (value == null) return 1.0f;
        return (float) Math.max(0.5D, Math.min(1.0D, value));
    }

    public static boolean enableBuiltinQuestPack() {
        return ENABLE_BUILTIN_QUEST_PACK.get();
    }

    public static boolean enableQuestSearchBox() {
        return ENABLE_QUEST_SEARCH_BOX.get();
    }

    public static boolean enableDescriptionColors() {
        return ENABLE_DESCRIPTION_COLORS.get();
    }

    public static boolean enableQuestToasts() {
        return ENABLE_QUEST_TOASTS.get();
    }

    public static boolean disableQuestPinning() {
        return DISABLE_QUEST_PINNING.get();
    }

    public static boolean autoClaimQuestRewards() {
        return AUTO_CLAIM_QUEST_REWARDS.get();
    }

    public static boolean enableQuestScrolls() {
        return ENABLE_QUEST_SCROLLS.get();
    }

    public static boolean disableQuestBook() {
        return DISABLE_QUEST_BOOK.get();
    }

    public static boolean hideQuestBookToggle() {
        return hideQuestBookInInventory();
    }

    private static boolean containsNormalized(List<? extends String> values, String id) {
        if (values == null || id == null || id.isBlank()) return false;
        for (String value : values) {
            if (id.equals(normalizeQuestPackId(value))) return true;
        }
        return false;
    }

    private static List<String> normalizedCopy(List<? extends String> values) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                String normalized = normalizeQuestPackId(value);
                if (!normalized.isBlank()) out.add(normalized);
            }
        }
        return new ArrayList<>(out);
    }

    private static String normalizeQuestPackId(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean validHudPosition(Object value) {
        if (!(value instanceof String s)) return false;
        s = s.trim().toLowerCase(Locale.ROOT);
        return s.equals("top_left") || s.equals("top_right") || s.equals("bottom_left") || s.equals("bottom_right");
    }

    private static boolean validInventoryButtonPosition(Object value) {
        if (!(value instanceof String s)) return false;
        s = s.trim().toLowerCase(Locale.ROOT);
        return s.equals("beside_recipe_book") || s.equals("above_offhand_slot");
    }

    private static boolean validFilterDisplayMode(Object value) {
        if (!(value instanceof String s)) return false;
        s = s.trim().toLowerCase(Locale.ROOT);
        return s.equals("tabs") || s.equals("buttons") || s.equals("hidden");
    }

    private static <T> ConfigValue<T> value(String key, T defaultValue, Predicate<Object> validator) {
        return new ConfigValue<>(key, defaultValue, validator);
    }

    private static ConfigValue<List<? extends String>> list(String key, List<String> defaultValue, Predicate<Object> validator) {
        return new ConfigValue<>(key, defaultValue, value -> {
            if (!(value instanceof List<?> list)) return false;
            for (Object item : list) {
                if (!validator.test(item)) return false;
            }
            return true;
        });
    }

    private static DoubleValue doubleValue(String key, double defaultValue, double min, double max) {
        return new DoubleValue(key, defaultValue, min, max);
    }

    private static void load() {
        if (!Files.exists(CONFIG_PATH)) return;
        loading = true;
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (ConfigValue<?> configValue : ConfigValue.VALUES) {
                configValue.read(root);
            }
        } catch (Exception e) {
            BoundlessMod.LOGGER.warn("[Boundless] Failed to load config {}, using defaults", CONFIG_PATH, e);
        } finally {
            loading = false;
        }
    }

    private static void save() {
        if (loading) return;
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject root = new JsonObject();
            for (ConfigValue<?> configValue : ConfigValue.VALUES) {
                configValue.write(root);
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            BoundlessMod.LOGGER.warn("[Boundless] Failed to save config {}", CONFIG_PATH, e);
        }
    }

    public static class ConfigValue<T> {
        private static final List<ConfigValue<?>> VALUES = new ArrayList<>();

        private final String key;
        private final T defaultValue;
        private final Predicate<Object> validator;
        private T value;

        ConfigValue(String key, T defaultValue, Predicate<Object> validator) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.validator = validator;
            this.value = copy(defaultValue);
            VALUES.add(this);
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            if (value == null || !validator.test(value)) {
                this.value = copy(defaultValue);
            } else {
                this.value = copy(value);
            }
            save();
        }

        @SuppressWarnings("unchecked")
        private void read(JsonObject root) {
            if (!root.has(key)) return;
            try {
                Object decoded = GSON.fromJson(root.get(key), defaultValue.getClass());
                if (defaultValue instanceof List<?>) {
                    decoded = GSON.fromJson(root.get(key), List.class);
                }
                if (decoded != null && validator.test(decoded)) {
                    value = copy((T) decoded);
                }
            } catch (Exception ignored) {
                value = copy(defaultValue);
            }
        }

        private void write(JsonObject root) {
            root.add(key, GSON.toJsonTree(value));
        }

        @SuppressWarnings("unchecked")
        private static <T> T copy(T value) {
            if (value instanceof List<?> list) return (T) List.copyOf(list);
            return value;
        }
    }

    public static final class DoubleValue extends ConfigValue<Double> {
        private final double min;
        private final double max;

        DoubleValue(String key, double defaultValue, double min, double max) {
            super(key, defaultValue, Number.class::isInstance);
            this.min = min;
            this.max = max;
        }

        @Override
        public void set(Double value) {
            if (value == null) {
                super.set(null);
            } else {
                super.set(Math.max(min, Math.min(max, value)));
            }
        }
    }

    public static final class ConfigSpec {
        public void save() {
            Config.save();
        }
    }
}
