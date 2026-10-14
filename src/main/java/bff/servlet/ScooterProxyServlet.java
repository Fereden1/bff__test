package bff.servlet;

import bff.client.ScooterServiceClient;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/scooters/*")
public class ScooterProxyServlet extends HttpServlet {

    private final ScooterServiceClient scooters = new ScooterServiceClient();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String jwt = ServletSupport.requireJwt(req, resp);
        if (jwt == null) return;

        String path = req.getPathInfo();
        try {
            if ("/nearest".equals(path)) {
                String posX = req.getParameter("posX");
                String posY = req.getParameter("posY");
                if (posX == null || posY == null || posX.isBlank() || posY.isBlank()) {
                    ServletSupport.error(resp, 400, "posX and posY required");
                    return;
                }
                ServletSupport.proxy(resp, scooters.nearest(jwt, posX, posY));
                return;
            }

            if (path == null || "/".equals(path)) {
                boolean ready = "READY".equalsIgnoreCase(req.getParameter("status"))
                        || "true".equalsIgnoreCase(req.getParameter("available"));
                ServletSupport.proxy(resp, scooters.list(jwt, ready));
                return;
            }

            Long id = Long.parseLong(path.substring(1));
            ServletSupport.proxy(resp, scooters.get(id, jwt));
        } catch (NumberFormatException e) {
            ServletSupport.error(resp, 400, "Invalid id");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ServletSupport.error(resp, 503, "Interrupted");
        }
    }
}
