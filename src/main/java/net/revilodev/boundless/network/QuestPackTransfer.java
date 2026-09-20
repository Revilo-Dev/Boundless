package net.revilodev.boundless.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class QuestPackTransfer {
    static final int CHUNK_BYTES = 60_000;
    static final int MAX_UPLOAD_BYTES = 64 * 1024 * 1024;
    static final int MAX_EXPANDED_BYTES = 256 * 1024 * 1024;
    static final int MAX_ZIP_ENTRIES = 20_000;
    static final int MAX_SYNC_BYTES = 128 * 1024 * 1024;
    static final int MAX_UPLOAD_PARTS = (MAX_UPLOAD_BYTES + CHUNK_BYTES - 1) / CHUNK_BYTES;
    static final int MAX_SYNC_PARTS = (MAX_SYNC_BYTES + CHUNK_BYTES - 1) / CHUNK_BYTES;
    static final int MAX_UPLOAD_SESSIONS = 8;
    static final long UPLOAD_SESSION_TIMEOUT_MS = 60_000L;
    static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "boundless-questpack-io");
        thread.setDaemon(true);
        return thread;
    });

    private QuestPackTransfer() {}

    // this expands an upload while keeping it inside safe limits
    static void extractZip(InputStream input, Path targetRoot) throws IOException {
        if (input == null || targetRoot == null) throw new IOException("Upload data unavailable");
        byte[] buffer = new byte[16 * 1024];
        long expandedBytes = 0L;
        int entries = 0;

        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries++;
                if (entries > MAX_ZIP_ENTRIES) throw new IOException("Questpack contains too many files");
                String name = entry.getName();
                if (name == null || name.isBlank()) {
                    zip.closeEntry();
                    continue;
                }

                Path destination = targetRoot.resolve(name).normalize();
                if (!destination.startsWith(targetRoot)) throw new IOException("Invalid questpack zip entry");
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                    zip.closeEntry();
                    continue;
                }

                Path parent = destination.getParent();
                if (parent != null) Files.createDirectories(parent);
                try (OutputStream output = Files.newOutputStream(destination,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                    int read;
                    while ((read = zip.read(buffer)) >= 0) {
                        if (read == 0) continue;
                        expandedBytes += read;
                        if (expandedBytes > MAX_EXPANDED_BYTES) {
                            throw new IOException("Questpack expands beyond the safe size limit");
                        }
                        output.write(buffer, 0, read);
                    }
                }
                zip.closeEntry();
            }
        }
    }
}
