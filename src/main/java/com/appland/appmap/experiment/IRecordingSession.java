import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;

public interface IRecordingSession {
  public void add(Event event);
  public void add(CodeObject codeObject);
  public void start();
  public String stop();
}