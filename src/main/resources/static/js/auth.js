/* ============================================
   SafeShare — Auth Module
   Register/Login modals, Google OAuth, Logout
   ============================================ */

document.addEventListener('DOMContentLoaded', () => {
    // If on index.html and already authenticated, redirect to dashboard
    if (window.location.pathname === '/' || window.location.pathname === '/index.html') {
        if (isAuthenticated()) {
            window.location.href = '/dashboard.html';
            return;
        }
    }

    setupAuthModals();
});

function setupAuthModals() {
    const registerBtn = document.getElementById('registerBtn');
    const registerHeroBtn = document.getElementById('registerHeroBtn');
    const loginBtn = document.getElementById('loginBtn');
    const registerModal = document.getElementById('registerModal');
    const loginModal = document.getElementById('loginModal');
    const forgotPasswordModal = document.getElementById('forgotPasswordModal');

    if (registerBtn && registerModal) {
        registerBtn.addEventListener('click', () => openModal(registerModal));
    }

    if (registerHeroBtn && registerModal) {
        registerHeroBtn.addEventListener('click', () => openModal(registerModal));
    }

    if (loginBtn && loginModal) {
        loginBtn.addEventListener('click', () => openModal(loginModal));
    }

    const forgotPasswordBtn = document.getElementById('forgotPasswordBtn');
    if (forgotPasswordBtn && forgotPasswordModal) {
        forgotPasswordBtn.addEventListener('click', () => {
            const email = document.getElementById('loginEmail')?.value.trim();
            const forgotEmail = document.getElementById('forgotEmail');
            if (forgotEmail && email) {
                forgotEmail.value = email;
            }
            openModal(forgotPasswordModal);
        });
    }

    // Close buttons
    document.querySelectorAll('.modal-close').forEach(btn => {
        btn.addEventListener('click', () => {
            const overlay = btn.closest('.modal-overlay');
            if (overlay) closeModal(overlay);
        });
    });

    // Click outside to close
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) closeModal(overlay);
        });
    });

    // Register form
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', handleRegister);
    }

    // Login form
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }

    const forgotPasswordForm = document.getElementById('forgotPasswordForm');
    if (forgotPasswordForm) {
        forgotPasswordForm.addEventListener('submit', handleForgotPassword);
    }

    // Google login button
    const googleBtn = document.getElementById('googleLoginBtn');
    if (googleBtn) {
        googleBtn.addEventListener('click', handleGoogleLogin);
    }

    const googleModalBtn = document.getElementById('googleLoginModalBtn');
    if (googleModalBtn) {
        googleModalBtn.addEventListener('click', handleGoogleLogin);
    }
}

function openModal(overlay) {
    // Close any open modals first
    document.querySelectorAll('.modal-overlay.active').forEach(m => closeModal(m));

    overlay.classList.add('active');
    document.body.style.overflow = 'hidden';

    // Clear previous errors
    const alert = overlay.querySelector('.modal-alert');
    if (alert) {
        alert.className = 'modal-alert';
        alert.textContent = '';
    }
}

function closeModal(overlay) {
    overlay.classList.remove('active');
    document.body.style.overflow = '';
}

async function handleRegister(e) {
    e.preventDefault();

    const form = e.target;
    const alert = form.closest('.modal').querySelector('.modal-alert') || form.querySelector('.modal-alert');
    const submitBtn = form.querySelector('button[type="submit"]');

    const name = form.querySelector('#regName').value.trim();
    const email = form.querySelector('#regEmail').value.trim();
    const password = form.querySelector('#regPassword').value;

    if (!name || !email || !password) {
        showModalAlert(alert, 'Please fill in all fields', 'error');
        return;
    }

    if (password.length < 6) {
        showModalAlert(alert, 'Password must be at least 6 characters', 'error');
        return;
    }

    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner"></span> Creating account...';

    try {
        const data = await apiPost('/api/auth/register', { name, email, password });

        form.reset();
        showModalAlert(alert, data.message || 'Registration successful. Please login to continue.', 'success');
    } catch (error) {
        showModalAlert(alert, error.message || 'Registration failed', 'error');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Create Account';
    }
}

async function handleLogin(e) {
    e.preventDefault();

    const form = e.target;
    const alert = form.closest('.modal').querySelector('.modal-alert') || form.querySelector('.modal-alert');
    const submitBtn = form.querySelector('button[type="submit"]');

    const email = form.querySelector('#loginEmail').value.trim();
    const password = form.querySelector('#loginPassword').value;

    if (!email || !password) {
        showModalAlert(alert, 'Please fill in all fields', 'error');
        return;
    }

    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner"></span> Logging in...';

    try {
        const data = await apiPost('/api/auth/login', { email, password });

        localStorage.setItem('safeshare_token', data.token);
        localStorage.setItem('safeshare_user', JSON.stringify({ name: data.name, email: data.email }));

        window.location.href = '/dashboard.html';
    } catch (error) {
        showModalAlert(alert, error.message || 'Login failed', 'error');
        submitBtn.disabled = false;
        submitBtn.textContent = 'Login';
    }
}

async function handleForgotPassword(e) {
    e.preventDefault();

    const form = e.target;
    const alert = form.closest('.modal').querySelector('.modal-alert');
    const submitBtn = form.querySelector('button[type="submit"]');
    const email = form.querySelector('#forgotEmail').value.trim();

    if (!email) {
        showModalAlert(alert, 'Please enter your email address', 'error');
        return;
    }

    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner"></span> Sending...';

    try {
        const data = await apiPost('/api/auth/forgot-password', { email });
        form.reset();
        showModalAlert(alert, data.message || 'If that email exists, a password reset link has been sent.', 'success');
    } catch (error) {
        showModalAlert(alert, error.message || 'Unable to send reset link', 'error');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Send Reset Link';
    }
}

function handleGoogleLogin() {
    // Redirect to Spring Security's OAuth2 authorization endpoint
    window.location.href = '/oauth2/authorization/google';
}

function showModalAlert(alertEl, message, type) {
    if (!alertEl) return;
    alertEl.className = `modal-alert ${type}`;
    alertEl.textContent = message;
}

/**
 * Logout — clear client-side token and redirect to landing page.
 */
function logout() {
    localStorage.removeItem('safeshare_token');
    localStorage.removeItem('safeshare_user');
    window.location.href = '/index.html';
}
