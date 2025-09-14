package dev.zenith.http;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class HttpParser {
    private HttpParser(){}

    static final class BadRequest extends IOException {
        BadRequest(String m) {
            super(m);
        }
    }

    static final class RequestTimeout extends IOException {
        RequestTimeout() {
            super("timeout");
        }
    }

    static final class HeaderTooLarge extends IOException {
        HeaderTooLarge() {
            super("header too large");
        }
    }

    static final class PayloadTooLarge extends IOException {
        PayloadTooLarge() {
            super("payload too large");
        }
    }

    static final class HttpVersionUnsupported extends IOException {
        HttpVersionUnsupported(){
            super("version");
        }
    }

    static Request parseRequest(InputStream in,
                                int maxStartLine,
                                int maxHeaderLine,
                                int maxHeaderTotal,
                                int maxHeaderCount,
                                int maxBody) throws IOException {

        String start = readLineCRLF(in, maxStartLine);
        if(start== null) throw new EOFException();
        if(start.isEmpty()) throw new BadRequest("Empty Start line");

        String p[] = start.split(" ",3);
        String method = p[0];
        String path = p[1];
        String protocolVersion = p[2];

        if(!"HTTP/1.1".equals(protocolVersion)) throw new HttpVersionUnsupported();

        // validate the headers now
        Map<String,String> headers  = new LinkedHashMap<>();
        int tot =0,cnt = 0;
        while(true){
            String line = readLineCRLF(in,maxHeaderLine);
            if(line == null) throw new BadRequest("Unexpected EOF in header");
            if(line.isEmpty()) break;
            tot += line.length();
            if(tot > maxHeaderTotal) throw new HeaderTooLarge();
            int idx = line.indexOf(':');
            if (idx <= 0) throw new BadRequest("bad header: " + line);
            String name = line.substring(0, idx).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(idx + 1).trim();
            if(++cnt > maxHeaderCount) throw new HeaderTooLarge();
            if(value.startsWith("\t")|| value.startsWith(" ")) throw new BadRequest("obs-fold not allowed");
            headers.put(name,value);
        }

        if(!headers.containsKey("host")) throw new BadRequest("missing host");

        // now weneed to parse the body that was sent in the request
        byte[] body = null;
        String clstr = headers.get("content-length");
        if(clstr != null){
            int cl;
            try {
                cl = Integer.parseInt(clstr);
            } catch (NumberFormatException e){
                throw new BadRequest("Incorrect content length");
            }
            if(cl < 0 || cl > maxBody) throw new PayloadTooLarge();
            body = readNBytes(in,cl);
        }
        return new Request(method,path,headers,body);
    }

    private static String readLineCRLF(InputStream in, int maxLen) throws IOException{
        ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
        int prev = -1;
        while(true){
            int b = in.read();
            if(b == -1){
                if(buf.size() == 0) return null;
                throw new BadRequest("EOF mid line");
            }
            if(buf.size() >= maxLen){
                throw new HeaderTooLarge();
            }
            buf.write(b);
            if(prev == '\r'  && b == '\n'){
                break;
            }
            prev = b;
        }
        byte[] arr = buf.toByteArray();
        int len = arr.length;
        if(len >= 2 && arr[len-2] == '\r' && arr[len-1] == '\n'){
            len -= 2;
        }

        return new String(arr,0,len, StandardCharsets.ISO_8859_1);
    }
    private static byte[] readNBytes(InputStream in, int n) throws IOException {
        byte b[] = in.readNBytes(n);
        if(b.length != n) throw new BadRequest("early EOF in body");
        return b;
    }
}

