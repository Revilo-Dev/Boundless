package net.revilodev.boundless.quest;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.revilodev.boundless.BoundlessDebug;
import net.revilodev.boundless.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class QuestPackBackupTicker {
    private static final ExecutorService BACKUP_EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "boundless-questpack-backups");
        thread.setDaemon(true);
        return thread;
    });

    private static long lastBackupAt;
    private static boolean backupRunning;

    private QuestPackBackupTicker() {
    }

    // take pack snapshots off the server thread at the selected interval
    public static void onServerTick(ServerTickEvent.Post event) {
        long interval = Config.questPackBackupIntervalMillis();
        if (interval <= 0L) return;

        long now = System.currentTimeMillis();
        synchronized (QuestPackBackupTicker.class) {
            if (lastBackupAt == 0L) {
                lastBackupAt = now;
                return;
            }
            if (backupRunning || now - lastBackupAt < interval) return;
            lastBackupAt = now;
            backupRunning = true;
        }

        BACKUP_EXECUTOR.execute(() -> {
            try {
                snapshotAllPacks();
            } finally {
                synchronized (QuestPackBackupTicker.class) {
                    backupRunning = false;
                }
            }
        });
    }

    private static void snapshotAllPacks() {
        Path root = Config.questPacksRoot();
        if (!Files.isDirectory(root)) return;

        try (var packs = Files.list(root)) {
            for (Path pack : (Iterable<Path>) packs::iterator) {
                if (!Files.isDirectory(pack)) continue;
                String name = pack.getFileName() == null ? "" : pack.getFileName().toString();
                if (!QuestPackStorage.isQuestPackDirectoryName(name)) continue;
                try {
                    QuestPackStorage.snapshotQuestPack(pack, name, "scheduled");
                } catch (IOException e) {
                    BoundlessDebug.warnRateLimited("scheduled-backup:" + name, 5_000L,
                            "could not back up questpack {}", name, e);
                }
            }
        } catch (IOException e) {
            BoundlessDebug.warnRateLimited("scheduled-backup-scan", 5_000L,
                    "could not scan questpacks for backup", e);
        }
    }
}
