package bff.servlet;

import bff.dto.DocumentRequest;
import bff.service.DocumentService;
import bff.service.ServiceException;
import bff.service.impl.DocumentServiceRestImpl;
import bff.service.impl.JsonText;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/api/documents/generate")
public class DocumentProxyServlet extends HttpServlet {

    private DocumentService documentService;

    @Override
    public void init() throws ServletException {
        documentService = new DocumentServiceRestImpl();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String jwt = requireJwt(req, resp);
        if (jwt == null) {
            return;
        }

        req.setCharacterEncoding("UTF-8");
        try {
            DocumentRequest body = readDocument(req);
            byte[] pdf = documentService.generate(jwt, body);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition", "attachment; filename=\"document.pdf\"");
            resp.getOutputStream().write(pdf);
        } catch (ServiceException e) {
            log("Не удалось сформировать документ", e);
            writeServiceError(resp, e);
        } catch (Exception e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage() != null ? e.getMessage() : "Некорректный запрос");
        }
    }

    private DocumentRequest readDocument(HttpServletRequest req) throws IOException {
        String json = JsonText.read(req.getInputStream());
        return new DocumentRequest(
                JsonText.longOrNull(json, "userId"),
                JsonText.string(json, "docType"),
                JsonText.longOrNull(json, "sourceId"),
                JsonText.string(json, "from"),
                JsonText.string(json, "to"),
                JsonText.string(json, "status"));
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
