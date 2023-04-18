package http_client;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.http.client.fluent.Request;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;

public class HttpClientTest {
  private String wsURL;

  public static void main(String[] argv) throws IOException {
    final String WS_URL = argv[0];

    final Recording recording = Recorder.getInstance().record(() -> {
      try {
        HttpClientTest.run(WS_URL);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    try {
      recording.readFully(true, new OutputStreamWriter(System.out));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void run(String wsURL) throws IOException {
    new HttpClientTest(wsURL).getVets();
  }

  public HttpClientTest(String wsURL) {
    this.wsURL = wsURL;
  }

  public int getVets() throws IOException {
    return Request.Get(wsURL + "/vets.html")
        .execute().returnResponse().getStatusLine().getStatusCode();
  }

}
