package bff.servlet;

import bff.client.HttpClientHelper.Response;
import bff.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

public final class ServletSupport {

    public static final String JWT = "jwt";

    private ServletSupport() {
    }

    public static void json(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        JsonUtil.write(resp.getWriter(), body);
    }

    public static void proxy(HttpServletResponse resp, Response r) throws IOException {
        resp.setStatus(r.status());
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        if (r.body() != null) {
            resp.getWriter().write(r.body());
        }
    }

    public static void error(HttpServletResponse resp, int status, String msg) throws IOException {
        json(resp, status, Map.of("error", msg, "status", status));
    }

    public static String jwt(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h != null && h.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return h.substring(7).trim();
        }
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute(JWT) instanceof String token) {
            return token;
        }
        return null;
    }

    public static String requireJwt(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = jwt(req);
        if (token == null || token.isBlank()) {
            error(resp, 401, "JWT required");
            return null;
        }
        return token;
    }

    public static <T> T body(HttpServletRequest req, Class<T> type) throws IOException {
        return JsonUtil.from(req.getInputStream(), type);
    }
}
