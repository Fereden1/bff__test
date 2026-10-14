package bff.servlet;

import bff.dto.JsonResponse;
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

@WebServlet("/api/users/*")
public class UserProxyServlet extends HttpServlet {

    private UserService userService;

    @Override
    public void init() throws ServletException {
        userService = new UserServiceRestImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        try {
            if ("/exists".equals(path)) {
                String login = req.getParameter("login");
                if (login == null || login.isBlank()) {
                    writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Укажите логин");
                    return;
                }
                JsonResponse exists = userService.exists(login);
                writeJson(resp, exists.status(), exists.body());
                return;
            }
            if ("/me".equals(path)) {
                String jwt = requireJwt(req, resp);
                if (jwt == null) {
                    return;
                }
                JsonResponse profile = userService.profile(jwt);
                writeJson(resp, profile.status(), profile.body());
                return;
            }
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Не найдено");
        } catch (ServiceException e) {
            log("Не удалось выполнить запрос пользователя", e);
            writeServiceError(resp, e);
        }
    }

    private String requireJwt(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = jwt(req);
        if (token == null || token.isBlank()) {
            writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Требуется авторизация");
            return null;
        }
        return token;
    }

    private String jwt(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String value = header.substring(7).trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("jwt") instanceof String token && !token.isBlank()) {
            return token;
        }
        return null;
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
