package net.revilodev.boundless.client.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.QuestPackStorage;
import net.revilodev.boundless.client.editor.QuestEditorModels.PackMeta;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestPack;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

// quest pack file operations
public final class QuestEditorPackFiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ResourceLocation GENERATED_PACK_ICON =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/pack.png");
    private static int questPackUploadId = 1;

    private QuestEditorPackFiles() {
    }

    // resolve the local quest pack roots
    public static Path resourcePacksRoot() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("resourcepacks");
    }

    // packs root
    public static Path packsRoot() {
        return modDataQuestPacksRoot();
    }

    // mod data quest packs root
    public static Path modDataQuestPacksRoot() {
        return Config.questPacksRoot();
    }

    // is invalid pack folder name
    public static boolean isInvalidPackFolderName(String name) {
        String value = safe(name).trim();
        String lower = value.toLowerCase(Locale.ROOT);
        return !hasPackNameContent(value) || !value.equals(normalizePackName(value))
                || value.startsWith(".") || lower.endsWith(".upload") || lower.endsWith(".tmp") || lower.endsWith(".temp");
    }

    // has pack name content
    public static boolean hasPackNameContent(String name) {
        return safe(name).toLowerCase(Locale.ROOT).matches(".*[a-z0-9].*");
    }

    // is invalid namespace
    public static boolean isInvalidNamespace(String namespace) {
        String value = safe(namespace).trim();
        return value.isBlank() || !value.matches("[a-z0-9_.-]+");
    }

    // namespace from pack name
    public static String namespaceFromPackName(String packName) {
        String value = normalizePackName(packName);
        if (value.isBlank()) return "pack";
        String normalized = value.replaceAll("^[_\\.-]+|[_\\.-]+$", "");
        if (normalized.isBlank()) return "pack";
        return normalized;
    }

    // normalize pack name
    public static String normalizePackName(String value) {
        String raw = safe(value).toLowerCase(Locale.ROOT);
        if (raw.isBlank()) return "";
        return raw.replaceAll("\\s+", "-").replaceAll("[^a-z0-9_.-]", "");
    }

    // normalize pack icon id
    public static String normalizePackIconId(String raw) {
        String value = safe(raw).trim();
        if (value.isBlank()) return "";
        String normalized = normalizeNamespacedId(value, false);
        return ResourceLocation.tryParse(normalized) == null ? "" : normalized;
    }

    // collect visible quest packs
    public static List<QuestPack> listPacks() {
        migrateLegacyResourcePackQuestPacks();
        QuestPackStorage.recoverStagedQuestPacks(packsRoot());
        List<QuestPack> packs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Path root = packsRoot();
        if (Files.exists(root)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                for (Path path : stream) {
                    if (!Files.isDirectory(path)) continue;
                    String name = path.getFileName().toString();
                    if (!isVisibleQuestPackDirectoryName(name)) continue;
                    String namespace = findNamespace(path);
                    boolean enabled = readPackMeta(path, name).enabled;
                    packs.add(new QuestPack(name, namespace, path, false, enabled));
                    seen.add(name);
                }
            } catch (IOException ignored) {
            }
        }

        Path rpRoot = resourcePacksRoot();
        if (Files.exists(rpRoot)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(rpRoot, "*.zip")) {
                for (Path zip : stream) {
                    String file = zip.getFileName().toString();
                    if (!file.toLowerCase(Locale.ROOT).endsWith(".zip")) continue;
                    String name = file.substring(0, file.length() - 4);
                    if (seen.contains(name)) continue;
                    String namespace = findNamespaceFromZip(zip);
                    if (namespace.isBlank()) continue;
                    packs.add(new QuestPack(name, namespace, packsRoot().resolve(name), true));
                    seen.add(name);
                }
            } catch (IOException ignored) {
            }
        }

        packs.sort(Comparator.comparing(a -> a.name.toLowerCase(Locale.ROOT)));
        return packs;
    }

    // hide temp and staging pack folders
    public static boolean isVisibleQuestPackDirectoryName(String name) {
        String value = safe(name).trim();
        if (value.isBlank()) return false;
        String lower = value.toLowerCase(Locale.ROOT);
        return !value.startsWith(".")
                && !lower.endsWith(".upload")
                && !lower.endsWith(".tmp")
                && !lower.endsWith(".temp");
    }

    // migrate legacy resourcepack packs into config storage
    public static void migrateLegacyResourcePackQuestPacks() {
        Path legacyRoot = resourcePacksRoot().resolve("boundless");
        if (!Files.isDirectory(legacyRoot)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(legacyRoot)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) continue;
                String namespace = findNamespace(path);
                if (namespace.isBlank()) continue;
                Path questsDir = path.resolve("data").resolve(namespace).resolve("quests");
                if (!Files.isDirectory(questsDir)) continue;
                Path target = packsRoot().resolve(path.getFileName().toString());
                if (Files.exists(target)) continue;
                mirrorDirectory(path, target);
            }
        } catch (Exception ignored) {
        }
    }

    // find pack by name
    public static QuestPack findPackByName(String name) {
        for (QuestPack pack : listPacks()) {
            if (pack.name.equals(name)) return pack;
        }
        return null;
    }

    // ensure pack workspace
    public static boolean ensurePackWorkspace(QuestPack pack) {
        if (pack == null) return false;
        Path root = pack.root;
        if (Files.isDirectory(root)) return true;
        return !Files.exists(root);
    }

    // find namespace
    public static String findNamespace(Path root) {
        Path data = root.resolve("data");
        if (!Files.isDirectory(data)) return "";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(data)) {
            String fallback = "";
            for (Path p : stream) {
                if (!Files.isDirectory(p)) continue;
                String namespace = p.getFileName().toString();
                if (fallback.isBlank()) fallback = namespace;
                Path questsDir = p.resolve("quests");
                if (Files.isDirectory(questsDir)) return namespace;
            }
            return fallback;
        } catch (IOException ignored) {
        }
        return "";
    }

    // find namespace from zip
    public static String findNamespaceFromZip(Path zipPath) {
        if (zipPath == null || !Files.exists(zipPath)) return "";
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            return zip.stream()
                    .map(ZipEntry::getName)
                    .filter(Objects::nonNull)
                    .map(name -> name.replace('\\', '/'))
                    .filter(name -> name.startsWith("data/"))
                    .map(name -> name.split("/"))
                    .filter(parts -> parts.length >= 3 && "data".equals(parts[0]) && "quests".equals(parts[2]))
                    .map(parts -> parts[1])
                    .filter(ns -> !ns.isBlank())
                    .findFirst()
                    .orElse("");
        } catch (IOException ignored) {
        }
        return "";
    }

    // read pack metadata and enabled state from disk
    public static PackMeta readPackMeta(Path root, String fallbackName) {
        PackMeta meta = new PackMeta();
        meta.description = "Boundless Quest Pack: " + safe(fallbackName);
        meta.enabled = true;
        if (root == null) return meta;
        Path packMeta = packMetaPath(root);
        Path legacyPackMeta = root.resolve("pack.mcmeta");
        Path source = Files.exists(packMeta) ? packMeta : legacyPackMeta;
        if (!Files.exists(source)) return meta;
        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!(parsed instanceof JsonObject obj)) return meta;
            JsonObject pack = obj.has("pack") && obj.get("pack").isJsonObject() ? obj.getAsJsonObject("pack") : null;
            if (pack != null && pack.has("description") && pack.get("description").isJsonPrimitive()) {
                meta.description = safe(pack.get("description").getAsString());
            }
            JsonObject boundless = obj.has("boundless") && obj.get("boundless").isJsonObject() ? obj.getAsJsonObject("boundless") : null;
            if (boundless != null && boundless.has("icon_path") && boundless.get("icon_path").isJsonPrimitive()) {
                meta.iconPath = safe(boundless.get("icon_path").getAsString());
            }
            if (boundless != null && boundless.has("enabled")) {
                JsonElement enabled = boundless.get("enabled");
                if (enabled != null && enabled.isJsonPrimitive()) {
                    try {
                        meta.enabled = enabled.getAsJsonPrimitive().isBoolean()
                                ? enabled.getAsBoolean()
                                : Boolean.parseBoolean(safe(enabled.getAsString()));
                    } catch (Exception ignored) {
                        meta.enabled = true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return meta;
    }

    // write the local enabled state for a pack
    public static boolean setQuestPackEnabled(QuestPack pack, boolean enabled) {
        if (pack == null || pack.root == null) return false;
        try {
            PackMeta meta = readPackMeta(pack.root, pack.name);
            writePackMeta(pack.root, pack.name, safe(meta.description), safe(meta.iconPath), enabled);
            QuestPackStorage.snapshotQuestPack(pack.root, pack.name, "enabled");
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    // send a pack enabled change to the server
    public static boolean sendQuestPackEnabledToServer(String id, boolean enabled, boolean builtin) {
        try {
            BoundlessNetwork.sendToServer(new BoundlessNetwork.SetQuestPackEnabled(id, enabled, builtin));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // upload a full pack snapshot to the server
    public static boolean sendQuestPackToServer(QuestPack pack, boolean enabled) {
        if (pack == null || pack.name == null || pack.name.isBlank() || pack.root == null || !Files.isDirectory(pack.root)) {
            return false;
        }
        try {
            byte[] zipBytes = zipDirectoryToBytes(pack.root);
            int uploadId = nextQuestPackUploadId();
            int chunkSize = 60000;
            int total = Math.max(1, (zipBytes.length + chunkSize - 1) / chunkSize);
            for (int i = 0; i < total; i++) {
                int start = i * chunkSize;
                int end = Math.min(zipBytes.length, start + chunkSize);
                byte[] part = java.util.Arrays.copyOfRange(zipBytes, start, end);
                BoundlessNetwork.sendToServer(new BoundlessNetwork.UploadQuestPackChunk(
                        pack.name,
                        enabled,
                        uploadId,
                        total,
                        i,
                        part
                ));
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // send delete quest pack to server
    public static boolean sendDeleteQuestPackToServer(String packName) {
        String normalizedName = safe(packName).trim();
        if (normalizedName.isBlank()) return false;
        try {
            BoundlessNetwork.sendToServer(new BoundlessNetwork.DeleteQuestPack(normalizedName));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // next quest pack upload id
    public static int nextQuestPackUploadId() {
        questPackUploadId++;
        if (questPackUploadId <= 0) questPackUploadId = 1;
        return questPackUploadId;
    }

    // zip directory to bytes
    public static byte[] zipDirectoryToBytes(Path sourceRoot) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            try (var walk = Files.walk(sourceRoot)) {
                for (Path src : (Iterable<Path>) walk::iterator) {
                    if (Files.isDirectory(src)) continue;
                    Path rel = sourceRoot.relativize(src);
                    String entryName = rel.toString().replace('\\', '/');
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(src, zos);
                    zos.closeEntry();
                }
            }
        }
        return out.toByteArray();
    }

    // run boundless reload in background
    public static void runBoundlessReloadInBackground() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            if (mc.getSingleplayerServer() != null) {
                mc.getSingleplayerServer().execute(() -> {
                    try {
                        var source = mc.getSingleplayerServer().createCommandSourceStack().withSuppressedOutput();
                        mc.getSingleplayerServer().getCommands().performPrefixedCommand(source, "boundless reload");
                    } catch (Throwable ignored) {
                    }
                });
                return;
            }
            if (mc.player != null && mc.player.connection != null) {
                try {
                    mc.player.connection.sendCommand("boundless reload");
                } catch (Throwable ignored) {
                    mc.player.connection.sendChat("/boundless reload");
                }
            }
        } catch (Throwable ignored) {
        }
    }

    // write pack meta
    public static void writePackMeta(Path root, String name, String description, String iconPath, boolean enabled) throws IOException {
        JsonObject pack = new JsonObject();
        JsonObject body = new JsonObject();
        body.addProperty("pack_format", 0);
        body.addProperty("description", description);
        pack.add("pack", body);
        JsonObject boundless = new JsonObject();
        if (iconPath != null && !iconPath.isBlank()) {
            boundless.addProperty("icon_path", iconPath);
        }
        boundless.addProperty("enabled", enabled);
        pack.add("boundless", boundless);

        Path meta = packMetaPath(root);
        QuestPackStorage.writeJsonAtomically(GSON, pack, meta);
    }

    // backup pack
    public static void backupPack(QuestPack pack, String reason) throws IOException {
        if (pack == null || pack.root == null || pack.name == null) return;
        QuestPackStorage.snapshotQuestPack(pack.root, pack.name, reason);
    }

    // pack meta path
    public static Path packMetaPath(Path root) {
        return root.resolve("boundless").resolve("pack.json");
    }

    // write pack icon
    public static void writePackIcon(Path root, String sourcePath) throws IOException {
        if (root == null) return;
        Path iconPath = root.resolve("pack.png");
        String rawSource = safe(sourcePath).trim();
        if (!rawSource.isBlank()) {
            Path source = Path.of(rawSource);
            if (!Files.exists(source) || Files.isDirectory(source)) {
                throw new IOException("Icon file missing");
            }
            Files.copy(source, iconPath, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        try (var in = Minecraft.getInstance().getResourceManager().open(GENERATED_PACK_ICON)) {
            Files.copy(in, iconPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // mirror pack directory safe
    public static boolean mirrorPackDirectorySafe(QuestPack pack, boolean singleplayerAuthority) {
        if (pack == null || pack.root == null) return false;
        boolean uploaded = false;
        if (!singleplayerAuthority) {
            uploaded = sendQuestPackToServer(pack, pack.enabled);
        }
        // skip local mirroring when the pack already lives in the active root
        try {
            Path targetRoot = packsRoot().resolve(pack.name);
            Path sourceReal = pack.root.toRealPath();
            Path targetReal = Files.exists(targetRoot) ? targetRoot.toRealPath() : targetRoot.toAbsolutePath().normalize();
            if (sourceReal.equals(targetReal)) {
                return uploaded;
            }
            mirrorDirectory(pack.root, targetRoot);
            return true;
        } catch (IOException ignored) {
        }
        return uploaded;
    }

    // mirror directory
    public static void mirrorDirectory(Path sourceRoot, Path targetRoot) throws IOException {
        if (sourceRoot == null || targetRoot == null) return;
        if (!Files.isDirectory(sourceRoot)) throw new IOException("Source pack folder missing");
        Path sourceReal = sourceRoot.toRealPath();
        Path targetReal = Files.exists(targetRoot) ? targetRoot.toRealPath() : targetRoot.toAbsolutePath().normalize();
        if (sourceReal.equals(targetReal)) {
            return;
        }
        if (Files.exists(targetRoot) && !Files.isDirectory(targetRoot)) {
            Files.deleteIfExists(targetRoot);
        }
        if (Files.exists(targetRoot)) {
            deleteDirectory(targetRoot);
        }
        Files.createDirectories(targetRoot);
        try (var walk = Files.walk(sourceRoot)) {
            for (Path src : (Iterable<Path>) walk::iterator) {
                Path rel = sourceRoot.relativize(src);
                if (rel.toString().isEmpty()) continue;
                Path dst = targetRoot.resolve(rel.toString());
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dst);
                } else {
                    Path parent = dst.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    // next export path
    public static Path nextExportPath(QuestPack pack) throws IOException {
        Path exportRoot = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("boundless")
                .resolve("exports");
        Files.createDirectories(exportRoot);
        String base = safe(pack == null ? "" : pack.name).isBlank() ? "questpack" : pack.name;
        Path zipPath = exportRoot.resolve(base + ".zip");
        int suffix = 1;
        while (Files.exists(zipPath)) {
            zipPath = exportRoot.resolve(base + "-" + suffix + ".zip");
            suffix++;
        }
        return zipPath;
    }

    // zip directory
    public static void zipDirectory(Path sourceRoot, Path zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            try (var walk = Files.walk(sourceRoot)) {
                for (Path src : (Iterable<Path>) walk::iterator) {
                    if (Files.isDirectory(src)) continue;
                    Path rel = sourceRoot.relativize(src);
                    String entryName = rel.toString().replace('\\', '/');
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(src, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    // delete directory
    public static void deleteDirectory(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    // delete applied pack artifacts safe
    public static boolean deleteAppliedPackArtifactsSafe(String packName) {
        String normalizedName = safe(packName).trim();
        if (normalizedName.isBlank()) return false;

        boolean changed = false;
        try {
            Path packRoot = packsRoot().resolve(normalizedName);
            if (Files.exists(packRoot)) {
                deleteDirectory(packRoot);
                changed = true;
            }
        } catch (IOException ignored) {
        }

        try {
            Path legacyZip = resourcePacksRoot().resolve(normalizedName + ".zip");
            if (Files.exists(legacyZip) && Files.deleteIfExists(legacyZip)) {
                changed = true;
            }
        } catch (IOException ignored) {
        }

        try {
            Path legacyDir = resourcePacksRoot().resolve("boundless").resolve(normalizedName);
            if (Files.exists(legacyDir)) {
                deleteDirectory(legacyDir);
                changed = true;
            }
        } catch (IOException ignored) {
        }

        return changed;
    }

    // open parent folder
    public static void openParentFolder(Path path) {
        if (path == null) return;
        Path parent = path.getParent();
        if (parent != null) {
            Util.getPlatform().openFile(parent.toFile());
        }
    }

    private static String normalizeNamespacedId(String raw, boolean allowTags) {
        String id = safe(raw).trim().toLowerCase(Locale.ROOT);
        if (id.isBlank()) return "";
        if (allowTags && id.startsWith("#")) {
            String rest = id.substring(1).trim();
            if (rest.isBlank()) return "";
            return rest.contains(":") ? "#" + rest : "#minecraft:" + rest;
        }
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
