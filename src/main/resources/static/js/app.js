/**
 * Register an event at the document for the specified selector,
 * so events are still caught after DOM changes.
 */

function handleEvent(eventType, selector, handler) {
  document.addEventListener(eventType, function (event) {
    if (event.target.matches(selector + ', ' + selector + ' *')) {
      handler.apply(event.target.closest(selector), arguments);
    }
  });
}

handleEvent('click', '.js-file-delete', function (event) {
  const $fileDiv = event.target.parentElement;
  const $fileRow = $fileDiv.previousElementSibling;
  $fileRow.removeAttribute('disabled');
  $fileRow.classList.remove('d-none');
  $fileDiv.remove();
});

document.addEventListener('DOMContentLoaded', function () {
  const menuToggle = document.getElementById('menu-toggle');
  const sidebar = document.querySelector('.sidebar');
  const sidebarOverlay = document.querySelector('.sidebar-overlay');

  menuToggle?.addEventListener('click', function () {
    sidebar.classList.toggle('active');
    sidebarOverlay.classList.toggle('active');
  });

  sidebarOverlay?.addEventListener('click', function () {
    sidebar.classList.remove('active');
    sidebarOverlay.classList.remove('active');
  });

  document.addEventListener('click', function (event) {
    const isClickInsideSidebar = sidebar.contains(event.target);
    const isClickOnMenuToggle = menuToggle.contains(event.target);

    if (!isClickInsideSidebar && !isClickOnMenuToggle && window.innerWidth < 768) {
      sidebar.classList.remove('active');
      sidebarOverlay.classList.remove('active');
    }
  });

  const dropdownToggles = document.querySelectorAll('.dropdown-toggle');
  dropdownToggles.forEach(toggle => {
    toggle.addEventListener('click', function (e) {
      e.stopPropagation();
      const dropdownMenu = this.nextElementSibling;
      const isOpen = dropdownMenu.classList.contains('show');

      document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
        if (menu !== dropdownMenu) {
          menu.classList.remove('show');
        }
      });

      dropdownMenu.classList.toggle('show', !isOpen);
    });
  });

  document.addEventListener('click', function () {
    document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
      menu.classList.remove('show');
    });
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
        menu.classList.remove('show');
      });
    }
  });

  // === GALLERY PAGE FUNCTIONALITY ===
  if (window.location.pathname.includes('/gallery')) {
    initializeGalleryFunctionality();
  }

  // === ADD FILE PAGE FUNCTIONALITY ===
  if (window.location.pathname.includes('/addFile')) {
    initializeFileUploadFunctionality();
  }

  // === SUPPORT CHAT FUNCTIONALITY ===
  if (!window.location.pathname.includes('/chat')) {
    const supportBtn = document.getElementById('support-chat-btn');
    const chatBox = document.getElementById('chat-box');
    const closeChatBtn = document.getElementById('close-chat-btn');
    const chatInput = document.getElementById('chat-input');
    const sendBtn = document.getElementById('send-message-btn');
    const chatMessages = document.getElementById('chat-messages');
    const chatHeader = document.getElementById('chat-header');

    let chatState = {
      isDragging: false,
      hasMoved: false,
      dragStartTime: 0,
      currentX: 0,
      currentY: 0,
      initialX: 0,
      initialY: 0,
      xOffset: 0,
      yOffset: 0
    };

    const botResponses = [
      "Thank you for reaching out! How can I help you with TaskMaster today?",
      "I'd be happy to assist you with that. Can you provide more details?",
      "That's a great question! Let me help you find the right solution.",
      "I understand your concern. Our support team will look into this for you.",
      "Is there anything specific about the process workflow you'd like to know?",
      "I'm here to help! Feel free to ask any questions about TaskMaster features."
    ];

    let messageCount = 0;

    function showChatBox() {
      chatBox.classList.remove('hidden');
      chatBox.classList.add('flex');
      supportBtn.style.display = 'none';
      chatBox.style.left = '50%';
      chatBox.style.top = '50%';
      chatBox.style.transform = 'translate(-50%, -50%)';
      chatState.xOffset = 0;
      chatState.yOffset = 0;

      chatBox.style.opacity = '0';
      chatBox.style.transform = 'translate(-50%, -50%) scale(0.8)';
      setTimeout(() => {
        chatBox.style.opacity = '1';
        chatBox.style.transform = 'translate(-50%, -50%) scale(1)';
        chatBox.style.transition = 'all 0.3s ease-out';
        setTimeout(() => {
          chatBox.style.transition = '';
          chatInput.focus();
        }, 300);
      }, 10);
    }

    function closeChatBox() {
      chatBox.style.opacity = '0';
      chatBox.style.transform = 'translate(-50%, -50%) scale(0.8)';
      setTimeout(() => {
        chatBox.classList.add('hidden');
        chatBox.classList.remove('flex');
        supportBtn.style.display = 'flex';
        chatBox.style.transform = '';
        chatBox.style.opacity = '';
        chatBox.style.transition = '';
      }, 300);
    }

    function handleDragStart(e) {
      if (!chatHeader.contains(e.target) || closeChatBtn.contains(e.target)) return;
      e.preventDefault();
      const clientX = e.type === "touchstart" ? e.touches[0].clientX : e.clientX;
      const clientY = e.type === "touchstart" ? e.touches[0].clientY : e.clientY;

      chatState.isDragging = true;
      chatState.hasMoved = false;
      chatState.dragStartTime = Date.now();
      chatState.initialX = clientX - chatState.xOffset;
      chatState.initialY = clientY - chatState.yOffset;

      chatBox.style.transition = 'none';
      chatBox.style.cursor = 'grabbing';
      document.body.style.userSelect = 'none';
    }

    function handleDragMove(e) {
      if (!chatState.isDragging) return;
      e.preventDefault();

      const clientX = e.type === "touchmove" ? e.touches[0].clientX : e.clientX;
      const clientY = e.type === "touchmove" ? e.touches[0].clientY : e.clientY;

      chatState.currentX = clientX - chatState.initialX;
      chatState.currentY = clientY - chatState.initialY;
      chatState.xOffset = chatState.currentX;
      chatState.yOffset = chatState.currentY;

      if (Math.abs(chatState.currentX) > 5 || Math.abs(chatState.currentY) > 5) {
        chatState.hasMoved = true;
      }

      const rect = chatBox.getBoundingClientRect();
      const newLeft = (window.innerWidth / 2) + chatState.currentX;
      const newTop = (window.innerHeight / 2) + chatState.currentY;

      const minLeft = rect.width / 2;
      const maxLeft = window.innerWidth - (rect.width / 2);
      const minTop = rect.height / 2;
      const maxTop = window.innerHeight - (rect.height / 2);

      const boundedLeft = Math.max(minLeft, Math.min(maxLeft, newLeft));
      const boundedTop = Math.max(minTop, Math.min(maxTop, newTop));

      chatBox.style.left = boundedLeft + 'px';
      chatBox.style.top = boundedTop + 'px';
      chatBox.style.transform = 'translate(-50%, -50%)';
    }

    function handleDragEnd(e) {
      if (!chatState.isDragging) return;
      chatState.isDragging = false;
      chatBox.style.cursor = '';
      document.body.style.userSelect = '';
      setTimeout(() => {
        chatState.hasMoved = false;
      }, 50);
    }

    function handleDocumentClick(e) {
      if (
        chatBox.classList.contains('hidden') ||
        chatBox.contains(e.target) ||
        supportBtn.contains(e.target) ||
        chatState.isDragging ||
        chatState.hasMoved
      ) return;

      closeChatBox();
    }

    supportBtn.addEventListener('click', function () {
      if (!chatState.hasMoved) {
        showChatBox();
      }
    });

    closeChatBtn.addEventListener('click', closeChatBox);
    chatHeader.addEventListener('mousedown', handleDragStart);
    document.addEventListener('mousemove', handleDragMove);
    document.addEventListener('mouseup', handleDragEnd);
    chatHeader.addEventListener('touchstart', handleDragStart, { passive: false });
    document.addEventListener('touchmove', handleDragMove, { passive: false });
    document.addEventListener('touchend', handleDragEnd);
    document.addEventListener('click', handleDocumentClick, true);
    chatBox.addEventListener('click', e => e.stopPropagation());

    function getCurrentTime() {
      const now = new Date();
      return now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    function sendMessage() {
      const message = chatInput.value.trim();
      if (!message) return;

      sendBtn.disabled = true;

      const userMessageDiv = document.createElement('div');
      userMessageDiv.className = 'flex items-start space-x-2 justify-end';
      userMessageDiv.innerHTML = `
        <div class="bg-blue-600 text-white p-3 rounded-lg rounded-tr-none max-w-xs">
          <p class="text-sm">${message}</p>
          <span class="text-xs text-blue-200 mt-1 block">${getCurrentTime()}</span>
        </div>
        <div class="w-8 h-8 bg-slate-600 rounded-full flex items-center justify-center flex-shrink-0">
          <span class="material-icons text-white text-sm">person</span>
        </div>
      `;
      chatMessages.appendChild(userMessageDiv);
      chatInput.value = '';
      chatMessages.scrollTop = chatMessages.scrollHeight;

      const typingDiv = document.createElement('div');
      typingDiv.className = 'flex items-start space-x-2';
      typingDiv.id = 'typing-indicator';
      typingDiv.innerHTML = `
        <div class="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center flex-shrink-0">
          <span class="material-icons text-white text-sm">support_agent</span>
        </div>
        <div class="bg-slate-700 text-white p-3 rounded-lg rounded-tl-none">
          <div class="flex space-x-1">
            <div class="w-2 h-2 bg-slate-400 rounded-full animate-bounce"></div>
            <div class="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style="animation-delay: 0.1s"></div>
            <div class="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style="animation-delay: 0.2s"></div>
          </div>
        </div>
      `;
      chatMessages.appendChild(typingDiv);
      chatMessages.scrollTop = chatMessages.scrollHeight;

      fetch('/api/chat/send', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: new URLSearchParams({ message })
      })
        .then(res => res.json())
        .then(data => {
          document.getElementById('typing-indicator')?.remove();

          const botMessageDiv = document.createElement('div');
          botMessageDiv.className = 'flex items-start space-x-2';
          botMessageDiv.innerHTML = `
            <div class="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center flex-shrink-0">
              <span class="material-icons text-white text-sm">support_agent</span>
            </div>
            <div class="bg-slate-700 text-white p-3 rounded-lg rounded-tl-none max-w-xs">
              <p class="text-sm">${data.response}</p>
              <span class="text-xs text-slate-400 mt-1 block">${getCurrentTime()}</span>
            </div>
          `;
          chatMessages.appendChild(botMessageDiv);
          chatMessages.scrollTop = chatMessages.scrollHeight;
          sendBtn.disabled = false;
          messageCount++;
        })
        .catch(err => {
          console.error('Error:', err);
          alert('There was a problem sending your message.');
        });
    }

    sendBtn.addEventListener('click', sendMessage);
    chatInput.addEventListener('keypress', e => {
      if (e.key === 'Enter' && !sendBtn.disabled) sendMessage();
    });
    chatInput.addEventListener('input', () => {
      sendBtn.disabled = chatInput.value.trim() === '';
    });

    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && !chatBox.classList.contains('hidden')) {
        closeChatBox();
      }
    });

    sendBtn.disabled = true;

    (function makeSupportButtonDraggableAndSnap() {
      const button = document.getElementById('support-chat-btn');
      let isDragging = false;
      let startX = 0, startY = 0, offsetX = 0, offsetY = 0;
      const SNAP_MARGIN = 20;

      function onMouseDown(e) {
        isDragging = true;
        chatState.hasMoved = false;
        const rect = button.getBoundingClientRect();
        startX = e.clientX;
        startY = e.clientY;
        offsetX = startX - rect.left;
        offsetY = startY - rect.top;
        button.style.transition = 'none';
        document.body.style.userSelect = 'none';
      }

      function onMouseMove(e) {
        if (!isDragging) return;
        const newLeft = e.clientX - offsetX;
        const newTop = e.clientY - offsetY;
        button.style.left = newLeft + 'px';
        button.style.top = newTop + 'px';
        button.style.right = 'auto';
        chatState.hasMoved = true;
      }

      function onMouseUp(e) {
        if (!isDragging) return;
        isDragging = false;
        document.body.style.userSelect = '';
        button.style.transition = 'all 0.2s ease';
        const rect = button.getBoundingClientRect();
        const snapToLeft = rect.left < window.innerWidth / 2;
        const snappedY = Math.max(0, Math.min(window.innerHeight - rect.height, rect.top));
        button.style.top = snappedY + 'px';
        button.style.left = snapToLeft ? SNAP_MARGIN + 'px' : 'auto';
        button.style.right = snapToLeft ? 'auto' : SNAP_MARGIN + 'px';
      }

      button.addEventListener('mousedown', onMouseDown);
      document.addEventListener('mousemove', onMouseMove);
      document.addEventListener('mouseup', onMouseUp);

      button.addEventListener('touchstart', function (e) {
        if (e.touches.length !== 1) return;
        onMouseDown({ clientX: e.touches[0].clientX, clientY: e.touches[0].clientY });
      }, { passive: false });

      document.addEventListener('touchmove', function (e) {
        if (!isDragging || e.touches.length !== 1) return;
        onMouseMove({ clientX: e.touches[0].clientX, clientY: e.touches[0].clientY });
      }, { passive: false });

      document.addEventListener('touchend', onMouseUp);
    })();
  }

  // Modal functionality
  function openProcessModal() {
    const modal = document.getElementById('processModal');
    modal.classList.remove('hidden', 'hide');
    modal.classList.add('show');
    document.body.style.overflow = 'hidden';
  }

  function closeProcessModal() {
    const modal = document.getElementById('processModal');
    modal.classList.remove('show');
    modal.classList.add('hide');
    modal.addEventListener('animationend', function handleAnimationEnd() {
      modal.classList.add('hidden');
      modal.classList.remove('hide');
      modal.removeEventListener('animationend', handleAnimationEnd);
    });
    document.body.style.overflow = 'auto';
  }

  document.querySelectorAll('.process-modal-toggle').forEach(button => {
    button.addEventListener('click', function (e) {
      e.preventDefault();
      e.stopPropagation();
      openProcessModal();
    });
  });

  const closeButton = document.getElementById('closeModal');
  if (closeButton) {
    closeButton.addEventListener('click', closeProcessModal);
  }

  const modal = document.getElementById('processModal');
  if (modal) {
    modal.addEventListener('click', function (e) {
      if (e.target === modal) {
        closeProcessModal();
      }
    });
  }

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      const modal = document.getElementById('processModal');
      if (modal && !modal.classList.contains('hidden')) {
        closeProcessModal();
      }
    }
  });

  // Dark mode toggle
  const modeToggleBtn = document.getElementById('mode-toggle');
  const body = document.body;
  const html = document.documentElement;
  const MODE_KEY = 'colorMode';
  const ICON_MOON = 'nightlight_round';
  const ICON_SUN = 'wb_sunny';

  function setMode(mode) {
    if (mode === 'light') {
      body.classList.add('light-mode');
      html.classList.remove('dark');
    } else {
      body.classList.remove('light-mode');
      html.classList.add('dark');
    }

    if (modeToggleBtn) {
      const icon = modeToggleBtn.querySelector('.material-icons');
      if (icon) {
        icon.textContent = mode === 'light' ? ICON_SUN : ICON_MOON;
      }
    }
  }

  const savedMode = localStorage.getItem(MODE_KEY);
  if (savedMode === 'light' || savedMode === 'dark') {
    setMode(savedMode);
  } else {
    setMode('dark');
  }

  if (modeToggleBtn) {
    modeToggleBtn.addEventListener('click', function () {
      const isCurrentlyLight = body.classList.contains('light-mode');
      const newMode = isCurrentlyLight ? 'dark' : 'light';
      localStorage.setItem(MODE_KEY, newMode);
      setMode(newMode);
    });
  }
});

// === GALLERY FUNCTIONALITY ===
function initializeGalleryFunctionality() {
  let currentTab = 'all';
  let allFiles = [];
  let isSearching = false;
  let currentPage = 1;
  let itemsPerPage = 5;
  let filteredFiles = [];

  // Initialize elements
  const tabs = document.querySelectorAll('.file-tab');
  const searchInput = document.getElementById('file-search');
  const tableBody = document.getElementById('files-table-body');
  const paginationContainer = document.getElementById('pagination-container');
  const paginationPrev = document.getElementById('pagination-prev');
  const paginationNext = document.getElementById('pagination-next');
  const paginationNumbers = document.getElementById('pagination-numbers');
  const paginationStart = document.getElementById('pagination-start');
  const paginationEnd = document.getElementById('pagination-end');
  const paginationTotal = document.getElementById('pagination-total');

  // Set initial active tab
  const allTab = document.querySelector('.file-tab[data-tab="all"]');
  if (allTab) {
    allTab.classList.add('bg-blue-600', 'text-white');
    allTab.classList.remove('text-gray-400');
  }

  // Load initial files from server-rendered content
  loadInitialFiles();

  // Tab switching functionality
  tabs.forEach(tab => {
    tab.addEventListener('click', function() {
      const tabType = this.getAttribute('data-tab');

      // Update active tab styling
      tabs.forEach(t => {
        t.classList.remove('bg-blue-600', 'text-white');
        t.classList.add('text-gray-400');
      });
      this.classList.add('bg-blue-600', 'text-white');
      this.classList.remove('text-gray-400');

      currentTab = tabType;
      currentPage = 1; // Reset to first page when switching tabs

      if (isSearching) {
        // If searching, perform search with new tab
        performSearch();
      } else {
        // Otherwise, just filter by tab
        filterByTab();
      }
    });
  });

  // Search functionality
  searchInput.addEventListener('input', function() {
    const query = this.value.trim();
    currentPage = 1; // Reset to first page when searching

    if (query === '') {
      isSearching = false;
      filterByTab(); // Show tab content when search is cleared
    } else {
      isSearching = true;
      performSearch();
    }
  });

  // Pagination event listeners
  paginationPrev.addEventListener('click', function() {
    if (currentPage > 1) {
      currentPage--;
      updateCurrentView();
    }
  });

  paginationNext.addEventListener('click', function() {
    const totalPages = Math.ceil(filteredFiles.length / itemsPerPage);
    if (currentPage < totalPages) {
      currentPage++;
      updateCurrentView();
    }
  });

  function loadInitialFiles() {
    const fileRows = document.querySelectorAll('.file-row');
    allFiles = [];

    fileRows.forEach(row => {
      const fileData = {
        id: row.getAttribute('data-file-id'),
        type: row.getAttribute('data-file-type'),
        name: row.cells[0].textContent.trim(),
        caseNumber: row.cells[1].textContent.trim(),
        size: row.cells[3].textContent.trim(),
        dateAdded: row.cells[4].textContent.trim(),
        element: row.cloneNode(true)
      };
      allFiles.push(fileData);
    });

    // Initialize filtered files and pagination after loading
    filterByTab();
  }

  function filterByTab() {
    if (currentTab === 'all') {
      filteredFiles = allFiles;
    } else {
      filteredFiles = allFiles.filter(file => file.type === currentTab);
    }

    updateCurrentView();
  }

  function performSearch() {
    const searchQuery = searchInput.value.toLowerCase().trim();

    filteredFiles = allFiles.filter(file => {
      const matchesType = currentTab === 'all' || file.type === currentTab;
      const matchesQuery = file.name.toLowerCase().includes(searchQuery) ||
                          file.caseNumber.toLowerCase().includes(searchQuery);
      return matchesType && matchesQuery;
    });

    updateCurrentView();
  }

  function updateCurrentView() {
    updateTable();
    updatePagination();
  }

  function updateTable() {
    tableBody.innerHTML = '';

    if (filteredFiles.length === 0) {
      const emptyRow = document.createElement('tr');
      emptyRow.innerHTML = `
        <td class="text-center py-10 text-gray-500" colspan="6">
          No files found.
        </td>
      `;
      tableBody.appendChild(emptyRow);
      return;
    }

    // Calculate pagination
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, filteredFiles.length);
    const paginatedFiles = filteredFiles.slice(startIndex, endIndex);

    paginatedFiles.forEach(file => {
      const row = document.createElement('tr');
      row.className = 'file-row border-b border-gray-700 hover:bg-gray-700';
      row.setAttribute('data-file-id', file.id);
      row.setAttribute('data-file-type', file.type);
      row.innerHTML = `
        <td class="py-4 px-6 font-medium text-white">${file.name}</td>
        <td class="py-4 px-6">${file.caseNumber}</td>
        <td class="py-4 px-6 capitalize">${file.type}</td>
        <td class="py-4 px-6">${file.size}</td>
        <td class="py-4 px-6">${file.dateAdded}</td>
        <td class="py-4 px-6 text-right">
          <button
            class="delete-file-btn text-red-500 hover:text-red-700 p-2 rounded-full hover:bg-gray-600"
            data-file-id="${file.id}"
            data-file-name="${file.name}"
            title="Delete file"
          >
            <span class="material-icons">delete</span>
          </button>
        </td>
      `;
      tableBody.appendChild(row);
    });
  }

  function updatePagination() {
    const totalFiles = filteredFiles.length;
    const totalPages = Math.ceil(totalFiles / itemsPerPage);

    // Hide pagination if 10 or fewer items
    if (totalFiles <= itemsPerPage) {
      paginationContainer.classList.add('hidden');
      return;
    }

    // Show pagination
    paginationContainer.classList.remove('hidden');

    // Update pagination info
    const startItem = totalFiles === 0 ? 0 : (currentPage - 1) * itemsPerPage + 1;
    const endItem = Math.min(currentPage * itemsPerPage, totalFiles);

    paginationStart.textContent = startItem;
    paginationEnd.textContent = endItem;
    paginationTotal.textContent = totalFiles;

    // Update previous button
    paginationPrev.disabled = currentPage === 1;

    // Update next button
    paginationNext.disabled = currentPage === totalPages;

    // Update page numbers
    updatePageNumbers(totalPages);
  }

  function updatePageNumbers(totalPages) {
    paginationNumbers.innerHTML = '';

    // Show max 5 page numbers
    let startPage = Math.max(1, currentPage - 2);
    let endPage = Math.min(totalPages, startPage + 4);

    // Adjust start if we're near the end
    if (endPage - startPage < 4) {
      startPage = Math.max(1, endPage - 4);
    }

    // Add first page and ellipsis if needed
    if (startPage > 1) {
      addPageButton(1);
      if (startPage > 2) {
        addEllipsis();
      }
    }

    // Add page numbers
    for (let i = startPage; i <= endPage; i++) {
      addPageButton(i);
    }

    // Add ellipsis and last page if needed
    if (endPage < totalPages) {
      if (endPage < totalPages - 1) {
        addEllipsis();
      }
      addPageButton(totalPages);
    }
  }

  function addPageButton(pageNum) {
    const button = document.createElement('button');
    button.className = `px-3 py-2 text-sm font-medium border border-gray-600 rounded-lg ${
      pageNum === currentPage
        ? 'bg-blue-600 text-white border-blue-600'
        : 'text-gray-400 bg-gray-800 hover:bg-gray-700 hover:text-white'
    }`;
    button.textContent = pageNum;
    button.addEventListener('click', function() {
      currentPage = pageNum;
      updateCurrentView();
    });
    paginationNumbers.appendChild(button);
  }

  function addEllipsis() {
    const ellipsis = document.createElement('span');
    ellipsis.className = 'px-3 py-2 text-sm text-gray-400';
    ellipsis.textContent = '...';
    paginationNumbers.appendChild(ellipsis);
  }

  // Delete functionality
  let fileToDelete = null;
  const deleteModal = document.getElementById('deleteModal');
  const deleteFileName = document.getElementById('deleteFileName');
  const cancelDeleteBtn = document.getElementById('cancelDelete');
  const confirmDeleteBtn = document.getElementById('confirmDelete');

  // Handle delete button clicks
  document.addEventListener('click', function(e) {
    if (e.target.closest('.delete-file-btn')) {
      const deleteBtn = e.target.closest('.delete-file-btn');
      const fileId = deleteBtn.getAttribute('data-file-id');
      const fileName = deleteBtn.getAttribute('data-file-name');

      fileToDelete = { id: fileId, name: fileName };
      deleteFileName.textContent = fileName;
      deleteModal.classList.remove('hidden');
      deleteModal.classList.add('flex');
    }
  });

  // Cancel delete
  cancelDeleteBtn.addEventListener('click', function() {
    deleteModal.classList.add('hidden');
    deleteModal.classList.remove('flex');
    fileToDelete = null;
  });

  // Confirm delete
  confirmDeleteBtn.addEventListener('click', function() {
    if (fileToDelete) {
      fetch(`/api/files/${fileToDelete.id}`, {
        method: 'DELETE'
      })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'success') {
          // Remove from allFiles array
          allFiles = allFiles.filter(file => file.id !== fileToDelete.id);

          // Check if current page becomes empty after deletion
          const totalPages = Math.ceil(filteredFiles.length / itemsPerPage);
          if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
          }

          // Refresh the current view
          if (isSearching) {
            performSearch();
          } else {
            filterByTab();
          }

          deleteModal.classList.add('hidden');
          deleteModal.classList.remove('flex');
          fileToDelete = null;
        } else {
          alert('Failed to delete file: ' + data.message);
        }
      })
      .catch(error => {
        console.error('Error deleting file:', error);
        alert('Failed to delete file. Please try again.');
      });
    }
  });

  // Close modal on escape key
  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape' && !deleteModal.classList.contains('hidden')) {
      deleteModal.classList.add('hidden');
      deleteModal.classList.remove('flex');
      fileToDelete = null;
    }
  });

  // Close modal on backdrop click
  deleteModal.addEventListener('click', function(e) {
    if (e.target === deleteModal) {
      deleteModal.classList.add('hidden');
      deleteModal.classList.remove('flex');
      fileToDelete = null;
    }
  });
}

// === FILE UPLOAD FUNCTIONALITY ===
function initializeFileUploadFunctionality() {
  const uploadForm = document.getElementById('uploadForm');
  const uploadArea = document.getElementById('uploadArea');
  const fileInput = document.getElementById('file-upload');
  const uploadProgress = document.getElementById('uploadProgress');
  const progressBar = document.getElementById('progressBar');
  const progressPercentage = document.getElementById('progressPercentage');
  const uploadStatus = document.getElementById('uploadStatus');
  const uploadResults = document.getElementById('uploadResults');
  const uploadedFilesList = document.getElementById('uploadedFilesList');
  const doneButton = document.getElementById('doneButton');
  const uploadError = document.getElementById('uploadError');
  const errorMessage = document.getElementById('errorMessage');
  const retryButton = document.getElementById('retryButton');

  let tempUploadedFiles = [];

  // Drag and drop functionality
  uploadArea.addEventListener('dragover', function(e) {
    e.preventDefault();
    uploadArea.classList.add('border-blue-500', 'bg-blue-50', 'bg-opacity-10');
  });

  uploadArea.addEventListener('dragleave', function(e) {
    e.preventDefault();
    uploadArea.classList.remove('border-blue-500', 'bg-blue-50', 'bg-opacity-10');
  });

  uploadArea.addEventListener('drop', function(e) {
    e.preventDefault();
    uploadArea.classList.remove('border-blue-500', 'bg-blue-50', 'bg-opacity-10');

    const files = e.dataTransfer.files;
    if (files.length > 0) {
      handleFileUpload(files);
    }
  });

  // File input change
  fileInput.addEventListener('change', function(e) {
    const files = e.target.files;
    if (files.length > 0) {
      handleFileUpload(files);
    }
  });

  // Done button functionality
  doneButton.addEventListener('click', function() {
    if (tempUploadedFiles.length > 0) {
      // Finalize uploads on server
      fetch('/api/files/finalize', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(tempUploadedFiles)
      })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'success') {
          window.location.href = '/gallery';
        } else {
          alert('Failed to finalize uploads: ' + data.message);
        }
      })
      .catch(error => {
        console.error('Error finalizing uploads:', error);
        alert('Failed to finalize uploads. Please try again.');
      });
    } else {
      window.location.href = '/gallery';
    }
  });

  // Retry button functionality
  retryButton.addEventListener('click', function() {
    hideAllSections();
    resetForm();
  });

  function handleFileUpload(files) {
    hideAllSections();
    uploadProgress.classList.remove('hidden');

    tempUploadedFiles = [];
    let completedUploads = 0;
    const totalFiles = files.length;

    uploadStatus.textContent = `Uploading ${totalFiles} file(s)...`;

    Array.from(files).forEach((file, index) => {
      uploadSingleFile(file, index, totalFiles, () => {
        completedUploads++;
        const progress = (completedUploads / totalFiles) * 100;
        updateProgress(progress);

        if (completedUploads === totalFiles) {
          showUploadResults();
        }
      });
    });
  }

  function uploadSingleFile(file, index, totalFiles, callback) {
    const formData = new FormData();
    formData.append('file', file);

    fetch('/api/files/upload', {
      method: 'POST',
      body: formData
    })
    .then(response => response.json())
    .then(data => {
      if (data.status === 'success') {
        tempUploadedFiles.push(data.fileInfo);
      }
      callback();
    })
    .catch(error => {
      console.error('Upload error:', error);
      callback();
    });
  }

  function updateProgress(percentage) {
    progressBar.style.width = percentage + '%';
    progressPercentage.textContent = Math.round(percentage) + '%';

    if (percentage === 100) {
      uploadStatus.textContent = 'Processing uploads...';
    }
  }

  function showUploadResults() {
    uploadProgress.classList.add('hidden');

    if (tempUploadedFiles.length === 0) {
      errorMessage.textContent = 'All uploads failed. Please try again.';
      uploadError.classList.remove('hidden');
    } else {
      uploadResults.classList.remove('hidden');

      uploadedFilesList.innerHTML = '';
      tempUploadedFiles.forEach(file => {
        const fileItem = document.createElement('div');
        fileItem.className = 'flex items-center justify-between p-2 bg-gray-700 rounded';
        fileItem.innerHTML = `
          <div class="flex items-center">
            <span class="material-icons text-green-500 mr-2">check_circle</span>
            <span class="text-white">${file.name}</span>
          </div>
          <span class="text-gray-400 text-sm">${file.size}</span>
        `;
        uploadedFilesList.appendChild(fileItem);
      });
    }
  }

  function hideAllSections() {
    uploadProgress.classList.add('hidden');
    uploadResults.classList.add('hidden');
    uploadError.classList.add('hidden');
  }

  function resetForm() {
    fileInput.value = '';
    tempUploadedFiles = [];
    progressBar.style.width = '0%';
    progressPercentage.textContent = '0%';
    uploadStatus.textContent = '';
  }
}
