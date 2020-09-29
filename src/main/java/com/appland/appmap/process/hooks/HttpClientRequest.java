package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.*;
import com.appland.appmap.util.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;

@Unique("http_client_request")
public class HttpClientRequest {

    private static final Recorder recorder = Recorder.getInstance();

    @HookClass(value = "java.net.HttpURLConnection")
    public static void connect(Event event, HttpURLConnection httpURLConnection) {
        Logger.println("okay that's where I want to call with " + httpURLConnection);
        event.setHttpClientRequest(httpURLConnection.getRequestMethod(), httpURLConnection.getURL().getHost(), httpURLConnection.getURL().getProtocol());
        recorder.add(event);
    }

    @CallbackOn(MethodEvent.METHOD_RETURN)
    @HookClass(value = "java.net.HttpURLConnection")
    public static void connect(Event event, HttpURLConnection httpURLConnection, Object ret) throws IOException {
        Logger.println("okay that's where I want to return with " + httpURLConnection);
        event.setHttpClientResponse(httpURLConnection.getResponseCode(), httpURLConnection.getContentType());
        recorder.add(event);
    }



}
