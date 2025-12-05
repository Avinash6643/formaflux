let currentMode = 'normal';

function setMode(mode) {
    currentMode = mode;
    document.querySelectorAll('.mode-option').forEach(el => el.classList.remove('active'));
    document.getElementById(`mode-${mode}`).classList.add('active');

    const mappingArea = document.getElementById('mapping-area');
    if (mode === 'advanced') {
        mappingArea.style.display = 'block';
        const fileInput = document.getElementById('file-upload');
        if (fileInput.files.length > 0) {
            analyzeFile(fileInput.files[0]);
        }
    } else {
        mappingArea.style.display = 'none';
    }
}

document.getElementById('file-upload').addEventListener('change', (e) => {
    if (currentMode === 'advanced' && e.target.files.length > 0) {
        analyzeFile(e.target.files[0]);
    }
});

async function analyzeFile(file) {
    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch('/api/analyze', {
            method: 'POST',
            body: formData
        });

        if (response.ok) {
            const data = await response.json();
            renderMappings(data.fields);
        } else {
            console.error('Analysis failed');
        }
    } catch (error) {
        console.error('Error:', error);
    }
}

function renderMappings(fields) {
    const container = document.getElementById('mapping-container');
    container.innerHTML = '';

    fields.forEach(field => {
        const div = document.createElement('div');
        div.className = 'mapping-item';
        div.innerHTML = `
            <div class="source-field">${field}</div>
            <span class="arrow">→</span>
            <input type="text" class="target-field" name="map_${field}" placeholder="Target key (optional)">
        `;
        container.appendChild(div);
    });
}

document.getElementById('conversion-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const formData = new FormData(e.target);

    if (currentMode === 'advanced') {
        const mappings = {};
        document.querySelectorAll('.mapping-item').forEach(item => {
            const source = item.querySelector('.source-field').textContent;
            const target = item.querySelector('.target-field').value;
            if (target) {
                mappings[source] = target;
            }
        });
        formData.append('mapping', JSON.stringify(mappings));
    }

    try {
        const response = await fetch('/api/convert', {
            method: 'POST',
            body: formData
        });

        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'converted.json';
            document.body.appendChild(a);
            a.click();
            a.remove();
        } else {
            alert('Conversion failed');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('An error occurred');
    }
});
