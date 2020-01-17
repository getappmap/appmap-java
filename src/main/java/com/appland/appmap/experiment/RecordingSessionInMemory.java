import com.alibaba.fastjson.JSON;
import com.appland.appmap.output.v1.AppMap;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;

public class RecordingSessionInMemory extends RecordingSessionGeneric {
  @Override
  public String stop() {
    AppMap appMap = new AppMap();
    appMap.classMap = this.codeObjects.toArray();
    appMap.events = new Event[this.events.size()];

    this.events.copyInto(appMap.events);

    return JSON.toJSONString(appMap);
  }
}