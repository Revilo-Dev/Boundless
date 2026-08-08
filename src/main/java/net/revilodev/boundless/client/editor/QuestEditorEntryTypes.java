package net.revilodev.boundless.client.editor;

import net.revilodev.boundless.client.editor.QuestEditorModels.EntryRowKind;
import net.revilodev.boundless.client.editor.QuestEditorModels.PickerMode;
import net.revilodev.boundless.compat.LevelUpCompat;

import java.util.List;
import java.util.Locale;

// editor entry type rules
public final class QuestEditorEntryTypes {
    private QuestEditorEntryTypes() {
    }

    // label
    public static String label(String type) {
        return switch (safe(type)) {
            case "collect" -> "COL";
            case "submit" -> "SUB";
            case "kill" -> "KIL";
            case "achieve" -> "ADV";
            case "effect" -> "EFF";
            case "observe" -> "OBS";
            case "check" -> "CHK";
            case "biome" -> "BIO";
            case "dimension" -> "DIM";
            case "xp" -> "XP";
            case "levelup" -> "LVL";
            case "field" -> "FLD";
            case "item" -> "ITM";
            case "command" -> "CMD";
            case "loot" -> "LOT";
            case "advancement" -> "ADV";
            case "toast" -> "TST";
            default -> safe(type).isBlank() ? "?" : safe(type).substring(0, Math.min(3, safe(type).length())).toUpperCase(Locale.ROOT);
        };
    }

    // placeholder
    public static String placeholder(EntryRowKind kind, String type) {
        if (kind == EntryRowKind.DEPENDENCY) return "boundless:quest_id";
        return switch (safe(type)) {
            case "collect", "submit", "item" -> "minecraft:diamond";
            case "kill" -> "minecraft:zombie";
            case "achieve", "advancement" -> "minecraft:story/mine_stone";
            case "check" -> "Understand";
            case "xp" -> "points 10";
            case "levelup" -> "levels 1";
            case "command" -> "say hello | icon: minecraft:command_block | title: Command";
            case "loot" -> "chests/simple_dungeon | icon: minecraft:chest | title: Loot";
            case "toast" -> "\"Title\" \"Description\" minecraft:diamond";
            case "effect" -> "minecraft:speed";
            case "observe" -> "minecraft:oak_log";
            case "field" -> "\"Expected\" \"Hint text\"";
            case "biome" -> "minecraft:plains";
            case "dimension" -> "minecraft:overworld";
            default -> "";
        };
    }

    // normalize
    public static String normalize(EntryRowKind kind, String rawType) {
        String type = safe(rawType).trim().toLowerCase(Locale.ROOT);
        if (kind == EntryRowKind.COMPLETION) {
            if (type.isBlank()) return "collect";
            return switch (type) {
                case "item" -> "collect";
                case "entity" -> "kill";
                case "advancement" -> "achieve";
                case "input" -> "field";
                default -> entryTypeOptions(kind).contains(type) ? type : "collect";
            };
        }
        if (kind == EntryRowKind.REWARD) {
            if (type.isBlank()) return "item";
            return switch (type) {
                case "submit" -> "item";
                case "exp" -> "xp";
                case "loottable" -> "loot";
                default -> entryTypeOptions(kind).contains(type) ? type : "item";
            };
        }
        return "dependency";
    }

    // picker mode for type
    public static PickerMode pickerModeForType(EntryRowKind kind, String type) {
        if (kind == EntryRowKind.REWARD && "item".equals(type)) return PickerMode.ITEMS;
        if (kind != EntryRowKind.COMPLETION) return PickerMode.NONE;
        return switch (type) {
            case "collect", "submit" -> PickerMode.ITEMS;
            case "kill", "entity" -> PickerMode.MOBS;
            case "effect" -> PickerMode.EFFECTS;
            default -> PickerMode.NONE;
        };
    }

    // has row browser
    public static boolean hasRowBrowser(EntryRowKind kind, String type) {
        return pickerModeForType(kind, type) != PickerMode.NONE;
    }

    // row has count
    public static boolean rowHasCount(EntryRowKind kind, String type) {
        if (kind == EntryRowKind.REWARD) return "item".equals(type) || "xp".equals(type) || "levelup".equals(type);
        if (kind != EntryRowKind.COMPLETION) return false;
        return "collect".equals(type) || "submit".equals(type) || "kill".equals(type) || "xp".equals(type) || "levelup".equals(type);
    }

    // entry type options
    public static List<String> entryTypeOptions(EntryRowKind kind) {
        if (kind == EntryRowKind.COMPLETION) {
            return LevelUpCompat.isAvailable()
                    ? List.of("collect", "submit", "kill", "achieve", "effect", "observe", "check", "biome", "dimension", "xp", "levelup", "field")
                    : List.of("collect", "submit", "kill", "achieve", "effect", "observe", "check", "biome", "dimension", "xp", "field");
        }
        if (kind == EntryRowKind.REWARD) {
            return LevelUpCompat.isAvailable()
                    ? List.of("item", "xp", "levelup", "command", "loot", "advancement", "toast")
                    : List.of("item", "xp", "command", "loot", "advancement", "toast");
        }
        return List.of();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
