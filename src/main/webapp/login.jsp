<%@ page contentType="text/html;charset=UTF-8" language="java" session="false" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Pragati Bank — Sign in</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/pragati.css">
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/assets/pragati-logo.svg">
</head>
<body class="login-page">
    <div class="login-card">
        <div class="login-brand">
            <img src="${pageContext.request.contextPath}/assets/pragati-logo.svg" alt="Pragati Bank">
            <h1>Pragati Bank</h1>
            <div class="tag">आपकी प्रगति, हमारी ज़िम्मेदारी।<br>
                <span style="font-size:11px">Your progress, our responsibility.</span>
            </div>
        </div>

        <div id="message" class="alert error hidden"></div>

        <form id="loginForm" autocomplete="on">
            <label for="username">Username</label>
            <input id="username" name="username" type="text" required autofocus autocomplete="username">

            <label for="password">Password</label>
            <div class="pwd-wrap">
                <input id="password" name="password" type="password" required autocomplete="current-password">
                <button type="button" class="pwd-toggle" data-target="password" aria-label="Show password">Show</button>
            </div>

            <button type="submit" id="loginBtn" style="width:100%; margin-top:18px;">Sign in</button>
        </form>
    </div>

    <script>
        const ctx = '${pageContext.request.contextPath}';
        const form = document.getElementById('loginForm');
        const msg  = document.getElementById('message');
        const btn  = document.getElementById('loginBtn');

        function showError(text) {
            msg.textContent = text;
            msg.classList.remove('hidden');
        }

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            msg.classList.add('hidden');
            btn.disabled = true;
            btn.textContent = 'Signing in...';
            try {
                const resp = await fetch(ctx + '/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        username: document.getElementById('username').value.trim(),
                        password: document.getElementById('password').value
                    })
                });
                if (!resp.ok) {
                    const err = await resp.json().catch(() => ({}));
                    showError(err.error || 'Invalid credentials');
                    return;
                }
                const user = await resp.json();
                window.location.href = ctx + (user.role === 'ADMIN' ? '/admin.jsp' : '/dashboard.jsp');
            } catch (ex) {
                showError('Network error: ' + ex.message);
            } finally {
                btn.disabled = false;
                btn.textContent = 'Sign in';
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
    </script>
</body>
</html>
