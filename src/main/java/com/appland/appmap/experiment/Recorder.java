import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.CodeObjectTree;

public class Recorder {
  private static final String DEFAULT_OUTPUT_DIRECTORY = "./";
  private static final String ERROR_SESSION_PRESENT = "an active recording session already exists";
  private static final String ERROR_NO_SESSION = "there is no active recording session";

  private IRecordingSession activeSession = null;
  private String outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
  private CodeObjectTree globalCodeObjects = new CodeObjectTree();

  private static Recorder instance = new Recorder();

  private Recorder() {
    String outputDirectory = System.getProperty("appmap.output.directory");
    if (outputDirectory != null) {
      this.outputDirectory = outputDirectory;
    }
  }

  private void setActiveSession(IRecordingSession activeSession) throws ActiveSessionException {
    if (this.hasActiveSession()) {
      throw new ActiveSessionException(ERROR_SESSION_PRESENT);
    }

    this.activeSession = activeSession;
    this.activeSession.start();
  }

  public static Recorder getInstance() {
    return Recorder.instance;
  }

  public Boolean hasActiveSession() {
    return this.activeSession != null;
  }

  public void start() throws ActiveSessionException {
    this.setActiveSession(new RecordingSessionInMemory());
  }

  public void start(String scenarioName) throws ActiveSessionException {
    this.setActiveSession(new RecordingSessionStreaming());
  }

  public String stop() throws ActiveSessionException {
    if (!this.hasActiveSession()) {
      throw new ActiveSessionException(ERROR_NO_SESSION);
    }

    String output = this.activeSession.stop();
    this.activeSession = null;

    return output;
  }

  public void add(Event event) {
    if (!this.hasActiveSession()) {
      return;
    }

    this.activeSession.add(event);

    // get the code object for this event and emit it
    // to the active session
    
  }

  public void add(CodeObject codeObject) {
    this.globalCodeObjects.add(codeObject);

    if (!this.hasActiveSession()) {
      return;
    }

    this.activeSession.add(codeObject);
  }
}