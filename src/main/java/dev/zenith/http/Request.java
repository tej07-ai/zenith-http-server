package dev.zenith.http;

import java.util.Collections;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final Map<String,String> headers;
    private final byte[] body;

    public Request(String method, String path, Map<String,String> headers, byte[] body){
        this.method = method;
        this.path = path;
        this.headers = Collections.unmodifiableMap(headers);
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }
}
