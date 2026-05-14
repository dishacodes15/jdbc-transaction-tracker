<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Dashboard — Pragati Bank</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/pragati.css">
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/assets/pragati-logo.svg">
</head>
<body>
    <div class="topbar">
        <div class="brand">
            <img src="${pageContext.request.contextPath}/assets/pragati-logo.svg" alt="">
            <div>
                <div class="brand-name">Pragati Bank</div>
                <div class="brand-tag">आपकी प्रगति, हमारी ज़िम्मेदारी।</div>
            </div>
        </div>
        <div class="user-info">
            <span id="userLabel">…</span>
            <span class="role-badge">USER</span>
            <button class="logout" id="logoutBtn">Logout</button>
        </div>
    </div>

    <div class="container">
        <h1>Welcome, <span id="welcomeName">…</span></h1>
        <div id="globalMsg" class="alert hidden"></div>

        <!-- Accounts -->
        <div class="card">
            <h2>My Accounts</h2>
            <table id="accountsTable">
                <thead>
                <tr><th>Account #</th><th>Type</th><th>Status</th><th>Balance (₹)</th></tr>
                </thead>
                <tbody></tbody>
            </table>
        </div>

        <!-- Operations -->
        <div class="grid-2">
            <div class="card">
                <h2>Deposit / Withdraw</h2>
                <label>Account</label>
                <select id="opAccount"></select>
                <label>Amount (₹)</label>
                <input id="opAmount" type="number" step="0.01" min="0.01" placeholder="e.g. 500.00">
                <label>Description</label>
                <input id="opDesc" type="text" placeholder="e.g. Cash deposit at branch">
                <div style="display:flex; gap:10px;">
                    <button id="depositBtn">Deposit</button>
                    <button id="withdrawBtn" class="secondary">Withdraw</button>
                </div>
            </div>

            <div class="card">
                <h2>Transfer</h2>
                <label>From Account</label>
                <select id="trFrom"></select>
                <label>To Account #</label>
                <input id="trTo" type="text" placeholder="e.g. ACC002">
                <label>Amount (₹)</label>
                <input id="trAmount" type="number" step="0.01" min="0.01">
                <label>Description</label>
                <input id="trDesc" type="text" placeholder="e.g. Rent for May">
                <button id="transferBtn">Send Transfer</button>
            </div>
        </div>

        <!-- Transactions -->
        <div class="card">
            <h2>Recent Transactions</h2>
            <label>Account</label>
            <select id="txAccount" style="max-width:280px;"></select>
            <table id="txTable" style="margin-top:14px;">
                <thead>
                <tr><th>Date</th><th>Type</th><th>Amount (₹)</th><th>Description</th></tr>
                </thead>
                <tbody></tbody>
            </table>
        </div>
    </div>

    <div class="footer">© Pragati Bank — demo project. Saffron, Navy, and Gold of a growing India.</div>

    <script>
        const ctx = '${pageContext.request.contextPath}';
        let accounts = [];

        // --- helpers ---
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
            if (resp.status === 401) {
                window.location.href = ctx + '/login.jsp';
                throw new Error('unauthenticated');
            }
            const json = await resp.json().catch(() => ({}));
            if (!resp.ok) throw new Error(json.error || ('HTTP ' + resp.status));
            return json;
        }

        // --- load dashboard ---
        async function loadDashboard() {
            const data = await api('GET', '/api/auth/me');
            $('userLabel').textContent  = data.user.fullName + ' (' + data.user.username + ')';
            $('welcomeName').textContent = data.user.fullName;
            accounts = data.accounts || [];
            renderAccounts();
            populateAccountSelects();
            if (accounts.length > 0) loadTransactions(accounts[0].accountNumber);
        }

        function renderAccounts() {
            const tbody = $('accountsTable').querySelector('tbody');
            tbody.innerHTML = '';
            if (accounts.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4">No accounts yet.</td></tr>';
                return;
            }
            for (const a of accounts) {
                const tr = document.createElement('tr');
                tr.innerHTML =
                    '<td><strong>' + a.accountNumber + '</strong></td>' +
                    '<td>' + a.accountType + '</td>' +
                    '<td>' + a.status + '</td>' +
                    '<td>₹ ' + fmt(a.balance) + '</td>';
                tbody.appendChild(tr);
            }
        }

        function populateAccountSelects() {
            for (const sel of ['opAccount', 'trFrom', 'txAccount']) {
                const el = $(sel);
                el.innerHTML = '';
                for (const a of accounts) {
                    const o = document.createElement('option');
                    o.value = a.accountNumber;
                    o.textContent = a.accountNumber + ' — ' + a.accountType;
                    el.appendChild(o);
                }
            }
            $('txAccount').addEventListener('change', e => loadTransactions(e.target.value));
        }

        async function loadTransactions(acct) {
            try {
                const list = await api('GET', '/api/transactions/account/' + encodeURIComponent(acct));
                const tbody = $('txTable').querySelector('tbody');
                tbody.innerHTML = '';
                if (list.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="4">No transactions.</td></tr>';
                    return;
                }
                for (const t of list.slice().reverse()) {
                    const cls = t.transactionType === 'CREDIT' ? 'amt-credit' : 'amt-debit';
                    const sign = t.transactionType === 'CREDIT' ? '+' : '−';
                    const tr = document.createElement('tr');
                    tr.innerHTML =
                        '<td>' + (t.createdAt || '') + '</td>' +
                        '<td>' + t.transactionType + '</td>' +
                        '<td class="' + cls + '">' + sign + ' ₹ ' + fmt(t.amount) + '</td>' +
                        '<td>' + (t.description || '') + '</td>';
                    tbody.appendChild(tr);
                }
            } catch (e) { flash('error', e.message); }
        }

        async function doOp(kind) {
            const acct = $('opAccount').value;
            const amount = $('opAmount').value;
            const desc = $('opDesc').value;
            if (!acct || !amount) { flash('error', 'Account and amount required'); return; }
            try {
                await api('POST', '/api/accounts/' + acct + '/' + kind,
                    { amount: amount, description: desc });
                flash('success', kind === 'deposit' ? 'Deposit successful' : 'Withdrawal successful');
                $('opAmount').value = ''; $('opDesc').value = '';
                await loadDashboard();
                loadTransactions(acct);
            } catch (e) { flash('error', e.message); }
        }

        $('depositBtn').addEventListener('click',  () => doOp('deposit'));
        $('withdrawBtn').addEventListener('click', () => doOp('withdraw'));

        $('transferBtn').addEventListener('click', async () => {
            const body = {
                fromAccount: $('trFrom').value,
                toAccount:   $('trTo').value.trim(),
                amount:      $('trAmount').value,
                description: $('trDesc').value
            };
            if (!body.fromAccount || !body.toAccount || !body.amount) {
                flash('error', 'From, To and amount are required'); return;
            }
            try {
                await api('POST', '/api/transfers', body);
                flash('success', 'Transfer completed');
                $('trTo').value = ''; $('trAmount').value = ''; $('trDesc').value = '';
                await loadDashboard();
                loadTransactions(body.fromAccount);
            } catch (e) { flash('error', e.message); }
        });

        $('logoutBtn').addEventListener('click', async () => {
            try { await api('POST', '/api/auth/logout', {}); } catch (e) {}
            window.location.href = ctx + '/login.jsp';
        });

        loadDashboard().catch(e => flash('error', e.message));
    </script>
</body>
</html>
