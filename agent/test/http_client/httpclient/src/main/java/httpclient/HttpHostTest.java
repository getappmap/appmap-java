package httpclient;

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;

public class HttpHostTest {
  private String host;
  private int port;
  private String path;

  public static void main(String[] argv) throws IOException {

    final Recording recording = Recorder.getInstance().record(() -> {
      try {
        new HttpHostTest(argv[0], Integer.parseInt(argv[1]), argv[2]).run();
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

  HttpHostTest(String host, int port, String path) {
    this.host = host;
    this.port = port;
    this.path = path;
  }

  public void run() throws IOException {
    getURL();
  }

  public int getURL() throws IOException {
    HttpClient client = HttpClients.createDefault();
    return client.execute(new HttpHost(host, port), new HttpGet(path)).getStatusLine()
        .getStatusCode();
  }
}
