package bff.servlet;

import bff.client.DocumentServiceClient;
import bff.client.HttpClientHelper.Response;
import bff.dto.DocumentRequest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/documents/generate")
public class DocumentProxyServlet extends HttpServlet {

    private final DocumentServiceClient documents = new DocumentServiceClient();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String jwt = ServletSupport.requireJwt(req, resp);
        if (jwt == null) return;

        try {
            DocumentRequest body = ServletSupport.body(req, DocumentRequest.class);
            Response r = documents.generate(body, jwt);
            if (r.bytes() != null) {
                resp.setStatus(r.status());
                resp.setContentType("application/pdf");
                resp.setHeader("Content-Disposition", "attachment; filename=\"document.pdf\"");
                resp.getOutputStream().write(r.bytes());
                return;
            }
            ServletSupport.proxy(resp, r);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ServletSupport.error(resp, 503, "Interrupted");
        } catch (Exception e) {
            ServletSupport.error(resp, 400, e.getMessage());
        }
    }
}
