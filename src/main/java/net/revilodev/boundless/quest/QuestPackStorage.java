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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class QuestPackStorage {
    // backup folder timestamp
    private static final DateTimeFormatter BACKUP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS", Locale.ROOT);
    private static final DateTimeFormatter BACKUP_DISPLAY_TIMESTAMP =
            DateTimeFormatter.ofPattern("d/M HH:mm", Locale.ROOT);

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

    public record BackupSnapshot(Path root, String timestamp, String reason) {}

    // this lists the newest full backups for one pack
    public static List<BackupSnapshot> listSnapshots(String packName) {
        Path root = packBackupRoot(packName);
        if (!Files.isDirectory(root)) return List.of();
        List<BackupSnapshot> snapshots = new ArrayList<>();
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().startsWith("_"))
                    .sorted(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed())
                    .limit(Math.max(1, Config.questPackBackupLimit()))
                    .forEach(path -> snapshots.add(snapshotInfo(path)));
        } catch (IOException e) {
            net.revilodev.boundless.BoundlessDebug.warnRateLimited("backup-list:" + sanitizeSegment(packName), 5_000L,
                    "could not list backups for {}", packName, e);
        }
        return List.copyOf(snapshots);
    }

    // this replaces a pack with one of its own backups
    public static void restoreSnapshot(Path packRoot, String packName, Path snapshotRoot) throws IOException {
        if (packRoot == null || packName == null || packName.isBlank() || snapshotRoot == null) {
            throw new IOException("Backup path unavailable");
        }
        Path backupRoot = packBackupRoot(packName).toAbsolutePath().normalize();
        Path snapshot = snapshotRoot.toAbsolutePath().normalize();
        if (!snapshot.startsWith(backupRoot) || !snapshot.getParent().equals(backupRoot) || !Files.isDirectory(snapshot)) {
            throw new IOException("Invalid questpack backup");
        }

        Path live = packRoot.toAbsolutePath().normalize();
        Path parent = live.getParent();
        if (parent == null) throw new IOException("Questpack parent unavailable");
        Path staged = parent.resolve("." + sanitizeSegment(packName) + ".rollback").normalize();
        deleteDirectory(staged);
        copyDirectory(snapshot, staged);
        replaceDirectoryWithArchive(live, staged, packName, "before-rollback");
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

        Path archivedRoot = null;
        if (liveRoot != null && Files.exists(liveRoot)) {
            String archivedReason = sanitizeSegment(reason);
            if (archivedReason.isBlank()) archivedReason = "replaced";
            snapshotQuestPack(liveRoot, packName, archivedReason);
            Path parent = liveRoot.toAbsolutePath().normalize().getParent();
            if (parent == null) throw new IOException("Questpack parent unavailable");
            archivedRoot = parent.resolve("." + sanitizeSegment(packName) + ".replaced");
            deleteDirectory(archivedRoot);
            moveReplace(liveRoot, archivedRoot);
        }

        try {
            moveReplace(stagedRoot, liveRoot);
        } catch (IOException failure) {
            if (archivedRoot != null && Files.exists(archivedRoot) && !Files.exists(liveRoot)) {
                try {
                    moveReplace(archivedRoot, liveRoot);
                } catch (IOException restoreFailure) {
                    failure.addSuppressed(restoreFailure);
                }
            }
            throw failure;
        }
        if (archivedRoot != null) deleteDirectory(archivedRoot);
        snapshotQuestPack(liveRoot, packName, "saved");
    }

    // this removes stale upload folders without loading them as packs
    public static void cleanupStagedQuestPacks(Path packsRoot) {
        if (packsRoot == null || !Files.isDirectory(packsRoot)) return;
        Path root = packsRoot.toAbsolutePath().normalize();
        try (var entries = Files.list(root)) {
            for (Path staged : (Iterable<Path>) entries::iterator) {
                if (!Files.isDirectory(staged)) continue;
                String name = staged.getFileName() == null ? "" : staged.getFileName().toString();
                if (!name.startsWith(".") || !name.endsWith(".upload")) continue;
                try {
                    deleteDirectory(staged);
                } catch (IOException e) {
                    net.revilodev.boundless.BoundlessDebug.warnRateLimited("stale-upload:" + name, 5_000L,
                            "could not remove stale upload {}", staged, e);
                }
            }
        } catch (IOException e) {
            net.revilodev.boundless.BoundlessDebug.warnRateLimited("stale-upload-scan", 5_000L,
                    "could not scan stale uploads in {}", root, e);
        }
    }

    public static boolean isQuestPackDirectoryName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return !name.isBlank() && !name.startsWith(".") && !name.contains("/") && !name.contains("\\")
                && !lower.endsWith(".upload") && !lower.endsWith(".tmp") && !lower.endsWith(".temp");
    }

    private static Path packBackupRoot(String packName) {
        return Config.questPackBackupsRoot().resolve(sanitizeSegment(packName));
    }

    private static String timestamp() {
        return BACKUP_TIMESTAMP.format(LocalDateTime.now());
    }

    private static BackupSnapshot snapshotInfo(Path path) {
        String name = path.getFileName().toString();
        String rawTimestamp = name.length() >= 19 ? name.substring(0, 19) : name;
        String reason = name.length() > 20 ? name.substring(20).replace('-', ' ') : "backup";
        String displayTimestamp = rawTimestamp;
        try {
            displayTimestamp = BACKUP_DISPLAY_TIMESTAMP.format(LocalDateTime.parse(rawTimestamp, BACKUP_TIMESTAMP));
        } catch (DateTimeParseException ignored) {
        }
        return new BackupSnapshot(path, displayTimestamp, reason.isBlank() ? "backup" : reason);
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
