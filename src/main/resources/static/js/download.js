/* ============================================
   SafeShare — Public Download Page Module
   Validates link, password prompt, preview, download
   ============================================ */

document.addEventListener('DOMContentLoaded', () => {
    initSharePage();
});

async function initSharePage() {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');

    if (!token) {
        showShareStatus('error', 'Invalid Link', 'No share token provided.');
        return;
    }

    // Store token for later use
    window.shareToken = token;

    try {
        const response = await fetch(`/public/s/${token}`);
        const data = await response.json();

        handleLinkStatus(data, token);
    } catch (error) {
        showShareStatus('error', 'Error', 'Failed to validate this link. Please try again.');
    }
}

function handleLinkStatus(data, token) {
    const statusSection = document.getElementById('statusSection');
    const passwordSection = document.getElementById('passwordSection');
    const contentSection = document.getElementById('contentSection');

    // Hide all sections first
    if (statusSection) statusSection.classList.add('hidden');
    if (passwordSection) passwordSection.classList.add('hidden');
    if (contentSection) contentSection.classList.add('hidden');

    switch (data.status) {
        case 'OK':
            showContent(data, token);
            break;

        case 'NEEDS_PASSWORD':
            showPasswordPrompt(data, token);
            break;

        case 'EXPIRED':
            showShareStatus('warning', 'Link Expired', data.message);
            break;

        case 'REVOKED':
            showShareStatus('error', 'Link Revoked', data.message);
            break;

        case 'LIMIT_REACHED':
            showShareStatus('warning', 'Limit Reached', data.message);
            break;

        case 'NOT_FOUND':
            showShareStatus('error', 'Not Found', data.message);
            break;

        default:
            showShareStatus('error', 'Error', data.message || 'Something went wrong');
    }
}

function showShareStatus(type, title, message) {
    const section = document.getElementById('statusSection');
    if (!section) return;

    section.classList.remove('hidden');
    document.getElementById('passwordSection')?.classList.add('hidden');
    document.getElementById('contentSection')?.classList.add('hidden');

    const iconMap = { error: '✕', warning: '⚠', success: '✓' };

    section.innerHTML = `
        <div class="share-status">
            <div class="status-icon ${type}">${iconMap[type] || '?'}</div>
            <h3>${title}</h3>
            <p>${message}</p>
        </div>
    `;
}

function showPasswordPrompt(data, token) {
    const section = document.getElementById('passwordSection');
    if (!section) return;

    section.classList.remove('hidden');
    document.getElementById('statusSection')?.classList.add('hidden');
    document.getElementById('contentSection')?.classList.add('hidden');

    // Set file name
    const fileNameEl = document.getElementById('protectedFileName');
    if (fileNameEl) fileNameEl.textContent = data.fileName || 'Protected File';

    // Setup password form
    const form = document.getElementById('passwordForm');
    const errorEl = document.getElementById('passwordError');

    if (form) {
        form.onsubmit = async (e) => {
            e.preventDefault();

            const passwordInput = document.getElementById('linkPassword');
            const password = passwordInput?.value;
            const submitBtn = form.querySelector('button[type="submit"]');

            if (!password) {
                if (errorEl) {
                    errorEl.textContent = 'Please enter a password';
                    errorEl.classList.add('visible');
                }
                return;
            }

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="spinner"></span> Verifying...';
            if (errorEl) errorEl.classList.remove('visible');

            try {
                const response = await fetch(`/public/s/${token}/verify`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ password })
                });

                const result = await response.json();

                if (response.ok && result.status === 'OK') {
                    // Password correct — show content
                    showContent(result, token);
                } else {
                    if (errorEl) {
                        errorEl.textContent = result.message || 'Incorrect password';
                        errorEl.classList.add('visible');
                    }
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Unlock';
                }
            } catch (error) {
                if (errorEl) {
                    errorEl.textContent = error.message || 'Verification failed';
                    errorEl.classList.add('visible');
                }
                submitBtn.disabled = false;
                submitBtn.textContent = 'Unlock';
            }
        };
    }
}

function showContent(data, token) {
    const section = document.getElementById('contentSection');
    if (!section) return;

    section.classList.remove('hidden');
    document.getElementById('statusSection')?.classList.add('hidden');
    document.getElementById('passwordSection')?.classList.add('hidden');

    // File name
    const fileNameEl = document.getElementById('fileName');
    if (fileNameEl) fileNameEl.textContent = data.fileName || 'File';

    // Preview
    const previewArea = document.getElementById('previewArea');
    const fileType = (data.fileType || '').toLowerCase();

    if (previewArea) {
        switch (fileType) {
            case 'pdf':
                previewArea.innerHTML = renderDocumentPreview(`/public/s/${token}/preview`, 'PDF Preview');
                previewArea.classList.remove('hidden');
                break;

            case 'jpg':
            case 'jpeg':
            case 'png':
                previewArea.innerHTML = renderImagePreview(`/public/s/${token}/preview`, 'Image Preview');
                previewArea.classList.remove('hidden');
                break;

            case 'docx':
                previewArea.innerHTML = renderDocumentPreview(`/public/s/${token}/preview`, 'DOCX Preview');
                previewArea.classList.remove('hidden');
                break;

            case 'xls':
            case 'xlsx':
                previewArea.innerHTML = renderDocumentPreview(`/public/s/${token}/preview`, 'Excel Preview');
                previewArea.classList.remove('hidden');
                break;

            case 'zip':
                previewArea.innerHTML = `<div class="no-preview">Preview not available for this file type — please download to view.</div>`;
                previewArea.classList.remove('hidden');
                break;

            default:
                previewArea.innerHTML = `<div class="no-preview">Preview not available for this file type — please download to view.</div>`;
                previewArea.classList.remove('hidden');
        }
    }

    // Download button
    const downloadBtn = document.getElementById('downloadBtn');
    if (downloadBtn) {
        downloadBtn.onclick = () => downloadFile(token);
    }
}

function renderDocumentPreview(src, title) {
    return `
        <div class="preview-toolbar">
            <span>${title}</span>
            <button class="btn btn-secondary btn-sm" type="button" onclick="togglePreviewFullscreen()">
                Full screen preview
            </button>
        </div>
        <iframe src="${src}" title="${title}"></iframe>
    `;
}

function renderImagePreview(src, title) {
    return `
        <div class="preview-toolbar">
            <span>${title}</span>
            <button class="btn btn-secondary btn-sm" type="button" onclick="togglePreviewFullscreen()">
                Full screen preview
            </button>
        </div>
        <div class="image-preview-frame">
            <img src="${src}" alt="${title}">
        </div>
    `;
}

function togglePreviewFullscreen() {
    const previewArea = document.getElementById('previewArea');
    if (!previewArea) return;

    const isFullscreen = previewArea.classList.toggle('preview-fullscreen');
    document.body.classList.toggle('preview-lock', isFullscreen);

    const button = previewArea.querySelector('.preview-toolbar button');
    if (button) {
        button.textContent = isFullscreen ? 'Exit full screen' : 'Full screen preview';
    }
}

document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
        const previewArea = document.getElementById('previewArea');
        if (previewArea?.classList.contains('preview-fullscreen')) {
            togglePreviewFullscreen();
        }
    }
});

async function downloadFile(token) {
    const btn = document.getElementById('downloadBtn');
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span> Downloading...';
    }

    try {
        const response = await fetch(`/public/s/${token}/download`);

        if (!response.ok) {
            const errorData = await response.json();
            showShareStatus('error', 'Download Failed', errorData.message || 'Could not download file');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = '⬇ Download File';
            }
            return;
        }

        // Get filename from Content-Disposition header
        const disposition = response.headers.get('Content-Disposition');
        let filename = 'download';
        if (disposition) {
            const match = disposition.match(/filename="?([^"]+)"?/);
            if (match) filename = match[1];
        }

        // Create blob and trigger download
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '⬇ Download File';
        }

    } catch (error) {
        showShareStatus('error', 'Download Failed', 'Network error. Please try again.');
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '⬇ Download File';
        }
    }
}
