package net.revilodev.boundless.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestPackTransferTest {
    @TempDir
    Path tempDir;

    @Test
    void extractsFilesInsideTheTarget() throws Exception {
        byte[] zip = zip("data/example/quests/test.json", "{}");

        QuestPackTransfer.extractZip(new ByteArrayInputStream(zip), tempDir);

        Path extracted = tempDir.resolve("data/example/quests/test.json");
        assertTrue(Files.isRegularFile(extracted));
        assertEquals("{}", Files.readString(extracted));
    }

    @Test
    void rejectsPathsOutsideTheTarget() throws Exception {
        byte[] zip = zip("../outside.json", "unsafe");

        assertThrows(IOException.class,
                () -> QuestPackTransfer.extractZip(new ByteArrayInputStream(zip), tempDir));
    }

    private static byte[] zip(String name, String value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(value.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
