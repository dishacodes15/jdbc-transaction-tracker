package com.banking.analyzer.service;

import com.banking.analyzer.dao.UserDAO;
import com.banking.analyzer.model.User;
import com.banking.analyzer.util.PasswordUtil;
import com.banking.analyzer.util.SessionUtil;

import javax.servlet.http.HttpSession;

/**
 * Login / logout brain. Knows nothing about HTTP requests — only the
 * {@link HttpSession}.
 *
 * <p>Throws {@link AuthenticationException} on bad credentials or locked
 * account; callers (servlets) map that to HTTP 401.</p>
 */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this(new UserDAO());
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User login(String username, String password, HttpSession session) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Username and password are required");
        }
        User user = userDAO.findByUsername(username.trim());
        if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password");
        }
        if (!user.isActive()) {
            throw new AuthenticationException("Account is locked. Contact administrator.");
        }
        SessionUtil.store(session, user);
        return user;
    }

    public void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    public User currentUser(HttpSession session) {
        if (session == null) return null;
        Long id = (Long) session.getAttribute(SessionUtil.ATTR_USER_ID);
        if (id == null) return null;
        return userDAO.findById(id);
    }
}
