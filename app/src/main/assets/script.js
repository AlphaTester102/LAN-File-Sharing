let isOwner = false;

async function checkOwnership() {
    try {
        const res = await fetch('/is-owner');
        const data = await res.json();
        isOwner = data.isOwner;
    } catch (e) {
        isOwner = false;
    }
}

function joinServer() {
    const ip = document.getElementById('ip').value;
    if (ip) {
        Android.joinServer(ip);
    } else {
        alert('Enter IP first');
    }
}

function scanQR() {
    Android.scanQRCode();
}

function setIp(ip) {
    document.getElementById('ip').value = ip;
}

function displayQRCode(base64Data) {
    const container = document.getElementById('qr-container');
    if (container) {
        container.innerHTML = '<h3>Scan to Connect</h3><img src="' + base64Data + '" alt="QR Code">';
    }
}

function showServerOptions() {
    document.getElementById('server-modal').style.display = 'block';
    document.getElementById('server-modal-backdrop').style.display = 'block';
    document.getElementById('server-password').value = '';
}

function hideServerOptions() {
    document.getElementById('server-modal').style.display = 'none';
    document.getElementById('server-modal-backdrop').style.display = 'none';
}

function startPublicServer() {
    hideServerOptions();
    Android.startServer('public', '');
}

function startPrivateServer() {
    const pw = document.getElementById('server-password').value;
    if (!pw) {
        alert('Please enter a password for the private server.');
        return;
    }
    hideServerOptions();
    Android.startServer('private', pw);
}

async function login() {
    const password = document.getElementById('pw-input').value;
    if (!password) return;

    try {
        const res = await fetch('/auth', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: 'password=' + encodeURIComponent(password)
        });
        if (res.ok) {
            window.location.href = '/';
        } else {
            document.getElementById('error-msg').style.display = 'block';
            document.getElementById('pw-input').value = '';
            document.getElementById('pw-input').focus();
        }
    } catch (e) {
        document.getElementById('error-msg').style.display = 'block';
    }
}

let filesData = [];

async function loadFiles() {
    try {
        const response = await fetch('/list');
        filesData = await response.json();
        renderFiles(filesData);
    } catch (error) {
        console.error('Error loading files:', error);
        document.getElementById('fileTable').innerHTML = '<tr><td colspan="4">Error loading files</td></tr>';
    }
}

function renderFiles(files) {
    const table = document.getElementById('fileTable');
    table.innerHTML = '';

    if (files.length === 0) {
        table.innerHTML = '<tr><td colspan="4">No files found</td></tr>';
        return;
    }

    files.forEach(file => {
        let actions = `<a href="/download/${encodeURIComponent(file.name)}" class="btn-download">Download</a>`;

        if (isOwner) {
            actions += `<button class="btn-delete" onclick="deleteFile('${file.name}')">Delete</button>`;
        }

        table.innerHTML += `
            <tr>
                <td>${file.name}</td>
                <td>${file.type.toUpperCase()}</td>
                <td>${formatFileSize(file.size)}</td>
                <td class="actions">${actions}</td>
            </tr>
        `;
    });
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
}

function deleteFile(name) {
    if (!confirm(`Delete ${name}?`)) return;
    fetch(`/delete?file=${encodeURIComponent(name)}`, { method: 'POST' })
        .then(response => {
            if (response.ok) loadFiles();
            else alert('Delete failed. Status: ' + response.status);
        });
}

function filterFiles() {
    const query = document.getElementById('search').value.toLowerCase();
    renderFiles(filesData.filter(file => file.name.toLowerCase().includes(query)));
}

function sortFiles() {
    const sortBy = document.getElementById('sort').value;
    filesData.sort((a, b) => sortBy === 'size' ? a.size - b.size : a.name.localeCompare(b.name));
    renderFiles(filesData);
}

function initUploadPage() {
    const dropArea = document.getElementById('drop-area');
    if (!dropArea) return;

    const fileInput = document.getElementById('file-input');
    const fileList  = document.getElementById('file-list');

    dropArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropArea.style.backgroundColor = '#005566';
    });

    dropArea.addEventListener('dragleave', () => {
        dropArea.style.backgroundColor = '';
    });

    dropArea.addEventListener('drop', (e) => {
        e.preventDefault();
        dropArea.style.backgroundColor = '';
        handleFiles(e.dataTransfer.files);
    });

    fileInput.addEventListener('change', () => {
        handleFiles(fileInput.files);
    });

    function handleFiles(files) {
        Array.from(files).forEach((file) => {
            const li = document.createElement('li');
            li.classList.add('file-item');
            li.innerHTML = file.name;
            fileList.appendChild(li);

            const progressContainer = document.createElement('div');
            progressContainer.classList.add('progress-bar-container');
            const progress = document.createElement('div');
            progress.classList.add('progress-bar');
            progressContainer.appendChild(progress);
            li.appendChild(progressContainer);

            uploadFile(file, progress);
        });
    }

    function uploadFile(file, progressElement) {
        const formData = new FormData();
        formData.append('file', file);

        const xhr = new XMLHttpRequest();
        xhr.open('POST', '/upload', true);

        xhr.upload.onprogress = function (e) {
            if (e.lengthComputable) {
                progressElement.style.width = (e.loaded / e.total * 100) + '%';
            }
        };

        xhr.onload = function () {
            if (xhr.status === 200) {
                console.log('Upload successful');
            } else {
                console.log('Error uploading');
            }
        };

        xhr.send(formData);
    }

    fetch('/server-info')
        .then(r => r.json())
        .then(data => {
            const el = document.getElementById('server-url');
            if (el) {
                el.textContent = data.url;
                if (el.tagName === 'A') {
                    el.href = data.url;
                    el.target = '_blank';

                    el.addEventListener('click', function (e) {
                        e.preventDefault();
                        copyTextToClipboard(data.url)
                            .then(() => {
                                showToast('Server URL copied to clipboard');
                            })
                            .catch(() => {
                                showToast('Copied to clipboard');
                            });
                    });
                }
            }
        })
        .catch(() => {
            const el = document.getElementById('server-url');
            if (el) {
                el.textContent = 'http://[your-device-ip]:[port]/';
                if (el.tagName === 'A') el.href = 'http://[your-device-ip]:[port]/';
            }
        });
}

function copyTextToClipboard(text) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
        return navigator.clipboard.writeText(text);
    }

    return new Promise((resolve, reject) => {
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed';
        textarea.style.left = '-9999px';
        document.body.appendChild(textarea);
        textarea.focus();
        textarea.select();

        try {
            const ok = document.execCommand('copy');
            document.body.removeChild(textarea);
            if (ok) resolve();
            else reject(new Error('execCommand failed'));
        } catch (err) {
            document.body.removeChild(textarea);
            reject(err);
        }
    });
}

function showToast(message, ms = 2200) {
    let el = document.getElementById('global-toast');
    if (!el) {
        el = document.createElement('div');
        el.id = 'global-toast';
        document.body.appendChild(el);
    }
    el.textContent = message;
    el.classList.add('show');
    clearTimeout(el._hideTimer);
    el._hideTimer = setTimeout(() => {
        el.classList.remove('show');
    }, ms);
}

function removeFile(button, filename) {
    button.closest('.file-item').remove();
    fetch(`/delete?file=${encodeURIComponent(filename)}`, { method: 'POST' })
        .then(r => r.json())
        .then(data => console.log('File deleted:', data.message))
        .catch(error => console.error('Error deleting file:', error));
}

document.addEventListener('DOMContentLoaded', async function () {

    if (document.getElementById('qr-container') && typeof Android !== 'undefined' && Android.requestQRCode) {
        Android.requestQRCode();
    }

    if (document.getElementById('fileTable')) {
        await checkOwnership();
        loadFiles();
    }

    if (document.getElementById('drop-area')) {
        await checkOwnership();
        initUploadPage();
    }

    const serverPasswordInput = document.getElementById('server-password');
    if (serverPasswordInput) {
        serverPasswordInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') startPrivateServer();
        });
    }

    const pwInput = document.getElementById('pw-input');
    if (pwInput) {
        pwInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') login();
        });
    }
});
