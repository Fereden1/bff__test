package bff.filter;

import bff.service.ServiceException;
import bff.service.UserService;
import bff.service.impl.JsonText;
import bff.service.impl.UserServiceRestImpl;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter("/api/*")
public class JwtAuthFilter implements Filter {

    private UserService userService;

    @Override
    public void init(FilterConfig filterConfig) {
        userService = new UserServiceRestImpl();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (path.startsWith("/api/auth/") || "/api/users/exists".equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        String jwt = jwt(req);
        if (jwt == null || jwt.isBlank()) {
            writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Требуется авторизация");
            return;
        }

        try {
            if (!userService.validate(jwt)) {
                writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Недействительный или просроченный токен");
                return;
            }
            chain.doFilter(request, response);
        } catch (ServiceException e) {
            writeError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Сервис пользователей недоступен");
        }
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

    private void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"error\":" + JsonText.quote(message) + "}");
    }
}
