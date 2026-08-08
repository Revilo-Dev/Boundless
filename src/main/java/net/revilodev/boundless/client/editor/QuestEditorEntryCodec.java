package net.revilodev.boundless.client.editor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.revilodev.boundless.client.editor.QuestEditorModels.CommandReward;
import net.revilodev.boundless.client.editor.QuestEditorModels.ParsedEntry;
import net.revilodev.boundless.client.editor.QuestEditorModels.ToastEditorReward;
import net.revilodev.boundless.quest.QuestItemSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

// quest entry text and json conversion
public final class QuestEditorEntryCodec {
    private QuestEditorEntryCodec() {
    }

    private static JsonElement parseJsonSilent(String raw) {

        try {

            return JsonParser.parseString(raw);

        } catch (Exception ignored) {

            return null;

        }

    }

    // parse completion entries
    public static JsonObject parseCompletionEntries(String raw, boolean raiseErrors, Consumer<String> errorSink) {

        List<String> lines = extractEntryLines(raw);

        com.google.gson.JsonArray targets = new com.google.gson.JsonArray();

        for (String line : lines) {

            JsonObject target = parseCompletionEntryLine(line, raiseErrors, errorSink);

            if (target == null) return null;

            targets.add(target);

        }

        JsonObject wrapper = new JsonObject();

        wrapper.add("complete", targets);

        return wrapper;

    }

    private static JsonObject parseCompletionEntryLine(String line, boolean raiseErrors, Consumer<String> errorSink) {

        ParsedEntry parsed = parseEntry(line);

        if (parsed == null) {

            if (raiseErrors) emitError(errorSink, "Invalid completion entry: " + safe(line));

            return null;

        }

        String type = parsed.type;

        String id = parsed.id;

        int count = parsed.count;

        JsonObject obj = new JsonObject();

        switch (type) {

            case "collect", "item" -> {

                List<String> acceptedItems = normalizeAcceptedEntryIds(parsed.acceptedIds, true, true);

                if (acceptedItems.isEmpty()) return failCompletion(line, raiseErrors, errorSink);

                obj.addProperty("collect", acceptedItems.get(0));

                if (acceptedItems.size() > 1) obj.add("acceptedItems", toJsonArray(acceptedItems));

                obj.addProperty("count", count);

            }

            case "submit" -> {

                List<String> acceptedItems = normalizeAcceptedEntryIds(parsed.acceptedIds, true, true);

                if (acceptedItems.isEmpty()) return failCompletion(line, raiseErrors, errorSink);

                obj.addProperty("submit", acceptedItems.get(0));

                if (acceptedItems.size() > 1) obj.add("acceptedItems", toJsonArray(acceptedItems));

                obj.addProperty("count", count);

            }

            case "kill", "entity" -> {

                List<String> acceptedMobs = normalizeAcceptedEntryIds(parsed.acceptedIds, false, false);

                if (acceptedMobs.isEmpty()) return failCompletion(line, raiseErrors, errorSink);

                obj.addProperty("kill", acceptedMobs.get(0));

                if (acceptedMobs.size() > 1) obj.add("acceptedMobs", toJsonArray(acceptedMobs));

                obj.addProperty("count", count);

            }

            case "achieve", "advancement" -> {

                String normalizedId = normalizeNamespacedId(id, false);

                if (normalizedId.isBlank()) return failCompletion(line, raiseErrors, errorSink);

                obj.addProperty("advancement", normalizedId);

            }

            case "effect" -> {

                String normalizedId = normalizeNamespacedId(id, false);

                if (normalizedId.isBlank()) return failCompletion(line, raiseErrors, errorSink);

                obj.addProperty("effect", normalizedId);

            }

            case "observe" -> {

                String normalizedId = normalizeNamespacedId(id, false);

                if (normalizedId.isBlank()) return failCompletion(line, raiseErrors, errorSink);

                obj.addProperty("observe", normalizedId);

            }

            case "check" -> {

                String text = safe(id).trim();

                obj.addProperty("check", text.isBlank() ? "Understand" : text);

            }

            case "biome" -> {

                String normalizedId = normalizeNamespacedId(id, false);

                if (normalizedId.isBlank()) return failCompletion(line, raiseErrors, errorSink);

                obj.addProperty("biome", normalizedId);

            }

            case "dimension" -> {

                String normalizedId = normalizeNamespacedId(id, false);

                if (normalizedId.isBlank()) return failCompletion(line, raiseErrors, errorSink);

                obj.addProperty("dimension", normalizedId);

            }

            case "xp" -> {

                String mode = safe(id).trim().toLowerCase(Locale.ROOT);

                if (!mode.equals("points") && !mode.equals("levels")) return failCompletion(line, raiseErrors, errorSink);

                obj.addProperty("xp", mode);

                obj.addProperty("count", count);

            }

            case "levelup" -> {

                String mode = safe(id).trim().toLowerCase(Locale.ROOT);

                if (mode.equals("levels")) mode = "level";

                if (!mode.equals("level")) return failCompletion(line, raiseErrors, errorSink);

                obj.addProperty("levelup_level", count);

            }

            case "field", "input" -> {

                String expected = safe(id).trim();

                if (expected.isBlank()) return failCompletion(line, raiseErrors, errorSink);

                obj.addProperty("field", expected);

                if (parsed.hint != null && !parsed.hint.isBlank()) {

                    obj.addProperty("field_text", parsed.hint.trim());

                }

            }

            default -> {

                return failCompletion(line, raiseErrors, errorSink);

            }

        }

        return obj;

    }

    private static JsonObject failCompletion(String line, boolean raiseErrors, Consumer<String> errorSink) {

        if (raiseErrors) emitError(errorSink, "Invalid completion entry: " + safe(line));

        return null;

    }

    private static com.google.gson.JsonArray toJsonArray(List<String> values) {

        com.google.gson.JsonArray out = new com.google.gson.JsonArray();

        if (values == null) return out;

        for (String value : values) {

            String normalized = safe(value).trim();

            if (!normalized.isBlank()) out.add(normalized);

        }

        return out;

    }

    private static List<String> normalizeAcceptedEntryIds(List<String> values, boolean allowItemComponents, boolean allowTags) {

        List<String> out = new ArrayList<>();

        if (values == null) return out;

        for (String value : values) {

            String normalized = allowItemComponents

                    ? normalizeItemIdWithComponents(value, allowTags)

                    : normalizeNamespacedId(value, allowTags);

            if (!normalized.isBlank() && !out.contains(normalized)) out.add(normalized);

        }

        return out;

    }

    private static List<String> readAcceptedEntryIds(JsonObject obj, String listKey, String fallbackKey) {

        List<String> out = new ArrayList<>();

        if (obj == null) return out;

        if (!safe(listKey).isBlank() && obj.has(listKey) && obj.get(listKey).isJsonArray()) {

            for (JsonElement element : obj.getAsJsonArray(listKey)) {

                if (element != null && element.isJsonPrimitive()) {

                    String value = element.getAsString();

                    if (value != null && !value.isBlank() && !out.contains(value)) out.add(value);

                }

            }

        }

        if (!safe(fallbackKey).isBlank() && obj.has(fallbackKey)) {

            JsonElement element = obj.get(fallbackKey);

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

    private static String formatAcceptedEntryLine(String type, List<String> acceptedIds, int count) {

        List<String> ids = new ArrayList<>();

        if (acceptedIds != null) {

            for (String acceptedId : acceptedIds) {

                String value = safe(acceptedId).trim();

                if (!value.isBlank() && !ids.contains(value)) ids.add(value);

            }

        }

        if (ids.isEmpty()) return "";

        if (ids.size() == 1) return type + ": " + ids.get(0) + " " + count;

        return type + ": [" + String.join(" | ", ids) + "] " + count;

    }

    // parse reward entries
    public static JsonObject parseRewardEntries(String raw, boolean raiseErrors, Consumer<String> errorSink) {

        List<String> lines = extractEntryLines(raw);

        com.google.gson.JsonArray items = new com.google.gson.JsonArray();

        com.google.gson.JsonArray commands = new com.google.gson.JsonArray();

        com.google.gson.JsonArray advancements = new com.google.gson.JsonArray();

        com.google.gson.JsonArray toasts = new com.google.gson.JsonArray();

        String expType = "";

        int expAmount = 0;

        for (String line : lines) {

            ParsedEntry parsed = parseEntry(line);

            if (parsed == null) {

                if (raiseErrors) emitError(errorSink, "Invalid reward entry: " + safe(line));

                return null;

            }

            switch (parsed.type) {

                case "item", "submit" -> {

                    List<String> acceptedItems = normalizeAcceptedEntryIds(

                            parsed.acceptedIds == null || parsed.acceptedIds.isEmpty() ? List.of(parsed.id) : parsed.acceptedIds,

                            true,

                            false

                    );

                    if (acceptedItems.isEmpty()) return failReward(line, raiseErrors, errorSink);

                    JsonObject item = new JsonObject();

                    item.addProperty("item", acceptedItems.get(0));

                    if (acceptedItems.size() > 1) item.add("acceptedItems", toJsonArray(acceptedItems));

                    item.addProperty("count", parsed.count);

                    items.add(item);

                }

                case "xp", "exp" -> {

                    String v = parsed.id.toLowerCase(Locale.ROOT);

                    if (v.isBlank()) v = "points";

                    if (!v.equals("points") && !v.equals("levels") && !v.equals("levelup")) return failReward(line, raiseErrors, errorSink);

                    expType = v;

                    expAmount = parsed.count;

                }

                case "levelup" -> {

                    String v = parsed.id.toLowerCase(Locale.ROOT);

                    if (v.isBlank() || v.equals("points")) v = "xp";

                    if (v.equals("xp")) {

                        expType = "levelup";

                    } else if (v.equals("level") || v.equals("levels")) {

                        expType = "levelup_levels";

                    } else {

                        return failReward(line, raiseErrors, errorSink);

                    }

                    expAmount = parsed.count;

                }

                case "command" -> {

                    CommandReward parsedCommand = parseCommandReward(parsed.id);

                    if (parsedCommand.command.isBlank()) return failReward(line, raiseErrors, errorSink);

                    JsonObject cmd = new JsonObject();

                    String commandValue = parsedCommand.command.startsWith("/")

                            ? parsedCommand.command.substring(1).trim()

                            : parsedCommand.command;

                    if (commandValue.isBlank()) return failReward(line, raiseErrors, errorSink);

                    cmd.addProperty("command", commandValue);

                    if (!parsedCommand.icon.isBlank()) {

                        String normalizedIcon = normalizeNamespacedId(parsedCommand.icon, false);

                        if (normalizedIcon.isBlank()) return failReward(line, raiseErrors, errorSink);

                        cmd.addProperty("icon", normalizedIcon);

                    }

                    cmd.addProperty("title", safe(parsedCommand.title));

                    commands.add(cmd);

                }

                case "loot", "loottable" -> {

                    CommandReward parsedLoot = parseCommandReward(parsed.id);

                    String lootTableId = normalizeNamespacedId(parsedLoot.command, false);

                    if (lootTableId.isBlank()) return failReward(line, raiseErrors, errorSink);

                    JsonObject cmd = new JsonObject();

                    cmd.addProperty("command", "loot give @s loot " + lootTableId);

                    if (!parsedLoot.icon.isBlank()) {

                        String normalizedIcon = normalizeNamespacedId(parsedLoot.icon, false);

                        if (normalizedIcon.isBlank()) return failReward(line, raiseErrors, errorSink);

                        cmd.addProperty("icon", normalizedIcon);

                    } else {

                        cmd.addProperty("icon", "minecraft:chest");

                    }

                    cmd.addProperty("title", safe(parsedLoot.title).isBlank() ? lootTableId : safe(parsedLoot.title));

                    commands.add(cmd);

                }

                case "advancement" -> {

                    String advancementId = normalizeNamespacedId(parsed.id, false);

                    if (advancementId.isBlank()) return failReward(line, raiseErrors, errorSink);

                    JsonObject advancement = new JsonObject();

                    advancement.addProperty("advancement", advancementId);

                    advancements.add(advancement);

                }

                case "toast" -> {

                    ToastEditorReward parsedToast = parseToastReward(parsed.id);

                    if (parsedToast == null || (parsedToast.title.isBlank() && parsedToast.description.isBlank())) {

                        return failReward(line, raiseErrors, errorSink);

                    }

                    JsonObject toast = new JsonObject();

                    toast.addProperty("title", parsedToast.title);

                    toast.addProperty("description", parsedToast.description);

                    if (!parsedToast.icon.isBlank()) {

                        String normalizedIcon = normalizeNamespacedId(parsedToast.icon, false);

                        if (normalizedIcon.isBlank()) return failReward(line, raiseErrors, errorSink);

                        toast.addProperty("icon", normalizedIcon);

                    }

                    toasts.add(toast);

                }

                default -> {

                    return failReward(line, raiseErrors, errorSink);

                }

            }

        }

        JsonObject out = new JsonObject();

        if (!items.isEmpty()) out.add("items", items);

        if (!commands.isEmpty()) out.add("commands", commands);

        if (!advancements.isEmpty()) out.add("advancements", advancements);

        if (!toasts.isEmpty()) out.add("toasts", toasts);

        if (!expType.isBlank()) {

            out.addProperty("exp", expType);

            out.addProperty("count", expAmount);

        }

        return out;

    }

    private static JsonObject failReward(String line, boolean raiseErrors, Consumer<String> errorSink) {

        if (raiseErrors) emitError(errorSink, "Invalid reward entry: " + safe(line));

        return null;

    }

    // extract entry lines
    public static List<String> extractEntryLines(String raw) {

        List<String> lines = new ArrayList<>();

        if (raw == null || raw.isBlank()) return lines;

        String[] parts = raw.split("\\R");

        for (String part : parts) {

            String line = safe(part).trim();

            if (!line.isBlank()) lines.add(line);

        }

        return lines;

    }

    // completion json to entries
    public static String completionJsonToEntries(String json) {

        JsonElement el = parseJsonSilent(json);

        if (el == null || el.isJsonNull()) return "";

        List<String> out = new ArrayList<>();

        parseCompletionElementToLines(el, out);

        return String.join("\n", out);

    }

    private static void parseCompletionElementToLines(JsonElement el, List<String> out) {

        if (el == null || out == null) return;

        if (el.isJsonObject()) {

            JsonObject obj = el.getAsJsonObject();

            if (obj.has("complete") && obj.get("complete").isJsonArray()) {

                for (JsonElement e : obj.getAsJsonArray("complete")) {

                    parseCompletionElementToLines(e, out);

                }

                return;

            }

            if (obj.has("targets") && obj.get("targets").isJsonArray()) {

                for (JsonElement e : obj.getAsJsonArray("targets")) {

                    parseCompletionElementToLines(e, out);

                }

                return;

            }

            if (obj.has("kind") && obj.has("id")) {

                String kind = optString(obj, "kind", "").toLowerCase(Locale.ROOT);

                List<String> acceptedIds = switch (kind) {

                    case "item", "submit" -> readAcceptedEntryIds(obj, "acceptedItems", "id");

                    case "entity" -> readAcceptedEntryIds(obj, "acceptedMobs", "id");

                    default -> List.of(optString(obj, "id", ""));

                };

                String id = acceptedIds.isEmpty() ? "" : acceptedIds.get(0);

                int count = parseIntFlexible(obj, "count", 1);

                switch (kind) {

                    case "item" -> out.add(formatAcceptedEntryLine("collect", acceptedIds, count));

                    case "submit" -> out.add(formatAcceptedEntryLine("submit", acceptedIds, count));

                    case "entity" -> out.add(formatAcceptedEntryLine("kill", acceptedIds, count));

                    case "advancement" -> out.add("achieve: " + id);

                    case "effect" -> out.add("effect: " + id);

                    case "observe" -> out.add("observe: " + id);

                    case "check" -> out.add("check: " + (id.isBlank() ? "Understand" : id));

                    case "biome" -> out.add("biome: " + id);

                    case "dimension" -> out.add("dimension: " + id);

                    case "xp" -> out.add("xp: " + id + " " + count);

                    case "levelup_level" -> out.add("levelup: levels " + count);

                    case "field" -> {

                        String hint = optString(obj, "hint", "");

                        String escapedValue = id.replace("\\", "\\\\").replace("\"", "\\\"");

                        String escapedHint = hint.replace("\\", "\\\\").replace("\"", "\\\"");

                        out.add("field: \"" + escapedValue + "\" \"" + escapedHint + "\"");

                    }

                }

                return;

            }

            if (obj.has("collect")) {

                int count = parseIntFlexible(obj, "count", 1);

                out.add(formatAcceptedEntryLine("collect", readAcceptedEntryIds(obj, "acceptedItems", "collect"), count));

                return;

            }

            if (obj.has("item")) out.add(formatAcceptedEntryLine("collect", readAcceptedEntryIds(obj, "acceptedItems", "item"), parseIntFlexible(obj, "count", 1)));

            else if (obj.has("acceptedItems") && !obj.has("submit")) out.add(formatAcceptedEntryLine("collect", readAcceptedEntryIds(obj, "acceptedItems", ""), parseIntFlexible(obj, "count", 1)));

            else if (obj.has("submit")) out.add(formatAcceptedEntryLine("submit", readAcceptedEntryIds(obj, "acceptedItems", "submit"), parseIntFlexible(obj, "count", 1)));

            else if (obj.has("kill") || obj.has("acceptedMobs")) out.add(formatAcceptedEntryLine("kill", readAcceptedEntryIds(obj, "acceptedMobs", "kill"), parseIntFlexible(obj, "count", 1)));

            else if (obj.has("entity")) out.add(formatAcceptedEntryLine("kill", readAcceptedEntryIds(obj, "acceptedMobs", "entity"), parseIntFlexible(obj, "count", 1)));

            else if (obj.has("achieve")) out.add("achieve: " + optString(obj, "achieve", ""));

            else if (obj.has("advancement")) out.add("achieve: " + optString(obj, "advancement", ""));

            else if (obj.has("effect")) out.add("effect: " + optString(obj, "effect", ""));

            else if (obj.has("observe")) out.add("observe: " + optString(obj, "observe", ""));

            else if (obj.has("check")) {

                String text = optString(obj, "check", "");

                out.add("check: " + (text.isBlank() || "true".equalsIgnoreCase(text) ? "Understand" : text));

            }

            else if (obj.has("biome")) out.add("biome: " + optString(obj, "biome", ""));

            else if (obj.has("dimension")) out.add("dimension: " + optString(obj, "dimension", ""));

            else if (obj.has("xp")) out.add("xp: " + optString(obj, "xp", "points") + " " + parseIntFlexible(obj, "count", 1));

            else if (obj.has("levelup_level")) out.add("levelup: levels " + parseIntFlexible(obj, "levelup_level", 1));

            else if (obj.has("field")) {

                String value = optString(obj, "field", "");

                String hint = optString(obj, "field_text", optString(obj, "fieldText", ""));

                String escapedValue = value.replace("\\", "\\\\").replace("\"", "\\\"");

                String escapedHint = hint.replace("\\", "\\\\").replace("\"", "\\\"");

                out.add("field: \"" + escapedValue + "\" \"" + escapedHint + "\"");

            }

            return;

        }

        if (el.isJsonArray()) {

            for (JsonElement e : el.getAsJsonArray()) {

                parseCompletionElementToLines(e, out);

            }

        }

    }

    // reward json to entries
    public static String rewardJsonToEntries(String json) {

        JsonElement el = parseJsonSilent(json);

        if (el == null || !el.isJsonObject()) return "";

        JsonObject obj = el.getAsJsonObject();

        List<String> out = new ArrayList<>();

        if (obj.has("items") && obj.get("items").isJsonArray()) {

            for (JsonElement e : obj.getAsJsonArray("items")) {

                if (!e.isJsonObject()) continue;

                JsonObject item = e.getAsJsonObject();

                String id = optString(item, "item", "");

                int count = parseIntFlexible(item, "count", 1);

                List<String> acceptedIds = readAcceptedEntryIds(item, "acceptedItems", "item");

                if (!acceptedIds.isEmpty()) out.add(formatAcceptedEntryLine("item", acceptedIds, count));

                else if (!id.isBlank()) out.add("item: " + id + " " + count);

            }

        }

        if (obj.has("commands") && obj.get("commands").isJsonArray()) {

            for (JsonElement e : obj.getAsJsonArray("commands")) {

                if (e.isJsonPrimitive()) {

                    String cmd = e.getAsString();

                    if (!cmd.isBlank()) out.add("command: " + cmd);

                    continue;

                }

                if (!e.isJsonObject()) continue;

                JsonObject cmdObj = e.getAsJsonObject();

                String cmd = optString(cmdObj, "command", "");

                if (cmd.isBlank()) continue;

                String icon = optString(cmdObj, "icon", "");

                String title = optString(cmdObj, "title", "");

                String lootPrefix = "loot give @s loot ";

                if (cmd.startsWith(lootPrefix)) {

                    StringBuilder line = new StringBuilder("loot: ").append(cmd.substring(lootPrefix.length()).trim());

                    if (!icon.isBlank() && !"minecraft:chest".equals(icon)) line.append(" | icon: ").append(icon);

                    if (!title.isBlank() && !title.equals(cmd.substring(lootPrefix.length()).trim())) line.append(" | title: ").append(title);

                    out.add(line.toString());

                } else {

                    StringBuilder line = new StringBuilder("command: ").append(cmd);

                    if (!icon.isBlank()) line.append(" | icon: ").append(icon);

                    line.append(" | title: ").append(title);

                    out.add(line.toString());

                }

            }

        }

        if (obj.has("lootTables") && obj.get("lootTables").isJsonArray()) {

            for (JsonElement e : obj.getAsJsonArray("lootTables")) {

                if (!e.isJsonObject()) continue;

                JsonObject lootObj = e.getAsJsonObject();

                String lootTable = optString(lootObj, "lootTable", "");

                if (lootTable.isBlank()) continue;

                String icon = optString(lootObj, "icon", "");

                String title = optString(lootObj, "title", "");

                StringBuilder line = new StringBuilder("loot: ").append(lootTable);

                if (!icon.isBlank()) line.append(" | icon: ").append(icon);

                line.append(" | title: ").append(title);

                out.add(line.toString());

            }

        }

        if (obj.has("advancements") && obj.get("advancements").isJsonArray()) {

            for (JsonElement e : obj.getAsJsonArray("advancements")) {

                if (e.isJsonPrimitive()) {

                    String advancement = e.getAsString();

                    if (!advancement.isBlank()) out.add("advancement: " + advancement);

                    continue;

                }

                if (!e.isJsonObject()) continue;

                String advancement = optString(e.getAsJsonObject(), "advancement", "");

                if (!advancement.isBlank()) out.add("advancement: " + advancement);

            }

        }

        if (obj.has("toasts") && obj.get("toasts").isJsonArray()) {

            for (JsonElement e : obj.getAsJsonArray("toasts")) {

                if (!e.isJsonObject()) continue;

                JsonObject toast = e.getAsJsonObject();

                String title = optString(toast, "title", "");

                String description = optString(toast, "description", "");

                String icon = optString(toast, "icon", "");

                String escapedTitle = title.replace("\\", "\\\\").replace("\"", "\\\"");

                String escapedDescription = description.replace("\\", "\\\\").replace("\"", "\\\"");

                StringBuilder line = new StringBuilder("toast: \"").append(escapedTitle).append("\" \"").append(escapedDescription).append("\"");

                if (!icon.isBlank()) line.append(" ").append(icon);

                out.add(line.toString());

            }

        }

        String exp = optString(obj, "exp", "");

        if (!exp.isBlank()) {

            int count = parseIntFlexible(obj, "count", 0);

            if ("levelup".equalsIgnoreCase(exp)) out.add("levelup: xp " + count);

            else if ("levelup_levels".equalsIgnoreCase(exp)) out.add("levelup: levels " + count);

            else out.add("xp: " + exp + " " + count);

        }

        return String.join("\n", out);

    }

    // parse int flexible
    public static int parseIntFlexible(JsonObject obj, String key, int def) {

        if (obj == null || !obj.has(key)) return def;

        JsonElement el = obj.get(key);

        if (el == null || !el.isJsonPrimitive()) return def;

        try {

            return el.getAsInt();

        } catch (Exception ignored) {

            return def;

        }

    }

    // parse entry
    public static ParsedEntry parseEntry(String line) {

        if (line == null) return null;

        int colon = line.indexOf(':');

        if (colon <= 0) return null;

        String type = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);

        String remainder = line.substring(colon + 1).trim();

        if (type.isBlank() || remainder.isBlank()) return null;

        if (type.equals("command")) {

            return new ParsedEntry(type, remainder, 1);

        }

        if (type.equals("toast")) {

            return new ParsedEntry(type, remainder, 1);

        }

        if (type.equals("field") || type.equals("input")) {

            List<String> quoted = parseQuotedSegments(remainder);

            if (quoted.size() < 2) return null;

            String expected = quoted.get(0).trim();

            String hint = quoted.get(1).trim();

            if (expected.isBlank()) return null;

            return new ParsedEntry(type, expected, 1, hint);

        }

        if ((type.equals("collect") || type.equals("item") || type.equals("submit") || type.equals("kill") || type.equals("entity"))

                && remainder.startsWith("[") && remainder.contains("]")) {

            int close = remainder.indexOf(']');

            String listPart = remainder.substring(1, close);

            String countPart = remainder.substring(close + 1).trim();

            int count = 1;

            if (!countPart.isBlank()) {

                try {

                    count = Math.max(1, Integer.parseInt(countPart));

                } catch (NumberFormatException ignored) {

                    return null;

                }

            }

            List<String> acceptedIds = new ArrayList<>();

            for (String token : listPart.split("\\|")) {

                String value = token.trim();

                if (!value.isBlank() && !acceptedIds.contains(value)) acceptedIds.add(value);

            }

            if (acceptedIds.isEmpty()) return null;

            return new ParsedEntry(type, acceptedIds.get(0), count, "", acceptedIds);

        }

        String[] tokens = remainder.split("\\s+");

        if (tokens.length == 0) return null;

        String id = tokens[0].trim();

        int count = 1;

        if (tokens.length >= 2) {

            String last = tokens[tokens.length - 1].trim();

            try {

                count = Integer.parseInt(last);

                if (tokens.length > 2) {

                    StringBuilder idBuilder = new StringBuilder();

                    for (int i = 0; i < tokens.length - 1; i++) {

                        if (i > 0) idBuilder.append(' ');

                        idBuilder.append(tokens[i]);

                    }

                    id = idBuilder.toString();

                }

            } catch (NumberFormatException ignored) {

                if (tokens.length > 1) {

                    StringBuilder idBuilder = new StringBuilder();

                    for (int i = 0; i < tokens.length; i++) {

                        if (i > 0) idBuilder.append(' ');

                        idBuilder.append(tokens[i]);

                    }

                    id = idBuilder.toString();

                }

            }

        }

        if (count < 1) count = 1;

        return new ParsedEntry(type, id.trim(), count);

    }

    private static ToastEditorReward parseToastReward(String raw) {

        if (raw == null) return null;

        List<String> quoted = parseQuotedSegments(raw);

        if (quoted.size() < 2) return null;

        String title = quoted.get(0).trim();

        String description = quoted.get(1).trim();

        if (title.isBlank() && description.isBlank()) return null;

        int secondQuoteIndex = raw.indexOf('"');

        if (secondQuoteIndex < 0) return new ToastEditorReward(title, description, "");

        int quoteCount = 0;

        boolean escaping = false;

        int endIndex = -1;

        for (int i = 0; i < raw.length(); i++) {

            char ch = raw.charAt(i);

            if (escaping) {

                escaping = false;

                continue;

            }

            if (ch == '\\') {

                escaping = true;

                continue;

            }

            if (ch == '"') {

                quoteCount++;

                if (quoteCount == 4) {

                    endIndex = i;

                    break;

                }

            }

        }

        String icon = endIndex >= 0 && endIndex + 1 < raw.length() ? raw.substring(endIndex + 1).trim() : "";

        return new ToastEditorReward(title, description, icon);

    }

    // parse quoted segments
    public static List<String> parseQuotedSegments(String text) {

        List<String> out = new ArrayList<>();

        if (text == null || text.isBlank()) return out;

        StringBuilder current = new StringBuilder();

        boolean inQuote = false;

        boolean escaping = false;

        for (int i = 0; i < text.length(); i++) {

            char ch = text.charAt(i);

            if (escaping) {

                current.append(ch);

                escaping = false;

                continue;

            }

            if (ch == '\\') {

                escaping = true;

                continue;

            }

            if (ch == '"') {

                if (inQuote) {

                    out.add(current.toString());

                    current.setLength(0);

                }

                inQuote = !inQuote;

                continue;

            }

            if (inQuote) current.append(ch);

        }

        return out;

    }

    // parse command reward
    public static CommandReward parseCommandReward(String raw) {

        String payload = safe(raw).trim();

        if (payload.isBlank()) return new CommandReward("", "", "");

        String command = "";

        String icon = "";

        String title = "";

        String[] segments = payload.split("\\|");

        for (int i = 0; i < segments.length; i++) {

            String segment = safe(segments[i]).trim();

            if (segment.isBlank()) continue;

            String lower = segment.toLowerCase(Locale.ROOT);

            if (lower.startsWith("command:")) {

                command = segment.substring("command:".length()).trim();

            } else if (lower.startsWith("icon:")) {

                icon = segment.substring("icon:".length()).trim();

            } else if (lower.startsWith("title:")) {

                title = segment.substring("title:".length()).trim();

            } else if (i == 0 && command.isBlank()) {

                command = segment;

            }

        }

        icon = unquotePlaceholder(icon);

        title = unquotePlaceholder(title);

        return new CommandReward(command, icon, title);

    }

    private static String unquotePlaceholder(String value) {

        String trimmed = safe(value).trim();

        if (trimmed.equals("\"\"")) return "";

        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {

            return trimmed.substring(1, trimmed.length() - 1).trim();

        }

        return trimmed;

    }

    // normalize namespaced id
    public static String normalizeNamespacedId(String raw, boolean allowTags) {

        String id = safe(raw).trim().toLowerCase(Locale.ROOT);

        if (id.isBlank()) return "";

        if (allowTags && id.startsWith("#")) {

            String rest = id.substring(1).trim();

            if (rest.isBlank()) return "";

            return rest.contains(":") ? "#" + rest : "#minecraft:" + rest;

        }

        return id.contains(":") ? id : "minecraft:" + id;

    }

    // normalize item id with components
    public static String normalizeItemIdWithComponents(String raw, boolean allowTags) {

        QuestItemSpec spec = QuestItemSpec.parse(raw);

        if (spec.id.isBlank()) return "";

        if (!allowTags && spec.tag) return "";

        return spec.serialized();

    }

    private static String optString(JsonObject obj, String key, String def) {
        if (obj != null && obj.has(key) && obj.get(key).isJsonPrimitive()) return obj.get(key).getAsString();
        return def;
    }

    private static void emitError(Consumer<String> errorSink, String message) {
        if (errorSink != null) errorSink.accept(message);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
