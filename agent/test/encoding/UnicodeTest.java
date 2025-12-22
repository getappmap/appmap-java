package test.pkg;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class UnicodeTest {
    public static String echo(String input) {
        return input;
    }

    public static byte[] echoBytes(byte[] input) {
        return input.clone();
    }

    public static void main(String[] args) {
        try {
            runTest();
        } catch (IOException e) {
            e.printStackTrace();
            // exit 1
            System.exit(1);
        }
    }

    public static void runTest() throws IOException {
        byte[] allBytes = Files.readAllBytes(Paths.get("encoding_test.cp1252"));

        String allString = new String(allBytes, "Cp1252");
        String echoedString = echo(allString);

        // print out the echoed string
        System.out.println(echoedString);

        byte[] echoedBytes = echoBytes(allBytes);
        // print out the echoed bytes as hex
        for (byte b : echoedBytes) {
            System.out.printf("%02X ", b);
        }
    }
}
