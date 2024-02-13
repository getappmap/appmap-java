package httpclient;

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.http.client.fluent.Request;

import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;

public class HttpClientTest {
  private String url;

  public static void main(String[] argv) throws IOException {

    final Recording recording = Recorder.getInstance().record(() -> {
      try {
        new HttpClientTest(argv[0], argv.length > 1 ? argv[1] : null).run();
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

  HttpClientTest(String url, String contentType) {
    this.url = url;
  }

  public void run() throws IOException {
    getURL();
  }

  public int getURL() throws IOException {
    return Request.Get(url).execute().returnResponse().getStatusLine().getStatusCode();
  }
}
