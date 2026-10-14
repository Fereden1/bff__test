package bff.servlet;

import bff.dto.AuthRequest;
import bff.dto.AuthResponse;
import bff.dto.JsonResponse;
import bff.dto.UserDto;
import bff.service.ServiceException;
import bff.service.UserService;
import bff.service.impl.JsonText;
import bff.service.impl.UserServiceRestImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {

    private UserService userService;

    @Override
    public void init() throws ServletException {
        userService = new UserServiceRestImpl();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getPathInfo();
        try {
            AuthRequest body = readAuth(req);
            if ("/register".equals(action)) {
                JsonResponse registered = userService.register(body);
                writeJson(resp, registered.status(), registered.body());
                return;
            }
            if ("/login".equals(action)) {
                AuthResponse auth = userService.login(body);
                HttpSession session = req.getSession(true);
                req.changeSessionId();
                session.setAttribute("jwt", auth.accessToken());
                if (auth.expiresInSeconds() > 0 && auth.expiresInSeconds() <= Integer.MAX_VALUE) {
                    session.setMaxInactiveInterval((int) auth.expiresInSeconds());
                }
                writeJson(resp, HttpServletResponse.SC_OK, toJson(auth));
                return;
            }
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Не найдено");
        } catch (ServiceException e) {
            log("Не удалось выполнить запрос авторизации", e);
            writeServiceError(resp, e);
        } catch (Exception e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage() != null ? e.getMessage() : "Некорректный запрос");
        }
    }

    private AuthRequest readAuth(HttpServletRequest req) throws IOException {
        String json = JsonText.read(req.getInputStream());
        return new AuthRequest(
                JsonText.string(json, "login"),
                JsonText.string(json, "password"),
                JsonText.string(json, "fullName"),
                JsonText.string(json, "birthDate"),
                JsonText.string(json, "avatarUrl"),
                JsonText.string(json, "role"));
    }

    private String toJson(AuthResponse auth) {
        return "{\"accessToken\":" + JsonText.quote(auth.accessToken())
                + ",\"expiresInSeconds\":" + auth.expiresInSeconds()
                + ",\"user\":" + userJson(auth.user()) + "}";
    }

    private String userJson(UserDto user) {
        if (user == null) {
            return "null";
        }
        return "{\"id\":" + (user.id() == null ? "null" : user.id())
                + ",\"login\":" + JsonText.quote(user.login())
                + ",\"role\":" + JsonText.quote(user.role())
                + ",\"fullName\":" + JsonText.quote(user.fullName())
                + ",\"birthDate\":" + JsonText.quote(user.birthDate())
                + ",\"avatarUrl\":" + JsonText.quote(user.avatarUrl())
                + ",\"deleted\":" + user.deleted()
                + ",\"blocked\":" + user.blocked() + "}";
    }

    private void writeJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        if (json != null && !json.isEmpty()) {
            resp.getWriter().write(json);
        }
    }

    private void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        writeJson(resp, status, "{\"error\":" + JsonText.quote(message) + "}");
    }

    private void writeServiceError(HttpServletResponse resp, ServiceException e) throws IOException {
        String message = e.getMessage() == null ? "" : e.getMessage().trim();
        if (message.startsWith("{") || message.startsWith("[")) {
            writeJson(resp, e.status(), message);
            return;
        }
        if (message.isBlank()) {
            message = "Ошибка сервиса";
        }
        writeError(resp, e.status() == 0 ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : e.status(), message);
    }
}
