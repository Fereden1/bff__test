package bff.client;

import bff.util.JsonUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HttpClientHelper {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private HttpClientHelper() {
    }

    public static Response get(String url, String jwt) throws IOException, InterruptedException {
        return send(request("GET", url, jwt, null), false);
    }

    public static Response post(String url, String jwt, Object body) throws IOException, InterruptedException {
        return send(request("POST", url, jwt, body), false);
    }

    public static Response put(String url, String jwt, Object body) throws IOException, InterruptedException {
        return send(request("PUT", url, jwt, body), false);
    }

    public static Response postBytes(String url, String jwt, Object body)
            throws IOException, InterruptedException {
        return send(request("POST", url, jwt, body), true);
    }

    private static HttpRequest request(String method, String url, String jwt, Object body)
            throws IOException {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "*/*");
        if (jwt != null && !jwt.isBlank()) {
            b.header("Authorization", "Bearer " + jwt);
        }
        if (body != null) {
            b.header("Content-Type", "application/json");
            b.method(method, HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(body)));
        } else {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return b.build();
    }

    private static Response send(HttpRequest req, boolean asBytes) throws IOException, InterruptedException {
        if (asBytes) {
            HttpResponse<byte[]> r = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
            return new Response(r.statusCode(), null, r.body());
        }
        HttpResponse<String> r = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        return new Response(r.statusCode(), r.body(), null);
    }

    public record Response(int status, String body, byte[] bytes) {
        public boolean ok() {
            return status >= 200 && status < 300;
        }
    }
}
