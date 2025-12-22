package test.pkg;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.record.Recording;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;

public class ReadFullyTest {
    public static void main(String[] args) {
        try {
            runTest();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void runTest() throws IOException {
        // Initialize AppMapConfig
        AppMapConfig.initialize(FileSystems.getDefault());

        // 1. Create a dummy AppMap file with known UTF-8 content
        String content = "Check: \u26A0\uFE0F \u041F\u0440\u0438\u0432\u0435\u0442";
        File tempFile = File.createTempFile("readfully", ".appmap.json");
        tempFile.deleteOnExit();

        try (Writer fw = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
            fw.write(content);
        }

        // 2. Create a Recording object pointing to it
        Recording recording = new Recording("test", tempFile);

        // 3. Call readFully and write to stdout using UTF-8
        // This validates that readFully correctly reads the UTF-8 file bytes into characters
        // regardless of the system's default encoding (which we will set to something else in BATS).
        Writer stdoutWriter = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
        recording.readFully(false, stdoutWriter);
        stdoutWriter.flush();
    }
}
