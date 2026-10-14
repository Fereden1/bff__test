package bff.servlet;

import bff.dto.JsonResponse;
import bff.dto.NearestScooterRequest;
import bff.service.ScooterService;
import bff.service.ServiceException;
import bff.service.impl.JsonText;
import bff.service.impl.ScooterServiceRestImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/api/scooters/*")
public class ScooterProxyServlet extends HttpServlet {

    private ScooterService scooterService;

    @Override
    public void init() throws ServletException {
        scooterService = new ScooterServiceRestImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String jwt = requireJwt(req, resp);
        if (jwt == null) {
            return;
        }

        String path = req.getPathInfo();
        try {
            if (path == null || "/".equals(path)) {
                boolean ready = "READY".equalsIgnoreCase(req.getParameter("status"))
                        || "true".equalsIgnoreCase(req.getParameter("available"));
                JsonResponse list = scooterService.list(jwt, ready);
                writeJson(resp, list.status(), list.body());
                return;
            }
            if ("/nearest".equals(path)) {
                writeError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                        "Используйте POST /api/scooters/nearest с posX и posY");
                return;
            }

            String numberPlate = URLDecoder.decode(path.substring(1), StandardCharsets.UTF_8);
            if (numberPlate.isBlank()) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Укажите номер самоката");
                return;
            }
            JsonResponse scooter = scooterService.findByNumberPlate(jwt, numberPlate);
            writeJson(resp, scooter.status(), scooter.body());
        } catch (ServiceException e) {
            log("Не удалось получить самокаты", e);
            writeServiceError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String jwt = requireJwt(req, resp);
        if (jwt == null) {
            return;
        }
        if (!"/nearest".equals(req.getPathInfo())) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Не найдено");
            return;
        }

        req.setCharacterEncoding("UTF-8");
        try {
            NearestScooterRequest body = readNearest(req);
            if (body.posX() == null || body.posY() == null) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Нужны posX и posY");
                return;
            }
            JsonResponse nearest = scooterService.nearest(jwt, body);
            writeJson(resp, nearest.status(), nearest.body());
        } catch (ServiceException e) {
            log("Не удалось найти ближайшие самокаты", e);
            writeServiceError(resp, e);
        } catch (Exception e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage() != null ? e.getMessage() : "Некорректный запрос");
        }
    }

    private NearestScooterRequest readNearest(HttpServletRequest req) throws IOException {
        String json = JsonText.read(req.getInputStream());
        return new NearestScooterRequest(JsonText.raw(json, "posX"), JsonText.raw(json, "posY"));
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
