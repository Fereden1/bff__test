package bff.service.impl;

import bff.dto.AuthRequest;
import bff.dto.AuthResponse;
import bff.dto.JsonResponse;
import bff.dto.UserDto;
import bff.service.ServiceException;
import bff.service.UserService;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class UserServiceRestImpl implements UserService {

    private static final String BASE = ServiceUrls.get("user.service.url", "http://localhost:8081");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    public JsonResponse register(AuthRequest request) {
        return send("POST", URI.create(BASE + "/api/users/register"), null, toJson(request));
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        JsonResponse response = send("POST", URI.create(BASE + "/api/users/login"), null, toJson(request));
        if (response.status() < 200 || response.status() >= 300) {
            throw failure(response);
        }
        return parseAuth(response.body());
    }

    @Override
    public JsonResponse profile(String jwt) {
        return send("GET", URI.create(BASE + "/api/users/me"), jwt, null);
    }

    @Override
    public UserDto currentUser(String jwt) {
        JsonResponse response = profile(jwt);
        if (response.status() < 200 || response.status() >= 300) {
            throw failure(response);
        }
        UserDto user = parseUser(response.body());
        if (user.id() == null) {
            throw new ServiceException(502, "Неожиданный формат ответа пользователя");
        }
        return user;
    }

    @Override
    public JsonResponse exists(String login) {
        String query = URLEncoder.encode(login, StandardCharsets.UTF_8);
        return send("GET", URI.create(BASE + "/api/users/exists?login=" + query), null, null);
    }

    @Override
    public boolean validate(String jwt) {
        JsonResponse response = send("GET", URI.create(BASE + "/api/users/validate"), jwt, null);
        return response.status() >= 200 && response.status() < 300;
    }

    @Override
    public boolean isBlocked(long userId, String jwt) {
        JsonResponse response = send(
                "GET", URI.create(BASE + "/api/users/" + userId + "/blocked"), jwt, null);
        if (response.status() < 200 || response.status() >= 300
                || response.body() == null || response.body().isBlank()) {
            return true;
        }
        String body = response.body().trim();
        if ("true".equals(body)) {
            return true;
        }
        if ("false".equals(body)) {
            return false;
        }
        return JsonText.bool(body, "blocked");
    }

    private String toJson(AuthRequest request) {
        return "{\"login\":" + JsonText.quote(request.login())
                + ",\"password\":" + JsonText.quote(request.password())
                + ",\"fullName\":" + JsonText.quote(request.fullName())
                + ",\"birthDate\":" + JsonText.quote(request.birthDate())
                + ",\"avatarUrl\":" + JsonText.quote(request.avatarUrl())
                + ",\"role\":" + JsonText.quote(request.role()) + "}";
    }

    private AuthResponse parseAuth(String json) {
        try {
            String token = JsonText.string(json, "accessToken");
            if (token == null || token.isBlank()) {
                throw new ServiceException(502, "Неожиданный формат ответа авторизации");
            }
            Long expires = JsonText.longOrNull(json, "expiresInSeconds");
            String userJson = JsonText.raw(json, "user");
            UserDto user = userJson == null || "null".equals(userJson) ? null : parseUser(userJson);
            return new AuthResponse(token, expires == null ? 0 : expires, user);
        } catch (ServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceException(502, "Неожиданный формат ответа авторизации", e);
        }
    }

    private UserDto parseUser(String json) {
        if (json == null || !json.trim().startsWith("{")) {
            throw new IllegalStateException("Неожиданный формат ответа пользователя");
        }
        return new UserDto(
                JsonText.longOrNull(json, "id"),
                JsonText.string(json, "login"),
                JsonText.string(json, "role"),
                JsonText.string(json, "fullName"),
                JsonText.string(json, "birthDate"),
                JsonText.string(json, "avatarUrl"),
                JsonText.bool(json, "deleted"),
                JsonText.bool(json, "blocked"));
    }

    private ServiceException failure(JsonResponse response) {
        String body = response.body();
        if (body == null || body.isBlank()) {
            body = "HTTP " + response.status();
        }
        return new ServiceException(response.status(), body);
    }

    private JsonResponse send(String method, URI uri, String jwt, String json) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10));
        if (jwt != null && !jwt.isBlank()) {
            builder.header("Authorization", "Bearer " + jwt);
        }
        if (json != null) {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(json));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        try {
            HttpResponse<String> response = httpClient.send(
                    builder.build(), HttpResponse.BodyHandlers.ofString());
            String body = response.body() == null ? "" : response.body();
            return new JsonResponse(response.statusCode(), body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(503, "Запрос прерван", e);
        } catch (IOException e) {
            throw new ServiceException(503, "Не удалось связаться с сервисом пользователей", e);
        }
    }
}
