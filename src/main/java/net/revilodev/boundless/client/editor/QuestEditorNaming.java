package net.revilodev.boundless.client.editor;

import net.revilodev.boundless.client.editor.QuestEditorModels.IndexName;
import net.revilodev.boundless.client.editor.QuestEditorModels.NamedEntry;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestPack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

// editor id naming
public final class QuestEditorNaming {
    private QuestEditorNaming() {
    }

    // normalize id input
    public static String normalizeIdInput(String value, boolean commaSeparated) {
        String raw = safe(value);
        if (raw.isEmpty()) return raw;
        if (!commaSeparated) {
            return raw.toLowerCase(Locale.ROOT).replace(' ', '-');
        }
        String[] parts = raw.split(",", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) out.append(',');
            String p = parts[i];
            String trimmedLeading = p.replaceAll("^\\s+", "");
            String normalized = trimmedLeading.toLowerCase(Locale.ROOT).replace(' ', '-');
            out.append(normalized);
        }
        return out.toString();
    }

    // normalize pack name
    public static String normalizePackName(String value) {
        String raw = safe(value).toLowerCase(Locale.ROOT);
        if (raw.isBlank()) return "";
        return raw.replaceAll("\\s+", "-").replaceAll("[^a-z0-9_.-]", "");
    }

    // next available id
    public static String nextAvailableId(String baseId, Path dir) {
        String base = safe(baseId).trim();
        if (base.isBlank() || dir == null) return baseId;
        String candidate = base + "_copy";
        int counter = 2;
        while (Files.exists(dir.resolve(candidate + ".json"))) {
            candidate = base + "_copy" + counter;
            counter++;
        }
        return candidate;
    }

    // next available pack name
    public static String nextAvailablePackName(String baseName) {
        String base = normalizePackName(baseName);
        if (base.isBlank()) return "new-pack";
        Set<String> existing = new HashSet<>();
        for (QuestPack pack : QuestEditorPackFiles.listPacks()) {
            if (pack != null && pack.name != null && !pack.name.isBlank()) {
                existing.add(pack.name);
            }
        }
        if (!existing.contains(base) && !Files.exists(QuestEditorPackFiles.packsRoot().resolve(base))) {
            return base;
        }
        String copyBase = base + "-copy";
        if (!existing.contains(copyBase) && !Files.exists(QuestEditorPackFiles.packsRoot().resolve(copyBase))) {
            return copyBase;
        }
        int index = 2;
        while (true) {
            String candidate = copyBase + "-" + index;
            if (!existing.contains(candidate) && !Files.exists(QuestEditorPackFiles.packsRoot().resolve(candidate))) {
                return candidate;
            }
            index++;
        }
    }

    // split index name
    public static IndexName splitIndexName(String raw) {
        if (raw == null) return new IndexName("", "");
        String trimmed = raw.trim();
        if (trimmed.isBlank()) return new IndexName("", "");
        int dash = trimmed.indexOf('-');
        if (dash > 0) {
            String left = trimmed.substring(0, dash).trim();
            String right = trimmed.substring(dash + 1).trim();
            if (!left.isBlank() && left.chars().allMatch(Character::isDigit)) {
                return new IndexName(left, right);
            }
        }
        return new IndexName("", trimmed);
    }

    // quest order token from path
    public static String questOrderTokenFromPath(Path path) {
        if (path == null) return "";
        return splitIndexName(QuestEditorJsonFiles.fileId(path)).index;
    }

    // next quest order token
    public static String nextQuestOrderToken(QuestPack pack) {
        if (pack == null) return "01";
        int max = 0;
        for (NamedEntry entry : QuestEditorJsonFiles.listQuestEntries(pack)) {
            String index = splitIndexName(QuestEditorJsonFiles.fileId(entry.path)).index;
            if (index.isBlank()) continue;
            try {
                max = Math.max(max, Integer.parseInt(index));
            } catch (NumberFormatException ignored) {
            }
        }
        return String.format(Locale.ROOT, "%02d", max + 1);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
