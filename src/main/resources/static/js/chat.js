// Global variables for chat functionality
let users = [];
let conversations = [];
let currentConversation = null;
const currentUserId = 'current_user'; // Will be determined by authentication
const currentUserName = 'You';

// Multi-recipient functionality
let selectedRecipients = [];
let modalSelectedUsers = []; // For the new chat modal

// API functions to fetch real data
async function fetchUsers() {
  try {
    console.log('Fetching users from /api/chat/users...');
    const response = await fetch('/api/users');
    console.log('Users API response status:', response.status);
    
    if (response.ok) {
      const userData = await response.json();
      console.log('Raw user data from API:', userData);
      
      if (userData && userData.length > 0) {
        users = userData.map(user => ({
          id: user.id,
          name: user.firstName + ' ' + user.lastName,
          avatar: user.avatar,
          online: user.online,
          email: user.email,
          userGroup: user.userGroup,
          status: user.status
        }));
        console.log('Processed users:', users);
      } else {
        console.log('No users returned from API');
        users = [];
      }
    } else {
      console.error('Failed to fetch users, status:', response.status);
      const errorText = await response.text();
      console.error('Error response:', errorText);
      users = [];
    }
  } catch (error) {
    console.error('Error fetching users:', error);
    users = [];
  }
}

async function fetchConversations() {
  try {
    console.log('Fetching conversations from /api/chat/conversations...');
    const response = await fetch('/api/conversations');
    console.log('Conversations API response status:', response.status);
    
    if (response.ok) {
      const conversationData = await response.json();
      console.log('Raw conversation data from API:', conversationData);
      
      if (conversationData && conversationData.length > 0) {
        const apiConversations = conversationData.map(conv => ({
          id: conv.id,
          name: conv.title || (conv.group ? 'Group Chat' : 'Unknown'),
          avatar: conv.title ? conv.title.charAt(0).toUpperCase() : 'G',
          last: conv.messages && conv.messages.length > 0 ? 
            conv.messages[conv.messages.length - 1].content : '',
          lastTime: conv.messages && conv.messages.length > 0 ? 
            formatTimeForDisplay(conv.messages[conv.messages.length - 1].sentAt) : '',
          online: true,
          group: conv.group,
          messages: conv.messages.map(msg => ({
            fromMe: msg.sender,
            text: msg.content,
            sentAt: msg.sentAt
          })),
          createdAt: conv.createdAt,
          fromAPI: true // Mark as coming from API
        }));
        
        // Merge API conversations with local conversations
        // Keep local conversations that aren't in API, and update API ones
        const localConversations = conversations.filter(conv => !conv.fromAPI);
        const mergedConversations = [...localConversations, ...apiConversations];
        
        conversations = mergedConversations;
        
        if (conversations.length > 0 && !currentConversation) {
          currentConversation = conversations[0];
        }
        console.log('Processed conversations:', conversations);
      } else {
        console.log('No conversations returned from API');
        // Don't clear conversations if API returns empty - keep local ones
        if (conversations.length === 0) {
          conversations = [];
        }
      }
    } else {
      console.error('Failed to fetch conversations, status:', response.status);
      const errorText = await response.text();
      console.error('Error response:', errorText);
      // Don't clear conversations on API error - keep existing ones
    }
  } catch (error) {
    console.error('Error fetching conversations:', error);
    // Don't clear conversations on error - keep existing ones
  }
}

function formatTimeForDisplay(sentAt) {
  // Convert backend time format to display format
  if (sentAt.includes('Today')) {
    return sentAt.replace('Today, ', '').replace(' PM', 'pm').replace(' AM', 'am');
  }
  return sentAt;
}

// Initialize data on page load
async function initializeChat() {
  await fetchUsers();
  await fetchConversations();
  renderConversations();
  renderChat(); // This will show empty state since no conversation is selected initially
  populateUserDropdown();
  populateModalUsers();
}

// Modal User Management Functions
function populateModalUsers() {
  const usersList = document.getElementById('modal-users-list');
  const emptyState = document.getElementById('modal-users-empty');
  
  if (!usersList || !emptyState) return;
  
  usersList.innerHTML = '';
  
  if (users && users.length > 0) {
    emptyState.classList.add('hidden');
    
    users.forEach(user => {
      const userOption = document.createElement('div');
      userOption.className = 'flex items-center p-2 hover:bg-slate-100 dark:hover:bg-slate-600 rounded cursor-pointer transition-colors';
      userOption.innerHTML = `
        <input type="checkbox" class="modal-user-checkbox mr-3" data-user-id="${user.id}" data-user-name="${user.name}">
        <span class="w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center text-white text-sm mr-3 flex-shrink-0">
          ${user.avatar}
        </span>
        <div class="flex-1 min-w-0">
          <div class="text-sm font-medium text-slate-900 dark:text-white truncate">${user.name}</div>
          <div class="text-xs text-slate-500 dark:text-slate-400 truncate">${user.email || user.userGroup || ''}</div>
        </div>
      `;
      
      // Make the entire row clickable
      userOption.addEventListener('click', (e) => {
        if (e.target.type !== 'checkbox') {
          const checkbox = userOption.querySelector('.modal-user-checkbox');
          checkbox.checked = !checkbox.checked;
          handleModalUserSelection(checkbox);
        }
      });
      
      // Handle checkbox change
      const checkbox = userOption.querySelector('.modal-user-checkbox');
      checkbox.addEventListener('change', () => handleModalUserSelection(checkbox));
      
      usersList.appendChild(userOption);
    });
  } else {
    emptyState.classList.remove('hidden');
  }
}

function handleModalUserSelection(checkbox) {
  const userId = checkbox.dataset.userId;
  const userName = checkbox.dataset.userName;
  
  if (checkbox.checked) {
    // Add user to selection
    if (!modalSelectedUsers.find(u => u.id === userId)) {
      modalSelectedUsers.push({ id: userId, name: userName });
    }
  } else {
    // Remove user from selection
    modalSelectedUsers = modalSelectedUsers.filter(u => u.id !== userId);
  }
  
  updateModalSelectedUsersDisplay();
  updateStartChatButton();
}

function updateModalSelectedUsersDisplay() {
  const display = document.getElementById('modal-selected-users-display');
  const container = document.getElementById('modal-selected-users-container');
  
  if (!display || !container) return;
  
  if (modalSelectedUsers.length === 0) {
    display.classList.add('hidden');
    return;
  }
  
  display.classList.remove('hidden');
  container.innerHTML = '';
  
  modalSelectedUsers.forEach(user => {
    const badge = document.createElement('span');
    badge.className = 'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200';
    badge.innerHTML = `
      ${user.name}
      <button type="button" class="ml-1 text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300" onclick="removeModalUser('${user.id}')">
        <span class="material-icons text-xs">close</span>
      </button>
    `;
    container.appendChild(badge);
  });
}

function removeModalUser(userId) {
  modalSelectedUsers = modalSelectedUsers.filter(u => u.id !== userId);
  
  // Uncheck the checkbox
  const checkbox = document.querySelector(`#modal-users-list .modal-user-checkbox[data-user-id="${userId}"]`);
  if (checkbox) checkbox.checked = false;
  
  updateModalSelectedUsersDisplay();
  updateStartChatButton();
}

function updateStartChatButton() {
  const startButton = document.getElementById('start-new-chat');
  if (startButton) {
    startButton.disabled = modalSelectedUsers.length === 0;
  }
}

function clearModalSelection() {
  modalSelectedUsers = [];
  document.querySelectorAll('#modal-users-list .modal-user-checkbox').forEach(cb => cb.checked = false);
  updateModalSelectedUsersDisplay();
  updateStartChatButton();
}

function filterModalUsers() {
  const searchInput = document.getElementById('modal-user-search');
  const usersList = document.getElementById('modal-users-list');
  
  if (!searchInput || !usersList) return;
  
  const searchTerm = searchInput.value.toLowerCase();
  const userOptions = usersList.querySelectorAll('.flex.items-center');
  
  userOptions.forEach(option => {
    const userName = option.querySelector('.text-sm.font-medium').textContent.toLowerCase();
    const userEmail = option.querySelector('.text-xs').textContent.toLowerCase();
    
    if (userName.includes(searchTerm) || userEmail.includes(searchTerm)) {
      option.style.display = 'flex';
    } else {
      option.style.display = 'none';
    }
  });
}

// Existing dropdown functionality (for multi-recipient in chat input)
function populateUserDropdown() {
  const dropdown = document.getElementById('recipients-dropdown');
  if (!dropdown) return;
  
  // Clear existing options
  const existingUsers = dropdown.querySelectorAll('.recipient-option');
  existingUsers.forEach(option => option.remove());
  
  // Add users to dropdown
  if (users && users.length > 0) {
    users.forEach(user => {
      if (!user.group) { // Only add individual users, not groups
        const option = document.createElement('div');
        option.className = 'recipient-option flex items-center p-2 hover:bg-slate-100 dark:hover:bg-slate-600';
        option.innerHTML = `
          <input type="checkbox" class="recipient-checkbox mr-2" 
                 data-id="${user.id}" data-type="user" data-name="${user.name}">
          <span class="w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center text-white text-sm mr-2 flex-shrink-0">
            ${user.avatar}
          </span>
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium truncate">${user.name}</div>
            <div class="text-xs text-slate-500 truncate">${user.email || ''}</div>
          </div>
        `;
        dropdown.appendChild(option);
      }
    });
  } else {
    // Show empty state in dropdown
    const emptyState = document.createElement('div');
    emptyState.className = 'p-4 text-center text-slate-500 dark:text-slate-400 text-sm';
    emptyState.innerHTML = 'No users available';
    dropdown.appendChild(emptyState);
  }
}

function renderConversations() {
  const list = document.getElementById('conversation-list');
  if (!list) return;
  
  list.innerHTML = '';
  
  if (conversations && conversations.length > 0) {
    conversations.forEach(conv => {
      const li = document.createElement('li');
      li.className = 'p-4 hover:bg-slate-700 cursor-pointer flex items-center' + (currentConversation && currentConversation.id === conv.id ? ' bg-slate-700' : '');
      li.onclick = () => { currentConversation = conv; renderChat(); renderConversations(); };
      li.innerHTML = `
        <span class="w-10 h-10 rounded-full ${conv.group ? 'bg-green-500' : 'bg-blue-500'} flex items-center justify-center text-white font-bold mr-3">${conv.avatar}</span>
        <div class="flex-1">
          <div class="font-semibold text-white">${conv.name}</div>
          <div class="text-slate-400 text-xs truncate">${conv.last}</div>
        </div>
        <span class="text-xs text-slate-400 ml-2">${conv.lastTime}</span>
      `;
      list.appendChild(li);
    });
  } else {
    // Show empty state with start chat button
    const emptyState = document.createElement('li');
    emptyState.className = 'p-8 text-center';
    emptyState.innerHTML = `
      <div class="text-slate-400 dark:text-slate-500">
        <div class="text-4xl mb-3"><span class="material-icons text-4xl">chat</span></div>
        <div class="font-medium mb-2">No conversations yet</div>
        <div class="text-xs mb-4">Start chatting with your colleagues</div>
        <button id="start-first-chat" class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors inline-flex items-center">
          <span class="material-icons text-sm mr-1">add</span>
          Start New Chat
        </button>
      </div>
    `;
    list.appendChild(emptyState);
    
    // Add click handler for the start chat button
    const startFirstChatBtn = emptyState.querySelector('#start-first-chat');
    if (startFirstChatBtn) {
      startFirstChatBtn.onclick = () => {
        document.getElementById('new-chat-modal').classList.remove('hidden');
      };
    }
  }
}

function renderChat() {
  const title = document.getElementById('chat-title');
  const avatar = document.getElementById('chat-avatar');
  const status = document.getElementById('chat-status');
  const messagesDiv = document.getElementById('chat-messages');
  const videoCallBtn = document.getElementById('video-call-btn');
  
  if (!title || !avatar || !status || !messagesDiv) return;
  
  if (!currentConversation) {
    // Show empty state in chat area
    title.textContent = 'No conversation selected';
    avatar.innerHTML = '<span class="material-icons">chat</span>';
    status.textContent = '';
    
    // Hide video call button when no conversation selected
    if (videoCallBtn) {
      videoCallBtn.style.display = 'none';
    }
    
    messagesDiv.innerHTML = `
      <div class="flex-1 flex items-center justify-center">
        <div class="text-center text-slate-400 dark:text-slate-500">
          <div class="text-6xl mb-4"><span class="material-icons text-6xl">chat_bubble_outline</span></div>
          <div class="text-xl font-medium mb-2">Select a conversation</div>
          <div class="text-sm mb-4">Choose a conversation from the sidebar to start messaging</div>
          <button id="main-start-chat" class="bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-lg font-medium transition-colors inline-flex items-center">
            <span class="material-icons text-sm mr-2">add</span>
            Start New Chat
          </button>
        </div>
      </div>
    `;
    
    // Add click handler for the main start chat button
    const mainStartChatBtn = messagesDiv.querySelector('#main-start-chat');
    if (mainStartChatBtn) {
      mainStartChatBtn.onclick = () => {
        document.getElementById('new-chat-modal').classList.remove('hidden');
      };
    }
    return;
  }
  
  // Show conversation details
  title.textContent = currentConversation.name;
  avatar.textContent = currentConversation.avatar;
  status.textContent = currentConversation.online ? 'Online' : 'Offline';
  
  // Show video call button when conversation is selected
  if (videoCallBtn) {
    videoCallBtn.style.display = 'block';
  }
  
  messagesDiv.innerHTML = '';
  
  if (currentConversation.messages && currentConversation.messages.length > 0) {
    currentConversation.messages.forEach(msg => {
      const msgDiv = document.createElement('div');
      msgDiv.className = 'flex ' + (msg.fromMe ? 'justify-end' : 'justify-start');
      msgDiv.innerHTML = `<div class="${msg.fromMe ? 'bg-blue-600 text-white' : 'bg-slate-200 dark:bg-slate-700 text-slate-900 dark:text-white'} rounded-lg px-4 py-2 max-w-xs shadow">${msg.text}</div>`;
      messagesDiv.appendChild(msgDiv);
    });
  } else {
    // Show empty state for messages
    const emptyState = document.createElement('div');
    emptyState.className = 'flex-1 flex items-center justify-center text-center text-slate-400 dark:text-slate-500';
    emptyState.innerHTML = `
      <div>
        <div class="text-3xl mb-2"><span class="material-icons text-3xl">waving_hand</span></div>
        <div class="font-medium">No messages yet</div>
        <div class="text-sm mt-1">Send a message to start the conversation</div>
      </div>
    `;
    messagesDiv.appendChild(emptyState);
  }
  messagesDiv.scrollTop = messagesDiv.scrollHeight;
}

// Existing recipient functions for chat input
function updateRecipientsDisplay() {
  const display = document.getElementById('recipients-display');
  const container = document.getElementById('selected-recipients');
  if (!display || !container) return;

  if (selectedRecipients.length === 0) {
    display.classList.add('hidden');
    return;
  }

  display.classList.remove('hidden');
  container.innerHTML = '';
  
  selectedRecipients.forEach(recipient => {
    const badge = document.createElement('span');
    badge.className = 'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200';
    badge.innerHTML = `
      ${recipient.name}
      <button type="button" class="ml-1 text-blue-600 hover:text-blue-800" onclick="removeRecipient('${recipient.id}', '${recipient.type}')">
        <span class="material-icons text-xs">close</span>
      </button>
    `;
    container.appendChild(badge);
  });
}

function addRecipient(id, type, name) {
  const existing = selectedRecipients.find(r => r.id === id && r.type === type);
  if (!existing) {
    selectedRecipients.push({ id, type, name });
    updateRecipientsDisplay();
  }
}

function removeRecipient(id, type) {
  selectedRecipients = selectedRecipients.filter(r => !(r.id === id && r.type === type));
  const checkbox = document.querySelector(`input[data-id="${id}"][data-type="${type}"]`);
  if (checkbox) checkbox.checked = false;
  updateRecipientsDisplay();
}

function clearRecipients() {
  selectedRecipients = [];
  document.querySelectorAll('.recipient-checkbox').forEach(cb => cb.checked = false);
  updateRecipientsDisplay();
}

// Message sending functions (existing)
async function sendMessageToBackend(receiverId, receiverName, content, isGroup = false) {
  try {
    const formData = new FormData();
    formData.append('senderId', currentUserId);
    formData.append('senderName', currentUserName);
    formData.append('content', content);
    
    let url;
    if (isGroup) {
      url = '/api/chat/messages/send-group';
      formData.append('receiverIds', receiverId.split(','));
    } else {
      url = '/api/chat/messages/send';
      formData.append('receiverId', receiverId);
      formData.append('receiverName', receiverName);
    }
    
    const response = await fetch(url, {
      method: 'POST',
      body: formData
    });
    
    const result = await response.json();
    if (result.status === 'success') {
      console.log('Message sent successfully:', result);
      // Don't refresh conversations automatically to preserve local conversations
      // The UI is already updated immediately in the calling function
      return result;
    } else {
      console.error('Failed to send message:', result.message);
      return null;
    }
  } catch (error) {
    console.error('Error sending message:', error);
    return null;
  }
}

async function sendToMultipleRecipients(content) {
  const promises = selectedRecipients.map(recipient => {
    if (recipient.type === 'group') {
      return sendMessageToBackend(recipient.id, recipient.name, content, true);
    } else {
      return sendMessageToBackend(recipient.id, recipient.name, content, false);
    }
  });
  
  try {
    const results = await Promise.all(promises);
    const successful = results.filter(r => r !== null).length;
    console.log(`Message sent to ${successful}/${selectedRecipients.length} recipients`);
    
    if (successful > 0) {
      addRecipientsToSidebar(content);
    }
    
    return successful > 0;
  } catch (error) {
    console.error('Error sending to multiple recipients:', error);
    return false;
  }
}

function addRecipientsToSidebar(lastMessage) {
  selectedRecipients.forEach(recipient => {
    // Generate proper conversation ID for consistency with modal conversations
    let conversationId;
    if (recipient.type === 'group') {
      conversationId = 'group_' + recipient.id;
    } else {
      const userIds = [currentUserId, recipient.id].sort();
      conversationId = userIds.join('_');
    }
    
    const existingConv = conversations.find(conv => 
      conv.id.toString() === conversationId
    );
    
    if (!existingConv) {
      const newConv = {
        id: conversationId, // Use proper conversation ID instead of parseInt
        name: recipient.type === 'group' ? `Group: ${recipient.name}` : recipient.name,
        avatar: recipient.name.charAt(0).toUpperCase(),
        last: lastMessage.length > 30 ? lastMessage.substring(0, 30) + '...' : lastMessage,
        lastTime: 'now',
        online: true,
        group: recipient.type === 'group',
        messages: [
          { fromMe: true, text: lastMessage }
        ],
        members: [recipient.id], // Always store as array for consistency
        fromAPI: false // Mark as local conversation
      };
      
      conversations.unshift(newConv);
    } else {
      existingConv.last = lastMessage.length > 30 ? lastMessage.substring(0, 30) + '...' : lastMessage;
      existingConv.lastTime = 'now';
      existingConv.messages.push({ fromMe: true, text: lastMessage });
      
      const index = conversations.indexOf(existingConv);
      if (index > 0) {
        conversations.splice(index, 1);
        conversations.unshift(existingConv);
      }
    }
  });
  
  renderConversations();
}

document.addEventListener('DOMContentLoaded', function() {
  // Initialize chat with real data - starts completely empty
  initializeChat();

  // Recipients dropdown functionality (for chat input)
  const dropdownBtn = document.getElementById('recipients-dropdown-btn');
  const dropdown = document.getElementById('recipients-dropdown');
  const clearBtn = document.getElementById('clear-recipients');

  if (dropdownBtn && dropdown) {
    dropdownBtn.onclick = function(e) {
      e.stopPropagation();
      dropdown.classList.toggle('hidden');
    };
  }

  if (clearBtn) {
    clearBtn.onclick = clearRecipients;
  }

  // Handle recipient checkbox changes (delegated event handling)
  document.addEventListener('change', function(e) {
    if (e.target.classList.contains('recipient-checkbox')) {
      const id = e.target.dataset.id;
      const type = e.target.dataset.type;
      const name = e.target.dataset.name;
      
      if (e.target.checked) {
        addRecipient(id, type, name);
      } else {
        removeRecipient(id, type);
      }
    }
  });

  // Close dropdown when clicking outside
  document.addEventListener('click', function(e) {
    if (dropdown && !dropdown.contains(e.target) && e.target !== dropdownBtn) {
      dropdown.classList.add('hidden');
    }
  });

  const chatForm = document.getElementById('chat-form');
  if (chatForm) {
    chatForm.onsubmit = async function(e) {
      e.preventDefault();
      const input = document.getElementById('user-chat-input');
      const text = input.value.trim();
      if (!text) return;
      
      let success = false;
      
      if (selectedRecipients.length > 0) {
        success = await sendToMultipleRecipients(text);
        if (success) {
          clearRecipients();
        }
      } else if (currentConversation) {
        // Add message to UI immediately for better UX
        currentConversation.messages.push({ fromMe: true, text });
        currentConversation.last = text;
        currentConversation.lastTime = 'now';
        
        // Ensure the conversation is in the conversations array (for modal-created conversations)
        const existingConvIndex = conversations.findIndex(conv => conv.id === currentConversation.id);
        if (existingConvIndex === -1) {
          // Add to conversations array if not already there
          conversations.unshift(currentConversation);
        } else {
          // Update existing conversation and move to top
          conversations[existingConvIndex] = currentConversation;
          if (existingConvIndex > 0) {
            conversations.splice(existingConvIndex, 1);
            conversations.unshift(currentConversation);
          }
        }
        
        renderChat();
        renderConversations();
        
        // Fix receiverId logic for modal-created conversations
        let receiverId, receiverName, isGroup;
        
        if (currentConversation.group) {
          // For group conversations, use all member IDs
          receiverId = currentConversation.members?.join(',') || 'group';
          receiverName = currentConversation.name;
          isGroup = true;
        } else {
          // For individual conversations, use the other person's ID
          if (currentConversation.members && currentConversation.members.length > 0) {
            // Use the first (and only) member ID for individual chats
            receiverId = currentConversation.members[0];
          } else {
            // Fallback for old conversation format - extract from conversation ID
            const conversationIdParts = currentConversation.id.toString().split('_');
            receiverId = conversationIdParts.find(part => part !== currentUserId) || currentConversation.id.toString();
          }
          receiverName = currentConversation.name;
          isGroup = false;
        }
        
        const result = await sendMessageToBackend(receiverId, receiverName, text, isGroup);
        success = result !== null;
      }
      
      if (success) {
        input.value = '';
      } else {
        console.log('Message failed to send to backend');
      }
    };
  }

  // Sidebar collapse/expand
  const sidebar = document.getElementById('chat-sidebar');
  const root = document.getElementById('chat-root');
  const sidebarToggle = document.getElementById('sidebar-toggle');
  if (sidebarToggle && sidebar) {
    sidebarToggle.onclick = function() {
      sidebar.classList.toggle('active');
    };
  }
  
  // Hide sidebar on click outside (mobile)
  document.addEventListener('click', function(e) {
    if (window.innerWidth <= 768 && sidebar && sidebar.classList.contains('active') && !sidebar.contains(e.target) && e.target.id !== 'sidebar-toggle') {
      sidebar.classList.remove('active');
    }
  });

  // New chat modal functionality
  const modal = document.getElementById('new-chat-modal');
  const newChatBtn = document.getElementById('new-chat-btn');
  const cancelNewChat = document.getElementById('cancel-new-chat');
  const startNewChat = document.getElementById('start-new-chat');
  const modalDropdownBtn = document.getElementById('modal-user-dropdown-btn');
  const modalDropdown = document.getElementById('modal-user-dropdown');
  const modalUserSearch = document.getElementById('modal-user-search');
  
  if (newChatBtn && modal) {
    newChatBtn.onclick = function() {
      modal.classList.remove('hidden');
      clearModalSelection();
    };
  }
  
  if (modalDropdownBtn && modalDropdown) {
    modalDropdownBtn.onclick = function(e) {
      e.stopPropagation();
      modalDropdown.classList.toggle('hidden');
    };
  }
  
  if (modalUserSearch) {
    modalUserSearch.addEventListener('input', filterModalUsers);
  }
  
  if (cancelNewChat && modal) {
    cancelNewChat.onclick = function() {
      modal.classList.add('hidden');
      clearModalSelection();
    };
  }
  
  if (startNewChat && modal) {
    startNewChat.onclick = function() {
      if (modalSelectedUsers.length === 0) return;
      
      // Create conversation from selected users
      let group = modalSelectedUsers.length > 1;
      let name = group ? 
        `Group: ${modalSelectedUsers.map(u => u.name.split(' ')[0]).join(', ')}` : 
        modalSelectedUsers[0].name;
      let avatar = group ? 'G' : modalSelectedUsers[0].name.charAt(0).toUpperCase();
      
      // Generate proper conversation ID based on backend format
      let conversationId;
      if (group) {
        // For groups, include all selected users
        const sortedIds = modalSelectedUsers.map(u => u.id).sort();
        conversationId = 'group_' + sortedIds.join('_');
      } else {
        // For individual chats, create conversation ID with current user and selected user
        const otherUserId = modalSelectedUsers[0].id;
        const userIds = [currentUserId, otherUserId].sort();
        conversationId = userIds.join('_');
      }
      
      const newConv = {
        id: conversationId,
        name,
        avatar,
        last: '',
        lastTime: 'now',
        online: true,
        group,
        messages: [],
        // For individual chats, members should only include the other person
        // For group chats, include all selected users
        members: group ? modalSelectedUsers.map(u => u.id) : [modalSelectedUsers[0].id],
        fromAPI: false // Mark as local conversation so it doesn't get overwritten
      };
      
      conversations.unshift(newConv);
      currentConversation = newConv;
      renderConversations();
      renderChat();
      modal.classList.add('hidden');
      clearModalSelection();
    };
  }
  
  // Close modal dropdown when clicking outside
  document.addEventListener('click', function(e) {
    if (modalDropdown && !modalDropdown.contains(e.target) && e.target !== modalDropdownBtn) {
      modalDropdown.classList.add('hidden');
    }
  });
  
  // Close modal when clicking outside
  document.addEventListener('click', function(e) {
    if (modal && e.target === modal) {
      modal.classList.add('hidden');
      clearModalSelection();
    }
  });
});

/*
//TODO THis is to send message via web socket
function sendMessageOverWebSocket(text) {
  if (!stompClient || !stompClient.connected) {
    console.warn("STOMP not connected. Message not sent.");
    return;
  }

  const message = {
    conversationId: 123,
    content: text
  };
  stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(message));
}

//TODO THis is to receive messages via websockets
let stompClient = null //At the beginning
const socket = new SockJS('/ws');
  stompClient = Stomp.over(socket);

  stompClient.connect({}, function (frame) {
    console.log('Connected:', frame);

    const conv = conversations[1];
    stompClient.subscribe(`/topic/group/123`, function (messageOutput) {
      console.log(messageOutput);
      const message = JSON.parse(messageOutput.body);
      conv.messages.push({fromMe: false, text: message.content});
      conv.last = message.content;
      conv.lastTime = 'now';
      if (currentConversation.id === conv.id) {
        renderChat();
        renderConversations();
      }
    });
  });
*/
