import java.io.FileWriter;

import com.alibaba.fastjson.JSONWriter;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;

public class RecordingSessionStreaming extends RecordingSessionGeneric {
  private static final Integer MAX_EVENTS = 128;
  private static final String DEFAULT_FILENAME = "appmap.json";

  private JSONWriter eventWriter;

  public RecordingSessionStreaming(String scenarioName) {
    String fileName = DEFAULT_FILENAME;
    if (!scenarioName.isEmpty()) {
      fileName = String.format("%s.appmap.json", scenarioName);
    }

    this.eventWriter = new JSONWriter(new FileWriter(fileName));
  }

  @Override
  public void add(Event event) {
    super.add(event);

    if (this.events.size() == MAX_EVENTS) {
      for (Event e : this.events) {
        this.eventWriter.writeObject(e);
      }

      this.eventWriter.flush();
      this.events.clear();
    }
  }

  @Override
  public void start() {
    this.eventWriter.startObject();

    this.eventWriter.writeKey("version");
    this.eventWriter.writeValue("1.2");

    this.eventWriter.writeKey("metadata");
    this.eventWriter.writeValue("{}");

    this.eventWriter.writeKey("events");
    this.eventWriter.startArray();
  }

  @Override
  public String stop() {
    this.eventWriter.endArray();

    this.eventWriter.writeKey("classMap");
    this.eventWriter.writeValue(this.codeObjects.toArray());

    this.eventWriter.endObject();

    return "";
  }
}