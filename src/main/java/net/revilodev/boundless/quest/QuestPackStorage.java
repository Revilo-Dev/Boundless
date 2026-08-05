package net.revilodev.boundless.quest;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.revilodev.boundless.Config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class QuestPackStorage {
    // backup folder timestamp
    private static final DateTimeFormatter BACKUP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS", Locale.ROOT);

    private QuestPackStorage() {}

    public static void writeJsonAtomically(Gson gson, JsonElement element, Path target) throws IOException {
        if (gson == null) throw new IOException("Gson unavailable");
        if (element == null) throw new IOException("JSON payload unavailable");
        writeBytesAtomically(gson.toJson(element).getBytes(StandardCharsets.UTF_8), target);
    }

    // write files through a temp path
    public static void writeBytesAtomically(byte[] bytes, Path target) throws IOException {
        if (target == null) throw new IOException("Target path unavailable");
        if (bytes == null || bytes.length == 0) throw new IOException("Refusing to write empty file");

        Path parent = target.getParent();
        if (parent == null) throw new IOException("Target parent unavailable");
        Files.createDirectories(parent);

        Path temp = parent.resolve("." + target.getFileName() + ".saving");
        Files.write(temp, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        if (!Files.exists(temp) || Files.size(temp) <= 0L) {
            Files.deleteIfExists(temp);
            throw new IOException("Temporary save file was empty");
        }

        try {
            moveReplace(temp, target);
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    public static void snapshotQuestPack(Path packRoot, String packName, String reason) throws IOException {
        if (packRoot == null || packName == null || packName.isBlank() || !Files.isDirectory(packRoot)) return;

        Path packBackupRoot = packBackupRoot(packName);
        Files.createDirectories(packBackupRoot);

        String suffix = sanitizeSegment(reason);
        String name = timestamp() + (suffix.isBlank() ? "" : "-" + suffix);
        Path snapshotRoot = packBackupRoot.resolve(name);
        copyDirectory(packRoot, snapshotRoot);
        pruneSnapshots(packBackupRoot);
    }

    public static void archiveReplacedFile(Path packRoot, String packName, Path original, String reason) throws IOException {
        if (packRoot == null || packName == null || packName.isBlank() || original == null || !Files.exists(original)) return;

        Path normalizedPackRoot = packRoot.toAbsolutePath().normalize();
        Path normalizedOriginal = original.toAbsolutePath().normalize();
        if (!normalizedOriginal.startsWith(normalizedPackRoot)) return;

        Path packBackupRoot = packBackupRoot(packName).resolve("_replaced");
        Path relative = normalizedPackRoot.relativize(normalizedOriginal);
        Path destination = packBackupRoot
                .resolve(timestamp() + "-" + sanitizeSegment(reason))
                .resolve(relative);

        Files.createDirectories(destination.getParent());
        Files.copy(normalizedOriginal, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    // archive the old pack before swap
    public static void replaceDirectoryWithArchive(Path liveRoot, Path stagedRoot, String packName, String reason) throws IOException {
        if (stagedRoot == null || !Files.isDirectory(stagedRoot)) {
            throw new IOException("Replacement questpack is missing");
        }

        if (liveRoot != null && Files.exists(liveRoot)) {
            String archivedReason = sanitizeSegment(reason);
            if (archivedReason.isBlank()) archivedReason = "replaced";
            Path archivedRoot = packBackupRoot(packName)
                    .resolve("_replaced")
                    .resolve(timestamp() + "-" + archivedReason);
            Files.createDirectories(archivedRoot.getParent());
            moveReplace(liveRoot, archivedRoot);
        }

        moveReplace(stagedRoot, liveRoot);
        snapshotQuestPack(liveRoot, packName, "saved");
    }

    private static Path packBackupRoot(String packName) {
        return Config.questPackBackupsRoot().resolve(sanitizeSegment(packName));
    }

    private static String timestamp() {
        return BACKUP_TIMESTAMP.format(LocalDateTime.now());
    }

    private static String sanitizeSegment(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9._-]+", "_");
        normalized = normalized.replaceAll("^[_\\.-]+|[_\\.-]+$", "");
        return normalized;
    }

    private static void pruneSnapshots(Path packBackupRoot) throws IOException {
        if (packBackupRoot == null || !Files.isDirectory(packBackupRoot)) return;

        List<Path> snapshots = new ArrayList<>();
        try (var stream = Files.list(packBackupRoot)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().startsWith("_"))
                    .sorted(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed())
                    .forEach(snapshots::add);
        }

        int limit = Math.max(1, Config.questPackBackupLimit());
        for (int i = limit; i < snapshots.size(); i++) {
            deleteDirectory(snapshots.get(i));
        }
    }

    private static void copyDirectory(Path sourceRoot, Path targetRoot) throws IOException {
        if (!Files.isDirectory(sourceRoot)) throw new IOException("Source questpack folder missing");
        Files.createDirectories(targetRoot);
        try (var walk = Files.walk(sourceRoot)) {
            for (Path source : (Iterable<Path>) walk::iterator) {
                Path destination = targetRoot.resolve(sourceRoot.relativize(source).toString()).normalize();
                if (!destination.startsWith(targetRoot)) {
                    throw new IOException("Invalid backup destination");
                }
                if (Files.isDirectory(source)) {
                    Files.createDirectories(destination);
                    continue;
                }
                Path parent = destination.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void deleteDirectory(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (var walk = Files.walk(root)) {
            List<Path> paths = new ArrayList<>();
            for (Path path : (Iterable<Path>) walk::iterator) {
                paths.add(path);
            }
            paths.sort(Comparator.comparingInt(Path::getNameCount).reversed());
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void moveReplace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
