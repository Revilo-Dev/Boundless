package net.revilodev.boundless;

import java.util.List;
import java.nio.file.Path;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    // questPackBackupLimit keeps backup history per pack
    private static final int QUEST_PACK_BACKUP_LIMIT = 5;
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // disabledQuestCategories hides matching category ids everywhere
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_CATEGORIES =
            BUILDER.comment("A list of quest category IDs to completely disable.")
                    .defineListAllowEmpty(List.of("disabledQuestCategories"), List::of, o -> o instanceof String);
    // appliedQuestPacks forces quest packs on for the current authority
    public static final ModConfigSpec.ConfigValue<List<? extends String>> APPLIED_QUEST_PACKS =
            BUILDER.comment("Instance questpack IDs explicitly enabled by the server.")
                    .defineListAllowEmpty(List.of("appliedQuestPacks"), List::of, o -> o instanceof String);
    // disabledQuestPacks forces quest packs off for the current authority
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_QUEST_PACKS =
            BUILDER.comment("Instance questpack IDs explicitly disabled by the server.")
                    .defineListAllowEmpty(List.of("disabledQuestPacks"), List::of, o -> o instanceof String);

    // ui config section
    static {
        BUILDER.push("UI");
    }
    // pinnedQuestHudPosition chooses the pinned quest hud corner
    public static final ModConfigSpec.ConfigValue<String> PINNED_QUEST_HUD_POSITION =

            BUILDER.comment("Pin the Quest hud to the: bottom_left, bottom_right, top_left, top_right")
                    .define("pinnedQuestHudPosition", "bottom_left", o -> {
                if (!(o instanceof String s)) return false;
                s = s.trim().toLowerCase();
                return s.equals("top_left") || s.equals("top_right") || s.equals("bottom_left") || s.equals("bottom_right");
            });

    // hideQuestBookInInventory removes the inventory quest button
    public static final ModConfigSpec.ConfigValue<Boolean> HIDE_QUEST_BOOK_IN_INVENTORY =
            BUILDER.comment("If true, hides the quest book button in the inventory screen.")
                    .define("hideQuestBookInInventory", false);
    // questBookInventoryButtonPosition moves the inventory quest button
    public static final ModConfigSpec.ConfigValue<String> QUEST_BOOK_INVENTORY_BUTTON_POSITION =
            BUILDER.comment("Quest book button position in inventory: beside_recipe_book, above_offhand_slot")
                    .define("questBookInventoryButtonPosition", "beside_recipe_book", o -> {
                        if (!(o instanceof String s)) return false;
                        s = s.trim().toLowerCase();
                        return s.equals("beside_recipe_book") || s.equals("above_offhand_slot");
                    });
    // centerInventoryWithQuestPanel keeps both panels centered together
    public static final ModConfigSpec.ConfigValue<Boolean> CENTER_INVENTORY_WITH_QUEST_PANEL =
            BUILDER.comment("If true, centers inventory and quest panel together when the quest panel is open.")
                    .define("centerInventoryWithQuestPanel", true);
    // hideCategoryHeader removes the category banner above quest lists
    public static final ModConfigSpec.ConfigValue<Boolean> HIDE_CATEGORY_HEADER =
            BUILDER.comment("If true, hides the category header bar.")
                    .define("hideCategoryHeader", false);
    // filterDisplayMode switches between tabs buttons or hidden filters
    public static final ModConfigSpec.ConfigValue<String> FILTER_DISPLAY_MODE =
            BUILDER.comment("How quest filters are displayed: tabs, buttons, hidden.")
                    .define("filterDisplayMode", "tabs", o -> {
                        if (!(o instanceof String s)) return false;
                        s = s.trim().toLowerCase();
                        return s.equals("tabs") || s.equals("buttons") || s.equals("hidden");
                    });
    // disableCategories removes category filtering and tabs
    public static final ModConfigSpec.ConfigValue<Boolean> DISABLE_CATEGORIES =
            BUILDER.comment("If true, disables category tabs and category-based filtering.")
                    .define("disableCategories", false);
    // enableBuiltinQuestPack keeps the shipped quest pack active
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_BUILTIN_QUEST_PACK =
            BUILDER.comment("If false, disables the built-in Boundless quest pack.")
                    .define("enableBuiltinQuestPack", true);
    // hideQuestWidgetIcons removes icons from quest rows
    public static final ModConfigSpec.ConfigValue<Boolean> HIDE_QUEST_WIDGET_ICONS =
            BUILDER.comment("If true, hides icons in quest list widgets.")
                    .define("hideQuestWidgetIcons", false);
    // questTextScale scales quest text in list and details panels
    public static final ModConfigSpec.DoubleValue QUEST_TEXT_SCALE =
            BUILDER.comment("Scales quest list widget titles and quest detail description, task, and reward text. Range: 0.5 to 1.0.")
                    .defineInRange("questTextScale", 1.0D, 0.5D, 1.0D);
    // questIconScale scales quest icons in list and details panels
    public static final ModConfigSpec.DoubleValue QUEST_ICON_SCALE =
            BUILDER.comment("Scales quest widget icons and quest detail panel icons. Range: 0.5 to 1.0.")
                    .defineInRange("questIconScale", 1.0D, 0.5D, 1.0D);
    // enableQuestSearchBox shows the quest search field
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_QUEST_SEARCH_BOX =
            BUILDER.comment("If true, shows the quest search box above the quest list.")
                    .define("enableQuestSearchBox", false);
    // enableDescriptionColors enables inline color tokens in quest text
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_DESCRIPTION_COLORS =
            BUILDER.comment("If true, allows colored quest descriptions to render with Boundless color tokens.")
                    .define("enableDescriptionColors", true);
    // questWidgetTextColor sets the default row title color
    public static final ModConfigSpec.ConfigValue<String> QUEST_WIDGET_TEXT_COLOR =
            BUILDER.comment("Hex color used for quest widget titles. Example: FFFFFF")
                    .define("questWidgetTextColor", "FFFFFF", Config::isHexColor);
    // descriptionTextColor sets the default quest description color
    public static final ModConfigSpec.ConfigValue<String> DESCRIPTION_TEXT_COLOR =
            BUILDER.comment("Hex color used for quest description text when no inline color token overrides it. Example: CFCFCF")
                    .define("descriptionTextColor", "CFCFCF", Config::isHexColor);
    // enableDescriptionReadMore collapses long descriptions
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_DESCRIPTION_READ_MORE =
            BUILDER.comment("If true, long quest descriptions collapse behind a read-more toggle.")
                    .define("enableDescriptionReadMore", true);
    // enableDescriptionTextWrapping wraps quest descriptions to panel width
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_DESCRIPTION_TEXT_WRAPPING =
            BUILDER.comment("If true, quest descriptions wrap to fit the detail panel width.")
                    .define("enableDescriptionTextWrapping", true);
    // descriptionTextAlignment changes quest description alignment
    public static final ModConfigSpec.ConfigValue<String> DESCRIPTION_TEXT_ALIGNMENT =
            BUILDER.comment("Quest description text alignment: left, center, right, adjust.")
                    .define("descriptionTextAlignment", "left", o -> {
                        if (!(o instanceof String s)) return false;
                        s = s.trim().toLowerCase();
                        return s.equals("left") || s.equals("center") || s.equals("right") || s.equals("adjust");
                    });
    // enableQuestToasts shows quest unlock toasts
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_QUEST_TOASTS =
            BUILDER.comment("If true, shows quest unlocked toasts.")
                    .define("enableQuestToasts", true);
    // functionality config section
    static {
        BUILDER.pop();
        BUILDER.push("Functionality");
    }
    // disableQuestPinning turns off pinned quests and the pinned hud
    public static final ModConfigSpec.ConfigValue<Boolean> DISABLE_QUEST_PINNING =
            BUILDER.comment("If true, quest pinning and pinned HUD are disabled.")
                    .define("disableQuestPinning", false);
    // autoClaimQuestRewards claims rewards as soon as quests complete
    public static final ModConfigSpec.ConfigValue<Boolean> AUTO_CLAIM_QUEST_REWARDS =
            BUILDER.comment("If true, quest rewards are automatically claimed when a quest becomes complete.")
                    .define("autoClaimQuestRewards", false);
    // enableQuestScrolls allows creating and redeeming quest scrolls
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_QUEST_SCROLLS =
            BUILDER.comment("If true, quest completion scrolls can be created and used.")
                    .define("enableQuestScrolls", true);
    // gameplay config section
    static {
        BUILDER.pop();
        BUILDER.push("Gameplay");
    }
    // disableQuestBook blocks the quest book item and keybind
    public static final ModConfigSpec.ConfigValue<Boolean> DISABLE_QUEST_BOOK =
            BUILDER.comment("If true, quest book opening is disabled.")
                    .define("disableQuestBook", false);
    // spawnWithQuestBook gives new players the quest book
    public static final ModConfigSpec.ConfigValue<Boolean> SPAWN_WITH_QUEST_BOOK =
            BUILDER.comment("If true, players spawn with the quest book.")
                    .define("spawnWithQuestBook", false);
    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    // boundless config root under the game directory
    public static Path boundlessConfigRoot() {
        return FMLPaths.GAMEDIR.get().resolve("config").resolve("boundless").normalize();
    }

    // authoritative quest pack storage root
    public static Path questPacksRoot() {
        return boundlessConfigRoot().resolve("questpacks").normalize();
    }

    // stored backup root for quest pack snapshots
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

    // decide if a pack id is enabled after authority overrides
    public static boolean isQuestPackApplied(String id, boolean defaultEnabled) {
        String normalized = normalizeQuestPackId(id);
        if (normalized.isBlank()) return false;
        if (containsNormalized(DISABLED_QUEST_PACKS.get(), normalized)) return false;
        if (containsNormalized(APPLIED_QUEST_PACKS.get(), normalized)) return true;
        return defaultEnabled;
    }

    // write the enabled state for one quest pack id
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

    // apply the current authority config snapshot on clients
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
            boolean spawnWithQuestBook) {
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
        QUEST_WIDGET_TEXT_COLOR.set(normalizeHexColor(questWidgetTextColor, "FFFFFF"));
        DESCRIPTION_TEXT_COLOR.set(normalizeHexColor(descriptionTextColor, "CFCFCF"));
        ENABLE_DESCRIPTION_READ_MORE.set(enableDescriptionReadMore);
        ENABLE_DESCRIPTION_TEXT_WRAPPING.set(enableDescriptionTextWrapping);
        DESCRIPTION_TEXT_ALIGNMENT.set(descriptionTextAlignment);
        ENABLE_QUEST_TOASTS.set(enableQuestToasts);
        DISABLE_QUEST_PINNING.set(disableQuestPinning);
        AUTO_CLAIM_QUEST_REWARDS.set(autoClaimQuestRewards);
        ENABLE_QUEST_SCROLLS.set(enableQuestScrolls);
        DISABLE_QUEST_BOOK.set(disableQuestBook);
        SPAWN_WITH_QUEST_BOOK.set(spawnWithQuestBook);
    }

    // normalize list checks for quest pack ids
    private static boolean containsNormalized(List<? extends String> values, String id) {
        if (values == null || id == null || id.isBlank()) return false;
        for (String value : values) {
            if (id.equals(normalizeQuestPackId(value))) return true;
        }
        return false;
    }

    // copy quest pack ids into a stable normalized list
    private static List<String> normalizedCopy(List<? extends String> values) {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                String normalized = normalizeQuestPackId(value);
                if (!normalized.isBlank()) out.add(normalized);
            }
        }
        return new java.util.ArrayList<>(out);
    }

    // normalize quest pack ids for config storage
    private static String normalizeQuestPackId(String id) {
        return id == null ? "" : id.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static String pinnedQuestHudPosition() {
        String s = PINNED_QUEST_HUD_POSITION.get();
        if (s == null) return "bottom_left";
        s = s.trim().toLowerCase();
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
        s = s.trim().toLowerCase();
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
        s = s.trim().toLowerCase();
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

    public static int questWidgetTextColor() {
        return parseHexColor(QUEST_WIDGET_TEXT_COLOR.get(), 0xFFFFFF);
    }

    public static int descriptionTextColor() {
        return parseHexColor(DESCRIPTION_TEXT_COLOR.get(), 0xCFCFCF);
    }

    public static boolean enableDescriptionReadMore() {
        return ENABLE_DESCRIPTION_READ_MORE.get();
    }

    public static boolean enableDescriptionTextWrapping() {
        return ENABLE_DESCRIPTION_TEXT_WRAPPING.get();
    }

    public static String descriptionTextAlignment() {
        String s = DESCRIPTION_TEXT_ALIGNMENT.get();
        if (s == null) return "left";
        s = s.trim().toLowerCase();
        return (s.equals("left") || s.equals("center") || s.equals("right") || s.equals("adjust")) ? s : "left";
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

    // keep older callers working
    public static boolean hideQuestBookToggle() {
        return hideQuestBookInInventory();
    }

    // log the loaded config snapshot
    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading e) {
        if (e.getConfig().getSpec() == SPEC)
            BoundlessMod.LOGGER.info("[Boundless] Config loaded: categories={}, pos={}, hideInvBtn={}, invBtnPos={}, centerInv={}, hideHeader={}, filterMode={}, disableCategories={}, builtinPack={}, hideWidgetIcons={}, textScale={}, iconScale={}, searchBox={}, descColors={}, widgetTextColor=#{}, descriptionTextColor=#{}, descReadMore={}, descWrap={}, descAlign={}, questToasts={}, disablePinning={}, autoClaim={}, questScrolls={}, disableBook={}, spawnBook={}",
                    disabledCategories(),
                    pinnedQuestHudPosition(),
                    hideQuestBookInInventory(),
                    questBookInventoryButtonPosition(),
                    centerInventoryWithQuestPanel(),
                    hideCategoryHeader(),
                    filterDisplayMode(),
                    disableCategories(),
                    enableBuiltinQuestPack(),
                    hideQuestWidgetIcons(),
                    questTextScale(),
                    questIconScale(),
                    enableQuestSearchBox(),
                    enableDescriptionColors(),
                    normalizeHexColor(QUEST_WIDGET_TEXT_COLOR.get(), "FFFFFF"),
                    normalizeHexColor(DESCRIPTION_TEXT_COLOR.get(), "CFCFCF"),
                    enableDescriptionReadMore(),
                    enableDescriptionTextWrapping(),
                    descriptionTextAlignment(),
                    enableQuestToasts(),
                    disableQuestPinning(),
                    autoClaimQuestRewards(),
                    enableQuestScrolls(),
                    disableQuestBook(),
                    spawnWithQuestBook());
    }

    // log the reloaded config snapshot
    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading e) {
        if (e.getConfig().getSpec() == SPEC)
            BoundlessMod.LOGGER.info("[Boundless] Config reloaded: categories={}, pos={}, hideInvBtn={}, invBtnPos={}, centerInv={}, hideHeader={}, filterMode={}, disableCategories={}, builtinPack={}, hideWidgetIcons={}, textScale={}, iconScale={}, searchBox={}, descColors={}, widgetTextColor=#{}, descriptionTextColor=#{}, descReadMore={}, descWrap={}, descAlign={}, questToasts={}, disablePinning={}, autoClaim={}, questScrolls={}, disableBook={}, spawnBook={}",
                    disabledCategories(),
                    pinnedQuestHudPosition(),
                    hideQuestBookInInventory(),
                    questBookInventoryButtonPosition(),
                    centerInventoryWithQuestPanel(),
                    hideCategoryHeader(),
                    filterDisplayMode(),
                    disableCategories(),
                    enableBuiltinQuestPack(),
                    hideQuestWidgetIcons(),
                    questTextScale(),
                    questIconScale(),
                    enableQuestSearchBox(),
                    enableDescriptionColors(),
                    normalizeHexColor(QUEST_WIDGET_TEXT_COLOR.get(), "FFFFFF"),
                    normalizeHexColor(DESCRIPTION_TEXT_COLOR.get(), "CFCFCF"),
                    enableDescriptionReadMore(),
                    enableDescriptionTextWrapping(),
                    descriptionTextAlignment(),
                    enableQuestToasts(),
                    disableQuestPinning(),
                    autoClaimQuestRewards(),
                    enableQuestScrolls(),
                    disableQuestBook(),
                    spawnWithQuestBook());
    }

    // validate six digit hex color strings
    private static boolean isHexColor(Object raw) {
        if (!(raw instanceof String s)) return false;
        String value = s.trim();
        if (value.startsWith("#")) value = value.substring(1);
        return value.matches("(?i)[0-9a-f]{6}");
    }

    // normalize config colors into uppercase hex
    private static String normalizeHexColor(String raw, String fallback) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("#")) value = value.substring(1);
        value = value.toUpperCase(java.util.Locale.ROOT);
        return value.matches("[0-9A-F]{6}") ? value : fallback;
    }

    // parse config colors with a fallback value
    private static int parseHexColor(String raw, int fallback) {
        try {
            return Integer.parseInt(normalizeHexColor(raw, String.format(java.util.Locale.ROOT, "%06X", fallback)), 16);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
