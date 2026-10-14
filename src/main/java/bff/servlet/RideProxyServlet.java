package bff.servlet;

import bff.dto.EndRideRequest;
import bff.dto.JsonResponse;
import bff.dto.StartRideRequest;
import bff.dto.UserDto;
import bff.service.RideService;
import bff.service.ServiceException;
import bff.service.UserService;
import bff.service.impl.JsonText;
import bff.service.impl.RideServiceRestImpl;
import bff.service.impl.UserServiceRestImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/api/rides/*")
public class RideProxyServlet extends HttpServlet {

    private RideService rideService;
    private UserService userService;

    @Override
    public void init() throws ServletException {
        rideService = new RideServiceRestImpl();
        userService = new UserServiceRestImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String jwt = requireJwt(req, resp);
        if (jwt == null) {
            return;
        }
        try {
            UserDto me = userService.currentUser(jwt);
            JsonResponse rides = rideService.findByUser(jwt, me.id());
            writeJson(resp, rides.status(), rides.body());
        } catch (ServiceException e) {
            log("Не удалось получить поездки", e);
            writeServiceError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String jwt = requireJwt(req, resp);
        if (jwt == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        req.setCharacterEncoding("UTF-8");
        try {
            if (path.isEmpty() || "/".equals(path)) {
                StartRideRequest body = readStart(req);
                UserDto me = userService.currentUser(jwt);
                if (userService.isBlocked(me.id(), jwt)) {
                    writeError(resp, HttpServletResponse.SC_FORBIDDEN, "Пользователь заблокирован");
                    return;
                }
                JsonResponse started = rideService.start(jwt, body);
                writeJson(resp, started.status(), started.body());
                return;
            }
            if (path.matches("^/\\d+/pay$")) {
                long id = Long.parseLong(path.split("/")[1]);
                JsonResponse paid = rideService.pay(jwt, id);
                writeJson(resp, paid.status(), paid.body());
                return;
            }
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Не найдено");
        } catch (ServiceException e) {
            log("Не удалось выполнить операцию с поездкой", e);
            writeServiceError(resp, e);
        } catch (Exception e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage() != null ? e.getMessage() : "Некорректный запрос");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String jwt = requireJwt(req, resp);
        if (jwt == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        req.setCharacterEncoding("UTF-8");
        try {
            if (path.matches("^/\\d+/finish$")) {
                long id = Long.parseLong(path.split("/")[1]);
                EndRideRequest body = readFinish(req);
                JsonResponse finished = rideService.finish(jwt, id, body);
                writeJson(resp, finished.status(), finished.body());
                return;
            }
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Не найдено");
        } catch (ServiceException e) {
            log("Не удалось завершить поездку", e);
            writeServiceError(resp, e);
        } catch (Exception e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage() != null ? e.getMessage() : "Некорректный запрос");
        }
    }

    private StartRideRequest readStart(HttpServletRequest req) throws IOException {
        String json = JsonText.read(req.getInputStream());
        return new StartRideRequest(
                JsonText.string(json, "numberPlate"),
                JsonText.longOrNull(json, "rateId"),
                JsonText.raw(json, "posX"),
                JsonText.raw(json, "posY"));
    }

    private EndRideRequest readFinish(HttpServletRequest req) throws IOException {
        String json = JsonText.read(req.getInputStream());
        return new EndRideRequest(JsonText.raw(json, "posX"), JsonText.raw(json, "posY"));
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
