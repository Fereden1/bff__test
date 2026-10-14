package bff.servlet;

import bff.client.UserServiceClient;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/users/*")
public class UserProxyServlet extends HttpServlet {

    private final UserServiceClient users = new UserServiceClient();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        try {
            if ("/exists".equals(path)) {
                String login = req.getParameter("login");
                if (login == null || login.isBlank()) {
                    ServletSupport.error(resp, 400, "login required");
                    return;
                }
                ServletSupport.proxy(resp, users.exists(login));
                return;
            }
            if ("/me".equals(path)) {
                String jwt = ServletSupport.requireJwt(req, resp);
                if (jwt == null) return;
                ServletSupport.proxy(resp, users.me(jwt));
                return;
            }
            ServletSupport.error(resp, 404, "Not found");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ServletSupport.error(resp, 503, "Interrupted");
        }
    }
}
