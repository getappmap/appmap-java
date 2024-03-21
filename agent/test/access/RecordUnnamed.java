import java.io.IOException;
import java.io.OutputStreamWriter;

import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;

public class RecordUnnamed {
  public static void main(String[] argv) {
    final Recording recording = Recorder.INSTANCE.record(() -> {
      new HelloWorld().getGreetingWithPunctuation("!");
    });

    try {
      recording.readFully(true, new OutputStreamWriter(System.out));
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
