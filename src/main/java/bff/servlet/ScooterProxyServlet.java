package bff.servlet;

import bff.client.ScooterServiceClient;
import bff.dto.NearestScooterRequest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/api/scooters/*")
public class ScooterProxyServlet extends HttpServlet {

    private final ScooterServiceClient scooters = new ScooterServiceClient();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String jwt = ServletSupport.requireJwt(req, resp);
        if (jwt == null) return;

        String path = req.getPathInfo();
        try {
            if (path == null || "/".equals(path)) {
                boolean ready = "READY".equalsIgnoreCase(req.getParameter("status"))
                        || "true".equalsIgnoreCase(req.getParameter("available"));
                ServletSupport.proxy(resp, scooters.list(jwt, ready));
                return;
            }

            if ("/nearest".equals(path)) {
                ServletSupport.error(resp, 405, "Use POST /api/scooters/nearest with posX, posY");
                return;
            }

            String numberPlate = URLDecoder.decode(path.substring(1), StandardCharsets.UTF_8);
            if (numberPlate.isBlank()) {
                ServletSupport.error(resp, 400, "number_plate required");
                return;
            }
            ServletSupport.proxy(resp, scooters.getByNumberPlate(numberPlate, jwt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ServletSupport.error(resp, 503, "Interrupted");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String jwt = ServletSupport.requireJwt(req, resp);
        if (jwt == null) return;

        String path = req.getPathInfo();
        if (!"/nearest".equals(path)) {
            ServletSupport.error(resp, 404, "Not found");
            return;
        }

        try {
            NearestScooterRequest body = ServletSupport.body(req, NearestScooterRequest.class);
            if (body.getPosX() == null || body.getPosY() == null) {
                ServletSupport.error(resp, 400, "posX and posY required");
                return;
            }
            ServletSupport.proxy(resp, scooters.nearest(jwt, body));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ServletSupport.error(resp, 503, "Interrupted");
        } catch (Exception e) {
            ServletSupport.error(resp, 400, e.getMessage());
        }
    }
}
