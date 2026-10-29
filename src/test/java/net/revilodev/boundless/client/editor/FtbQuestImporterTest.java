package net.revilodev.boundless.client.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FtbQuestImporterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void importsTreeDataWithoutChangingFtbSource() throws Exception {
        Path source = temporaryDirectory.resolve("instance/config/ftbquests/quests");
        Files.createDirectories(source.resolve("chapters"));
        Files.createDirectories(source.resolve("lang"));
        Path chapterFile = source.resolve("chapters/getting_started.snbt");
        String chapterSource = """
                {
                    id: "1000000000000001"
                    icon: { id: "minecraft:oak_sapling" }
                    order_index: 2
                    quests: [
                        {
                            id: "1100000000000001"
                            max_completable_dependents: 1
                            icon: { id: "minecraft:oak_log" }
                            tasks: [{
                                id: "1300000000000001"
                                type: "item"
                                item: { id: "minecraft:oak_log" }
                                count: 16L
                            }]
                            rewards: [{
                                id: "1200000000000001"
                                type: "item"
                                item: { id: "minecraft:apple" }
                                count: 4
                            }]
                            x: 0.0d
                            y: 0.0d
                        }
                        {
                            id: "1100000000000002"
                            dependencies: ["1100000000000001"]
                            tasks: [{
                                id: "1300000000000002"
                                type: "kill"
                                entity: "minecraft:zombie"
                                value: 3L
                            }]
                            rewards: [{
                                id: "1200000000000002"
                                type: "command"
                                command: "/say {p} finished"
                            }]
                            x: 2.0d
                            y: -1.0d
                        }
                        {
                            id: "1100000000000003"
                            dependencies: ["1100000000000001"]
                            tasks: [{
                                id: "1300000000000003"
                                type: "fluid"
                                fluid: { id: "minecraft:water", amount: 1000 }
                            }]
                            x: 2.0d
                            y: 1.0d
                        }
                    ]
                }
                """;
        Files.writeString(chapterFile, chapterSource);
        Files.writeString(source.resolve("lang/en_us.snbt"), """
                {
                    chapter.1000000000000001.title: "Getting Started"
                    quest.1100000000000001.title: "Gather Logs"
                    quest.1100000000000001.quest_desc: ["Gather &a16 logs."]
                    quest.1100000000000002.title: "Fight Back"
                    quest.1100000000000003.title: "Bring Water"
                }
                """);

        Path packs = temporaryDirectory.resolve("boundless-packs");
        FtbQuestImporter.ImportResult result = FtbQuestImporter.importPack(source, packs, "Imported Pack");

        assertEquals(1, result.chapters());
        assertEquals(3, result.quests());
        assertEquals(3, result.tasks());
        assertEquals(2, result.rewards());
        assertEquals(1, result.unsupportedEntries());
        assertEquals(chapterSource, Files.readString(chapterFile));
        assertTrue(Files.exists(result.reportPath()));
        assertTrue(Files.readString(result.reportPath()).contains("Fluid task"));

        Path packRoot = packs.resolve("imported-pack");
        JsonObject metadata = json(packRoot.resolve("boundless/pack.json"));
        assertEquals("tree", metadata.getAsJsonObject("boundless").get("layout").getAsString());

        Path categoryPath = packRoot.resolve("data/imported-pack/quests/categories/getting_started.json");
        JsonObject category = json(categoryPath);
        assertEquals("Getting Started", category.get("name").getAsString());
        assertFalse(category.get("background_darkening").getAsBoolean());

        JsonObject gather = json(findQuest(packRoot, "gather_logs"));
        assertTrue(gather.get("tree_origin").getAsBoolean());
        assertEquals(-48, gather.get("tree_x").getAsInt());
        assertEquals("minecraft:oak_log", gather.getAsJsonObject("completion")
                .getAsJsonArray("complete").get(0).getAsJsonObject().get("collect").getAsString());
        assertEquals(4, gather.getAsJsonObject("reward").getAsJsonArray("items")
                .get(0).getAsJsonObject().get("count").getAsInt());
        assertTrue(gather.get("description").getAsString().contains("§a16 logs"));

        JsonObject fight = json(findQuest(packRoot, "fight_back"));
        assertEquals("gather_logs", fight.get("dependencies").getAsString());
        assertEquals(48, fight.get("tree_x").getAsInt());
        assertEquals("minecraft:zombie", fight.getAsJsonObject("completion")
                .getAsJsonArray("complete").get(0).getAsJsonObject().get("kill").getAsString());
        assertEquals("say @s finished", fight.getAsJsonObject("reward").getAsJsonArray("commands")
                .get(0).getAsJsonObject().get("command").getAsString());
        JsonArray fightIncompatible = fight.getAsJsonArray("incompatible_dependencies");
        assertEquals("bring_water", fightIncompatible.get(0).getAsString());

        JsonObject water = json(findQuest(packRoot, "bring_water"));
        assertEquals("fight_back", water.getAsJsonArray("incompatible_dependencies").get(0).getAsString());
        assertTrue(water.getAsJsonObject("completion").getAsJsonArray("complete")
                .get(0).getAsJsonObject().get("check").getAsString().contains("1000 mB"));
    }

    @Test
    void importsSmartFiltersAndFtbRewardTables() throws Exception {
        Path instance = temporaryDirectory.resolve("second-instance");
        Path source = instance.resolve("config/ftbquests/quests");
        Files.createDirectories(source.resolve("chapters"));
        Files.createDirectories(source.resolve("reward_tables"));
        Files.writeString(source.resolve("chapters/filters.json5"), """
                {
                    // JSON5 commas and comments are accepted too.
                    id: "2000000000000001",
                    title: "Filters",
                    quests: [{
                        id: "2100000000000001",
                        title: "Copper Choices",
                        consume_items: true,
                        tasks: [{
                            type: "item",
                            item: {
                                id: "ftbfiltersystem:smart_filter",
                                components: {
                                    "ftbfiltersystem:filter": "or(item(minecraft:copper_ingot)item(minecraft:raw_copper))"
                                }
                            },
                            count: 3L,
                        }],
                        rewards: [{ type: "choice", table_id: 5764607523034234881L }],
                        x: 0.0d,
                        y: 0.0d,
                    }],
                }
                """);
        Files.writeString(source.resolve("reward_tables/5000000000000001.snbt"), """
                {
                    id: "5000000000000001"
                    rewards: [
                        { item: { id: "minecraft:iron_sword" } }
                        { type: "xp", xp: 50 }
                    ]
                }
                """);

        Path packs = temporaryDirectory.resolve("second-packs");
        FtbQuestImporter.ImportResult result = FtbQuestImporter.importPack(instance, packs, "Filter Import");
        JsonObject quest = json(findQuest(packs.resolve("filter-import"), "copper_choices", "filter-import"));
        JsonObject completion = quest.getAsJsonObject("completion").getAsJsonArray("complete")
                .get(0).getAsJsonObject();

        assertEquals("minecraft:copper_ingot", completion.get("submit").getAsString());
        assertEquals(2, completion.getAsJsonArray("acceptedItems").size());
        assertEquals(3, completion.get("count").getAsInt());
        assertEquals("minecraft:iron_sword", quest.getAsJsonObject("reward").getAsJsonArray("items")
                .get(0).getAsJsonObject().get("item").getAsString());
        assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("first supported reward")));
    }

    private static Path findQuest(Path packRoot, String questId) throws Exception {
        return findQuest(packRoot, questId, "imported-pack");
    }

    private static Path findQuest(Path packRoot, String questId, String namespace) throws Exception {
        Path quests = packRoot.resolve("data").resolve(namespace).resolve("quests");
        try (var files = Files.list(quests)) {
            return files.filter(path -> path.getFileName().toString().endsWith("-" + questId + ".json"))
                    .findFirst().orElseThrow();
        }
    }

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
