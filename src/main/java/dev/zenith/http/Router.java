package dev.zenith.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Router {
    private final Map<String,Handler> get = new HashMap<>();
    private final Map<String,Handler> post = new HashMap<>();
    private final Map<String,Handler> head = new HashMap<>();

    public void get(String path, Handler h) { get.put(norm(path), Objects.requireNonNull(h)); }
    public void post(String path, Handler h) { post.put(norm(path), Objects.requireNonNull(h)); }
    public void head(String path, Handler h) { head.put(norm(path), Objects.requireNonNull(h)); }

    private static String norm(String p) {
        if (p == null || p.isEmpty()) return "/";
        return p.startsWith("/") ? p : "/" + p;
    }

    Handler route(String method,String path){
        String p = norm(path);
        switch(method){
            case "GET" : return get.get(p);
            case "POST" : return post.get(p);
            case "HEAD" : return head.get(p);
            default: return req -> Response.text(501,"Not Implemented","501 method not implemented\n");
        }
    }
}
