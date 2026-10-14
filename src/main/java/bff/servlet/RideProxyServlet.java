package bff.servlet;

import bff.client.HttpClientHelper.Response;
import bff.client.ModerationServiceClient;
import bff.client.RideServiceClient;
import bff.client.UserServiceClient;
import bff.dto.EndRideRequest;
import bff.dto.StartRideRequest;
import bff.dto.UserDto;
import bff.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/rides/*")
public class RideProxyServlet extends HttpServlet {

    private final RideServiceClient rides = new RideServiceClient();
    private final UserServiceClient users = new UserServiceClient();
    private final ModerationServiceClient moderation = new ModerationServiceClient();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String jwt = ServletSupport.requireJwt(req, resp);
        if (jwt == null) return;

        try {
            UserDto me = me(jwt);
            ServletSupport.proxy(resp, rides.byUser(me.getId(), jwt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ServletSupport.error(resp, 503, "Interrupted");
        } catch (IOException e) {
            ServletSupport.error(resp, 502, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String jwt = ServletSupport.requireJwt(req, resp);
        if (jwt == null) return;

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        try {
            if (path.isEmpty() || "/".equals(path)) {
                StartRideRequest body = ServletSupport.body(req, StartRideRequest.class);
                UserDto me = me(jwt);
                if (blocked(me.getId(), jwt)) {
                    ServletSupport.error(resp, 403, "User is blocked");
                    return;
                }
                ServletSupport.proxy(resp, rides.start(body, jwt));
                return;
            }
            if (path.matches("^/\\d+/pay$")) {
                Long id = Long.parseLong(path.split("/")[1]);
                ServletSupport.proxy(resp, rides.pay(id, jwt));
                return;
            }
            ServletSupport.error(resp, 404, "Not found");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ServletSupport.error(resp, 503, "Interrupted");
        } catch (Exception e) {
            ServletSupport.error(resp, 400, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String jwt = ServletSupport.requireJwt(req, resp);
        if (jwt == null) return;

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        try {
            if (path.matches("^/\\d+/finish$")) {
                Long id = Long.parseLong(path.split("/")[1]);
                EndRideRequest body = ServletSupport.body(req, EndRideRequest.class);
                ServletSupport.proxy(resp, rides.finish(id, body, jwt));
                return;
            }
            ServletSupport.error(resp, 404, "Not found");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ServletSupport.error(resp, 503, "Interrupted");
        } catch (Exception e) {
            ServletSupport.error(resp, 400, e.getMessage());
        }
    }

    private UserDto me(String jwt) throws IOException, InterruptedException {
        Response r = users.me(jwt);
        if (!r.ok()) {
            throw new IOException("Cannot load user");
        }
        return JsonUtil.from(r.body(), UserDto.class);
    }

    private boolean blocked(Long userId, String jwt) throws IOException, InterruptedException {
        Response r = moderation.isBlocked(userId, jwt);
        if (!r.ok() || r.body() == null) {
            return true;
        }
        Map<?, ?> map = JsonUtil.from(r.body(), Map.class);
        return Boolean.TRUE.equals(map.get("blocked"));
    }
}
