package net.revilodev.boundless.client.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestPack;
import net.revilodev.boundless.quest.QuestPackStorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class FtbQuestImporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int POSITION_SCALE = 48;
    private static final int DEFAULT_OPACITY_PERCENT = 45;
    private static final Pattern RESOURCE_ID = Pattern.compile("#?[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern FILTER_ITEM = Pattern.compile("(?:item|item_tag)\\((#?[a-z0-9_.-]+:[a-z0-9_./-]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORMAT_CODE = Pattern.compile("&([0-9a-fk-or])", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMAGE_MARKUP = Pattern.compile("\\{image:([^ }]+)[^}]*}", Pattern.CASE_INSENSITIVE);

    private FtbQuestImporter() {
    }

    public record ImportResult(QuestPack pack, int chapters, int quests, int tasks, int rewards,
                               int unsupportedEntries, Path reportPath, List<String> warnings) {
    }

    public static ImportResult importPack(Path selectedPath, Path packsRoot, String requestedName) throws IOException {
        Path sourceRoot = resolveQuestRoot(selectedPath);
        Path normalizedPacksRoot = packsRoot.toAbsolutePath().normalize();
        String packName = QuestEditorNaming.cleanPackName(requestedName);
        if (!QuestEditorPackFiles.hasPackNameContent(packName)
                || QuestEditorPackFiles.isInvalidPackFolderName(packName)) {
            throw new IOException("Enter a valid pack name before importing");
        }
        String namespace = QuestEditorPackFiles.namespaceFromPackName(packName);
        Path target = normalizedPacksRoot.resolve(packName).normalize();
        Path staging = normalizedPacksRoot.resolve("." + packName + ".ftb-import.tmp").normalize();
        if (!target.startsWith(normalizedPacksRoot) || !staging.startsWith(normalizedPacksRoot)) {
            throw new IOException("Invalid import destination");
        }
        if (Files.exists(target)) throw new IOException("Pack already exists: " + packName);

        Files.createDirectories(normalizedPacksRoot);
        deleteStagingDirectory(staging, normalizedPacksRoot);
        ImportContext context = new ImportContext(sourceRoot, packName, namespace, staging);
        try {
            context.read();
            context.write();
            moveStaging(staging, target);
        } catch (Exception exception) {
            deleteStagingDirectory(staging, normalizedPacksRoot);
            if (exception instanceof IOException ioException) throw ioException;
            throw new IOException("FTB Quests import failed: " + exception.getMessage(), exception);
        }

        QuestPack pack = new QuestPack(packName, namespace, target, false, true);
        Path report = target.resolve("ftb-import-report.txt");
        return new ImportResult(pack, context.chapters.size(), context.questCount, context.taskCount,
                context.rewardCount, context.unsupportedEntries, report, List.copyOf(context.warnings));
    }

    static Path resolveQuestRoot(Path selectedPath) throws IOException {
        if (selectedPath == null) throw new IOException("No FTB Quests folder selected");
        Path selected = selectedPath.toAbsolutePath().normalize();
        if (Files.isRegularFile(selected)) selected = selected.getParent();
        if (selected == null || !Files.isDirectory(selected)) throw new IOException("Selected folder does not exist");

        List<Path> candidates = List.of(
                selected,
                selected.resolve("quests"),
                selected.resolve("ftbquests").resolve("quests"),
                selected.resolve("config").resolve("ftbquests").resolve("quests"),
                selected.getFileName() != null && selected.getFileName().toString().equalsIgnoreCase("chapters")
                        ? selected.getParent() : selected
        );
        for (Path candidate : candidates) {
            if (hasChapterFiles(candidate)) return candidate.toAbsolutePath().normalize();
        }
        try (Stream<Path> paths = Files.find(selected, 5,
                (path, attributes) -> attributes.isDirectory() && hasChapterFiles(path))) {
            return paths.findFirst().orElseThrow(() -> new IOException(
                    "No FTB Quests chapters were found. Select config/ftbquests/quests."));
        }
    }

    private static boolean hasChapterFiles(Path root) {
        if (root == null || !Files.isDirectory(root.resolve("chapters"))) return false;
        try (Stream<Path> files = Files.list(root.resolve("chapters"))) {
            return files.anyMatch(FtbQuestImporter::isQuestDataFile);
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean isQuestDataFile(Path path) {
        if (path == null || !Files.isRegularFile(path)) return false;
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".snbt") || name.endsWith(".json5") || name.endsWith(".json");
    }

    private static void moveStaging(Path staging, Path target) throws IOException {
        try {
            Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(staging, target);
        }
    }

    private static void deleteStagingDirectory(Path staging, Path packsRoot) throws IOException {
        if (!Files.exists(staging)) return;
        Path normalized = staging.toAbsolutePath().normalize();
        if (!normalized.startsWith(packsRoot) || !normalized.getFileName().toString().endsWith(".ftb-import.tmp")) {
            throw new IOException("Refusing to remove unsafe staging path");
        }
        try (Stream<Path> walk = Files.walk(normalized)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static final class ImportContext {
        private final Path sourceRoot;
        private final String packName;
        private final String namespace;
        private final Path stagingRoot;
        private final List<ChapterDraft> chapters = new ArrayList<>();
        private final Map<String, QuestDraft> questsByFtbId = new LinkedHashMap<>();
        private final Map<String, List<String>> language = new LinkedHashMap<>();
        private final Map<String, CompoundTag> rewardTables = new HashMap<>();
        private final Map<String, Integer> chapterGroupOrder = new HashMap<>();
        private final LinkedHashSet<String> warnings = new LinkedHashSet<>();
        private final Set<String> usedCategoryIds = new HashSet<>();
        private final Set<String> usedQuestIds = new HashSet<>();
        private CompoundTag bookData = new CompoundTag();
        private int questCount;
        private int taskCount;
        private int rewardCount;
        private int unsupportedEntries;

        private ImportContext(Path sourceRoot, String packName, String namespace, Path stagingRoot) {
            this.sourceRoot = sourceRoot;
            this.packName = packName;
            this.namespace = namespace;
            this.stagingRoot = stagingRoot;
        }

        private void read() throws IOException {
            Path dataFile = firstExisting(sourceRoot.resolve("data.snbt"), sourceRoot.resolve("data.json5"),
                    sourceRoot.resolve("data.json"));
            if (dataFile != null) bookData = readCompound(dataFile);
            readLanguageFiles();
            readChapterGroups();
            readRewardTables();

            List<Path> chapterFiles;
            try (Stream<Path> files = Files.list(sourceRoot.resolve("chapters"))) {
                chapterFiles = files.filter(FtbQuestImporter::isQuestDataFile)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .toList();
            }
            if (chapterFiles.isEmpty()) throw new IOException("The selected FTB Quests folder has no chapters");

            int sourceIndex = 0;
            for (Path chapterFile : chapterFiles) {
                CompoundTag chapterTag = readCompound(chapterFile);
                String stem = stripExtension(chapterFile.getFileName().toString());
                String ftbId = normalizedFtbId(stringValue(chapterTag, "id"));
                if (ftbId.isBlank()) ftbId = String.format(Locale.ROOT, "SYNTHETIC_CHAPTER_%04d", sourceIndex);
                String title = localized("chapter." + ftbId + ".title", stringValue(chapterTag, "title"));
                if (title.isBlank()) title = humanize(stem);
                String categoryId = uniqueId(slug(title), stem, ftbId, usedCategoryIds);
                int order = chapterTag.contains("order_index") ? chapterTag.getInt("order_index") : sourceIndex;
                String icon = extractFirstItemId(chapterTag.get("icon"));
                String groupId = normalizedFtbId(stringValue(chapterTag, "group"));
                ChapterDraft chapter = new ChapterDraft(chapterFile, chapterTag, ftbId, categoryId,
                        title, icon, order, chapterGroupOrder.getOrDefault(groupId, -1), sourceIndex++);
                chapters.add(chapter);
                readQuests(chapter);
                if (!chapterTag.getList("quest_links", Tag.TAG_COMPOUND).isEmpty()) {
                    unsupported("Quest links in chapter " + title + " cannot be represented and were omitted.");
                }
                if (!chapterTag.getList("images", Tag.TAG_COMPOUND).isEmpty()) {
                    unsupported("Decorative images in chapter " + title + " cannot be represented and were omitted.");
                }
            }
            chapters.sort(Comparator.comparingInt((ChapterDraft chapter) -> chapter.groupOrder)
                    .thenComparingInt(chapter -> chapter.order)
                    .thenComparingInt(chapter -> chapter.sourceIndex));
            resolveExclusiveDependencies();
        }

        private void readLanguageFiles() throws IOException {
            Path languageRoot = sourceRoot.resolve("lang");
            if (!Files.isDirectory(languageRoot)) return;
            List<Path> files;
            try (Stream<Path> walk = Files.walk(languageRoot, 8)) {
                files = walk.filter(FtbQuestImporter::isQuestDataFile)
                        .filter(path -> path.toString().toLowerCase(Locale.ROOT).contains("en_us"))
                        .sorted()
                        .toList();
            }
            for (Path file : files) {
                try {
                    flattenLanguage(readCompound(file), "");
                } catch (IOException exception) {
                    warn("Language file " + sourceRoot.relativize(file) + " could not be read: " + exception.getMessage());
                }
            }
        }

        private void flattenLanguage(CompoundTag tag, String prefix) {
            for (String key : tag.getAllKeys()) {
                Tag value = tag.get(key);
                String path = prefix.isBlank() ? key : prefix + "." + key;
                List<String> lines = textLines(value);
                if (!lines.isEmpty()) {
                    language.put(path, lines);
                    language.putIfAbsent(key, lines);
                } else if (value instanceof CompoundTag compound) {
                    flattenLanguage(compound, path);
                }
            }
        }

        private void readRewardTables() throws IOException {
            Path tablesRoot = sourceRoot.resolve("reward_tables");
            if (!Files.isDirectory(tablesRoot)) return;
            try (Stream<Path> files = Files.list(tablesRoot)) {
                for (Path file : files.filter(FtbQuestImporter::isQuestDataFile).sorted().toList()) {
                    try {
                        CompoundTag table = readCompound(file);
                        String id = normalizedFtbId(stringValue(table, "id"));
                        if (id.isBlank()) id = normalizedFtbId(stripExtension(file.getFileName().toString()));
                        putRewardTableIds(id, table);
                    } catch (IOException exception) {
                        warn("Reward table " + sourceRoot.relativize(file) + " could not be read: " + exception.getMessage());
                    }
                }
            }
        }

        private void readChapterGroups() throws IOException {
            Path groupsFile = firstExisting(sourceRoot.resolve("chapter_groups.snbt"),
                    sourceRoot.resolve("chapter_groups.json5"), sourceRoot.resolve("chapter_groups.json"));
            if (groupsFile == null) return;
            CompoundTag groups = readCompound(groupsFile);
            int order = 0;
            for (CompoundTag group : compoundList(groups, "chapter_groups")) {
                String id = normalizedFtbId(stringValue(group, "id"));
                if (!id.isBlank()) chapterGroupOrder.put(id, order++);
            }
            if (!chapterGroupOrder.isEmpty()) {
                warn("FTB chapter groups were flattened into ordered Boundless category tabs.");
            }
        }

        private void putRewardTableIds(String id, CompoundTag table) {
            if (id == null || id.isBlank()) return;
            rewardTables.put(id.toUpperCase(Locale.ROOT), table);
            try {
                long value = Long.parseUnsignedLong(id, 16);
                rewardTables.put(Long.toUnsignedString(value), table);
                rewardTables.put(String.format(Locale.ROOT, "%016X", value), table);
            } catch (NumberFormatException ignored) {
            }
        }

        private void readQuests(ChapterDraft chapter) {
            ListTag questTags = chapter.tag.getList("quests", Tag.TAG_COMPOUND);
            int index = 0;
            for (Tag element : questTags) {
                if (!(element instanceof CompoundTag questTag)) continue;
                String ftbId = normalizedFtbId(stringValue(questTag, "id"));
                if (ftbId.isBlank()) ftbId = chapter.ftbId + "_QUEST_" + index;
                String title = localized("quest." + ftbId + ".title", stringValue(questTag, "title"));
                if (title.isBlank()) title = taskDerivedTitle(questTag, index + 1);
                String questId = uniqueId(slug(title), chapter.categoryId + "_quest", ftbId, usedQuestIds);
                String icon = extractFirstItemId(questTag.get("icon"));
                if (icon.isBlank()) icon = firstTaskIcon(questTag);
                if (icon.isBlank()) icon = chapter.icon.isBlank() ? "minecraft:paper" : chapter.icon;
                QuestDraft quest = new QuestDraft(chapter, questTag, ftbId, questId, title, icon,
                        questTag.getDouble("x"), questTag.getDouble("y"), index++);
                chapter.quests.add(quest);
                questsByFtbId.put(ftbId, quest);
                questCount++;
            }
            if (chapter.icon.isBlank() && !chapter.quests.isEmpty()) chapter.icon = chapter.quests.getFirst().icon;
        }

        private String taskDerivedTitle(CompoundTag questTag, int number) {
            ListTag tasks = questTag.getList("tasks", Tag.TAG_COMPOUND);
            if (!tasks.isEmpty()) {
                CompoundTag task = tasks.getCompound(0);
                String taskId = normalizedFtbId(stringValue(task, "id"));
                String title = localized("task." + taskId + ".title", stringValue(task, "title"));
                if (!title.isBlank()) return title;
                String item = extractFirstItemId(task.get("item"));
                if (!item.isBlank()) return "Obtain " + humanize(resourcePath(item));
                String type = stringValue(task, "type");
                if (!type.isBlank()) return humanize(type) + " Quest";
            }
            return "Quest " + number;
        }

        private void resolveExclusiveDependencies() {
            for (QuestDraft parent : questsByFtbId.values()) {
                if (parent.tag.getInt("max_completable_dependents") != 1) continue;
                List<QuestDraft> dependents = questsByFtbId.values().stream()
                        .filter(quest -> stringList(quest.tag, "dependencies").contains(parent.ftbId))
                        .toList();
                for (int left = 0; left < dependents.size(); left++) {
                    for (int right = left + 1; right < dependents.size(); right++) {
                        dependents.get(left).incompatible.add(dependents.get(right).id);
                        dependents.get(right).incompatible.add(dependents.get(left).id);
                    }
                }
            }
        }

        private void write() throws IOException {
            Files.createDirectories(stagingRoot);
            QuestEditorPackFiles.writePackMeta(stagingRoot, packName,
                    "Imported from FTB Quests: " + packName, firstPackIcon(), "tree", true);
            QuestPack stagingPack = new QuestPack(packName, namespace, stagingRoot, false, true);
            stagingPack.ensureDirs();
            int categoryOrder = 0;
            int questOrder = 1;
            for (ChapterDraft chapter : chapters) {
                writeCategory(stagingPack, chapter, categoryOrder++);
                centerChapter(chapter);
                QuestDraft origin = chapter.quests.stream()
                        .filter(quest -> resolvedDependencies(quest).isEmpty())
                        .findFirst().orElse(chapter.quests.isEmpty() ? null : chapter.quests.getFirst());
                for (QuestDraft quest : chapter.quests) {
                    writeQuest(stagingPack, quest, quest == origin, questOrder++);
                }
            }
            writeReport();
        }

        private String firstPackIcon() {
            return chapters.stream().map(chapter -> chapter.icon).filter(icon -> !icon.isBlank())
                    .findFirst().orElse("minecraft:book");
        }

        private void writeCategory(QuestPack pack, ChapterDraft chapter, int order) throws IOException {
            JsonObject category = new JsonObject();
            category.addProperty("id", chapter.categoryId);
            category.addProperty("name", chapter.title);
            category.addProperty("icon", chapter.icon.isBlank() ? "minecraft:book" : chapter.icon);
            category.addProperty("order", order);
            category.addProperty("auto_complete", false);
            category.addProperty("layout", "tree");
            category.addProperty("background_darkening", false);
            category.addProperty("background_darkening_opacity", DEFAULT_OPACITY_PERCENT / 100f);
            QuestPackStorage.writeJsonAtomically(GSON, category,
                    pack.categoriesDir.resolve(chapter.categoryId + ".json"));
        }

        private void centerChapter(ChapterDraft chapter) {
            if (chapter.quests.isEmpty()) return;
            double minX = chapter.quests.stream().mapToDouble(quest -> quest.x).min().orElse(0d);
            double maxX = chapter.quests.stream().mapToDouble(quest -> quest.x).max().orElse(0d);
            double minY = chapter.quests.stream().mapToDouble(quest -> quest.y).min().orElse(0d);
            double maxY = chapter.quests.stream().mapToDouble(quest -> quest.y).max().orElse(0d);
            double centerX = (minX + maxX) / 2d;
            double centerY = (minY + maxY) / 2d;
            for (QuestDraft quest : chapter.quests) {
                quest.treeX = (int) Math.round((quest.x - centerX) * POSITION_SCALE);
                quest.treeY = (int) Math.round((quest.y - centerY) * POSITION_SCALE);
            }
        }

        private void writeQuest(QuestPack pack, QuestDraft quest, boolean origin, int order) throws IOException {
            JsonObject json = new JsonObject();
            json.addProperty("id", quest.id);
            json.addProperty("name", quest.title);
            json.addProperty("icon", quest.icon);
            String description = questDescription(quest);
            if (!description.isBlank()) json.addProperty("description", description);
            json.addProperty("category", quest.chapter.categoryId);
            json.addProperty("optional", quest.tag.getBoolean("optional"));
            json.addProperty("repeatable", booleanValue(quest.tag, "can_repeat", false)
                    || quest.tag.getLong("repeat_time") > 0L || quest.tag.getLong("repeat_cooldown") > 0L);
            json.addProperty("auto_complete", false);
            json.addProperty("hiddenUnderDependency", booleanValue(quest.tag, "hide_until_deps_visible", false)
                    || booleanValue(quest.tag, "hide_until_deps_complete", false)
                    || booleanValue(quest.tag, "invisible", false));
            json.addProperty("lock_after_dependency", false);
            json.addProperty("tree_x", quest.treeX);
            json.addProperty("tree_y", quest.treeY);
            json.addProperty("tree_background", questShape(quest));
            if (origin) json.addProperty("tree_origin", true);

            List<String> dependencies = resolvedDependencies(quest);
            if (dependencies.size() == 1) {
                json.addProperty("dependencies", dependencies.getFirst());
            } else if (!dependencies.isEmpty()) {
                JsonArray array = new JsonArray();
                dependencies.forEach(array::add);
                json.add("dependencies", array);
            }
            if (!quest.incompatible.isEmpty()) {
                JsonArray array = new JsonArray();
                quest.incompatible.forEach(array::add);
                json.add("incompatible_dependencies", array);
            }

            JsonObject completion = convertTasks(quest);
            if (completion != null) json.add("completion", completion);
            JsonObject reward = convertRewards(quest);
            if (reward != null) json.add("reward", reward);
            Path target = pack.questsDir.resolve(String.format(Locale.ROOT, "%04d-%s.json", order, quest.id));
            QuestPackStorage.writeJsonAtomically(GSON, json, target);
        }

        private List<String> resolvedDependencies(QuestDraft quest) {
            List<String> out = new ArrayList<>();
            List<String> raw = stringList(quest.tag, "dependencies");
            for (String ftbDependency : raw) {
                QuestDraft dependency = questsByFtbId.get(normalizedFtbId(ftbDependency));
                if (dependency == null) {
                    warn("Quest " + quest.title + " references missing dependency " + ftbDependency + ".");
                } else if (!out.contains(dependency.id)) {
                    out.add(dependency.id);
                }
            }
            String requirement = stringValue(quest.tag, "dependency_requirement").toLowerCase(Locale.ROOT);
            int minimum = quest.tag.getInt("min_required_dependencies");
            if ((!requirement.isBlank() && !requirement.equals("all_completed") && !requirement.equals("default"))
                    || minimum > 0 && minimum < out.size()) {
                warn("Quest " + quest.title + " uses partial/OR dependencies; Boundless imports all dependencies as required.");
            }
            return out;
        }

        private JsonObject convertTasks(QuestDraft quest) {
            List<CompoundTag> tasks = compoundList(quest.tag, "tasks");
            List<CompoundTag> required = tasks.stream().filter(task -> !task.getBoolean("optional_task")).toList();
            if (!required.isEmpty() && required.size() < tasks.size()) {
                warn("Quest " + quest.title + " has optional tasks; optional tasks were omitted.");
                tasks = required;
            } else if (!tasks.isEmpty() && required.isEmpty()) {
                warn("Every task in " + quest.title + " is optional; all were imported as required.");
            }
            String requirement = stringValue(quest.tag, "task_requirement").toLowerCase(Locale.ROOT);
            int minimum = quest.tag.getInt("min_required_tasks");
            if ((requirement.contains("one") || minimum > 0 && minimum < tasks.size()) && tasks.size() > 1) {
                warn("Quest " + quest.title + " uses partial/OR task completion; only the first required task was imported.");
                tasks = List.of(tasks.getFirst());
            }

            JsonArray converted = new JsonArray();
            for (CompoundTag task : tasks) {
                converted.add(convertTask(quest, task));
                taskCount++;
            }
            if (converted.isEmpty()) {
                JsonObject check = new JsonObject();
                check.addProperty("check", "Complete quest");
                converted.add(check);
                if (tasks.isEmpty()) warn("Quest " + quest.title + " had no tasks and was imported as a checkmark quest.");
            }
            JsonObject completion = new JsonObject();
            completion.add("complete", converted);
            return completion;
        }

        private JsonObject convertTask(QuestDraft quest, CompoundTag task) {
            String type = stringValue(task, "type").toLowerCase(Locale.ROOT);
            if (type.isBlank() && task.contains("item")) type = "item";
            int count = positiveInt(task, "count", positiveInt(task, "value", 1));
            JsonObject target = new JsonObject();
            switch (type) {
                case "item" -> {
                    if (hasMeaningfulComponents(task.get("item"))) {
                        warn("Component/NBT matching in item task for " + quest.title + " was simplified to an item ID.");
                    }
                    if (task.getBoolean("only_from_crafting")) {
                        warn("Crafting-only detection in item task for " + quest.title + " is not available in Boundless.");
                    }
                    List<String> items = extractItemIds(task.get("item"));
                    if (!items.isEmpty()) {
                        boolean consume = inheritedBoolean(quest, "consume_items", false);
                        target.addProperty(consume ? "submit" : "collect", items.getFirst());
                        if (items.size() > 1) {
                            JsonArray accepted = new JsonArray();
                            items.forEach(accepted::add);
                            target.add("acceptedItems", accepted);
                        }
                        target.addProperty("count", count);
                        return target;
                    }
                }
                case "kill", "entity" -> {
                    String entity = resourceId(stringValue(task, "entity"));
                    if (!entity.isBlank()) {
                        target.addProperty("kill", entity);
                        target.addProperty("count", count);
                        if (!stringValue(task, "entityTypeTag").isBlank()) {
                            warn("Kill task in " + quest.title + " used an entity tag; its displayed entity was imported.");
                        }
                        return target;
                    }
                }
                case "advancement" -> {
                    String advancement = resourceId(stringValue(task, "advancement"));
                    if (!advancement.isBlank()) {
                        target.addProperty("advancement", advancement);
                        return target;
                    }
                }
                case "dimension" -> {
                    String dimension = resourceId(stringValue(task, "dimension"));
                    if (!dimension.isBlank()) {
                        target.addProperty("dimension", dimension);
                        return target;
                    }
                }
                case "biome" -> {
                    String biome = resourceId(stringValue(task, "biome"));
                    if (!biome.isBlank()) {
                        target.addProperty("biome", biome);
                        return target;
                    }
                }
                case "observation", "observe" -> {
                    String observed = resourceId(stringValue(task, "to_observe"));
                    if (!observed.isBlank()) {
                        target.addProperty("observe", observed);
                        return target;
                    }
                }
                case "xp", "experience" -> {
                    target.addProperty("xp", task.getBoolean("points") ? "points" : "levels");
                    target.addProperty("count", count);
                    return target;
                }
                case "checkmark", "check" -> {
                    target.addProperty("check", taskTitle(task, "Understand"));
                    return target;
                }
                case "fluid" -> {
                    CompoundTag fluid = task.getCompound("fluid");
                    String fluidId = resourceId(stringValue(fluid, "id"));
                    int amount = positiveInt(fluid, "amount", count);
                    target.addProperty("check", "Obtain " + amount + " mB of " + (fluidId.isBlank() ? "fluid" : fluidId));
                    unsupported("Fluid task in " + quest.title + " requires manual confirmation after import.");
                    return target;
                }
                case "structure" -> {
                    target.addProperty("check", "Visit " + stringValue(task, "structure"));
                    unsupported("Structure task in " + quest.title + " requires manual confirmation after import.");
                    return target;
                }
                case "location" -> {
                    target.addProperty("check", "Visit the requested location");
                    unsupported("Location task in " + quest.title + " requires manual confirmation after import.");
                    return target;
                }
                case "stat" -> {
                    target.addProperty("check", "Reach " + count + " " + stringValue(task, "stat"));
                    unsupported("Statistic task in " + quest.title + " requires manual confirmation after import.");
                    return target;
                }
                default -> {
                }
            }
            target.addProperty("check", taskTitle(task, type.isBlank() ? "Complete task" : humanize(type)));
            unsupported("Unsupported " + (type.isBlank() ? "unknown" : type) + " task in " + quest.title
                    + " was replaced with a checkmark.");
            return target;
        }

        private boolean inheritedBoolean(QuestDraft quest, String key, boolean fallback) {
            if (quest.tag.contains(key)) return booleanValue(quest.tag, key, fallback);
            if (quest.chapter.tag.contains(key)) return booleanValue(quest.chapter.tag, key, fallback);
            if (bookData.contains("default_" + key)) return booleanValue(bookData, "default_" + key, fallback);
            return fallback;
        }

        private JsonObject convertRewards(QuestDraft quest) {
            RewardBundle bundle = new RewardBundle();
            for (CompoundTag reward : compoundList(quest.tag, "rewards")) {
                int added = convertReward(quest, reward, bundle, 0);
                if (added > 0) rewardCount += added;
            }
            return bundle.toJson();
        }

        private int convertReward(QuestDraft quest, CompoundTag reward, RewardBundle bundle, int depth) {
            if (depth > 5) {
                unsupported("Reward table nesting was too deep in " + quest.title + ".");
                return 0;
            }
            String type = stringValue(reward, "type").toLowerCase(Locale.ROOT);
            if (type.isBlank() && reward.contains("item")) type = "item";
            switch (type) {
                case "item" -> {
                    if (hasMeaningfulComponents(reward.get("item"))) {
                        warn("Components on an item reward in " + quest.title + " were simplified to an item ID.");
                    }
                    List<String> items = extractItemIds(reward.get("item"));
                    if (items.isEmpty()) break;
                    JsonObject item = new JsonObject();
                    item.addProperty("item", items.getFirst());
                    if (items.size() > 1) {
                        JsonArray accepted = new JsonArray();
                        items.forEach(accepted::add);
                        item.add("acceptedItems", accepted);
                    }
                    int embeddedCount = reward.get("item") instanceof CompoundTag stack
                            ? positiveInt(stack, "count", 1) : 1;
                    item.addProperty("count", positiveInt(reward, "count", embeddedCount));
                    bundle.items.add(item);
                    if (reward.getInt("random_bonus") > 0) {
                        warn("Item reward in " + quest.title + " has a random bonus; only its base count was imported.");
                    }
                    return 1;
                }
                case "xp", "experience" -> {
                    bundle.addExperience("points", positiveInt(reward, "xp", positiveInt(reward, "value", 1)), quest.title, warnings);
                    return 1;
                }
                case "xp_levels", "levels" -> {
                    bundle.addExperience("levels", positiveInt(reward, "xp_levels", positiveInt(reward, "value", 1)), quest.title, warnings);
                    return 1;
                }
                case "command" -> {
                    String command = stringValue(reward, "command").trim();
                    if (command.startsWith("/")) command = command.substring(1).trim();
                    command = command.replace("{p}", "@s").replace("{player}", "@s").replace("{team}", "");
                    if (!command.isBlank()) {
                        JsonObject object = new JsonObject();
                        object.addProperty("command", command);
                        String icon = extractFirstItemId(reward.get("icon"));
                        if (!icon.isBlank()) object.addProperty("icon", icon);
                        object.addProperty("title", rewardTitle(reward, "Command reward"));
                        bundle.commands.add(object);
                        return 1;
                    }
                }
                case "advancement" -> {
                    String advancementId = resourceId(stringValue(reward, "advancement"));
                    if (!advancementId.isBlank()) {
                        JsonObject object = new JsonObject();
                        object.addProperty("advancement", advancementId);
                        bundle.advancements.add(object);
                        return 1;
                    }
                }
                case "toast" -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("title", rewardTitle(reward, "Quest complete"));
                    object.addProperty("description", formatText(stringValue(reward, "description")));
                    String icon = extractFirstItemId(reward.get("icon"));
                    if (!icon.isBlank()) object.addProperty("icon", icon);
                    bundle.toasts.add(object);
                    return 1;
                }
                case "all_table", "choice", "random", "loot" -> {
                    CompoundTag table = rewardTable(reward.get("table_id"));
                    List<CompoundTag> entries = table == null
                            ? compoundList(reward, "rewards") : compoundList(table, "rewards");
                    if (entries.isEmpty()) {
                        unsupported("Reward table " + tagValue(reward.get("table_id")) + " used by " + quest.title + " was not found.");
                        return 0;
                    }
                    int added = 0;
                    if (type.equals("all_table")) {
                        for (CompoundTag entry : entries) added += convertReward(quest, entry, bundle, depth + 1);
                    } else {
                        for (CompoundTag entry : entries) {
                            int converted = convertReward(quest, entry, bundle, depth + 1);
                            if (converted > 0) {
                                added += converted;
                                break;
                            }
                        }
                        warn("Random/choice reward table in " + quest.title + " was reduced to its first supported reward.");
                    }
                    return added;
                }
                default -> {
                }
            }
            unsupported("Unsupported " + (type.isBlank() ? "unknown" : type) + " reward in " + quest.title + " was omitted.");
            return 0;
        }

        private CompoundTag rewardTable(Tag idTag) {
            String raw = tagValue(idTag).replace("L", "").trim();
            if (raw.isBlank()) return null;
            CompoundTag direct = rewardTables.get(raw.toUpperCase(Locale.ROOT));
            if (direct != null) return direct;
            try {
                long value = idTag instanceof NumericTag numeric ? numeric.getAsLong() : Long.parseLong(raw);
                direct = rewardTables.get(String.format(Locale.ROOT, "%016X", value));
                if (direct != null) return direct;
                return rewardTables.get(Long.toUnsignedString(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private String rewardTitle(CompoundTag reward, String fallback) {
            String id = normalizedFtbId(stringValue(reward, "id"));
            return localized("reward." + id + ".title", stringValue(reward, "title"), fallback);
        }

        private String taskTitle(CompoundTag task, String fallback) {
            String id = normalizedFtbId(stringValue(task, "id"));
            return localized("task." + id + ".title", stringValue(task, "title"), fallback);
        }

        private String questDescription(QuestDraft quest) {
            List<String> lines = new ArrayList<>();
            String subtitle = localized("quest." + quest.ftbId + ".quest_subtitle",
                    stringValue(quest.tag, "subtitle"));
            if (!subtitle.isBlank()) lines.add(subtitle);
            List<String> description = localizedLines("quest." + quest.ftbId + ".quest_desc");
            if (description.isEmpty()) description = textLines(quest.tag.get("description"));
            if (!description.isEmpty() && !lines.isEmpty()) lines.add("");
            lines.addAll(description.stream().map(this::formatText).toList());
            return String.join("\n", lines).trim();
        }

        private String localized(String key, String... fallbacks) {
            List<String> lines = localizedLines(key);
            if (!lines.isEmpty()) return formatText(String.join("\n", lines));
            for (String fallback : fallbacks) {
                String formatted = formatText(fallback);
                if (!formatted.isBlank()) return formatted;
            }
            return "";
        }

        private List<String> localizedLines(String key) {
            List<String> direct = language.get(key);
            if (direct != null) return direct;
            String lower = key.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, List<String>> entry : language.entrySet()) {
                String candidate = entry.getKey().toLowerCase(Locale.ROOT);
                if (candidate.equals(lower) || candidate.endsWith("." + lower)) return entry.getValue();
            }
            return List.of();
        }

        private String formatText(String raw) {
            if (raw == null || raw.isBlank()) return "";
            String text = raw.trim();
            if ((text.startsWith("{") || text.startsWith("[")) && text.contains("\"")) {
                try {
                    text = flattenJsonText(JsonParser.parseString(text));
                } catch (RuntimeException ignored) {
                }
            }
            text = text.replace("{@pagebreak}", "\n\n");
            Matcher image = IMAGE_MARKUP.matcher(text);
            text = image.replaceAll("[Image: $1]");
            text = text.replace("\\&", "\u0000");
            text = FORMAT_CODE.matcher(text).replaceAll("§$1");
            return text.replace('\u0000', '&').trim();
        }

        private void writeReport() throws IOException {
            StringBuilder report = new StringBuilder();
            report.append("Boundless FTB Quests Import Report\n")
                    .append("Source: ").append(sourceRoot).append('\n')
                    .append("Pack: ").append(packName).append('\n')
                    .append("Chapters imported: ").append(chapters.size()).append('\n')
                    .append("Quests imported: ").append(questCount).append('\n')
                    .append("Tasks imported: ").append(taskCount).append('\n')
                    .append("Rewards imported: ").append(rewardCount).append('\n')
                    .append("Unsupported/fallback entries: ").append(unsupportedEntries).append('\n')
                    .append("Warnings: ").append(warnings.size()).append("\n\n");
            if (warnings.isEmpty()) {
                report.append("No conversion warnings.\n");
            } else {
                report.append("Warnings:\n");
                int index = 1;
                for (String warning : warnings) report.append(index++).append(". ").append(warning).append('\n');
            }
            Files.writeString(stagingRoot.resolve("ftb-import-report.txt"), report.toString(), StandardCharsets.UTF_8);
        }

        private void unsupported(String warning) {
            unsupportedEntries++;
            warn(warning);
        }

        private void warn(String warning) {
            if (warning != null && !warning.isBlank()) warnings.add(warning);
        }

        private String firstTaskIcon(CompoundTag quest) {
            for (CompoundTag task : compoundList(quest, "tasks")) {
                String item = extractFirstItemId(task.get("item"));
                if (!item.isBlank()) return item;
                String observed = resourceId(stringValue(task, "to_observe"));
                if (!observed.isBlank()) return observed;
            }
            return "";
        }

        private List<String> extractItemIds(Tag itemTag) {
            LinkedHashSet<String> ids = new LinkedHashSet<>();
            extractItemIds(itemTag, ids, 0);
            return List.copyOf(ids);
        }

        private void extractItemIds(Tag itemTag, Set<String> ids, int depth) {
            if (itemTag == null || depth > 6) return;
            if (itemTag instanceof StringTag stringTag) {
                String id = resourceId(stringTag.getAsString());
                if (!id.isBlank()) ids.add(id);
                return;
            }
            if (itemTag instanceof ListTag list) {
                for (Tag entry : list) extractItemIds(entry, ids, depth + 1);
                return;
            }
            if (!(itemTag instanceof CompoundTag item)) return;
            String id = resourceId(stringValue(item, "id"));
            CompoundTag components = item.getCompound("components");
            if (id.equals("ftbquests:missing_item")) {
                String rawOriginal = stringValue(components, "ftbquests:missing_item");
                String original = resourceId(rawOriginal);
                if (!original.isBlank()) {
                    ids.add(original);
                } else if (rawOriginal.trim().startsWith("{")) {
                    try {
                        extractItemIds(FtbSnbtParser.parse(rawOriginal), ids, depth + 1);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            } else if (id.equals("ftbquests:custom_icon")) {
                warn("Custom FTB texture icons were replaced with item fallbacks.");
            } else if (id.endsWith(":smart_filter")) {
                String filter = stringValue(components, "ftbfiltersystem:filter");
                Matcher matcher = FILTER_ITEM.matcher(filter);
                while (matcher.find()) {
                    String value = resourceId(matcher.group(1));
                    if (!value.isBlank()) ids.add(filter.regionMatches(true, matcher.start(), "item_tag", 0, 8)
                            && !value.startsWith("#") ? "#" + value : value);
                }
            } else if (!id.isBlank()) {
                ids.add(id);
            }
            for (String key : List.of("item", "stack", "items")) {
                if (item.contains(key)) extractItemIds(item.get(key), ids, depth + 1);
            }
        }

        private String extractFirstItemId(Tag tag) {
            List<String> ids = extractItemIds(tag);
            return ids.isEmpty() || ids.getFirst().startsWith("#") ? "" : ids.getFirst();
        }

        private boolean hasMeaningfulComponents(Tag tag) {
            if (!(tag instanceof CompoundTag stack)) return false;
            String id = resourceId(stringValue(stack, "id"));
            if (id.endsWith(":smart_filter") || id.equals("ftbquests:missing_item")
                    || id.equals("ftbquests:custom_icon")) return false;
            return !stack.getCompound("components").isEmpty();
        }

        private String questShape(QuestDraft quest) {
            String shape = stringValue(quest.tag, "shape");
            if (shape.isBlank() || shape.equalsIgnoreCase("default")) {
                shape = stringValue(quest.chapter.tag, "default_quest_shape");
            }
            if (shape.isBlank() || shape.equalsIgnoreCase("default")) {
                shape = stringValue(bookData, "default_quest_shape");
            }
            return treeShape(shape);
        }
    }

    private static final class RewardBundle {
        private final JsonArray items = new JsonArray();
        private final JsonArray commands = new JsonArray();
        private final JsonArray advancements = new JsonArray();
        private final JsonArray toasts = new JsonArray();
        private String experienceMode = "";
        private int experienceAmount;

        private void addExperience(String mode, int amount, String questTitle, Set<String> warnings) {
            if (experienceMode.isBlank() || experienceMode.equals(mode)) {
                experienceMode = mode;
                experienceAmount += amount;
                return;
            }
            JsonObject command = new JsonObject();
            command.addProperty("command", "experience add @s " + amount + " " + mode);
            command.addProperty("title", humanize(mode) + " reward");
            commands.add(command);
            warnings.add("Mixed XP reward types in " + questTitle + " were preserved with an experience command.");
        }

        private JsonObject toJson() {
            if (items.isEmpty() && commands.isEmpty() && advancements.isEmpty() && toasts.isEmpty()
                    && experienceMode.isBlank()) return null;
            JsonObject reward = new JsonObject();
            if (!items.isEmpty()) reward.add("items", items);
            if (!commands.isEmpty()) reward.add("commands", commands);
            if (!advancements.isEmpty()) reward.add("advancements", advancements);
            if (!toasts.isEmpty()) reward.add("toasts", toasts);
            if (!experienceMode.isBlank()) {
                reward.addProperty("exp", experienceMode);
                reward.addProperty("count", experienceAmount);
            }
            return reward;
        }
    }

    private static final class ChapterDraft {
        private final Path source;
        private final CompoundTag tag;
        private final String ftbId;
        private final String categoryId;
        private final String title;
        private String icon;
        private final int order;
        private final int groupOrder;
        private final int sourceIndex;
        private final List<QuestDraft> quests = new ArrayList<>();

        private ChapterDraft(Path source, CompoundTag tag, String ftbId, String categoryId,
                             String title, String icon, int order, int groupOrder, int sourceIndex) {
            this.source = source;
            this.tag = tag;
            this.ftbId = ftbId;
            this.categoryId = categoryId;
            this.title = title;
            this.icon = icon;
            this.order = order;
            this.groupOrder = groupOrder;
            this.sourceIndex = sourceIndex;
        }
    }

    private static final class QuestDraft {
        private final ChapterDraft chapter;
        private final CompoundTag tag;
        private final String ftbId;
        private final String id;
        private final String title;
        private final String icon;
        private final double x;
        private final double y;
        private final int sourceIndex;
        private final LinkedHashSet<String> incompatible = new LinkedHashSet<>();
        private int treeX;
        private int treeY;

        private QuestDraft(ChapterDraft chapter, CompoundTag tag, String ftbId, String id,
                           String title, String icon, double x, double y, int sourceIndex) {
            this.chapter = chapter;
            this.tag = tag;
            this.ftbId = ftbId;
            this.id = id;
            this.title = title;
            this.icon = icon;
            this.x = x;
            this.y = y;
            this.sourceIndex = sourceIndex;
        }
    }

    private static CompoundTag readCompound(Path file) throws IOException {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        try {
            return FtbSnbtParser.parse(source);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid quest data in " + file.getFileName() + ": " + exception.getMessage(), exception);
        }
    }

    private static Path firstExisting(Path... paths) {
        for (Path path : paths) if (Files.isRegularFile(path)) return path;
        return null;
    }

    private static List<CompoundTag> compoundList(CompoundTag parent, String key) {
        if (parent == null || !parent.contains(key, Tag.TAG_LIST)) return List.of();
        ListTag list = parent.getList(key, Tag.TAG_COMPOUND);
        List<CompoundTag> out = new ArrayList<>(list.size());
        for (Tag value : list) if (value instanceof CompoundTag compound) out.add(compound);
        return out;
    }

    private static List<String> stringList(CompoundTag parent, String key) {
        if (parent == null || !parent.contains(key, Tag.TAG_LIST)) return List.of();
        ListTag list = parent.getList(key, Tag.TAG_STRING);
        List<String> out = new ArrayList<>(list.size());
        for (Tag value : list) out.add(normalizedFtbId(value.getAsString()));
        return out;
    }

    private static List<String> textLines(Tag tag) {
        if (tag instanceof StringTag stringTag) return List.of(stringTag.getAsString());
        if (tag instanceof ListTag list) {
            List<String> lines = new ArrayList<>();
            for (Tag value : list) if (value instanceof StringTag stringTag) lines.add(stringTag.getAsString());
            return lines;
        }
        return List.of();
    }

    private static String stringValue(CompoundTag tag, String key) {
        if (tag == null || !tag.contains(key)) return "";
        Tag value = tag.get(key);
        return value == null ? "" : tagValue(value);
    }

    private static String tagValue(Tag tag) {
        if (tag == null) return "";
        if (tag instanceof StringTag stringTag) return stringTag.getAsString();
        if (tag instanceof NumericTag numericTag) return numericTag.getAsNumber().toString();
        return tag.getAsString();
    }

    private static int positiveInt(CompoundTag tag, String key, int fallback) {
        if (tag == null || !tag.contains(key)) return Math.max(1, fallback);
        long value = tag.getLong(key);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, value));
    }

    private static boolean booleanValue(CompoundTag tag, String key, boolean fallback) {
        if (tag == null || !tag.contains(key)) return fallback;
        Tag value = tag.get(key);
        if (value instanceof NumericTag numeric) return numeric.getAsByte() != 0;
        String text = tagValue(value).trim().toLowerCase(Locale.ROOT);
        if (text.equals("true") || text.equals("enabled") || text.equals("yes")) return true;
        if (text.equals("false") || text.equals("disabled") || text.equals("no")) return false;
        return fallback;
    }

    private static String normalizedFtbId(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        if (normalized.matches("[-+]?\\d+[lL]")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static String resourceId(String value) {
        if (value == null) return "";
        String candidate = value.trim().toLowerCase(Locale.ROOT);
        return RESOURCE_ID.matcher(candidate).matches() ? candidate : "";
    }

    private static String resourcePath(String value) {
        int separator = value == null ? -1 : value.indexOf(':');
        return separator < 0 ? value : value.substring(separator + 1);
    }

    private static String treeShape(String shape) {
        return switch (shape == null ? "" : shape.trim().toLowerCase(Locale.ROOT)) {
            case "circle" -> "circle";
            case "diamond" -> "diamond";
            case "rsquare", "rounded_square", "hexagon", "octagon" -> "rounded_square";
            default -> "square";
        };
    }

    private static String uniqueId(String preferred, String fallback, String ftbId, Set<String> used) {
        String base = preferred.isBlank() ? slug(fallback) : preferred;
        if (base.isBlank()) base = "quest";
        if (base.length() > 63) base = base.substring(0, 48) + "_" + slug(ftbId);
        String candidate = base;
        int suffix = 2;
        while (!used.add(candidate)) candidate = base + "_" + suffix++;
        return candidate;
    }

    private static String slug(String value) {
        if (value == null) return "";
        String stripped = value.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
        return stripped.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static String humanize(String value) {
        if (value == null || value.isBlank()) return "";
        String clean = resourcePath(value).replace('_', ' ').replace('-', ' ').trim();
        StringBuilder out = new StringBuilder(clean.length());
        boolean upper = true;
        for (int index = 0; index < clean.length(); index++) {
            char character = clean.charAt(index);
            out.append(upper ? Character.toUpperCase(character) : character);
            upper = Character.isWhitespace(character);
        }
        return out.toString();
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot <= 0 ? name : name.substring(0, dot);
    }

    private static String flattenJsonText(JsonElement element) {
        if (element == null || element.isJsonNull()) return "";
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonArray()) {
            StringBuilder out = new StringBuilder();
            for (JsonElement child : element.getAsJsonArray()) out.append(flattenJsonText(child));
            return out.toString();
        }
        JsonObject object = element.getAsJsonObject();
        StringBuilder out = new StringBuilder();
        if (object.has("text")) out.append(flattenJsonText(object.get("text")));
        else if (object.has("translate")) out.append(flattenJsonText(object.get("translate")));
        if (object.has("with")) out.append(flattenJsonText(object.get("with")));
        if (object.has("extra")) out.append(flattenJsonText(object.get("extra")));
        return out.toString();
    }
}
