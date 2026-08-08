package net.revilodev.boundless.client.editor;

import net.revilodev.boundless.client.editor.QuestEditorModels.EntryRowKind;
import net.revilodev.boundless.client.editor.QuestEditorModels.ParsedEntry;
import net.revilodev.boundless.quest.QuestItemSpec;

import java.util.List;
import java.util.Locale;
import java.util.Map;

// editor entry row values
public final class QuestEditorEntryRows {
    private QuestEditorEntryRows() {
    }

    // display value
    public static String displayValue(EntryRowKind kind, String type, ParsedEntry parsed, String rawLine) {
        String normalizedType = QuestEditorEntryTypes.normalize(kind, type);
        if (!usesCompactBody(kind, normalizedType)) return safe(rawLine);
        if (parsed == null) return bodyWithoutType(rawLine);
        if ("check".equals(normalizedType)) {
            String value = safe(parsed.id).trim();
            return value.isBlank() ? "Understand" : value;
        }
        return safe(parsed.id).trim();
    }

    // uses compact body
    public static boolean usesCompactBody(EntryRowKind kind, String type) {
        if (kind != EntryRowKind.COMPLETION) return false;
        return "observe".equals(type)
                || "check".equals(type)
                || "biome".equals(type)
                || "dimension".equals(type);
    }

    // compose line
    public static String composeLine(EntryRowKind kind, ScaledMultiLineEditBox box, RowMaps maps) {
        String raw = safe(box == null ? "" : box.getValue()).trim();
        if (kind == EntryRowKind.DEPENDENCY) return raw;
        String type = effectiveType(kind, box, maps);
        if (raw.isBlank()) return "";
        if (kind == EntryRowKind.COMPLETION && "levelup".equals(type)) {
            return type + ": levels " + count(box, maps);
        }
        if (QuestEditorEntryTypes.hasRowBrowser(kind, type)) {
            String itemId = safe(maps.selectedItemIdByBox.get(box)).trim();
            List<String> acceptedIds = selectedIds(box, maps);
            String components = safe(maps.selectedItemComponentsByBox.get(box)).trim();
            int count = count(box, maps);
            if (!itemId.isBlank() && !acceptedIds.isEmpty()) {
                if (acceptedIds.size() > 1) {
                    return type + ": [" + String.join(" | ", acceptedIds) + "]"
                            + (QuestEditorEntryTypes.rowHasCount(kind, type) ? " " + count : "");
                }
                return type + ": " + itemId + (components.isBlank() ? "" : components)
                        + (QuestEditorEntryTypes.rowHasCount(kind, type) ? " " + count : "");
            }
        }
        ParsedEntry parsed = parsedBody(kind, box, raw, maps);
        String body;
        if (parsed != null) {
            if ("field".equals(type)) {
                String hint = safe(parsed.hint).replace("\\", "\\\\").replace("\"", "\\\"");
                body = "\"" + safe(parsed.id).replace("\\", "\\\\").replace("\"", "\\\"") + "\" \"" + hint + "\"";
            } else if ("command".equals(type)) {
                body = parsed.id;
            } else {
                int count = QuestEditorEntryTypes.rowHasCount(kind, type) ? count(box, maps) : parsed.count;
                body = parsed.id + (QuestEditorEntryTypes.rowHasCount(kind, type) ? " " + count : "");
            }
        } else {
            body = bodyWithoutType(raw);
            if (QuestEditorEntryTypes.rowHasCount(kind, type)) {
                body = bodyWithCount(body, count(box, maps));
            }
        }
        body = safe(body).trim();
        return body.isBlank() ? "" : type + ": " + body;
    }

    // parsed body
    public static ParsedEntry parsedBody(EntryRowKind kind, ScaledMultiLineEditBox box, String raw, RowMaps maps) {
        String value = safe(raw).trim();
        if (value.isBlank()) return null;
        ParsedEntry parsed = QuestEditorEntryCodec.parseEntry(value);
        if (!containsExplicitType(kind, value)) {
            String type = effectiveType(kind, box, maps);
            if ("field".equals(type)) {
                List<String> quoted = QuestEditorEntryCodec.parseQuotedSegments(value);
                if (quoted.size() < 2) return null;
                String expected = quoted.get(0).trim();
                String hint = quoted.get(1).trim();
                if (expected.isBlank()) return null;
                return new ParsedEntry(type, expected, 1, hint);
            }
            int count = QuestEditorEntryTypes.rowHasCount(kind, type) ? count(box, maps) : 1;
            return new ParsedEntry(type, value, count);
        }
        return parsed;
    }

    // contains explicit type
    public static boolean containsExplicitType(EntryRowKind kind, String raw) {
        String value = safe(raw).trim();
        if (value.isBlank()) return false;
        int colon = value.indexOf(':');
        if (colon <= 0) return false;
        String prefix = value.substring(0, colon).trim().toLowerCase(Locale.ROOT);
        if (prefix.isBlank()) return false;
        if (kind == EntryRowKind.DEPENDENCY) return true;
        return QuestEditorEntryTypes.entryTypeOptions(kind).contains(prefix)
                || ("advancement".equals(prefix) && kind == EntryRowKind.COMPLETION)
                || ("entity".equals(prefix) && kind == EntryRowKind.COMPLETION)
                || ("item".equals(prefix) && kind == EntryRowKind.COMPLETION)
                || ("submit".equals(prefix) && kind == EntryRowKind.REWARD)
                || ("exp".equals(prefix) && kind == EntryRowKind.REWARD)
                || ("loottable".equals(prefix) && kind == EntryRowKind.REWARD)
                || ("input".equals(prefix) && kind == EntryRowKind.COMPLETION);
    }

    // body with count
    public static String bodyWithCount(String body, int count) {
        String value = safe(body).trim();
        if (value.isBlank()) return value;
        String[] tokens = value.split("\\s+");
        if (tokens.length > 0) {
            try {
                Integer.parseInt(tokens[tokens.length - 1]);
                StringBuilder withoutCount = new StringBuilder();
                for (int i = 0; i < tokens.length - 1; i++) {
                    if (i > 0) withoutCount.append(' ');
                    withoutCount.append(tokens[i]);
                }
                value = withoutCount.toString().trim();
            } catch (NumberFormatException ignored) {
            }
        }
        return value.isBlank() ? value : value + " " + Math.max(1, count);
    }

    // body without type
    public static String bodyWithoutType(String raw) {
        String line = safe(raw).trim();
        int colon = line.indexOf(':');
        return colon > 0 ? line.substring(colon + 1).trim() : line;
    }

    // effective type
    public static String effectiveType(EntryRowKind kind, ScaledMultiLineEditBox box, RowMaps maps) {
        String cached = maps.entryTypeByBox.get(box);
        if (cached != null && !cached.isBlank()) return cached;
        ParsedEntry parsed = QuestEditorEntryCodec.parseEntry(safe(box == null ? "" : box.getValue()));
        String fallback = switch (kind) {
            case COMPLETION -> "collect";
            case REWARD -> "item";
            case DEPENDENCY -> "";
        };
        String detected = parsed == null ? fallback : QuestEditorEntryTypes.normalize(kind, parsed.type);
        if (box != null && !detected.isBlank()) maps.entryTypeByBox.put(box, detected);
        return detected;
    }

    // count
    public static int count(ScaledMultiLineEditBox box, RowMaps maps) {
        Integer value = maps.entryCountByBox.get(box);
        return value == null || value < 1 ? 1 : value;
    }

    // display name for row
    public static String displayNameForRow(EntryRowKind kind, ScaledMultiLineEditBox box, String itemId, RowMaps maps) {
        String type = effectiveType(kind, box, maps);
        String normalizedItemId = QuestItemSpec.stripComponents(itemId);
        if ("kill".equals(type) || "entity".equals(type)) return QuestEditorItemIcons.mobDisplayNameForMobId(normalizedItemId).getString();
        if ("effect".equals(type)) return QuestEditorItemIcons.effectDisplayName(normalizedItemId);
        if ("achieve".equals(type) || "advancement".equals(type)) return normalizedItemId;
        if ("observe".equals(type) || "biome".equals(type) || "dimension".equals(type)) return normalizedItemId;
        return QuestEditorItemIcons.displayNameForItem(normalizedItemId);
    }

    // display name for selection
    public static String displayNameForSelection(EntryRowKind kind, ScaledMultiLineEditBox box, List<String> ids, RowMaps maps) {
        List<String> acceptedIds = ids == null ? List.of() : ids;
        if (acceptedIds.isEmpty()) return "";
        if (acceptedIds.size() == 1) return displayNameForRow(kind, box, acceptedIds.get(0), maps);
        String type = effectiveType(kind, box, maps);
        String first = displayNameForRow(kind, box, acceptedIds.get(0), maps);
        String label = ("kill".equals(type) || "entity".equals(type)) ? " mobs" : " items";
        return first + " +" + (acceptedIds.size() - 1) + label;
    }

    // selected ids
    public static List<String> selectedIds(ScaledMultiLineEditBox box, RowMaps maps) {
        List<String> ids = maps.selectedItemIdsByBox.get(box);
        if (ids != null && !ids.isEmpty()) return ids;
        String single = safe(maps.selectedItemIdByBox.get(box)).trim();
        return single.isBlank() ? List.of() : List.of(single);
    }

    // should expand focused
    public static boolean shouldExpandFocused(EntryRowKind kind, ScaledMultiLineEditBox box) {
        if (kind == EntryRowKind.DEPENDENCY || box == null || !box.isFocused()) return false;
        String value = safe(box.getValue());
        return value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    // row maps
    public record RowMaps(
            Map<ScaledMultiLineEditBox, String> entryTypeByBox,
            Map<ScaledMultiLineEditBox, Integer> entryCountByBox,
            Map<ScaledMultiLineEditBox, String> selectedItemIdByBox,
            Map<ScaledMultiLineEditBox, List<String>> selectedItemIdsByBox,
            Map<ScaledMultiLineEditBox, String> selectedItemComponentsByBox
    ) {}
}
