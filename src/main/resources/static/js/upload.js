/* ============================================
   SafeShare — File Upload Module
   Upload with drag-and-drop & progress tracking
   ============================================ */

const ALLOWED_TYPES = ['pdf', 'jpg', 'jpeg', 'png', 'docx', 'xls', 'xlsx', 'zip'];
const MAX_FILE_SIZE = 25 * 1024 * 1024; // 25 MB

document.addEventListener('DOMContentLoaded', () => {
    setupUpload();
});

function setupUpload() {
    const uploadArea = document.getElementById('uploadArea');
    const fileInput = document.getElementById('fileInput');

    if (!uploadArea || !fileInput) return;

    // Click to open file picker
    uploadArea.addEventListener('click', () => fileInput.click());

    // File selection
    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            handleFileUpload(e.target.files[0]);
        }
    });

    // Drag and drop
    uploadArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadArea.classList.add('dragover');
    });

    uploadArea.addEventListener('dragleave', () => {
        uploadArea.classList.remove('dragover');
    });

    uploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('dragover');
        if (e.dataTransfer.files.length > 0) {
            handleFileUpload(e.dataTransfer.files[0]);
        }
    });
}

function validateFile(file) {
    const ext = file.name.split('.').pop().toLowerCase();

    if (!ALLOWED_TYPES.includes(ext)) {
        showToast(`Invalid file type (.${ext}). Allowed: PDF, JPG, PNG, DOCX, XLS, XLSX, ZIP`, 'error');
        return false;
    }

    if (file.size > MAX_FILE_SIZE) {
        showToast(`File too large (${formatSize(file.size)}). Maximum: 25 MB`, 'error');
        return false;
    }

    return true;
}

async function handleFileUpload(file) {
    if (!validateFile(file)) return;

    const progressContainer = document.getElementById('uploadProgress');
    const progressFill = document.getElementById('progressFill');
    const progressText = document.getElementById('progressText');
    const uploadArea = document.getElementById('uploadArea');

    if (progressContainer) progressContainer.classList.add('active');
    if (uploadArea) uploadArea.style.pointerEvents = 'none';

    const formData = new FormData();
    formData.append('file', file);

    try {
        const result = await apiUpload('/api/files/upload', formData, (percent) => {
            if (progressFill) progressFill.style.width = percent + '%';
            if (progressText) progressText.textContent = `Uploading... ${percent}%`;
        });

        showToast('File uploaded successfully!', 'success');

        // Reset upload area
        if (progressFill) progressFill.style.width = '0%';
        if (progressContainer) progressContainer.classList.remove('active');
        if (uploadArea) uploadArea.style.pointerEvents = '';

        // Reset file input
        const fileInput = document.getElementById('fileInput');
        if (fileInput) fileInput.value = '';

        // Refresh file list
        if (typeof loadFiles === 'function') {
            loadFiles();
        }
    } catch (error) {
        showToast(error.message || 'Upload failed', 'error');
        if (progressFill) progressFill.style.width = '0%';
        if (progressContainer) progressContainer.classList.remove('active');
        if (uploadArea) uploadArea.style.pointerEvents = '';
    }
}

/**
 * Upload a new version for an existing file.
 */
async function uploadNewVersion(fileId) {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.pdf,.jpg,.jpeg,.png,.docx,.xls,.xlsx,.zip';

    input.addEventListener('change', async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        if (!validateFile(file)) return;

        const formData = new FormData();
        formData.append('file', file);

        try {
            const result = await apiUpload(`/api/files/${fileId}/versions`, formData, null);
            showToast(`New version (v${result.versionNumber}) uploaded!`, 'success');

            if (typeof loadFiles === 'function') {
                loadFiles();
            }
        } catch (error) {
            showToast(error.message || 'Version upload failed', 'error');
        }
    });

    input.click();
}
