// Document Controller
const DocumentController = {
    // Initialize all document-related functionality
    init() {
        // Check if jsPDF is available (only when needed)
        this.hasPDFSupport = typeof window.jspdf !== 'undefined';

        this.setupTabs();
        this.setupFileUploads();
        this.setupModals();
        this.setupDocumentActions();
        this.setupHTMXHandlers();
        this.setupEditModeToggle();
    },

    // Tab functionality
    setupTabs() {
        const tabs = document.querySelectorAll('.case-tab-item');
        if (!tabs.length) return;

        tabs.forEach(tab => {
            tab.addEventListener('click', () => this.handleTabClick(tab));
        });
    },

    handleTabClick(tab) {
        // Remove active class from all tabs
        document.querySelectorAll('.case-tab-item').forEach(t => {
            t.classList.remove('active');
        });

        // Add active class to clicked tab
        tab.classList.add('active');

        // Hide all tab contents
        document.querySelectorAll('.case-tab-content').forEach(content => {
            content.classList.remove('active');
        });

        // Show the selected tab content
        const tabId = tab.getAttribute('data-tab');
        const tabContent = document.getElementById(tabId);
        if (tabContent) {
            tabContent.classList.add('active');
        }
    },

    // File upload functionality
    setupFileUploads() {
        const fileInputs = document.querySelectorAll('[type="file"]');
        if (!fileInputs.length) return;

        fileInputs.forEach(input => {
            input.addEventListener('change', (e) => this.handleFileUpload(e));
        });
    },

    handleFileUpload(e) {
        const label = e.target.nextElementSibling;
        if (!label) return;

        const fileName = e.target.files[0]?.name || 'No file selected';
        const textElement = label.querySelector('[style*="font-weight: 500"]');
        if (textElement) {
            textElement.textContent = fileName;
        }
    },

    setupModals() {
        this.modal = document.getElementById('document-name-modal');
        this.editorModal = document.getElementById('editor-modal');
        this.newDocumentName = document.getElementById('new-document-name');
        this.editorTextarea = document.getElementById('document-editor');

        // Add client document modal
        const addClientDocBtn = document.getElementById('add-client-document');
        if (addClientDocBtn) {
            addClientDocBtn.addEventListener('click', () => this.showAddDocumentModal());
        }

        // Close modal handlers
        const closeModalBtn = document.getElementById('close-modal');
        if (closeModalBtn) {
            closeModalBtn.addEventListener('click', () => this.hideModal(this.modal));
        }

        const cancelAddDocBtn = document.getElementById('cancel-add-document');
        if (cancelAddDocBtn) {
            cancelAddDocBtn.addEventListener('click', () => this.hideModal(this.modal));
        }

        const confirmAddDocBtn = document.getElementById('confirm-add-document');
        if (confirmAddDocBtn) {
            confirmAddDocBtn.addEventListener('click', () => this.addNewDocument());
        }

        // Editor modal
        const editorCloseBtn = document.getElementById('editor-close');
        if (editorCloseBtn) {
            editorCloseBtn.addEventListener('click', () => this.hideModal(this.editorModal));
        }

        const editorCancelBtn = document.getElementById('editor-cancel');
        if (editorCancelBtn) {
            editorCancelBtn.addEventListener('click', () => this.hideModal(this.editorModal));
        }

        const editorSaveBtn = document.getElementById('editor-save');
        if (editorSaveBtn) {
            editorSaveBtn.addEventListener('click', () => this.saveEditorChanges());
        }

        // Close modal when clicking outside
        if (this.editorModal) {
            this.editorModal.addEventListener('click', (e) => {
                if (e.target === this.editorModal) {
                    this.hideModal(this.editorModal);
                }
            });
        }
    },

    showAddDocumentModal() {
        if (!this.modal || !this.newDocumentName) return;

        this.showModal(this.modal);
        this.newDocumentName.value = '';
        this.newDocumentName.focus();
    },

    showModal(modal) {
        if (modal) {
            modal.style.display = 'flex';
        }
    },

    hideModal(modal) {
        if (modal) {
            modal.style.display = 'none';
        }
    },

    // Document actions
    setupDocumentActions() {
        // Remove document buttons
        const docCloseBtns = document.querySelectorAll('.document-close');
        if (docCloseBtns.length) {
            docCloseBtns.forEach(btn => {
                btn.addEventListener('click', (e) => this.removeDocument(e));
            });
        }

        // Edit and download buttons (for dynamically added elements)
        document.addEventListener('click', (e) => {
            if (e.target.closest('.edit-btn')) {
                this.handleEditButtonClick(e.target.closest('.edit-btn'));
            }

            if (e.target.closest('.download-btn')) {
                this.handleDownloadButtonClick(e.target.closest('.download-btn'));
            }
        });
    },

    // Setup edit mode toggle functionality
    setupEditModeToggle() {
        document.addEventListener('click', (e) => {
            if (e.target.closest('.edit-btn')) {
                this.toggleEditMode(e.target.closest('.edit-btn'));
            }
        });

        // HTMX beforeRequest handler to disable generate button during submission
        document.body.addEventListener('htmx:beforeRequest', (evt) => {
            if (evt.detail.elt.classList.contains('generate-btn')) {
                evt.detail.elt.disabled = true;
            }
        });

        document.body.addEventListener('htmx:afterRequest', (evt) => {
            if (evt.detail.elt.classList.contains('generate-btn')) {
                evt.detail.elt.disabled = false;
            }
        });
    },

    toggleEditMode(button) {
        if (!button) return;

        const index = button.id.split('-')[2];
        const displayDiv = document.getElementById('display-content-' + index);
        const editTextarea = document.getElementById('edit-content-' + index);

        if (!displayDiv || !editTextarea) return;

        if (editTextarea.style.display === 'none') {
            // Switch to edit mode
            editTextarea.value = displayDiv.textContent;
            displayDiv.style.display = 'none';
            editTextarea.style.display = 'block';
            button.innerHTML = '<span class="material-icons case-btn-icon">save</span> Save Document';
            button.classList.remove('case-btn-secondary');
            button.classList.add('case-btn-primary');
        } else {
            // Switch back to display mode
            displayDiv.textContent = editTextarea.value;
            editTextarea.style.display = 'none';
            displayDiv.style.display = 'block';
            button.innerHTML = '<span class="material-icons case-btn-icon">edit</span> Edit Document';
            button.classList.remove('case-btn-primary');
            button.classList.add('case-btn-secondary');
        }
    },

    addNewDocument() {
        if (!this.newDocumentName || !this.modal) return;

        if (this.newDocumentName.value.trim() === '') {
            alert('Please enter a document name');
            return;
        }

        const container = document.getElementById('client-documents-container');
        if (!container) return;

        const docId = 'doc-' + Date.now();

        const newDocument = document.createElement('div');
        newDocument.className = 'case-document-card';
        newDocument.innerHTML = `
            <button type="button" class="material-icons document-close">close</button>
            <label class="case-form-label" for="${docId}-name">Document Name</label>
            <input class="case-form-control" id="${docId}-name" type="text" value="${this.newDocumentName.value}"/>
            <div class="case-file-upload">
                <input type="file" id="${docId}-file" accept=".pdf,.doc,.docx,.jpg,.png">
                <label for="${docId}-file" class="case-file-upload-label">
                    <div style="display: flex; align-items: center;">
                        <span class="material-icons" style="color: var(--primary-light); margin-right: 0.75rem; font-size: 1.25rem;">upload</span>
                        <span style="color: var(--text-primary); font-size: 0.875rem; font-weight: 500;">Upload Document</span>
                    </div>
                    <span class="case-file-upload-browse">Browse</span>
                </label>
            </div>
        `;

        container.appendChild(newDocument);

        // Add event listeners to new elements
        const fileInput = newDocument.querySelector('[type="file"]');
        if (fileInput) {
            fileInput.addEventListener('change', (e) => this.handleFileUpload(e));
        }

        const closeBtn = newDocument.querySelector('.document-close');
        if (closeBtn) {
            closeBtn.addEventListener('click', (e) => this.removeDocument(e));
        }

        this.hideModal(this.modal);
    },

    removeDocument(e) {
        const documentCard = e.target.closest('.case-document-card');
        if (documentCard) {
            documentCard.remove();
        }
    },

    handleEditButtonClick(button) {
        if (!button) return;

        const card = button.closest('.case-document-card');
        if (!card) return;

        const textarea = card.querySelector('.case-generated-content');
        if (!textarea) return;

        if (textarea.readOnly) {
            // Switch to edit mode
            textarea.readOnly = false;
            button.innerHTML = '<span class="material-icons case-btn-icon">save</span>Save Changes';
            button.classList.remove('case-btn-secondary');
            button.classList.add('case-btn-primary');
            textarea.focus();
        } else {
            // Switch back to view mode
            textarea.readOnly = true;
            button.innerHTML = '<span class="material-icons case-btn-icon">edit</span>Edit Document';
            button.classList.remove('case-btn-primary');
            button.classList.add('case-btn-secondary');
        }
    },

    handleDownloadButtonClick(button) {
        if (!button) return;

        const card = button.closest('.case-document-card');
        if (!card) return;

        // Try to find content in either textarea or display div
        const textarea = card.querySelector('.case-generated-content-edit');
        const displayDiv = card.querySelector('.case-generated-content-display');
        const content = textarea ? textarea.value : (displayDiv ? displayDiv.textContent : '');

        const titleElement = card.querySelector('[style*="font-weight: 500"]');
        if (!titleElement) return;

        const title = titleElement.textContent;

        // Check if we have a lawyer document (content to download)
        if (!content.trim()) {
            alert('No document content available to download');
            return;
        }

        // Check if PDF support is available
        if (!this.hasPDFSupport) {
            alert('PDF generation is not available. Please make sure the jsPDF library is loaded.');
            return;
        }

        try {
            // Destructure jsPDF only when needed
            const { jsPDF } = window.jspdf;

            // Create a new PDF document
            const doc = new jsPDF();

            // Set document properties
            doc.setProperties({
                title: title,
                subject: 'Legal Document',
                author: 'Legal System',
                keywords: 'legal, document',
                creator: 'Legal App'
            });

            // Add title
            doc.setFontSize(18);
            doc.text(title, 15, 20);

            // Add content (trimming any extra whitespace)
            doc.setFontSize(12);

            // Split long text into multiple pages if needed
            const pageHeight = doc.internal.pageSize.height - 30;
            const lines = doc.splitTextToSize(content.trim(), 180);
            let y = 30;

            for (let i = 0; i < lines.length; i++) {
                if (y > pageHeight) {
                    doc.addPage();
                    y = 20;
                }
                doc.text(lines[i], 15, y);
                y += 7;
            }

            // Save the PDF
            doc.save(`${title.replace(/\s+/g, '_')}.pdf`);
        } catch (error) {
            console.error('Error generating PDF:', error);
            alert('An error occurred while generating the PDF. Please try again.');
        }
    },

    saveEditorChanges() {
        if (!this.editorTextarea || !this.editorModal) return;

        const targetId = this.editorTextarea.dataset.targetContent;
        if (targetId) {
            const targetContent = document.getElementById(targetId);
            if (targetContent) {
                targetContent.textContent = this.editorTextarea.value;
            }
        }
        this.hideModal(this.editorModal);
    },

    // HTMX integration for lawyer document generation
    setupHTMXHandlers() {
        document.body.addEventListener('htmx:afterRequest', (evt) => {
            if (evt.detail.successful && evt.detail.requestConfig.path.includes('/forms/generate-lawyer-doc/')) {
                const button = evt.detail.elt;
                if (!button) return;

                const card = button.closest('.case-document-card');
                if (!card) return;

                // Extract the index from the button ID
                const index = button.id.split('-')[2];

                const status = document.getElementById('status-' + index);
                const generateBtn = document.getElementById('generate-btn-' + index);
                const editBtn = document.getElementById('edit-btn-' + index);
                const downloadBtn = document.getElementById('download-btn-' + index);
                const displayDiv = document.getElementById('display-content-' + index);
                const editTextarea = document.getElementById('edit-content-' + index);

                if (status && generateBtn && editBtn && downloadBtn && displayDiv) {
                    // Get the raw text content from the response and trim whitespace
                    let generatedContent = evt.detail.xhr.responseText.trim();

                    // Update the display div with the clean content
                    displayDiv.textContent = generatedContent;
                    displayDiv.style.display = 'block';

                    // Update status
                    status.textContent = 'Generated';
                    status.style.backgroundColor = 'var(--success)';

                    // Show action buttons
                    generateBtn.style.display = 'none';
                    editBtn.style.display = 'inline-flex';
                    downloadBtn.style.display = 'inline-flex';
                }
            }
        });

        // Handle HTMX afterSwap event for content updates
        document.body.addEventListener('htmx:afterSwap', function(evt) {
            if (evt.detail.target.id.startsWith('display-content-')) {
                const index = evt.detail.target.id.split('-')[2];
                const statusBadge = document.getElementById(`status-${index}`);
                const editBtn = document.getElementById(`edit-btn-${index}`);
                const downloadBtn = document.getElementById(`download-btn-${index}`);

                if (statusBadge) {
                    statusBadge.textContent = 'Generated';
                    statusBadge.style.backgroundColor = 'var(--success)';
                }

                if (editBtn) {
                    editBtn.style.display = 'inline-flex';
                }

                if (downloadBtn) {
                    downloadBtn.style.display = 'inline-flex';
                }
            }
        });
    }
};

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    DocumentController.init();
});