<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Banking Transaction Analyzer</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 24px 16px;
        }

        .container {
            background: #fff;
            border-radius: 12px;
            box-shadow: 0 20px 60px rgba(0,0,0,.3);
            width: 100%;
            max-width: 1300px;
            margin: 0 auto;
            overflow: hidden;
        }

        /* ── Header ─────────────────────────────────────────── */
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #fff;
            padding: 20px 28px;
            text-align: center;
        }
        .header h1 { font-size: 22px; font-weight: 600; letter-spacing: .4px; }

        /* ── Content ─────────────────────────────────────────── */
        .content { padding: 24px 28px; }

        /* ── Status message ──────────────────────────────────── */
        .status-message { padding: 11px 14px; border-radius: 6px; margin-bottom: 16px; font-size: 14px; display: none; }
        .status-message.success { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; display: block; }
        .status-message.error   { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; display: block; }

        /* ── Account stats ───────────────────────────────────── */
        .account-stats {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 12px;
            margin-bottom: 20px;
            padding: 16px;
            background: #f4f4fb;
            border-radius: 8px;
            border: 1px solid #e0e0f0;
        }
        .stat { text-align: center; }
        .stat-label { font-size: 11px; color: #666; font-weight: 600; text-transform: uppercase; letter-spacing: .5px; margin-bottom: 4px; }
        .stat-value { font-size: 22px; font-weight: 700; color: #333; }
        .stat-value.credit-val { color: #1a7f3c; }
        .stat-value.debit-val  { color: #c0392b; }

        /* ── Form ────────────────────────────────────────────── */
        .form-section { margin-bottom: 20px; }
        .form-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 14px;
            align-items: end;
        }
        .form-field label { display: block; font-size: 13px; font-weight: 600; color: #444; margin-bottom: 5px; }
        .form-field input,
        .form-field select {
            width: 100%;
            padding: 9px 11px;
            border: 1px solid #ccc;
            border-radius: 6px;
            font-size: 13px;
            font-family: inherit;
            transition: border-color .2s, box-shadow .2s;
        }
        .form-field input:focus,
        .form-field select:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 3px rgba(102,126,234,.15);
        }
        .radio-group { display: flex; gap: 16px; padding-top: 6px; }
        .radio-option { display: flex; align-items: center; gap: 6px; cursor: pointer; font-size: 13px; font-weight: 500; }
        .radio-option input[type="radio"] { width: 16px; height: 16px; cursor: pointer; }

        /* ── Buttons ─────────────────────────────────────────── */
        .button-row { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 20px; }
        button {
            padding: 9px 18px;
            border: none;
            border-radius: 6px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            transition: all .2s;
            letter-spacing: .3px;
        }
        .btn-primary {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #fff;
        }
        .btn-primary:hover { opacity: .88; transform: translateY(-1px); }
        .btn-secondary { background: #f0f0f0; color: #333; border: 1px solid #ddd; }
        .btn-secondary:hover { background: #e4e4e4; }
        .btn-secondary.active { background: #667eea; color: #fff; border-color: #667eea; }

        /* ── Table wrapper ───────────────────────────────────── */
        .table-wrapper {
            overflow-x: auto;
            border: 1px solid #e0e0e0;
            border-radius: 8px;
        }

        /* ── Transaction table ───────────────────────────────── */
        .tx-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 13px;
        }
        .tx-table thead {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #fff;
            position: sticky;
            top: 0;
            z-index: 1;
        }
        .tx-table th {
            padding: 12px 14px;
            text-align: left;
            font-weight: 600;
            white-space: nowrap;
            letter-spacing: .3px;
        }
        .tx-table th.sortable {
            cursor: pointer;
            user-select: none;
        }
        .tx-table th.sortable:hover { background: rgba(255,255,255,.15); }
        .sort-icon { margin-left: 5px; font-size: 11px; opacity: .75; }
        .tx-table tbody tr { transition: background .15s; }
        .tx-table tbody tr:nth-child(even) { background: #fafafa; }
        .tx-table tbody tr:hover { background: #f0f0fb; }
        .tx-table td {
            padding: 11px 14px;
            border-bottom: 1px solid #ebebeb;
            vertical-align: middle;
            white-space: nowrap;
        }
        .tx-table tbody tr:last-child td { border-bottom: none; }

        /* ── Type badge ──────────────────────────────────────── */
        .badge {
            display: inline-block;
            padding: 3px 9px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: 700;
            letter-spacing: .4px;
            text-transform: uppercase;
        }
        .badge.credit { background: #d4edda; color: #155724; }
        .badge.debit  { background: #f8d7da; color: #721c24; }

        /* ── Amount cells ────────────────────────────────────── */
        .amount-credit { color: #1a7f3c; font-weight: 600; }
        .amount-debit  { color: #c0392b; font-weight: 600; }

        /* ── Empty state ─────────────────────────────────────── */
        .empty-row td {
            text-align: center;
            padding: 40px;
            color: #999;
            font-size: 14px;
        }

        /* ── Responsive ──────────────────────────────────────── */
        @media (max-width: 700px) {
            .content { padding: 16px; }
            .account-stats { grid-template-columns: 1fr; }
            .form-grid { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
<div class="container">

    <div class="header">
        <h1>Banking Transaction Analyzer</h1>
    </div>

    <div class="content">

        <!-- Status message -->
        <div id="statusMessage" class="status-message"></div>

        <!-- Account statistics -->
        <div id="accountStats" class="account-stats" style="display:none;">
            <div class="stat">
                <div class="stat-label">Total Credits</div>
                <div class="stat-value credit-val" id="totalCredits">₹0.00</div>
            </div>
            <div class="stat">
                <div class="stat-label">Total Debits</div>
                <div class="stat-value debit-val" id="totalDebits">₹0.00</div>
            </div>
            <div class="stat">
                <div class="stat-label">Balance</div>
                <div class="stat-value" id="balance">₹0.00</div>
            </div>
        </div>

        <!-- Input form -->
        <div class="form-section">
            <div class="form-grid">
                <div class="form-field">
                    <label for="accountNumber">Account Number</label>
                    <input type="text" id="accountNumber" placeholder="e.g. ACC001" value="ACC001">
                </div>
                <div class="form-field">
                    <label>Type</label>
                    <div class="radio-group">
                        <label class="radio-option">
                            <input type="radio" name="transactionType" value="CREDIT" checked> Credit
                        </label>
                        <label class="radio-option">
                            <input type="radio" name="transactionType" value="DEBIT"> Debit
                        </label>
                    </div>
                </div>
                <div class="form-field">
                    <label for="amount">Amount</label>
                    <input type="number" id="amount" placeholder="0.00" step="0.01" min="0">
                </div>
                <div class="form-field">
                    <label for="transactionDate">Date</label>
                    <input type="date" id="transactionDate">
                </div>
                <div class="form-field">
                    <label for="transactionTime">Time</label>
                    <input type="time" id="transactionTime">
                </div>
                <div class="form-field">
                    <label for="description">Description</label>
                    <input type="text" id="description" placeholder="e.g. Salary">
                </div>
            </div>
        </div>

        <!-- Action buttons -->
        <div class="button-row">
            <button class="btn-primary" onclick="addTransaction()">Add Transaction</button>
            <button class="btn-secondary" onclick="showLargeTransactions()">Show &gt; ₹10 000</button>
            <button class="btn-secondary" id="sortBtn" onclick="cycleSortByAmount()">Sort by Amount ↕</button>
            <button class="btn-secondary" onclick="resetView()">Reset</button>
        </div>

        <!-- Transaction table -->
        <div class="table-wrapper">
            <table class="tx-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Account</th>
                        <th>Type</th>
                        <th class="sortable" id="amountHeader" onclick="cycleSortByAmount()">
                            Amount <span class="sort-icon" id="amountSortIcon">↕</span>
                        </th>
                        <th>Date &amp; Time</th>
                        <th>Description</th>
                        <th>Balance After</th>
                        <th>Created At</th>
                    </tr>
                </thead>
                <tbody id="txBody"></tbody>
            </table>
        </div>

    </div><!-- /.content -->
</div><!-- /.container -->

<script>
    /* ── State ─────────────────────────────────────────────── */
    var transactions   = [];          // full dataset from server
    var currentAccount = 'ACC001';
    var sortDir        = 'none';      // 'none' | 'asc' | 'desc'

    /* ── Init ──────────────────────────────────────────────── */
    window.addEventListener('DOMContentLoaded', function () {
        var today = new Date();
        document.getElementById('transactionDate').valueAsDate = today;
        document.getElementById('transactionTime').value =
            pad(today.getHours()) + ':' + pad(today.getMinutes());
        currentAccount = document.getElementById('accountNumber').value;
        loadTransactions();
    });

    document.getElementById('accountNumber').addEventListener('change', function () {
        currentAccount = this.value;
        sortDir = 'none';
        updateSortUI();
        loadTransactions();
    });

    /* ── Helpers ───────────────────────────────────────────── */
    function pad(n) { return String(n).padStart(2, '0'); }

    function formatCurrency(v) {
        return '₹' + parseFloat(v).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    }

    function formatDateTime(s) {
        if (!s) return '—';
        // Server sends "yyyy-MM-dd HH:mm:ss"
        var parts = s.split(' ');
        return parts[0] + ' ' + (parts[1] ? parts[1].substring(0, 5) : '');
    }

    function setStatus(msg, type) {
        var el = document.getElementById('statusMessage');
        el.textContent = msg;
        el.className = 'status-message ' + type;
        setTimeout(function () { el.className = 'status-message'; }, 3500);
    }

    /* ── Load transactions ─────────────────────────────────── */
    function loadTransactions() {
        fetch('/transaction-analyzer/api/transactions/account/' + encodeURIComponent(currentAccount))
            .then(function (r) { return r.json(); })
            .then(function (data) {
                transactions = data;
                updateStats();
                renderTable(applySortDir(transactions));
            })
            .catch(function (err) { setStatus('Error loading transactions: ' + err.message, 'error'); });
    }

    /* ── Stats ─────────────────────────────────────────────── */
    function updateStats() {
        var credits = 0, debits = 0;
        transactions.forEach(function (t) {
            if (t.transactionType === 'CREDIT') credits += parseFloat(t.amount);
            else                                 debits  += parseFloat(t.amount);
        });
        var bal = credits - debits;
        document.getElementById('totalCredits').textContent = formatCurrency(credits);
        document.getElementById('totalDebits').textContent  = formatCurrency(debits);
        var balEl = document.getElementById('balance');
        balEl.textContent = formatCurrency(bal);
        balEl.style.color = bal >= 0 ? '#1a7f3c' : '#c0392b';
        document.getElementById('accountStats').style.display = 'grid';
    }

    /* ── Render table ──────────────────────────────────────── */
    function renderTable(rows) {
        var tbody = document.getElementById('txBody');
        if (!rows || rows.length === 0) {
            tbody.innerHTML =
                '<tr class="empty-row"><td colspan="8">No transactions found for account <strong>' +
                currentAccount + '</strong></td></tr>';
            return;
        }
        tbody.innerHTML = rows.map(function (t) {
            var isCredit = t.transactionType === 'CREDIT';
            var amtClass = isCredit ? 'amount-credit' : 'amount-debit';
            var amtSign  = isCredit ? '+' : '-';
            return '<tr>' +
                '<td>' + (t.id || '—') + '</td>' +
                '<td>' + escapeHtml(t.accountNumber) + '</td>' +
                '<td><span class="badge ' + (isCredit ? 'credit' : 'debit') + '">' +
                    t.transactionType + '</span></td>' +
                '<td class="' + amtClass + '">' + amtSign + formatCurrency(t.amount) + '</td>' +
                '<td>' + formatDateTime(t.transactionDate) + '</td>' +
                '<td>' + escapeHtml(t.description || '—') + '</td>' +
                '<td>' + formatCurrency(t.balanceAfter) + '</td>' +
                '<td>' + formatDateTime(t.createdAt) + '</td>' +
            '</tr>';
        }).join('');
    }

    /* ── Sorting ───────────────────────────────────────────── */
    function applySortDir(arr) {
        if (sortDir === 'none') return arr;
        var sorted = arr.slice().sort(function (a, b) {
            return parseFloat(a.amount) - parseFloat(b.amount);
        });
        return sortDir === 'desc' ? sorted.reverse() : sorted;
    }

    function cycleSortByAmount() {
        sortDir = sortDir === 'none' ? 'asc' : sortDir === 'asc' ? 'desc' : 'none';
        updateSortUI();
        renderTable(applySortDir(transactions));
    }

    function updateSortUI() {
        var icon   = { none: '↕', asc: '▲', desc: '▼' }[sortDir];
        var btn    = document.getElementById('sortBtn');
        document.getElementById('amountSortIcon').textContent = icon;
        btn.textContent = 'Sort by Amount ' + icon;
        btn.className   = sortDir === 'none' ? 'btn-secondary' : 'btn-secondary active';
    }

    /* ── Filter: large transactions ────────────────────────── */
    function showLargeTransactions() {
        var large = transactions.filter(function (t) { return parseFloat(t.amount) > 10000; });
        if (large.length === 0) {
            setStatus('No transactions greater than \u20b910,000', 'error');
        } else {
            renderTable(applySortDir(large));
        }
    }

    /* ── Reset ─────────────────────────────────────────────── */
    function resetView() {
        sortDir = 'none';
        updateSortUI();
        renderTable(transactions);
    }

    /* ── Add transaction ───────────────────────────────────── */
    function addTransaction() {
        var amount      = parseFloat(document.getElementById('amount').value);
        var type        = document.querySelector('input[name="transactionType"]:checked').value;
        var accountNum  = document.getElementById('accountNumber').value.trim();
        var description = document.getElementById('description').value.trim();
        var dateStr     = document.getElementById('transactionDate').value;
        var timeStr     = document.getElementById('transactionTime').value;

        if (!amount || amount <= 0) { setStatus('Please enter a valid amount.', 'error'); return; }
        if (!accountNum)            { setStatus('Please enter an account number.', 'error'); return; }

        // Backend expects "yyyy-MM-dd HH:mm:ss"
        var dateTime = dateStr + ' ' + (timeStr.length === 5 ? timeStr + ':00' : timeStr);

        var credits = 0, debits = 0;
        transactions.forEach(function (t) {
            if (t.transactionType === 'CREDIT') credits += parseFloat(t.amount);
            else                                 debits  += parseFloat(t.amount);
        });
        var currentBalance = credits - debits;
        var balanceAfter   = type === 'CREDIT' ? currentBalance + amount : currentBalance - amount;

        var payload = {
            accountNumber:   accountNum,
            transactionType: type,
            amount:          amount,
            transactionDate: dateTime,
            description:     description || 'Transaction',
            balanceAfter:    balanceAfter
        };

        fetch('/transaction-analyzer/api/transactions', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(payload)
        })
        .then(function (r) {
            if (r.ok) {
                setStatus('Transaction added successfully!', 'success');
                document.getElementById('amount').value      = '';
                document.getElementById('description').value = '';
                currentAccount = accountNum;
                loadTransactions();
            } else {
                r.json().then(function (e) { setStatus('Failed: ' + (e.error || 'unknown error'), 'error'); });
            }
        })
        .catch(function (err) { setStatus('Network error: ' + err.message, 'error'); });
    }

    /* ── Security helper ───────────────────────────────────── */
    function escapeHtml(str) {
        if (str == null) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
</script>
</body>
</html>
