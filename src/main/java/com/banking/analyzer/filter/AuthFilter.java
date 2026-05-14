package com.banking.analyzer.filter;

import com.banking.analyzer.util.JsonUtil;
import com.banking.analyzer.util.SessionUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * Authentication / authorisation filter for the API and protected pages.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>Public paths (login, logout, login page, static assets) → pass through.</li>
 *   <li>Authenticated paths → must have a session with {@code userId} set.</li>
 *   <li>Admin-only paths (matched by prefix) → must have role ADMIN.</li>
 *   <li>For {@code /api/*}: missing/invalid auth → JSON 401/403.</li>
 *   <li>For protected pages: redirect to {@code /login.jsp}.</li>
 * </ul>
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = {"/api/*", "/dashboard.jsp", "/admin.jsp", "/statement.jsp", "/app.jsp"})
public class AuthFilter implements Filter {

    /** Paths (relative to context) that do NOT require a session. */
    private static final Set<String> PUBLIC_API_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/health"
    );

    /** Admin-only path prefixes (relative to context). */
    private static final Set<String> ADMIN_PREFIXES = Set.of(
            "/api/users",
            "/api/admin"
    );

    @Override public void init(FilterConfig filterConfig) {}
    @Override public void destroy() {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());

        // Always allow OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        boolean isApi = path.startsWith("/api/");

        if (isApi && PUBLIC_API_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        if (!SessionUtil.isLoggedIn(req)) {
            if (isApi) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            } else {
                resp.sendRedirect(req.getContextPath() + "/login.jsp");
            }
            return;
        }

        // Admin guard
        for (String prefix : ADMIN_PREFIXES) {
            if (path.startsWith(prefix) && !SessionUtil.isAdmin(req)) {
                if (isApi) {
                    JsonUtil.writeError(resp, HttpServletResponse.SC_FORBIDDEN, "Admin role required");
                } else {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin role required");
                }
                return;
            }
        }

        // For /app.jsp (legacy raw view) — admin only
        if ("/app.jsp".equals(path) && !SessionUtil.isAdmin(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin role required");
            return;
        }

        chain.doFilter(request, response);
    }
}
