<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Admin Console — Pragati Bank</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/pragati.css">
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/assets/pragati-logo.svg">
</head>
<body>
    <div class="topbar">
        <div class="brand">
            <img src="${pageContext.request.contextPath}/assets/pragati-logo.svg" alt="">
            <div>
                <div class="brand-name">Pragati Bank — Admin</div>
                <div class="brand-tag">आपकी प्रगति, हमारी ज़िम्मेदारी।</div>
            </div>
        </div>
        <div class="user-info">
            <span id="userLabel">…</span>
            <span class="role-badge" style="background:var(--gold);">ADMIN</span>
            <button class="logout" id="logoutBtn">Logout</button>
        </div>
    </div>

    <div class="container">
        <h1>System Overview</h1>
        <div id="globalMsg" class="alert hidden"></div>

        <!-- KPIs -->
        <div class="kpi-row" id="kpis">
            <div class="kpi"><div class="label">Users</div>          <div class="value" id="kpiUsers">–</div></div>
            <div class="kpi saffron"><div class="label">Accounts</div>  <div class="value" id="kpiAccts">–</div></div>
            <div class="kpi gold"><div class="label">Transactions</div><div class="value" id="kpiTx">–</div></div>
            <div class="kpi"><div class="label">System Balance (₹)</div><div class="value" id="kpiBal">–</div></div>
        </div>

        <div class="grid-2" style="margin-top:20px;">
            <!-- Create user -->
            <div class="card">
                <h2>Create User</h2>
                <label>Username</label><input id="nuUser" type="text" autocomplete="off">
                <label>Password</label>
                <div class="pwd-wrap">
                    <input id="nuPass" type="password" autocomplete="new-password">
                    <button type="button" class="pwd-toggle" data-target="nuPass" aria-label="Show password">Show</button>
                </div>
                <label>Full name</label><input id="nuName" type="text">
                <label>Role</label>
                <select id="nuRole"><option value="USER">USER</option><option value="ADMIN">ADMIN</option></select>
                <label>Initial account number (optional)</label><input id="nuAcct" type="text" placeholder="e.g. ACC010">
                <label>Account type</label>
                <select id="nuAcctType"><option value="SAVINGS">SAVINGS</option><option value="CHECKING">CHECKING</option></select>
                <button id="createUserBtn">Create User</button>
            </div>

            <!-- Account status -->
            <div class="card">
                <h2>Account Status</h2>
                <p style="color:var(--muted); font-size:13px; margin-top:0;">
                    Freeze a suspicious account, or re-activate it after review.
                </p>
                <label>Account #</label><input id="acctNum" type="text" placeholder="e.g. ACC001">
                <label>New status</label>
                <select id="acctStatus"><option value="ACTIVE">ACTIVE</option><option value="FROZEN">FROZEN</option></select>
                <button id="updateAcctBtn">Update Status</button>
            </div>
        </div>

        <!-- Users -->
        <div class="card">
            <h2>All Users</h2>
            <table id="usersTable">
                <thead>
                <tr><th>ID</th><th>Username</th><th>Full name</th><th>Role</th><th>Status</th></tr>
                </thead>
                <tbody></tbody>
            </table>
        </div>

        <!-- Accounts -->
        <div class="card">
            <h2>All Accounts</h2>
            <table id="acctsTable">
                <thead>
                <tr><th>Account #</th><th>Owner</th><th>Type</th><th>Status</th></tr>
                </thead>
                <tbody></tbody>
            </table>
            <p id="acctsEmpty" class="hidden" style="color:var(--muted); font-size:13px; margin-top:8px;">
                Tip: a new user only appears here once they have at least one account. Use the <em>Create User</em> form's optional account fields, or create accounts later.
            </p>
        </div>

        <!-- Maintenance -->
        <div class="card" style="border-top:3px solid var(--danger);">
            <h2>Maintenance</h2>
            <p style="color:var(--muted); font-size:13px; margin-top:0;">
                Destructive demo-only tools. Resetting wipes <strong>all users, accounts and
                transactions</strong> and restores the original seed data
                (<code>admin</code>, <code>alice</code>, <code>bob</code> with sample accounts).
                You will be logged out and must sign in again with the seed credentials.
            </p>
            <button id="resetBtn" class="danger" style="margin-top:6px;">Reset to seed data</button>
        </div>
    </div>

    <div class="footer">© Pragati Bank — Admin Console</div>

    <script>
        const ctx = '${pageContext.request.contextPath}';
        const $ = (id) => document.getElementById(id);
        const fmt = (n) => Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

        function flash(kind, text) {
            const el = $('globalMsg');
            el.className = 'alert ' + kind;
            el.textContent = text;
            el.classList.remove('hidden');
            if (kind === 'success') setTimeout(() => el.classList.add('hidden'), 4000);
        }

        async function api(method, path, body) {
            const opts = { method, headers: { 'Accept': 'application/json' } };
            if (body !== undefined) {
                opts.headers['Content-Type'] = 'application/json';
                opts.body = JSON.stringify(body);
            }
            const resp = await fetch(ctx + path, opts);
            if (resp.status === 401) { window.location.href = ctx + '/login.jsp'; throw new Error('unauthenticated'); }
            const json = await resp.json().catch(() => ({}));
            if (!resp.ok) throw new Error(json.error || ('HTTP ' + resp.status));
            return json;
        }

        async function loadAll() {
            const me = await api('GET', '/api/auth/me');
            if (me.user.role !== 'ADMIN') { window.location.href = ctx + '/dashboard.jsp'; return; }
            $('userLabel').textContent = me.user.fullName + ' (' + me.user.username + ')';

            const stats = await api('GET', '/api/admin/stats');
            $('kpiUsers').textContent = stats.totalUsers;
            $('kpiAccts').textContent = stats.totalAccounts;
            $('kpiTx').textContent    = stats.totalTransactions;
            $('kpiBal').textContent   = '₹ ' + fmt(stats.systemBalance);

            const users = await api('GET', '/api/users');
            const userMap = {};
            for (const u of users) { userMap[u.id] = u.username + ' (' + (u.fullName || '') + ')'; }
            const utb = $('usersTable').querySelector('tbody');
            utb.innerHTML = '';
            for (const u of users) {
                const tr = document.createElement('tr');
                tr.innerHTML =
                    '<td>' + u.id + '</td>' +
                    '<td>' + u.username + '</td>' +
                    '<td>' + (u.fullName || '') + '</td>' +
                    '<td>' + u.role + '</td>' +
                    '<td>' + u.status + '</td>';
                utb.appendChild(tr);
            }

            const accts = await api('GET', '/api/accounts');
            const atb = $('acctsTable').querySelector('tbody');
            atb.innerHTML = '';
            for (const a of accts) {
                const tr = document.createElement('tr');
                tr.innerHTML =
                    '<td><strong>' + a.accountNumber + '</strong></td>' +
                    '<td>' + (userMap[a.userId] || ('user #' + a.userId)) + '</td>' +
                    '<td>' + a.accountType + '</td>' +
                    '<td>' + a.status + '</td>';
                atb.appendChild(tr);
            }
            $('acctsEmpty').classList.toggle('hidden', accts.length >= users.length);
        }

        $('createUserBtn').addEventListener('click', async () => {
            const body = {
                username: $('nuUser').value.trim(),
                password: $('nuPass').value,
                fullName: $('nuName').value.trim(),
                role:     $('nuRole').value
            };
            const acct = $('nuAcct').value.trim();
            if (acct) { body.accountNumber = acct; body.accountType = $('nuAcctType').value; }
            if (!body.username || !body.password || !body.fullName) {
                flash('error', 'Username, password, and full name are required'); return;
            }
            try {
                await api('POST', '/api/users', body);
                flash('success', acct
                    ? 'User created with account ' + acct
                    : 'User created (no account attached — add one later)');
                ['nuUser','nuPass','nuName','nuAcct'].forEach(id => $(id).value = '');
                // reset password input to hidden state
                const p = $('nuPass'); if (p) p.type = 'password';
                document.querySelectorAll('.pwd-toggle').forEach(b => b.textContent = 'Show');
                loadAll();
            } catch (e) { flash('error', e.message); }
        });

        $('updateAcctBtn').addEventListener('click', async () => {
            const acct = $('acctNum').value.trim();
            if (!acct) { flash('error', 'Account number required'); return; }
            try {
                await api('PUT', '/api/accounts/' + acct + '/status', { status: $('acctStatus').value });
                flash('success', 'Account status updated');
                loadAll();
            } catch (e) { flash('error', e.message); }
        });

        $('logoutBtn').addEventListener('click', async () => {
            try { await api('POST', '/api/auth/logout', {}); } catch (e) {}
            window.location.href = ctx + '/login.jsp';
        });

        $('resetBtn').addEventListener('click', async () => {
            if (!confirm('This will DELETE all users, accounts and transactions, then restore the seed data.\n\nAre you sure you want to continue?')) return;
            const typed = prompt('To confirm, type RESET (in capitals):');
            if (typed !== 'RESET') { flash('error', 'Reset cancelled.'); return; }
            const btn = $('resetBtn');
            btn.disabled = true;
            btn.textContent = 'Resetting...';
            try {
                const resp = await fetch(ctx + '/api/admin/reset', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: '{}'
                });
                const json = await resp.json().catch(() => ({}));
                if (!resp.ok) throw new Error(json.error || ('HTTP ' + resp.status));
                alert(json.message || 'Database reset. Please sign in again.');
                window.location.href = ctx + '/login.jsp';
            } catch (e) {
                flash('error', e.message);
                btn.disabled = false;
                btn.textContent = 'Reset to seed data';
            }
        });

        // Password show/hide toggles
        document.querySelectorAll('.pwd-toggle').forEach(btn => {
            btn.addEventListener('click', () => {
                const input = document.getElementById(btn.dataset.target);
                if (!input) return;
                const showing = input.type === 'text';
                input.type = showing ? 'password' : 'text';
                btn.textContent = showing ? 'Show' : 'Hide';
                btn.setAttribute('aria-label', (showing ? 'Show' : 'Hide') + ' password');
            });
        });

        loadAll().catch(e => flash('error', e.message));
    </script>
</body>
</html>
