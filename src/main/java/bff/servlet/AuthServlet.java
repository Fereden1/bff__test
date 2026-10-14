package bff.servlet;

import bff.client.HttpClientHelper.Response;
import bff.client.UserServiceClient;
import bff.dto.AuthRequest;
import bff.dto.AuthResponse;
import bff.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {

    private final UserServiceClient users = new UserServiceClient();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = req.getPathInfo();
        try {
            AuthRequest body = ServletSupport.body(req, AuthRequest.class);

            if ("/register".equals(action)) {
                ServletSupport.proxy(resp, users.register(body));
                return;
            }

            if ("/login".equals(action)) {
                Response r = users.login(body);
                if (!r.ok()) {
                    ServletSupport.proxy(resp, r);
                    return;
                }
                AuthResponse auth = JsonUtil.from(r.body(), AuthResponse.class);
                HttpSession session = req.getSession(true);
                session.setAttribute(ServletSupport.JWT, auth.getAccessToken());
                if (auth.getExpiresInSeconds() > 0 && auth.getExpiresInSeconds() <= Integer.MAX_VALUE) {
                    session.setMaxInactiveInterval((int) auth.getExpiresInSeconds());
                }
                ServletSupport.json(resp, 200, auth);
                return;
            }

            ServletSupport.error(resp, 404, "Not found");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ServletSupport.error(resp, 503, "Interrupted");
        } catch (Exception e) {
            ServletSupport.error(resp, 400, e.getMessage() != null ? e.getMessage() : "Bad request");
        }
    }
}
