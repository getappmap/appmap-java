package test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpClientTest {

  private static final String WS_URL = "http://localhost:8080";

  public static void main(String[] argv) throws IOException {
    URL url;
    HttpURLConnection connection;

    url = new URL(WS_URL + "/vets.html");
    connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.connect();
    System.out.println(connection.getResponseCode());


    url = new URL(WS_URL + "/oups");
    connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.connect();
    System.out.println(connection.getResponseCode());

    url = new URL(WS_URL + "/owners/new");
    connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);
    String data = "firstName=John&lastName=Doe&address=Ample+Street&city=London&telephone=1234567891";
    OutputStream os = connection.getOutputStream();
    byte[] input = data.getBytes("utf-8");
    os.write(input, 0, input.length);
    System.out.println(connection.getResponseCode());
  }
}
