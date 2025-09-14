package dev.zenith.http;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class Response {
    private final int status;
    private final String reason;
    private final Map<String,String> headers;
    private final byte[] body;

    public Response(int status, String reason, Map<String, String> headers, byte[] body) {
        this.status = status;
        this.reason = reason;
        this.headers = new LinkedHashMap<>(headers);
        this.body = body;
        if(!this.headers.containsKey("Content-Length") && body != null){
            this.headers.put("Content-Length",String.valueOf(body.length));
        }
    }

    public int getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public Response withHeader(String name, String value){
        Map<String,String> h = new LinkedHashMap<>(this.headers);
        h.put(name,value);
        return new Response(status,reason,h,body);
    }

    public static Response text(int code, String reason, String body){
        Map<String,String> h = new LinkedHashMap<>();
        h.put("Content-Type","text/plain; charset=utf-8");
        return new Response(code,reason,h,body.getBytes(StandardCharsets.UTF_8));
    }

    public static Response json(int code,String reason, String json){
        Map<String,String> h = new LinkedHashMap<>();
        h.put("Content-Type","application/json; charset=utf-8");
        return new Response(code,reason,h,json.getBytes(StandardCharsets.UTF_8));
    }

}
