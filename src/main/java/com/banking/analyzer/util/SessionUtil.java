package com.banking.analyzer.util;

import com.banking.analyzer.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Reads the current user's auth attributes from {@link HttpSession}.
 *
 * <p>Session attributes set at login:</p>
 * <ul>
 *   <li>{@code userId}   — Long</li>
 *   <li>{@code username} — String</li>
 *   <li>{@code role}     — {@code "USER"} or {@code "ADMIN"}</li>
 *   <li>{@code fullName} — String</li>
 * </ul>
 */
public final class SessionUtil {

    public static final String ATTR_USER_ID   = "userId";
    public static final String ATTR_USERNAME  = "username";
    public static final String ATTR_ROLE      = "role";
    public static final String ATTR_FULL_NAME = "fullName";

    private SessionUtil() {
    }

    public static boolean isLoggedIn(HttpServletRequest req) {
        return getUserId(req) != null;
    }

    public static Long getUserId(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (Long) s.getAttribute(ATTR_USER_ID);
    }

    public static String getRole(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (String) s.getAttribute(ATTR_ROLE);
    }

    public static String getUsername(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (String) s.getAttribute(ATTR_USERNAME);
    }

    public static boolean isAdmin(HttpServletRequest req) {
        return User.ROLE_ADMIN.equals(getRole(req));
    }

    public static void store(HttpSession session, User user) {
        session.setAttribute(ATTR_USER_ID, user.getId());
        session.setAttribute(ATTR_USERNAME, user.getUsername());
        session.setAttribute(ATTR_ROLE, user.getRole());
        session.setAttribute(ATTR_FULL_NAME, user.getFullName());
    }
}
