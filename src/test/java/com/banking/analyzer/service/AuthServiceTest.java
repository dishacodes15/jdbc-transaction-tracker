package com.banking.analyzer.service;

import com.banking.analyzer.AbstractDbTest;
import com.banking.analyzer.dao.UserDAO;
import com.banking.analyzer.model.User;
import com.banking.analyzer.util.PasswordUtil;
import com.banking.analyzer.util.SessionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest extends AbstractDbTest {

    private final UserDAO userDAO = new UserDAO();
    private final AuthService service = new AuthService(userDAO);

    @BeforeEach
    void seed() {
        userDAO.save(new User("alice", PasswordUtil.hash("alice123"), "Alice", User.ROLE_USER));
        User locked = new User("locked", PasswordUtil.hash("p"), "Locked User", User.ROLE_USER);
        locked.setStatus(User.STATUS_LOCKED);
        userDAO.save(locked);
    }

    @Test
    void loginSuccess() {
        FakeSession session = new FakeSession();
        User u = service.login("alice", "alice123", session);
        assertNotNull(u);
        assertEquals(u.getId(), session.attrs.get(SessionUtil.ATTR_USER_ID));
        assertEquals(User.ROLE_USER, session.attrs.get(SessionUtil.ATTR_ROLE));
    }

    @Test
    void loginWrongPasswordThrows() {
        assertThrows(AuthenticationException.class,
                () -> service.login("alice", "wrong", new FakeSession()));
    }

    @Test
    void loginUnknownUserThrows() {
        assertThrows(AuthenticationException.class,
                () -> service.login("ghost", "x", new FakeSession()));
    }

    @Test
    void loginLockedUserThrows() {
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> service.login("locked", "p", new FakeSession()));
        assertTrue(ex.getMessage().toLowerCase().contains("locked"));
    }

    @Test
    void loginRejectsBlankInputs() {
        assertThrows(AuthenticationException.class,
                () -> service.login("", "x", new FakeSession()));
        assertThrows(AuthenticationException.class,
                () -> service.login("alice", "", new FakeSession()));
    }

    /** Minimal HttpSession stand-in — only setAttribute/getAttribute are used. */
    static class FakeSession implements HttpSession {
        final Map<String, Object> attrs = new HashMap<>();
        @Override public Object getAttribute(String name) { return attrs.get(name); }
        @Override public void setAttribute(String name, Object value) { attrs.put(name, value); }
        @Override public void removeAttribute(String name) { attrs.remove(name); }
        @Override public void invalidate() { attrs.clear(); }

        // Unused — return safe defaults
        @Override public long getCreationTime() { return 0; }
        @Override public String getId() { return "test"; }
        @Override public long getLastAccessedTime() { return 0; }
        @Override public javax.servlet.ServletContext getServletContext() { return null; }
        @Override public void setMaxInactiveInterval(int interval) {}
        @Override public int getMaxInactiveInterval() { return 0; }
        @Override @SuppressWarnings("deprecation")
        public javax.servlet.http.HttpSessionContext getSessionContext() { return null; }
        @Override public Object getValue(String name) { return getAttribute(name); }
        @Override public java.util.Enumeration<String> getAttributeNames() {
            return java.util.Collections.enumeration(attrs.keySet());
        }
        @Override public String[] getValueNames() { return attrs.keySet().toArray(new String[0]); }
        @Override public void putValue(String name, Object value) { setAttribute(name, value); }
        @Override public void removeValue(String name) { removeAttribute(name); }
        @Override public boolean isNew() { return false; }
    }
}
