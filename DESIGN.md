# DESIGN.md — Simple HTTP Server

## 1. Purpose
I want to build my own HTTP/HTTPS server from scratch in Java to learn how real servers like Nginx and Jetty work.  
The server will support HTTP/1.1, serve static files, handle multiple connections, and later add HTTPS.

## 2. Goals
- Accept HTTP requests and send correct responses.
- Support GET, POST, and HEAD.
- Add simple routing (e.g., `/` → hello, `/health` → JSON).
- Serve static files from a directory.
- Keep connections alive (HTTP/1.1 default).
- Add HTTPS with TLS later.

**Non-Goals**
- I will not build full features like authentication, templates, or HTTP/2.

## 3. Architecture (high-level)
- **Listener**: Accepts TCP connections.
- **Parser**: Reads request line + headers.
- **Router**: Decides which handler to call based on path.
- **Handler**: Returns a Response (status, headers, body).
- **Writer**: Sends back the response.

## Simple flow:
- Socket → Parser → Router → Handler → Response → Socket


## 4. Milestones
1. Blocking server with a thread pool, minimal routing.
2. Add static file serving + gzip.
3. Switch to Java NIO (selectors) for scalability.
4. Add TLS/HTTPS.
5. Add logging + metrics.

## 5. Risks & Mitigations
- **Slow clients**: add read/write timeouts.
- **Large requests**: enforce max header and body size.
- **Path traversal**: sanitize file paths for static files.
