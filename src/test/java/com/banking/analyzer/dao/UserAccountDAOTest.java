package com.banking.analyzer.dao;

import com.banking.analyzer.AbstractDbTest;
import com.banking.analyzer.model.Account;
import com.banking.analyzer.model.User;
import com.banking.analyzer.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserAccountDAOTest extends AbstractDbTest {

    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    @Test
    void saveAndFindUser() {
        User u = new User("alice", PasswordUtil.hash("alice123"), "Alice", User.ROLE_USER);
        Long id = userDAO.save(u);
        assertNotNull(id);

        User found = userDAO.findByUsername("alice");
        assertNotNull(found);
        assertEquals("Alice", found.getFullName());
        assertEquals(User.ROLE_USER, found.getRole());
        assertEquals(User.STATUS_ACTIVE, found.getStatus());
        assertTrue(PasswordUtil.verify("alice123", found.getPasswordHash()));
    }

    @Test
    void findByUsernameReturnsNullWhenMissing() {
        assertNull(userDAO.findByUsername("ghost"));
    }

    @Test
    void updateUserStatus() {
        User u = new User("bob", PasswordUtil.hash("bob123"), "Bob", User.ROLE_USER);
        userDAO.save(u);
        assertTrue(userDAO.updateStatus(u.getId(), User.STATUS_LOCKED));
        assertEquals(User.STATUS_LOCKED, userDAO.findById(u.getId()).getStatus());
    }

    @Test
    void listAllUsersAndCount() {
        userDAO.save(new User("u1", PasswordUtil.hash("p"), "One", User.ROLE_USER));
        userDAO.save(new User("u2", PasswordUtil.hash("p"), "Two", User.ROLE_ADMIN));
        assertEquals(2, userDAO.countAll());
        List<User> all = userDAO.listAll();
        assertEquals(2, all.size());
    }

    @Test
    void accountOwnershipCheck() {
        User alice = new User("alice", PasswordUtil.hash("p"), "Alice", User.ROLE_USER);
        userDAO.save(alice);
        User bob = new User("bob", PasswordUtil.hash("p"), "Bob", User.ROLE_USER);
        userDAO.save(bob);

        accountDAO.save(new Account("ACC001", alice.getId(), Account.TYPE_SAVINGS));

        assertTrue(accountDAO.isOwnedBy("ACC001", alice.getId()));
        assertFalse(accountDAO.isOwnedBy("ACC001", bob.getId()));
        assertFalse(accountDAO.isOwnedBy("DOES_NOT_EXIST", alice.getId()));
    }

    @Test
    void findAccountsByUserId() {
        User u = new User("u", PasswordUtil.hash("p"), "U", User.ROLE_USER);
        userDAO.save(u);
        accountDAO.save(new Account("ACC100", u.getId(), Account.TYPE_SAVINGS));
        accountDAO.save(new Account("ACC101", u.getId(), Account.TYPE_CHECKING));

        List<Account> accts = accountDAO.findByUserId(u.getId());
        assertEquals(2, accts.size());
    }

    @Test
    void freezeAndUnfreezeAccount() {
        User u = new User("u", PasswordUtil.hash("p"), "U", User.ROLE_USER);
        userDAO.save(u);
        accountDAO.save(new Account("ACC200", u.getId(), Account.TYPE_SAVINGS));

        assertTrue(accountDAO.updateStatus("ACC200", Account.STATUS_FROZEN));
        assertEquals(Account.STATUS_FROZEN, accountDAO.findByNumber("ACC200").getStatus());

        assertTrue(accountDAO.updateStatus("ACC200", Account.STATUS_ACTIVE));
        assertTrue(accountDAO.findByNumber("ACC200").isActive());
    }
}
