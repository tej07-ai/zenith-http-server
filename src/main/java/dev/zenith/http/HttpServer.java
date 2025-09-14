package dev.zenith.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    private final int port;
    private final ExecutorService pool;

    public HttpServer(int port){
        this.port = port;
        int threads= Math.max(4,Runtime.getRuntime().availableProcessors() * 2);
        this.pool = Executors.newFixedThreadPool(threads);
    }

    public void start() throws IOException {
        try(ServerSocket server = new ServerSocket()){
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(port));
            Router router = buildRouter();
            while(true){
                Socket s = server.accept();
                s.setSoTimeout((int) Duration.ofSeconds(10).toMillis());
                pool.submit(new HttpConnection(s,router));
            }
        }
    }

    private Router buildRouter() {
        Router r = new Router();
        r.get("/", req -> Response.text(200,"OK","Hello from the zenith server\n"));
        r.get("/health", req -> Response.json(200,"OK","{\"status\":\"ok\"}\n"));
        return r;
    }

    public static void main(String args[]) throws IOException{
        int port = 8080;
        if(args.length == 1) port = Integer.parseInt(Objects.requireNonNull(args[0]));
        new HttpServer(port).start();
    }
}
