# Pragati Bank — User Guide

> *आपकी प्रगति, हमारी ज़िम्मेदारी।* — Your progress, our responsibility.

A guided tour of the Pragati Bank demo app. Read this if you want to know
**what each screen does, what to click, and what to expect** — without
diving into code. Developers: see [DESIGN.md](DESIGN.md) for architecture.

---

## What is this app?

Pragati Bank is a tiny, fictitious retail bank. Two kinds of people use it:

* **Customers** (role `USER`) — sign in, see their accounts and balances,
  deposit, withdraw, transfer money to another account, review recent
  transactions.
* **Bank staff** (role `ADMIN`) — sign in to a separate console that shows
  bank-wide KPIs and lets them create users, freeze suspicious accounts,
  lock/unlock users and (for the demo) reset everything back to its seed
  state.

All money is fictional. Amounts are shown in Indian Rupees (₹) and formatted
with Indian thousands separators (e.g. ₹ 1,23,500.00).

---

## Getting in

1. Browse to the app — locally that's
   **<http://localhost:8080/transaction-analyzer/>** (or your Render URL).
2. The landing page redirects you to the **login** screen.
3. Pick one of the demo logins shown on the card:

   | Role  | Username | Password    | Lands on        |
   |-------|----------|-------------|-----------------|
   | USER  | `alice`  | `alice123`  | dashboard.jsp   |
   | USER  | `bob`    | `bob123`    | dashboard.jsp   |
   | ADMIN | `admin`  | `admin123`  | admin.jsp       |

4. Tap **Show** next to the password if you want to verify what you've typed
   (hidden by default, the same toggle is on every password field in the app).
5. Click **Sign in**. On success you land on your dashboard; on failure the
   card shows the reason ("Invalid credentials", "User is locked", …).

If your session times out or you visit a protected page directly, you'll be
bounced back to login.

---

## Customer journey (USER)

After login as `alice` or `bob` you're on the **dashboard**:

```
┌─────────────────────────────────────────────────────────────┐
│ Pragati Bank — आपकी प्रगति…   Alice Sharma [USER] [Logout] │
├─────────────────────────────────────────────────────────────┤
│ My Accounts                                                 │
│ ┌───────────┬─────────┬────────┬──────────────┐             │
│ │ Account # │ Type    │ Status │ Balance (₹)  │             │
│ │ ACC001    │ SAVINGS │ ACTIVE │ 23,500.00    │             │
│ └───────────┴─────────┴────────┴──────────────┘             │
│                                                             │
│ ┌────────────────────────┐  ┌──────────────────────────┐    │
│ │ Deposit / Withdraw     │  │ Transfer                 │    │
│ │ Account: [ ACC001 ▾ ]  │  │ From:   [ ACC001 ▾ ]     │    │
│ │ Amount:  [_______]     │  │ To:     [_______]        │    │
│ │ Note:    [_______]     │  │ Amount: [_______]        │    │
│ │  [Deposit] [Withdraw]  │  │ Note:   [_______]        │    │
│ └────────────────────────┘  │  [Send money]            │    │
│                             └──────────────────────────┘    │
│                                                             │
│ Recent Transactions  Account [ ACC001 ▾ ]                   │
│  Date · Type · Amount · Description · Balance after         │
└─────────────────────────────────────────────────────────────┘
```

### Use case 1 — Check balance

* The **My Accounts** table lists every account you own and the current
  balance for each.
* If you own multiple accounts (like `bob`), they are all listed and selectable
  in the dropdowns below.

### Use case 2 — Deposit money

1. In the **Deposit / Withdraw** card pick the destination account.
2. Type an **amount** (must be greater than 0).
3. Optionally add a **description** (e.g. "Cash deposit at branch").
4. Click **Deposit**.
5. You'll see a green confirmation banner at the top. The account row in
   *My Accounts* updates with the new balance, and a new row appears in
   *Recent Transactions* (type `CREDIT`).

If something is wrong (amount ≤ 0, account is frozen, …) you'll get a red
error banner with the reason. The form is not cleared so you can fix and
retry.

### Use case 3 — Withdraw money

Same as deposit but click **Withdraw**. Pragati Bank refuses to overdraw —
if the amount would push the balance below zero, you get:

> *Insufficient balance*

…and nothing is debited.

### Use case 4 — Transfer to another account

1. **From** — pick one of your own accounts.
2. **To** — type the destination account number. It can belong to anyone
   (you can transfer to your other account, or to a friend's account number).
3. **Amount** + optional **description**.
4. Click **Send money**.
5. Two transactions are recorded atomically: a DEBIT on your account and a
   CREDIT on the destination. If the destination doesn't exist, is frozen, or
   you don't have enough money, **neither** side is touched and you see an
   error banner.

### Use case 5 — Review transactions

* Use the *Account* dropdown in **Recent Transactions** to switch between
  your accounts.
* Each row shows the date, CREDIT/DEBIT badge, amount, description and the
  balance *after* that transaction.

### Use case 6 — Sign out

Click **Logout** in the top bar. Your session is invalidated immediately and
you land back on the login screen.

---

## Bank staff journey (ADMIN)

Sign in as `admin / admin123`. You're sent to **admin.jsp**, which is a
completely separate console — admins do not get a customer dashboard. (If
you visit `/dashboard.jsp` as admin you stay there for a beat then get
redirected here.)

```
┌─────────────────────────────────────────────────────────────┐
│ Pragati Bank — Admin            Pragati Admin [ADMIN]       │
├─────────────────────────────────────────────────────────────┤
│ [ Users 3 ]  [ Accounts 3 ]  [ Transactions 5 ]  [ ₹ 83,500 │
│                                                             │
│ ┌────────────────────────┐  ┌──────────────────────────┐    │
│ │ Create User            │  │ Account Status           │    │
│ │ Username:  [_______]   │  │ Account #: [_______]     │    │
│ │ Password:  [_____][🙈] │  │ Status:    [ACTIVE ▾]    │    │
│ │ Full name: [_______]   │  │  [Update Status]         │    │
│ │ Role:      [USER ▾]    │  └──────────────────────────┘    │
│ │ Initial acct (opt):    │                                  │
│ │ Type:      [SAVINGS▾]  │                                  │
│ │  [Create User]         │                                  │
│ └────────────────────────┘                                  │
│                                                             │
│ All Users  (id, username, full name, role, status, action)  │
│ All Accounts  (account #, owner, type, status)              │
│                                                             │
│ Maintenance                                                 │
│ ┌─────────────────────────────────────────────────────┐     │
│ │ Reset to seed data — wipes everything, restores the │     │
│ │ original admin/alice/bob accounts.   [Reset]        │     │
│ └─────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### Use case 7 — See the bank at a glance

The four KPI tiles at the top show, in real time:

* **Users** — total user count (admin + customers).
* **Accounts** — total bank accounts.
* **Transactions** — total CREDIT + DEBIT rows ever written.
* **System Balance** — sum of balances across every account.

They refresh after every action you take on the page.

### Use case 8 — Create a new customer

1. Fill in **Username**, **Password**, **Full name** (all required).
2. Pick a **Role** — `USER` for normal customers, `ADMIN` for staff.
3. **Optional starter account** — if you fill in *Initial account number* and
   *Account type*, the user is created **with** that account already opened.
4. Click **Create User**. The success banner tells you whether an account
   was attached.

> *Why isn't my new user in **All Accounts**?*
> Because they have no account yet. The empty-state hint below the table
> explains this. Either add a starter account in the form above, or create
> one for them later.

### Use case 9 — Lock / unlock a user

* In **All Users** every row has a **Lock** or **Unlock** button.
* **Lock** prevents the user from logging in (their next login attempt gets
  *"User is locked"*).
* **Unlock** restores `ACTIVE` status so they can sign in again.

### Use case 10 — Freeze / un-freeze an account

* Use the **Account Status** card on the right.
* Type the account number, pick `FROZEN` or `ACTIVE`, click **Update Status**.
* A `FROZEN` account is read-only — deposits, withdrawals and outgoing
  transfers all fail with *"Account is not active"*.

### Use case 11 — Reset the demo to seed data

Useful when the demo data has drifted (lots of test transactions, fake
users, etc.) and you want a clean slate before showing the app.

1. Scroll to **Maintenance** at the bottom of the admin page.
2. Click **Reset to seed data**.
3. Confirm the first dialog ("This will DELETE all users…").
4. Type **`RESET`** (capitals) in the second prompt. Anything else aborts.
5. The server wipes all users / accounts / transactions, re-seeds the
   `admin / alice / bob` demo dataset, invalidates your session and bounces
   you back to login.
6. Sign in again as `admin / admin123` (the previous password you may have
   set on `admin` is gone — only seed passwords work now).

---

## Cheat sheet

| I want to…                            | Where                                                    |
|---------------------------------------|----------------------------------------------------------|
| Sign in                               | `/login.jsp`                                             |
| See my accounts and balances          | Dashboard → *My Accounts*                                |
| Deposit / withdraw                    | Dashboard → *Deposit / Withdraw* card                    |
| Transfer money                        | Dashboard → *Transfer* card                              |
| See transactions for an account       | Dashboard → *Recent Transactions* (account selector)     |
| Reveal a password I'm typing          | Click **Show** next to any password field                |
| Sign out                              | **Logout** button in the top bar                         |
| Add a new user                        | Admin → *Create User*                                    |
| Lock or unlock a user                 | Admin → *All Users* → row button                         |
| Freeze or activate an account         | Admin → *Account Status*                                 |
| See bank-wide totals                  | Admin → KPI tiles at the top                             |
| Reset the demo                        | Admin → *Maintenance* → **Reset to seed data**           |

---

## Common error messages

| Banner                                       | What it means                                                   | How to fix                          |
|----------------------------------------------|-----------------------------------------------------------------|-------------------------------------|
| *Invalid credentials*                         | Wrong username or password.                                     | Try again. Demo creds are on card.  |
| *User is locked*                              | Admin has set your status to `LOCKED`.                          | Ask an admin to unlock you.         |
| *Amount must be greater than zero*            | Deposit/withdraw/transfer with 0 or negative amount.            | Enter a positive number.            |
| *Insufficient balance*                        | Withdraw / outgoing transfer would overdraw the account.        | Reduce the amount.                  |
| *Account is not active*                       | The source or destination account is `FROZEN`.                  | Ask admin to set it back to ACTIVE. |
| *Destination account not found*               | Transfer typed a non-existent account number.                   | Double-check the account number.    |
| *Admin role required* (403)                   | A USER tried to call an ADMIN-only endpoint or page.            | Log in as admin.                    |
| *Session expired* / redirected to login       | Your session cookie has lapsed or was invalidated.              | Sign in again.                      |

---

## Privacy & security notes

* Passwords are **never stored or returned in plaintext**. They are hashed
  with **BCrypt** on creation; the API strips `passwordHash` from every JSON
  response.
* Sessions are server-side `HttpSession` cookies; logging out invalidates
  the session immediately.
* Customer endpoints check **ownership** — e.g. you can deposit only into
  your own accounts; the server returns `403` if you try someone else's.
* Admin endpoints are guarded by `AuthFilter` — every request to
  `/api/admin/*` or `/api/users` requires the `ADMIN` role; otherwise it
  returns `403`.

> This is a teaching demo. There are no CSRF tokens, no rate-limiting, no
> 2FA, and the DB file lives on local disk. **Do not** put real money or
> personal data into it.
