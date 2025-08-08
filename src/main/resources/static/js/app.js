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

  // Theme toggle functionality
  const modeToggleBtn = document.getElementById('mode-toggle');
  const body = document.body;
  const html = document.documentElement;
  const MODE_KEY = 'colorMode';
  const ICON_MOON = 'nightlight_round';
  const ICON_SUN = 'wb_sunny';

  function setMode(mode) {
    // Remove all theme classes first
    body.classList.remove('light-mode');
    html.classList.remove('dark');
    
    // Apply the new theme
    if (mode === 'light') {
      body.classList.add('light-mode');
      html.classList.remove('dark');
    } else {
      body.classList.remove('light-mode');
      html.classList.add('dark');
    }

    // Update the icon
    if (modeToggleBtn) {
      const icon = modeToggleBtn.querySelector('.material-icons');
      if (icon) {
        icon.textContent = mode === 'light' ? ICON_SUN : ICON_MOON;
      }
    }

    // Trigger a custom event for other components that might need to react to theme changes
    document.dispatchEvent(new CustomEvent('themeChanged', { detail: { mode } }));
  }

  // Initialize theme on page load
  function initializeTheme() {
  const savedMode = localStorage.getItem(MODE_KEY);
  if (savedMode === 'light' || savedMode === 'dark') {
    setMode(savedMode);
  } else {
      // Default to dark mode if no preference is saved
    setMode('dark');
    }
  }

  // Initialize theme immediately
  initializeTheme();

  // Add click event listener for theme toggle
  if (modeToggleBtn) {
    modeToggleBtn.addEventListener('click', function (e) {
      e.preventDefault();
      e.stopPropagation();
      
      const isCurrentlyLight = body.classList.contains('light-mode');
      const newMode = isCurrentlyLight ? 'dark' : 'light';
      
      // Save to localStorage
      localStorage.setItem(MODE_KEY, newMode);
      
      // Apply the new theme
      setMode(newMode);
      
      console.log('Theme changed to:', newMode);
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
        const matchesQuery = searchQuery === '' ||
                           file.name.toLowerCase().includes(searchQuery) ||
                           (file.caseNumber && file.caseNumber.toLowerCase().includes(searchQuery));
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
                                                class="download-file-btn text-blue-500 hover:text-blue-700 p-2 rounded-full hover:bg-gray-600 mr-1"
                                                data-file-id="${file.id}"
                                                data-file-name="${file.name}"
                                                title="Download file"
                                        >
                                            <span class="material-icons">download</span>
                                        </button>
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

  // Handle download button clicks
  document.addEventListener('click', function(e) {
    if (e.target.closest('.download-file-btn')) {
      const downloadBtn = e.target.closest('.download-file-btn');
      const fileId = downloadBtn.getAttribute('data-file-id');
      const fileName = downloadBtn.getAttribute('data-file-name');

      // Show loading state
      const originalIcon = downloadBtn.querySelector('.material-icons').textContent;
      downloadBtn.querySelector('.material-icons').textContent = 'downloading';
      downloadBtn.disabled = true;

      // Create a fetch request to download the file
      fetch(`/api/files/download/${fileId}`)
        .then(response => {
          if (!response.ok) {
            throw new Error('Download failed');
          }
          return response.blob();
        })
        .then(blob => {
          // Create a temporary URL for the blob
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = fileName || 'download';
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(url);
        })
        .catch(error => {
          console.error('Download error:', error);
          alert('Failed to download file: ' + error.message);
        })
        .finally(() => {
          // Restore button state
          downloadBtn.querySelector('.material-icons').textContent = originalIcon;
          downloadBtn.disabled = false;
        });
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
        // Add loading state
        const originalText = confirmDeleteBtn.innerHTML;
        confirmDeleteBtn.disabled = true;
        confirmDeleteBtn.innerHTML = '<span class="spinner">Deleting...</span>';

        fetch(`/api/files/${fileToDelete.id}`, {
          method: 'DELETE'
        })
        .then(response => {
          if (!response.ok) {
            throw new Error('Failed to delete file');
          }
          return response.json();
        })
        .then(data => {
          if (data.status === 'success') {
            // Update both file arrays
            allFiles = allFiles.filter(file => file.id !== fileToDelete.id);
            filteredFiles = filteredFiles.filter(file => file.id !== fileToDelete.id);

            // Adjust pagination if needed
            const totalPages = Math.ceil(filteredFiles.length / itemsPerPage);
            if (currentPage > totalPages && totalPages > 0) {
              currentPage = totalPages;
            }

            updateCurrentView();
          } else {
            throw new Error(data.message || 'Failed to delete file');
          }
        })
        .catch(error => {
          console.error('Delete error:', error);
          alert(error.message); // Simple alert instead of toast
        })
        .finally(() => {
          deleteModal.classList.add('hidden');
          deleteModal.classList.remove('flex');
          confirmDeleteBtn.disabled = false;
          confirmDeleteBtn.innerHTML = originalText;
          fileToDelete = null;
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

// FILE UPLOAD FUNCTIONALITY
// Form-based file upload functionality
document.addEventListener('DOMContentLoaded', function() {
    initializeFormUploadFunctionality();
});

function initializeFormUploadFunctionality() {
  // Elements
  const uploadArea = document.getElementById('uploadArea');
  const fileInput = document.getElementById('file-upload');
    const addMoreButton = document.getElementById('addMoreCaseTag');
    const caseTagsList = document.getElementById('caseTagsList');
    const uploadForm = document.getElementById('uploadForm');

    // Add More Case Tag functionality
    if (addMoreButton) {
        addMoreButton.addEventListener('click', () => {
            const newCaseTagItem = document.createElement('div');
            newCaseTagItem.className = 'case-tag-item bg-gray-800 p-4 rounded-lg mb-4';

            // Get the case tag options from the first existing select element
            const existingSelect = document.querySelector('.case-tag-select');
            const optionsHTML = existingSelect ? existingSelect.innerHTML : `
                <option value="">Select a case tag</option>
                <option value="evidence">Evidence</option>
                <option value="witness-statement">Witness Statement</option>
                <option value="forensic-report">Forensic Report</option>
                <option value="legal-document">Legal Document</option>
                <option value="investigation-notes">Investigation Notes</option>
                <option value="surveillance">Surveillance</option>
                <option value="interview-recording">Interview Recording</option>
                <option value="crime-scene-photo">Crime Scene Photo</option>
                <option value="expert-testimony">Expert Testimony</option>
                <option value="case-summary">Case Summary</option>
            `;

            newCaseTagItem.innerHTML = `
                <div class="flex gap-4 mb-3">
                    <div class="flex-1">
                        <label class="block text-sm font-medium text-gray-300 mb-2">Case Tags</label>
                        <select name="caseTags" class="case-tag-select w-full p-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500">
                            ${optionsHTML}
                        </select>
                    </div>
                    <div class="flex-1">
                        <label class="block text-sm font-medium text-gray-300 mb-2">Value</label>
                        <input type="text" name="caseValues" class="case-title-input w-full p-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="Enter case title">
                    </div>
                    <div class="flex items-end">
                        <button type="button" class="remove-case-tag bg-red-600 hover:bg-red-700 text-white p-2 rounded-lg transition-colors duration-200">
                            <span class="material-icons">remove</span>
                        </button>
                    </div>
                </div>
            `;

            caseTagsList.appendChild(newCaseTagItem);
        });
    }

    // Remove case tag functionality (using event delegation)
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('remove-case-tag') || e.target.closest('.remove-case-tag')) {
            const caseTagItem = e.target.closest('.case-tag-item');
            if (caseTagItem) {
                caseTagItem.remove();
            }
        }
    });

    // Drag and Drop functionality for the upload area
    if (uploadArea && fileInput) {
        // Drag and Drop Handlers
        uploadArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadArea.classList.add('border-blue-500', 'bg-blue-50');
        });

        uploadArea.addEventListener('dragleave', (e) => {
            e.preventDefault();
            uploadArea.classList.remove('border-blue-500', 'bg-blue-50');
        });

        uploadArea.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadArea.classList.remove('border-blue-500', 'bg-blue-50');

            if (e.dataTransfer.files.length > 0) {
                // Set the files to the file input
                fileInput.files = e.dataTransfer.files;

                // Update the upload area to show selected files
                updateUploadAreaDisplay(e.dataTransfer.files);
            }
        });

        // File input change handler
        fileInput.addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                updateUploadAreaDisplay(e.target.files);
            }
        });

        // Click handler for upload area - removed to prevent double triggering
        // The label already handles the click to open file picker
    }

    // Form validation before submission
    if (uploadForm) {
        uploadForm.addEventListener('submit', function(e) {
            const fileInput = document.getElementById('file-upload');

            if (!fileInput.files || fileInput.files.length === 0) {
                e.preventDefault();
                alert('Please select at least one file to upload.');
                return false;
            }

            // Check file sizes (10MB limit)
    const maxFileSize = 10 * 1024 * 1024; // 10MB in bytes
            const oversizedFiles = Array.from(fileInput.files).filter(file => file.size > maxFileSize);

    if (oversizedFiles.length > 0) {
                e.preventDefault();
      const fileNames = oversizedFiles.map(f => f.name).join(', ');
                alert(`The following files are too large (max 10MB): ${fileNames}`);
                return false;
            }

            // Show loading state
            const submitButton = uploadForm.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.disabled = true;
                submitButton.innerHTML = 'Uploading...';
            }

            return true;
        });
    }

    // Function to update upload area display when files are selected
    function updateUploadAreaDisplay(files) {
        const uploadArea = document.getElementById('uploadArea');
        const fileCount = files.length;

        // Find the existing label and update its content instead of replacing the entire innerHTML
        const label = uploadArea.querySelector('label[for="file-upload"]');

        if (fileCount > 0) {
            const fileList = Array.from(files).map(file => file.name).join(', ');
            const displayText = fileCount === 1 ?
                `Selected: ${files[0].name}` :
                `Selected ${fileCount} files: ${fileList.length > 50 ? fileList.substring(0, 50) + '...' : fileList}`;

            if (label) {
                label.innerHTML = `
                    <span class="material-icons text-6xl text-green-500">check_circle</span>
                    <p class="mt-4 text-lg font-semibold text-green-400">${displayText}</p>
                    <p class="text-gray-400">Click to change files</p>
                    <span class="mt-2 upload-btn-primary text-white font-bold py-2 px-4 rounded">
                        Change Files
                    </span>
                `;
            }
        } else {
            // Reset to original state
            if (label) {
                label.innerHTML = `
                    <span class="material-icons text-6xl text-gray-500">cloud_upload</span>
                    <p class="mt-4 text-lg font-semibold">Drag & Drop your files here</p>
                    <p class="text-gray-400">or</p>
                    <span class="mt-2 upload-btn-primary text-white font-bold py-2 px-4 rounded">
                        Browse Files
                    </span>
                `;
            }
        }

        // No need to recreate the file input or reassign files since we're keeping the original input
    }
}

// CASE TAG SUMMARY
//      // Accordion functionality with smooth animations
//      const accordionHeaders = document.querySelectorAll('.accordion-header');
//
//      accordionHeaders.forEach(header => {
//          header.addEventListener('click', function() {
//              const target = this.getAttribute('data-target');
//              const content = document.getElementById(target);
//              const icon = this.querySelector('.accordion-icon');
//              const isOpen = content.style.maxHeight && content.style.maxHeight !== '0px';
//
//              // Close all other accordions
//              accordionHeaders.forEach(otherHeader => {
//                  if (otherHeader !== this) {
//                      const otherTarget = otherHeader.getAttribute('data-target');
//                      const otherContent = document.getElementById(otherTarget);
//                      const otherIcon = otherHeader.querySelector('.accordion-icon');
//
//                      otherContent.style.maxHeight = '0px';
//                      otherIcon.style.transform = 'rotate(0deg)';
//                      otherHeader.classList.remove('bg-gray-600');
//                      otherHeader.classList.add('bg-gray-700');
//                  }
//              });
//
//              // Toggle current accordion
//              if (isOpen) {
//                  // Close current accordion
//                  content.style.maxHeight = '0px';
//                  icon.style.transform = 'rotate(0deg)';
//                  this.classList.remove('bg-gray-600');
//                  this.classList.add('bg-gray-700');
//              } else {
//                  // Open current accordion
//                  content.style.maxHeight = content.scrollHeight + 'px';
//                  icon.style.transform = 'rotate(180deg)';
//                  this.classList.remove('bg-gray-700');
//                  this.classList.add('bg-gray-600');
//              }
//          });
//      });
//
//      function editSummary(index) {
//          const viewDiv = document.getElementById('summary-view-' + index);
//          const editDiv = document.getElementById('summary-edit-' + index);
//
//          viewDiv.classList.add('hidden');
//          editDiv.classList.remove('hidden');
//        }
//
//        function cancelEditSummary(index) {
//          const viewDiv = document.getElementById('summary-view-' + index);
//          const editDiv = document.getElementById('summary-edit-' + index);
//          const textarea = document.getElementById('summary-textarea-' + index);
//          const originalText = document.getElementById('summary-text-' + index).textContent;
//
//          textarea.value = originalText;
//          editDiv.classList.add('hidden');
//          viewDiv.classList.remove('hidden');
//        }

  // CASE TAG DETAILS
   // Reuse the same delete functionality from gallery.html
      document.addEventListener('DOMContentLoaded', function() {
          const deleteModal = document.getElementById('deleteModal');
          const deleteFileName = document.getElementById('deleteFileName');
          const cancelDelete = document.getElementById('cancelDelete');
          const confirmDelete = document.getElementById('confirmDelete');
          let fileToDelete = null;

          // Add event listeners to delete buttons
          document.querySelectorAll('.delete-file-btn').forEach(button => {
              button.addEventListener('click', function() {
                  fileToDelete = {
                      id: this.getAttribute('data-file-id'),
                      name: this.getAttribute('data-file-name')
                  };
                  deleteFileName.textContent = fileToDelete.name;
                  deleteModal.classList.remove('hidden');
                  deleteModal.classList.add('flex');
              });
          });

          // Cancel delete
          cancelDelete.addEventListener('click', function() {
              deleteModal.classList.add('hidden');
              deleteModal.classList.remove('flex');
              fileToDelete = null;
          });

          // Confirm delete
          confirmDelete.addEventListener('click', function() {
              if (fileToDelete) {
                  fetch(`/api/files/${fileToDelete.id}`, {
                      method: 'DELETE'
    })
    .then(response => response.json())
    .then(data => {
      if (data.status === 'success') {
                          location.reload();
      } else {
                          alert('Failed to delete file: ' + data.message);
      }
    })
    .catch(error => {
                      console.error('Error:', error);
                      alert('Failed to delete file');
                  });
              }
              deleteModal.classList.add('hidden');
              deleteModal.classList.remove('flex');
          });

          // Add event listeners to download buttons
          document.querySelectorAll('.download-file-btn').forEach(button => {
              button.addEventListener('click', function() {
                  const fileId = this.getAttribute('data-file-id');
                  const fileName = this.getAttribute('data-file-name');

                  window.location.href = `/api/files/download/${fileId}`;
              });
          });
      });


// Main accordion functionality with auto-close
document.addEventListener('DOMContentLoaded', function() {
    // Main accordion functionality
    const mainAccordionHeaders = document.querySelectorAll('.main-accordion-header');

    mainAccordionHeaders.forEach(header => {
        header.addEventListener('click', function() {
            const target = this.getAttribute('data-target');
            const content = document.getElementById(target);
            const icon = this.querySelector('.main-accordion-icon');

            // Check if this accordion is currently open
            const isCurrentlyOpen = content.style.maxHeight !== '0px' && content.style.maxHeight !== '';

            // Close all main accordions first
            mainAccordionHeaders.forEach(otherHeader => {
                const otherTarget = otherHeader.getAttribute('data-target');
                const otherContent = document.getElementById(otherTarget);
                const otherIcon = otherHeader.querySelector('.main-accordion-icon');

                if (otherContent && otherIcon) {
                    otherContent.style.maxHeight = '0px';
                    otherIcon.style.transform = 'rotate(0deg)';
                }
            });

            // If the clicked accordion wasn't open, open it
            if (!isCurrentlyOpen && content && icon) {
                // Use a large max-height to ensure content is always fully visible
                content.style.maxHeight = '9999px';
                icon.style.transform = 'rotate(180deg)';
            }
        });
    });

    // Sub-accordion functionality
    const subAccordionHeaders = document.querySelectorAll('.sub-accordion-header');

    subAccordionHeaders.forEach(header => {
        header.addEventListener('click', function() {
            const target = this.getAttribute('data-target');
            const content = document.getElementById(target);
            const icon = this.querySelector('.sub-accordion-icon');

            if (content && icon) {
                if (content.style.maxHeight === '0px' || content.style.maxHeight === '') {
                    // Open sub-accordion with large max-height to ensure full content visibility
                    content.style.maxHeight = '9999px';
                    icon.style.transform = 'rotate(180deg)';

                    // Update parent accordion height
                    setTimeout(() => {
                        updateParentAccordionHeight(content);
                    }, 50);
                } else {
                    // Close sub-accordion
                    content.style.maxHeight = '0px';
                    icon.style.transform = 'rotate(0deg)';

                    // Update parent accordion height after animation
                    setTimeout(() => {
                        updateParentAccordionHeight(content);
                    }, 300);
                }
            }
        });
    });

    // Legacy accordion functionality for pages that still use the old structure
    const legacyAccordionHeaders = document.querySelectorAll('.accordion-header');

    legacyAccordionHeaders.forEach(header => {
        header.addEventListener('click', function() {
            const target = this.getAttribute('data-target');
            const content = document.getElementById(target);
            const icon = this.querySelector('.accordion-icon');

            // Check if this accordion is currently open
            const isCurrentlyOpen = content && (content.style.maxHeight !== '0px' && content.style.maxHeight !== '');

            // Close all legacy accordions first
            legacyAccordionHeaders.forEach(otherHeader => {
                const otherTarget = otherHeader.getAttribute('data-target');
                const otherContent = document.getElementById(otherTarget);
                const otherIcon = otherHeader.querySelector('.accordion-icon');

                if (otherContent && otherIcon) {
                    otherContent.style.maxHeight = '0px';
                    otherIcon.style.transform = 'rotate(0deg)';
                }
            });

            // If the clicked accordion wasn't open, open it
            if (!isCurrentlyOpen && content && icon) {
                content.style.maxHeight = '9999px';
                icon.style.transform = 'rotate(180deg)';
            }
        });
    });

    // Risk assessment data is already loaded from backend via template
    console.log('Risk assessment data loaded from backend');

    // Check if we're on the risk assessment page (standalone)
    if (document.getElementById('risk-view')) {
        console.log('Risk assessment page loaded');

        // Make sure edit button is visible initially
        const editButton = document.getElementById('edit-button');
        if (editButton) {
            editButton.classList.remove('hidden');
        }
    }

    console.log('App.js loaded and initialized');
});

function updateParentAccordionHeight(subContent) {
    const mainAccordionContent = subContent.closest('.main-accordion-content');
    if (mainAccordionContent && mainAccordionContent.style.maxHeight !== '0px') {
        // Use large max-height to ensure parent can accommodate all sub-content
        mainAccordionContent.style.maxHeight = '9999px';
    }
}

// Case summary editing functions
function editSummary(index) {
    const viewDiv = document.getElementById('summary-view-' + index);
    const editDiv = document.getElementById('summary-edit-' + index);
    const textarea = document.getElementById('summary-textarea-' + index);

    if (viewDiv && editDiv) {
        viewDiv.style.display = 'none';
        editDiv.classList.remove('hidden');

        // Auto-resize textarea when edit mode is activated
        if (textarea) {
            // Initial resize
            setTimeout(() => autoResizeTextarea(textarea), 10);

            // Add event listeners for auto-resize
            textarea.addEventListener('input', function() {
                autoResizeTextarea(this);
            });

            textarea.addEventListener('keyup', function() {
                autoResizeTextarea(this);
            });
        }

        // Update parent accordion height
        setTimeout(() => {
            const subContent = editDiv.closest('.sub-accordion-content');
            updateParentAccordionHeight(subContent);
        }, 100);
    }
}

function cancelEditSummary(index) {
    const viewDiv = document.getElementById('summary-view-' + index);
    const editDiv = document.getElementById('summary-edit-' + index);

    if (viewDiv && editDiv) {
        viewDiv.style.display = 'block';
        editDiv.classList.add('hidden');

        // Update parent accordion height
        setTimeout(() => {
            const subContent = editDiv.closest('.sub-accordion-content');
            updateParentAccordionHeight(subContent);
        }, 100);
    }
}

// Risk assessment editing functions for nested accordions
function editRiskAssessment(index) {
    console.log('editRiskAssessment called with index:', index);

    const viewDiv = document.getElementById('risk-view-' + index);
    const editDiv = document.getElementById('risk-edit-' + index);
    const editButton = document.getElementById('edit-button-' + index);

    console.log('Elements found:', {
        viewDiv: !!viewDiv,
        editDiv: !!editDiv,
        editButton: !!editButton
    });

    if (viewDiv && editDiv) {
        viewDiv.style.display = 'none';
        editDiv.classList.remove('hidden');
        if (editButton) editButton.style.display = 'none';

        // Populate the form with current content
        populateRiskAssessmentForm(index);

        // Update parent accordion height
        setTimeout(() => {
            const subContent = editDiv.closest('.sub-accordion-content');
            updateParentAccordionHeight(subContent);
        }, 100);
    } else {
        console.error('Could not find required elements for editRiskAssessment');
    }
}

function cancelEditRiskAssessment(index) {
    console.log('cancelEditRiskAssessment called with index:', index);

    const viewDiv = document.getElementById('risk-view-' + index);
    const editDiv = document.getElementById('risk-edit-' + index);
    const editButton = document.getElementById('edit-button-' + index);

    if (viewDiv && editDiv) {
        viewDiv.style.display = 'block';
        editDiv.classList.add('hidden');
        if (editButton) editButton.style.display = 'block';

        // Update parent accordion height
        setTimeout(() => {
            const subContent = editDiv.closest('.sub-accordion-content');
            updateParentAccordionHeight(subContent);
        }, 100);
    }
}

// Standalone risk assessment functions (for the dedicated risk assessment page)
function editRiskAssessment() {
    console.log('Standalone editRiskAssessment called');

    const viewDiv = document.getElementById('risk-view');
    const editDiv = document.getElementById('risk-edit');
    const editButton = document.getElementById('edit-button');

    console.log('Standalone elements found:', {
        viewDiv: !!viewDiv,
        editDiv: !!editDiv,
        editButton: !!editButton
    });

    if (viewDiv && editDiv && editButton) {
        viewDiv.classList.add('hidden');
        editDiv.classList.remove('hidden');
        editButton.classList.add('hidden');

        // Populate the form with current content
        populateRiskAssessmentForm();
    } else {
        console.error('Could not find required elements for standalone editRiskAssessment');
    }
}

function cancelEditRiskAssessment() {
    console.log('Standalone cancelEditRiskAssessment called');

    const viewDiv = document.getElementById('risk-view');
    const editDiv = document.getElementById('risk-edit');
    const editButton = document.getElementById('edit-button');

    if (viewDiv && editDiv && editButton) {
        // Reset form values to original
        if (window.riskAssessmentData) {
            const riskLevelSelect = document.getElementById('risk-level-select');
            const riskSummaryTextarea = document.getElementById('risk-summary-textarea');
            const riskFactorsTextarea = document.getElementById('risk-factors-textarea');

            if (riskLevelSelect) riskLevelSelect.value = window.riskAssessmentData.originalRiskLevel;
            if (riskSummaryTextarea) riskSummaryTextarea.value = window.riskAssessmentData.originalRiskSummary;
            if (riskFactorsTextarea) riskFactorsTextarea.value = window.riskAssessmentData.originalRiskFactors.join('\n');
        }

        editDiv.classList.add('hidden');
        viewDiv.classList.remove('hidden');
        editButton.classList.remove('hidden');
    }
}

function populateRiskAssessmentForm(index) {
    console.log('populateRiskAssessmentForm called with index:', index);

    // Handle both standalone and nested accordion versions
    const suffix = index !== undefined ? '-' + index : '';

    // Get all the content from the view mode
    const riskLevelDisplay = document.getElementById('risk-level-display' + suffix);
    const riskSummaryDisplay = document.getElementById('risk-summary-display' + suffix);
    const riskFactorsDisplay = document.getElementById('risk-factors-display' + suffix);

    // Get form elements
    const riskLevelSelect = document.getElementById('risk-level-select' + suffix);
    const riskSummaryTextarea = document.getElementById('risk-summary-textarea' + suffix);
    const riskFactorsTextarea = document.getElementById('risk-factors-textarea' + suffix);

    console.log('Form elements found:', {
        riskLevelDisplay: !!riskLevelDisplay,
        riskSummaryDisplay: !!riskSummaryDisplay,
        riskFactorsDisplay: !!riskFactorsDisplay,
        riskLevelSelect: !!riskLevelSelect,
        riskSummaryTextarea: !!riskSummaryTextarea,
        riskFactorsTextarea: !!riskFactorsTextarea
    });

    // Populate risk level
    if (riskLevelDisplay && riskLevelSelect) {
        riskLevelSelect.value = riskLevelDisplay.textContent.trim();
        console.log('Set risk level to:', riskLevelDisplay.textContent.trim());
    }

    // Populate risk summary
    if (riskSummaryDisplay && riskSummaryTextarea) {
        riskSummaryTextarea.value = riskSummaryDisplay.textContent.trim();
        console.log('Set risk summary to:', riskSummaryDisplay.textContent.trim());
    }

    // Populate risk factors
    if (riskFactorsDisplay && riskFactorsTextarea) {
        const factorItems = riskFactorsDisplay.querySelectorAll('li');
        const factors = Array.from(factorItems).map(li => li.textContent.trim());
        riskFactorsTextarea.value = factors.join('\n');
        console.log('Set risk factors to:', factors);
    }
}

// Risk assessment data is now loaded directly from the backend via Thymeleaf template
// No need for JavaScript data loading since the template already contains the actual data
function loadRiskAssessmentData() {
    console.log('Risk assessment data is loaded directly from backend via template');
    // Data is already populated in the HTML template from the backend
}

// Auto-resize textarea function
function autoResizeTextarea(textarea) {
    if (textarea) {
        // Reset height to auto to get the correct scrollHeight
        textarea.style.height = 'auto';
        // Set height to scrollHeight to fit content
        textarea.style.height = textarea.scrollHeight + 'px';
    }
}

// Auto-resize textareas on input
document.addEventListener('input', function(e) {
    if (e.target.tagName.toLowerCase() === 'textarea') {
        autoResizeTextarea(e.target);
    }
});

// PDF generation functionality for red document page - using print dialog
function downloadPDF() {
    console.log('downloadPDF function called');

    // Get the main content to print
    const mainContent = document.getElementById('red-document-content');
    if (!mainContent) {
        console.error('red-document-content not found');
        alert('PDF content not found. Please ensure you are on the red document page.');
        return;
    }

    // Show loading state
    const downloadBtn = document.getElementById('downloadPdfBtn');
    if (downloadBtn) {
        const originalText = downloadBtn.innerHTML;
        downloadBtn.innerHTML = '<span class="material-icons animate-spin">refresh</span> Generating...';
        downloadBtn.disabled = true;

        // Reset button state after opening print dialog
        setTimeout(() => {
            downloadBtn.innerHTML = originalText;
            downloadBtn.disabled = false;
        }, 1000);
    }

    // Open print dialog
    const printWindow = window.open('', '_blank');
    printWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>Red Document - Risk Factors Report</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; color: #333; line-height: 1.6; }
                h1 { text-align: center; margin-bottom: 30px; }
                h2 { color: #1f2937; border-bottom: 2px solid #dc2626; padding-bottom: 10px; margin-top: 30px; }
                h3 { color: #dc2626; margin-bottom: 15px; }
                ul { margin-left: 20px; }
                li { margin-bottom: 8px; }
                .header-info { text-align: center; margin-bottom: 40px; color: #666; }
                .footer { margin-top: 50px; text-align: center; color: #666; font-size: 12px; }
                @media print {
                    body { margin: 15px; }
                    .no-print { display: none; }
                }
            </style>
        </head>
        <body>
            <h1>Red Document - Risk Factors Report</h1>
            <div class="header-info">Generated on ${new Date().toLocaleDateString()}</div>
            ${mainContent.innerHTML}
            <div class="footer">
                <p>This document contains confidential risk assessment information.</p>
                <p>Generated by Case Management System</p>
            </div>
        </body>
        </html>
    `);
    printWindow.document.close();
    printWindow.print();
}

// Nested risk assessment functions (different names to avoid conflicts)
function editRiskAssessmentNested(index) {
    console.log('editRiskAssessmentNested called with index:', index);

    const viewDiv = document.getElementById('risk-view-' + index);
    const editDiv = document.getElementById('risk-edit-' + index);
    const editButton = document.getElementById('edit-button-' + index);

    console.log('Nested elements found:', {
        viewDiv: !!viewDiv,
        editDiv: !!editDiv,
        editButton: !!editButton
    });

    if (viewDiv && editDiv && editButton) {
        viewDiv.classList.add('hidden');
        editDiv.classList.remove('hidden');
        editButton.classList.add('hidden');

        // Populate the form with current content for nested version
        populateRiskAssessmentFormNested(index);

        // Update parent accordion height
        setTimeout(() => {
            const subContent = editDiv.closest('.sub-accordion-content');
            updateParentAccordionHeight(subContent);
        }, 100);
    } else {
        console.error('Could not find required elements for editRiskAssessmentNested');
    }
}

function cancelEditRiskAssessmentNested(index) {
    console.log('cancelEditRiskAssessmentNested called with index:', index);

    const viewDiv = document.getElementById('risk-view-' + index);
    const editDiv = document.getElementById('risk-edit-' + index);
    const editButton = document.getElementById('edit-button-' + index);

    if (viewDiv && editDiv && editButton) {
        editDiv.classList.add('hidden');
        viewDiv.classList.remove('hidden');
        editButton.classList.remove('hidden');

        // Update parent accordion height
        setTimeout(() => {
            const subContent = editDiv.closest('.sub-accordion-content');
            updateParentAccordionHeight(subContent);
        }, 100);
    }
}

function populateRiskAssessmentFormNested(index) {
    console.log('populateRiskAssessmentFormNested called with index:', index);

    // Get all the content from the view mode
    const riskLevelDisplay = document.getElementById('risk-level-display-' + index);
    const riskSummaryDisplay = document.getElementById('risk-summary-display-' + index);
    const riskFactorsDisplay = document.getElementById('risk-factors-display-' + index);

    // Get form elements
    const riskLevelSelect = document.getElementById('risk-level-select-' + index);
    const riskSummaryTextarea = document.getElementById('risk-summary-textarea-' + index);
    const riskFactorsTextarea = document.getElementById('risk-factors-textarea-' + index);

    console.log('Nested form elements found:', {
        riskLevelDisplay: !!riskLevelDisplay,
        riskSummaryDisplay: !!riskSummaryDisplay,
        riskFactorsDisplay: !!riskFactorsDisplay,
        riskLevelSelect: !!riskLevelSelect,
        riskSummaryTextarea: !!riskSummaryTextarea,
        riskFactorsTextarea: !!riskFactorsTextarea
    });

    // Populate risk level
    if (riskLevelDisplay && riskLevelSelect) {
        riskLevelSelect.value = riskLevelDisplay.textContent.trim();
        console.log('Set nested risk level to:', riskLevelDisplay.textContent.trim());
    }

    // Populate risk summary
    if (riskSummaryDisplay && riskSummaryTextarea) {
        riskSummaryTextarea.value = riskSummaryDisplay.textContent.trim();
        console.log('Set nested risk summary to:', riskSummaryDisplay.textContent.trim());
    }

    // Populate risk factors
    if (riskFactorsDisplay && riskFactorsTextarea) {
        const factorItems = riskFactorsDisplay.querySelectorAll('li');
        const factors = Array.from(factorItems).map(li => li.textContent.trim());
        riskFactorsTextarea.value = factors.join('\n');
        console.log('Set nested risk factors to:', factors);
    }
}

// Make functions globally available
window.editSummary = editSummary;
window.cancelEditSummary = cancelEditSummary;
window.editRiskAssessment = editRiskAssessment;
window.cancelEditRiskAssessment = cancelEditRiskAssessment;
window.editRiskAssessmentNested = editRiskAssessmentNested;
window.cancelEditRiskAssessmentNested = cancelEditRiskAssessmentNested;
window.downloadPDF = downloadPDF;
