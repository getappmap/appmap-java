import java.io.IOException;
import java.io.OutputStreamWriter;

import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;

import com.appland.appmap.test.util.MyClass;

public class RecordPackage {
  public static void main(String[] argv) {
    final Recording recording = Recorder.getInstance().record(() -> {
      new MyClass().callNonPublic();
    });

    try {
      recording.readFully(true, new OutputStreamWriter(System.out));
    } catch(IOException e) {
      e.printStackTrace();
    }

  }
}