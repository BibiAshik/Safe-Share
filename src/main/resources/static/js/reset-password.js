document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('resetPasswordForm');
    const alert = document.getElementById('resetAlert');
    const loginLink = document.getElementById('loginAfterReset');
    const token = new URLSearchParams(window.location.search).get('token');

    if (!token) {
        showModalAlert(alert, 'This password reset link is invalid or has expired.', 'error');
        if (form) form.classList.add('hidden');
        return;
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const submitBtn = form.querySelector('button[type="submit"]');
        const password = document.getElementById('newPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        if (password.length < 6) {
            showModalAlert(alert, 'Password must be at least 6 characters', 'error');
            return;
        }

        if (password !== confirmPassword) {
            showModalAlert(alert, 'Passwords do not match', 'error');
            return;
        }

        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner"></span> Updating...';

        try {
            const data = await apiPost('/api/auth/reset-password', { token, password });
            form.classList.add('hidden');
            loginLink.classList.remove('hidden');
            showModalAlert(alert, data.message || 'Password updated successfully. You can now login to SafeShare.', 'success');
        } catch (error) {
            showModalAlert(alert, error.message || 'This password reset link is invalid or has expired.', 'error');
            submitBtn.disabled = false;
            submitBtn.textContent = 'Update Password';
        }
    });
});
