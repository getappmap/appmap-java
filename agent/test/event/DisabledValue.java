package event;

import java.io.IOException;
import java.io.OutputStreamWriter;

import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;

public class DisabledValue {

  public static void main(String[] argv) {
    final Recording recording = Recorder.getInstance().record(() -> {
      new Example().doSomething(new Example());
    });

    try {
      recording.readFully(true, new OutputStreamWriter(System.out));
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
