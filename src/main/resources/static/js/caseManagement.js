// Initialize jsPDF
        const { jsPDF } = window.jspdf;

        // Document Controller
        const DocumentController = {
            // Initialize all document-related functionality
            init() {
                this.setupTabs();
                this.setupFileUploads();
                this.setupModals();
                this.setupDocumentActions();
                this.setupHTMXHandlers();
            },

            // Tab functionality
            setupTabs() {
                document.querySelectorAll('.case-tab-item').forEach(tab => {
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
                document.getElementById(tabId).classList.add('active');
            },

            // File upload functionality
            setupFileUploads() {
                document.querySelectorAll('[type="file"]').forEach(input => {
                    input.addEventListener('change', (e) => this.handleFileUpload(e));
                });
            },

            handleFileUpload(e) {
                const label = e.target.nextElementSibling;
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
                document.getElementById('add-client-document')?.addEventListener('click', () => this.showAddDocumentModal());

                // Close modal handlers
                document.getElementById('close-modal')?.addEventListener('click', () => this.hideModal(this.modal));
                document.getElementById('cancel-add-document')?.addEventListener('click', () => this.hideModal(this.modal));
                document.getElementById('confirm-add-document')?.addEventListener('click', () => this.addNewDocument());

                // Editor modal
                document.getElementById('editor-close')?.addEventListener('click', () => this.hideModal(this.editorModal));
                document.getElementById('editor-cancel')?.addEventListener('click', () => this.hideModal(this.editorModal));
                document.getElementById('editor-save')?.addEventListener('click', () => this.saveEditorChanges());

                // Close modal when clicking outside
                this.editorModal?.addEventListener('click', (e) => {
                    if (e.target === this.editorModal) {
                        this.hideModal(this.editorModal);
                    }
                });
            },

            showAddDocumentModal() {
                this.showModal(this.modal);
                this.newDocumentName.value = '';
                this.newDocumentName.focus();
            },

            showModal(modal) {
                modal.style.display = 'flex';
            },

            hideModal(modal) {
                modal.style.display = 'none';
            },

            // Document actions
            setupDocumentActions() {
                // Remove document buttons
                document.querySelectorAll('.document-close').forEach(btn => {
                    btn.addEventListener('click', (e) => this.removeDocument(e));
                });

                // Edit buttons (for dynamically added elements)
                document.addEventListener('click', (e) => {
                    if (e.target.closest('.edit-btn')) {
                        this.handleEditButtonClick(e.target);
                    }

                    if (e.target.closest('.download-btn')) {
                        this.handleDownloadButtonClick(e.target);
                    }
                });
            },

            addNewDocument() {
                if (this.newDocumentName.value.trim() === '') {
                    alert('Please enter a document name');
                    return;
                }

                const container = document.getElementById('client-documents-container');
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
                newDocument.querySelector('[type="file"]').addEventListener('change', (e) => this.handleFileUpload(e));
                newDocument.querySelector('.document-close').addEventListener('click', (e) => this.removeDocument(e));

                this.hideModal(this.modal);
            },

            removeDocument(e) {
                const documentCard = e.target.closest('.case-document-card');
                if (documentCard) {
                    documentCard.remove();
                }
            },

            handleEditButtonClick(button) {
                const card = button.closest('.case-document-card');
                const textarea = card.querySelector('.case-generated-content');

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
                const card = button.closest('.case-document-card');
                const textarea = card.querySelector('.case-generated-content');
                const title = card.querySelector('[style*="font-weight: 500"]').textContent;

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
                const content = textarea.value.trim();
                doc.setFontSize(12);

                // Split long text into multiple pages if needed
                const pageHeight = doc.internal.pageSize.height - 30;
                const lines = doc.splitTextToSize(content, 180);
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
            },

            saveEditorChanges() {
                const targetId = this.editorTextarea.dataset.targetContent;
                if (targetId) {
                    const targetContent = document.getElementById(targetId);
                    if (targetContent) {
                        targetContent.textContent = this.editorTextarea.value;
                    }
                }
                this.hideModal(this.editorModal);
            },

            // HTMX integration
            setupHTMXHandlers() {
                document.body.addEventListener('htmx:afterRequest', (evt) => {
                    if (evt.detail.successful && evt.detail.requestConfig.path === '/forms/generate-lawyer-doc') {
                        const button = evt.detail.elt;
                        const card = button.closest('.case-document-card');
                        const status = card.querySelector('.case-status-badge');
                        const generateBtn = card.querySelector('.generate-btn');
                        const editBtn = card.querySelector('.edit-btn');
                        const downloadBtn = card.querySelector('.download-btn');
                        const textarea = card.querySelector('.case-generated-content');

                        if (status && generateBtn && editBtn && downloadBtn && textarea) {
                            // Get the raw text content from the response and trim whitespace
                            let generatedContent = evt.detail.xhr.responseText.trim();

                            // Strip any HTML tags if present
                            generatedContent = generatedContent.replace(/<[^>]*>/g, '');

                            // Update the textarea with the clean content
                            textarea.value = generatedContent;
                            textarea.style.display = 'block';

                            // Update status
                            status.textContent = 'Generated';
                            status.classList.add('generated');

                            // Transform buttons
                            generateBtn.style.display = 'none';
                            editBtn.style.display = 'inline-flex';
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