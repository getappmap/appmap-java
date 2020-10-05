package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

@Unique("http_client_request")
public class HttpClientRequest {

    private static final Recorder recorder = Recorder.getInstance();

    @HookClass(value = "java.net.HttpURLConnection")
    public static void connect(Event event, HttpURLConnection httpURLConnection) {
        //TODO: ReflectiveType can be used with HttpURLConnection
        event.setHttpClientRequest(httpURLConnection.getRequestMethod(), httpURLConnection.getURL().getHost(), httpURLConnection.getURL().getProtocol());
        recorder.add(event);
    }

    @CallbackOn(MethodEvent.METHOD_RETURN)
    @HookClass(value = "java.net.HttpURLConnection")
    public static void connect(Event event, HttpURLConnection httpURLConnection, Object ret) throws IOException {
        event.setHttpClientResponse(httpURLConnection.getResponseCode(), httpURLConnection.getContentType());
        recorder.add(event);
    }

}
