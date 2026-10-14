package bff.filter;

import bff.client.UserServiceClient;
import bff.servlet.ServletSupport;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter("/api/*")
public class JwtAuthFilter implements Filter {

    private final UserServiceClient users = new UserServiceClient();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (path.startsWith("/api/auth/") || path.equals("/api/users/exists")) {
            chain.doFilter(request, response);
            return;
        }

        String jwt = ServletSupport.jwt(req);
        if (jwt == null || jwt.isBlank()) {
            ServletSupport.error(resp, 401, "JWT required");
            return;
        }

        try {
            if (!users.validate(jwt).ok()) {
                ServletSupport.error(resp, 401, "Invalid or expired JWT");
                return;
            }
            chain.doFilter(request, response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ServletSupport.error(resp, 503, "User service unavailable");
        } catch (IOException e) {
            ServletSupport.error(resp, 503, "User service unavailable");
        }
    }
}
