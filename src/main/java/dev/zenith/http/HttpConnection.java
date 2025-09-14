package dev.zenith.http;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class HttpConnection implements Runnable{
    private static final int MAX_START_LINE = 8 * 1024;
    private static final int MAX_HEADER_LINE = 8 * 1024;
    private static final int MAX_HEADER_TOTAL = 64 * 1024;
    private static final int MAX_HEADER_COUNT = 100;
    private static final int MAX_BODY = 10 * 1024 * 1024; // 10 MB
    private static final int MAX_REQS_PER_CONN = 100;
    private static final int IDLE_TIMEOUT_MS = (int) Duration.ofSeconds(30).toMillis();

    private final Socket socket;
    private final Router router;

    HttpConnection(Socket s, Router r){
        this.socket = s;
        this.router  = r;
    }

    @Override
    public void run()  {
        String remote = socket.getRemoteSocketAddress().toString();
        try (socket;
             InputStream in = new BufferedInputStream(socket.getInputStream());
             OutputStream out = new BufferedOutputStream(socket.getOutputStream())) {

            int handled = 0;
            while(handled < MAX_REQS_PER_CONN){
                socket.setSoTimeout(IDLE_TIMEOUT_MS);
                Instant t0 = Instant.now();
                Request req;
                try {
                    req = HttpParser.parseRequest(in,MAX_START_LINE,MAX_HEADER_LINE,MAX_HEADER_TOTAL,MAX_HEADER_COUNT,MAX_BODY);
                } catch (HttpParser.HeaderTooLarge e) {
                    writeAndClose(out, Response.text(431, "Request Header Fields Too Large", "431 header too large\n"));
                    break;
                } catch (HttpParser.PayloadTooLarge e) {
                    writeAndClose(out, Response.text(413, "Payload Too Large", "413 payload too large\n"));
                    break;
                } catch (HttpParser.RequestTimeout e) {
                    writeAndClose(out, Response.text(408, "Request Timeout", "408 request timeout\n"));
                    break;
                } catch (HttpParser.BadRequest e) {
                    writeAndClose(out, Response.text(400, "Bad Request", "400 bad request\n"));
                    break;
                } catch (HttpParser.HttpVersionUnsupported e) {
                    writeAndClose(out, Response.text(505, "HTTP Version Not Supported", "505 http version not supported\n"));
                    break;
                } catch (EOFException eof) {
                    // client closed cleanly while idle
                    break;
                }

                if(req == null) break;

                boolean close = wantsClose(req.getHeaders());
                Response resp;
                try {
                    resp = dispatch(req);
                } catch (Exception e){
                    resp = Response.text(500,"Internal Server Error","500 Internal Server Error");
                }

                if(close) resp = resp.withHeader("Connection","close");
                else resp = resp.withHeader("Connection","keep-alive");

                writeResponse(out,resp);
                out.flush();

                long durMs = Duration.between(t0, Instant.now()).toMillis();
                log(remote, req, resp, durMs);

                handled++;
                if (close) break;
            }
        } catch (IOException e){
            //swallow
        }
    }

    private Response dispatch(Request req) throws Exception{
        Handler h = router.route(req.getMethod(),req.getPath());
        if(h == null) return Response.text(404,"Not Found","404 Not Found");
        return h.handle(req);
    }

    private static boolean wantsClose(Map<String,String> header){
        String c = header.getOrDefault("connection","");
        return "close".equalsIgnoreCase(c);
    }

    private static void writeResponse(OutputStream out,Response r) throws IOException{
        byte b[] = r.getBody() == null ? new byte[0] : r.getBody();
        StringBuilder sb = new StringBuilder(128);
        sb.append("HTTP/1.1").append(' ').append(r.getStatus()).append(' ').append(r.getReason()).append("\r\n");
        boolean hasCL = false;
        boolean hasCT = false;

        for(Map.Entry<String,String> entry : r.getHeaders().entrySet()){
            String name = entry.getKey();
            String value = entry.getValue();

            if(name.equalsIgnoreCase("content-length")) hasCL = true;
            if(name.equalsIgnoreCase("content-type")) hasCT = true;

            sb.append(name).append(": ").append(value).append("\r\n");
        }
        if(!hasCT) sb.append("Content-Type: text/plain; charset=utf-8\r\n");
        if(!hasCL) sb.append("Content-Length: ").append(b.length).append("\r\n");
        sb.append("\r\n");
        out.write(sb.toString().getBytes(StandardCharsets.ISO_8859_1));
        out.write(b);
    }

    private static void writeAndClose(OutputStream out, Response r) {
        try {
            writeResponse(out, r.withHeader("Connection", "close"));
            out.flush();
        } catch (IOException ignored) { }
    }

    private static void log(String remote, Request req, Response resp, long durMs) {
        System.out.printf("%s \"%s %s\" %d %dms%n",
                remote,
                req.getMethod(),
                req.getPath(),
                resp.getStatus(),
                durMs);
    }

}
