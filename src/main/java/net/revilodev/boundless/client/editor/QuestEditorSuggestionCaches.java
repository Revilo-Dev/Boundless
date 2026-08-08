package net.revilodev.boundless.client.editor;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobCategory;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestPack;
import net.revilodev.boundless.quest.QuestData;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// editor registry suggestion caches
public final class QuestEditorSuggestionCaches {
    private final List<String> itemIds = new ArrayList<>();
    private final List<String> itemTagIds = new ArrayList<>();
    private final List<String> entityIds = new ArrayList<>();
    private final List<String> effectIds = new ArrayList<>();
    private final List<String> advancementIds = new ArrayList<>();
    private final List<String> lootTableIds = new ArrayList<>();
    private final List<String> biomeIds = new ArrayList<>();
    private final List<String> dimensionIds = new ArrayList<>();
    private final List<String> observeIds = new ArrayList<>();

    public List<String> itemSuggestions() {
        ensureItemIdCache();
        return itemIds;
    }

    public List<String> itemTagSuggestions() {
        ensureItemTagIdCache();
        return itemTagIds;
    }

    public List<String> entitySuggestions() {
        ensureEntityIdCache();
        return entityIds;
    }

    public List<String> effectSuggestions() {
        ensureEffectIdCache();
        return effectIds;
    }

    // collect observe target suggestions
    public List<String> observeSuggestions() {
        ensureObserveIdCache();
        return observeIds;
    }

    public List<String> biomeSuggestions() {
        ensureBiomeIdCache();
        return biomeIds;
    }

    public List<String> dimensionSuggestions() {
        ensureDimensionIdCache();
        return dimensionIds;
    }

    public List<String> advancementSuggestions() {
        ensureAdvancementIdCache();
        return advancementIds;
    }

    public List<String> lootTableSuggestions(QuestPack currentPack) {
        ensureLootTableIdCache(currentPack);
        if (lootTableIds.isEmpty()) {
            populateFallbackLootTables(lootTableIds);
        }
        return lootTableIds;
    }

    public String computeIconSuggestion(String raw) {
        if (raw == null) return "";
        String input = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (input.isBlank()) return "";
        ensureItemIdCache();
        String suggestion = findSuggestion(itemIds, input);
        return suggestion == null ? "" : suggestion;
    }

    public void clearPackScopedCaches() {
        advancementIds.clear();
        lootTableIds.clear();
        biomeIds.clear();
        dimensionIds.clear();
        observeIds.clear();
    }

    public boolean isSelectableMobEntityId(String entityId) {
        ResourceLocation rl = ResourceLocation.tryParse(safe(entityId).trim());
        if (rl == null) return false;
        return BuiltInRegistries.ENTITY_TYPE.get(rl).getCategory() != MobCategory.MISC;
    }

    private void ensureItemIdCache() {
        if (!itemIds.isEmpty()) return;
        for (ResourceLocation rl : BuiltInRegistries.ITEM.keySet()) {
            if (rl != null) itemIds.add(rl.toString());
        }
        itemIds.sort(String::compareTo);
    }

    private void ensureItemTagIdCache() {
        if (!itemTagIds.isEmpty()) return;
        BuiltInRegistries.ITEM.getTagNames()
                .map(TagKey::location)
                .filter(Objects::nonNull)
                .filter(rl -> !"c".equals(rl.getNamespace()))
                .map(rl -> "#" + rl)
                .distinct()
                .sorted(String::compareTo)
                .forEach(itemTagIds::add);
    }

    private void ensureEntityIdCache() {
        if (!entityIds.isEmpty()) return;
        for (ResourceLocation rl : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (rl != null) entityIds.add(rl.toString());
        }
        entityIds.sort(String::compareTo);
    }

    private void ensureEffectIdCache() {
        if (!effectIds.isEmpty()) return;
        for (ResourceLocation rl : BuiltInRegistries.MOB_EFFECT.keySet()) {
            if (rl != null) effectIds.add(rl.toString());
        }
        effectIds.sort(String::compareTo);
    }

    private void ensureObserveIdCache() {
        if (!observeIds.isEmpty()) return;
        for (ResourceLocation rl : BuiltInRegistries.ITEM.keySet()) {
            if (rl == null || "minecraft:air".equals(rl.toString())) continue;
            if (!observeIds.contains(rl.toString())) observeIds.add(rl.toString());
        }
        for (ResourceLocation rl : BuiltInRegistries.BLOCK.keySet()) {
            if (rl == null || "minecraft:air".equals(rl.toString())) continue;
            if (!observeIds.contains(rl.toString())) observeIds.add(rl.toString());
        }
        ensureEntityIdCache();
        for (String entityId : entityIds) {
            if (isSelectableMobEntityId(entityId) && !observeIds.contains(entityId)) {
                observeIds.add(entityId);
            }
        }
        observeIds.sort(String::compareTo);
    }

    private void ensureBiomeIdCache() {
        if (!biomeIds.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.level != null) {
            minecraft.level.registryAccess()
                    .lookupOrThrow(Registries.BIOME)
                    .listElementIds()
                    .forEach(key -> biomeIds.add(key.location().toString()));
        }
        biomeIds.sort(String::compareTo);
    }

    private void ensureDimensionIdCache() {
        if (!dimensionIds.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.getConnection() != null) {
            for (var levelKey : minecraft.getConnection().levels()) {
                if (levelKey != null) dimensionIds.add(levelKey.location().toString());
            }
        } else if (minecraft != null && minecraft.level != null) {
            dimensionIds.add(minecraft.level.dimension().location().toString());
        }
        dimensionIds.sort(String::compareTo);
    }

    private void ensureAdvancementIdCache() {
        if (!advancementIds.isEmpty()) return;
        for (QuestData.Quest quest : QuestData.all()) {
            if (quest == null || quest.completion == null || quest.completion.targets == null) continue;
            for (QuestData.Target target : quest.completion.targets) {
                if (target != null && target.isAdvancement() && target.id != null && !target.id.isBlank() && !advancementIds.contains(target.id)) {
                    advancementIds.add(target.id);
                }
            }
        }
        if (Minecraft.getInstance().getConnection() != null) {
            try {
                Object advancements = Minecraft.getInstance().getConnection().getAdvancements();
                Object tree = invokeObject(advancements, "getTree", "tree");
                Object roots = invokeObject(tree, "roots", "getRoots");
                if (roots instanceof Iterable<?> iterable) {
                    for (Object node : iterable) {
                        collectAdvancementNodeIds(node);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        advancementIds.sort(String::compareTo);
    }

    private void collectAdvancementNodeIds(Object node) {
        if (node == null) return;
        Object holder = invokeObject(node, "holder", "getHolder", "advancement");
        Object id = invokeObject(holder, "id", "getId");
        if (id != null) {
            String sid = id.toString();
            if (!sid.isBlank() && !advancementIds.contains(sid)) advancementIds.add(sid);
        }
        Object children = invokeObject(node, "children", "getChildren");
        if (children instanceof Iterable<?> iterable) {
            for (Object child : iterable) {
                collectAdvancementNodeIds(child);
            }
        }
    }

    private void ensureLootTableIdCache(QuestPack currentPack) {
        if (!lootTableIds.isEmpty()) return;
        Set<String> found = new LinkedHashSet<>();

        try {
            Minecraft.getInstance().getResourceManager()
                    .listResources("loot_tables", path -> path != null && path.getPath().endsWith(".json"))
                    .keySet()
                    .forEach(rl -> addLootTableResourceLocation(rl, found));
            Minecraft.getInstance().getResourceManager()
                    .listResources("loot_table", path -> path != null && path.getPath().endsWith(".json"))
                    .keySet()
                    .forEach(rl -> addLootTableResourceLocation(rl, found));
        } catch (Exception ignored) {
        }

        collectLootTablesFromVersionJar(found);

        if (currentPack != null && currentPack.root != null) {
            collectLootTablesFromDirectory(currentPack.root, found);
        }

        Path rpRoot = QuestEditorPackFiles.resourcePacksRoot();
        if (Files.isDirectory(rpRoot)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(rpRoot)) {
                for (Path entry : stream) {
                    if (entry == null || !Files.exists(entry)) continue;
                    if (Files.isDirectory(entry)) {
                        collectLootTablesFromDirectory(entry, found);
                    } else if (entry.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) {
                        collectLootTablesFromZip(entry, found);
                    }
                }
            } catch (IOException ignored) {
            }
        }

        lootTableIds.addAll(found);
        lootTableIds.sort(String::compareTo);
    }

    private void collectLootTablesFromDirectory(Path root, Set<String> out) {
        if (root == null || out == null || !Files.isDirectory(root)) return;
        try (var walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .forEach(path -> addLootTablePath(root.relativize(path).toString().replace('\\', '/'), out));
        } catch (IOException ignored) {
        }
    }

    private void collectLootTablesFromZip(Path zipPath, Set<String> out) {
        if (zipPath == null || out == null || !Files.exists(zipPath)) return;
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            zip.stream()
                    .map(ZipEntry::getName)
                    .filter(Objects::nonNull)
                    .map(name -> name.replace('\\', '/'))
                    .forEach(name -> addLootTablePath(name, out));
        } catch (IOException ignored) {
        }
    }

    private void collectLootTablesFromVersionJar(Set<String> out) {
        if (out == null) return;
        Path versionsRoot = Minecraft.getInstance().gameDirectory.toPath().resolve("versions");
        Path jarPath = null;
        try {
            String version = SharedConstants.getCurrentVersion().getName();
            if (version != null && !version.isBlank()) {
                Path exact = versionsRoot.resolve(version).resolve(version + ".jar");
                if (Files.exists(exact)) jarPath = exact;
            }
        } catch (Exception ignored) {
        }
        if (jarPath == null && Files.isDirectory(versionsRoot)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(versionsRoot)) {
                List<Path> candidates = new ArrayList<>();
                for (Path entry : stream) {
                    if (entry == null || !Files.isDirectory(entry)) continue;
                    String name = entry.getFileName().toString();
                    if (!name.startsWith("1.21")) continue;
                    Path candidate = entry.resolve(name + ".jar");
                    if (Files.exists(candidate)) candidates.add(candidate);
                }
                candidates.sort(Comparator.comparing(Path::toString).reversed());
                if (!candidates.isEmpty()) jarPath = candidates.get(0);
            } catch (IOException ignored) {
            }
        }
        if (jarPath != null) {
            collectLootTablesFromZip(jarPath, out);
        }
    }

    private void addLootTablePath(String path, Set<String> out) {
        if (path == null || out == null) return;
        String clean = path.replace('\\', '/');
        if (!clean.endsWith(".json") || !clean.startsWith("data/")) return;

        String marker;
        if (clean.contains("/loot_tables/")) marker = "/loot_tables/";
        else if (clean.contains("/loot_table/")) marker = "/loot_table/";
        else return;

        int namespaceStart = "data/".length();
        int markerIndex = clean.indexOf(marker, namespaceStart);
        if (markerIndex <= namespaceStart) return;

        String namespace = clean.substring(namespaceStart, markerIndex);
        String idPath = clean.substring(markerIndex + marker.length(), clean.length() - ".json".length());
        if (namespace.isBlank() || idPath.isBlank()) return;
        addLootTableId(namespace, idPath, out);
    }

    private void addLootTableResourceLocation(ResourceLocation rl, Set<String> out) {
        if (rl == null || out == null) return;
        String path = rl.getPath();
        if (path == null || path.isBlank()) return;

        String marker;
        if (path.startsWith("loot_tables/")) marker = "loot_tables/";
        else if (path.startsWith("loot_table/")) marker = "loot_table/";
        else return;

        String idPath = path.substring(marker.length());
        if (idPath.endsWith(".json")) {
            idPath = idPath.substring(0, idPath.length() - ".json".length());
        }
        addLootTableId(rl.getNamespace(), idPath, out);
    }

    private void addLootTableId(String namespace, String idPath, Set<String> out) {
        if (namespace == null || namespace.isBlank() || idPath == null || idPath.isBlank() || out == null) return;
        String normalizedPath = idPath.replace('\\', '/');
        if (!(normalizedPath.startsWith("chests/") || normalizedPath.startsWith("entities/"))) return;
        if ("minecraft".equals(namespace)) out.add(normalizedPath);
        else out.add(namespace + ":" + normalizedPath);
    }

    private void populateFallbackLootTables(List<String> out) {
        if (out == null) return;
        Set<String> found = new LinkedHashSet<>(out);
        String[] chestDefaults = {
                "chests/abandoned_mineshaft",
                "chests/ancient_city",
                "chests/ancient_city_ice_box",
                "chests/bastion_bridge",
                "chests/bastion_hoglin_stable",
                "chests/bastion_other",
                "chests/bastion_treasure",
                "chests/buried_treasure",
                "chests/desert_pyramid",
                "chests/end_city_treasure",
                "chests/igloo_chest",
                "chests/jungle_temple",
                "chests/jungle_temple_dispenser",
                "chests/nether_bridge",
                "chests/pillager_outpost",
                "chests/ruined_portal",
                "chests/shipwreck_map",
                "chests/shipwreck_supply",
                "chests/shipwreck_treasure",
                "chests/simple_dungeon",
                "chests/spawn_bonus_chest",
                "chests/stronghold_corridor",
                "chests/stronghold_crossing",
                "chests/stronghold_library",
                "chests/trial_chambers/corridor",
                "chests/trial_chambers/entrance",
                "chests/trial_chambers/intersection",
                "chests/trial_chambers/reward",
                "chests/trial_chambers/reward_common",
                "chests/trial_chambers/reward_ominous",
                "chests/trial_chambers/supply",
                "chests/underwater_ruin_big",
                "chests/underwater_ruin_small",
                "chests/village/village_armorer",
                "chests/village/village_butcher",
                "chests/village/village_cartographer",
                "chests/village/village_desert_house",
                "chests/village/village_fisher",
                "chests/village/village_fletcher",
                "chests/village/village_mason",
                "chests/village/village_plains_house",
                "chests/village/village_savanna_house",
                "chests/village/village_shepherd",
                "chests/village/village_snowy_house",
                "chests/village/village_taiga_house",
                "chests/village/village_tannery",
                "chests/village/village_temple",
                "chests/village/village_toolsmith",
                "chests/village/village_weaponsmith",
                "chests/woodland_mansion"
        };
        for (String chest : chestDefaults) found.add(chest);
        for (ResourceLocation rl : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (rl != null) found.add("entities/" + rl.getPath());
        }
        out.clear();
        out.addAll(found);
        out.sort(String::compareTo);
    }

    private String findSuggestion(List<String> cache, String prefix) {
        if (cache == null || cache.isEmpty() || prefix == null || prefix.isBlank()) return "";
        String p = prefix.toLowerCase(java.util.Locale.ROOT);
        for (String id : cache) {
            String low = id.toLowerCase(java.util.Locale.ROOT);
            if (low.startsWith(p)) {
                return id;
            }
            int colon = low.indexOf(':');
            if (colon >= 0 && colon + 1 < low.length() && low.substring(colon + 1).startsWith(p)) {
                return id;
            }
        }
        return "";
    }

    private Object invokeObject(Object target, String... methods) {
        if (target == null || methods == null) return null;
        for (String method : methods) {
            if (method == null || method.isBlank()) continue;
            try {
                Method m = target.getClass().getMethod(method);
                Object value = m.invoke(target);
                if (value != null) return value;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
