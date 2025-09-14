package dev.zenith.http;

@FunctionalInterface
public interface Handler {
    Response handle(Request req) throws Exception;
}
