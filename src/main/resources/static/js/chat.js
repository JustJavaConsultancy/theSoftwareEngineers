// Nigerian names for demo
const users = [
  { id: 1, name: 'Chinedu', avatar: 'C', online: true },
  { id: 2, name: 'Amaka', avatar: 'A', online: false },
  { id: 3, name: 'Tunde', avatar: 'T', online: true },
  { id: 4, name: 'Ngozi', avatar: 'N', online: false },
  { id: 5, name: 'Group: Lagos Office', avatar: 'G', online: true, group: true, members: [1,2,3,4] }
];
let conversations = [
  { id: 1, name: 'Chinedu', avatar: 'C', last: 'See you soon!', lastTime: '2m', online: true, messages: [
    { fromMe: false, text: 'Hi! How can I help you?' },
    { fromMe: true, text: 'Hey Chinedu!' }
  ] },
  { id: 5, name: 'Group: Lagos Office', avatar: 'G', last: 'Meeting at 2pm', lastTime: '10m', online: true, group: true, messages: [
    { fromMe: false, text: 'Meeting at 2pm' },
    { fromMe: true, text: 'Thanks for the update!' }
  ] }
];
let currentConversation = conversations[0];
const currentUserId = 'current_user';
const currentUserName = 'You';

// Multi-recipient functionality
let selectedRecipients = [];

function renderConversations() {
  const list = document.getElementById('conversation-list');
  if (!list) return;
  list.innerHTML = '';
  conversations.forEach(conv => {
    const li = document.createElement('li');
    li.className = 'p-4 hover:bg-slate-700 cursor-pointer flex items-center' + (currentConversation.id === conv.id ? ' bg-slate-700' : '');
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
}

function renderChat() {
  const title = document.getElementById('chat-title');
  const avatar = document.getElementById('chat-avatar');
  const status = document.getElementById('chat-status');
  const messagesDiv = document.getElementById('chat-messages');
  if (!title || !avatar || !status || !messagesDiv) return;
  title.textContent = currentConversation.name;
  avatar.textContent = currentConversation.avatar;
  status.textContent = currentConversation.online ? 'Online' : 'Offline';
  messagesDiv.innerHTML = '';
  currentConversation.messages.forEach(msg => {
    const msgDiv = document.createElement('div');
    msgDiv.className = 'flex ' + (msg.fromMe ? 'justify-end' : 'justify-start');
    msgDiv.innerHTML = `<div class="${msg.fromMe ? 'bg-blue-600 text-white' : 'bg-slate-200 dark:bg-slate-700 text-slate-900 dark:text-white'} rounded-lg px-4 py-2 max-w-xs shadow">${msg.text}</div>`;
    messagesDiv.appendChild(msgDiv);
  });
  messagesDiv.scrollTop = messagesDiv.scrollHeight;
}

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
      // For groups, send as group message
      return sendMessageToBackend(recipient.id, recipient.name, content, true);
    } else {
      // For individual users, send individual message
      return sendMessageToBackend(recipient.id, recipient.name, content, false);
    }
  });
  
  try {
    const results = await Promise.all(promises);
    const successful = results.filter(r => r !== null).length;
    console.log(`Message sent to ${successful}/${selectedRecipients.length} recipients`);
    
    // Add conversations to sidebar for new recipients
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
    // Check if conversation already exists
    const existingConv = conversations.find(conv => 
      conv.id.toString() === recipient.id && 
      conv.group === (recipient.type === 'group')
    );
    
    if (!existingConv) {
      // Create new conversation
      const newConv = {
        id: parseInt(recipient.id),
        name: recipient.type === 'group' ? `Group: ${recipient.name}` : recipient.name,
        avatar: recipient.name.charAt(0).toUpperCase(),
        last: lastMessage.length > 30 ? lastMessage.substring(0, 30) + '...' : lastMessage,
        lastTime: 'now',
        online: true,
        group: recipient.type === 'group',
        messages: [
          { fromMe: true, text: lastMessage }
        ],
        members: recipient.type === 'group' ? [recipient.id] : undefined
      };
      
      // Add to conversations list
      conversations.unshift(newConv);
    } else {
      // Update existing conversation
      existingConv.last = lastMessage.length > 30 ? lastMessage.substring(0, 30) + '...' : lastMessage;
      existingConv.lastTime = 'now';
      existingConv.messages.push({ fromMe: true, text: lastMessage });
      
      // Move to top of conversations
      const index = conversations.indexOf(existingConv);
      if (index > 0) {
        conversations.splice(index, 1);
        conversations.unshift(existingConv);
      }
    }
  });
  
  // Re-render conversations to show updates
  renderConversations();
}

document.addEventListener('DOMContentLoaded', function() {
  renderConversations();
  renderChat();

  // Recipients dropdown functionality
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

  // Handle recipient checkbox changes
  document.querySelectorAll('.recipient-checkbox').forEach(checkbox => {
    checkbox.addEventListener('change', function() {
      const id = this.dataset.id;
      const type = this.dataset.type;
      const name = this.dataset.name;
      
      if (this.checked) {
        addRecipient(id, type, name);
      } else {
        removeRecipient(id, type);
      }
    });
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
      
      // If recipients are selected, send to multiple recipients
      if (selectedRecipients.length > 0) {
        success = await sendToMultipleRecipients(text);
        if (success) {
          // Clear recipients after successful send
          clearRecipients();
        }
      } else {
        // Send to current conversation only
        currentConversation.messages.push({ fromMe: true, text });
        currentConversation.last = text;
        currentConversation.lastTime = 'now';
        renderChat();
        renderConversations();
        
        const receiverId = currentConversation.group ? 
          currentConversation.members?.join(',') || 'group' : 
          currentConversation.id.toString();
        const receiverName = currentConversation.name;
        const isGroup = currentConversation.group || false;
        
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

  // New chat modal logic
  const modal = document.getElementById('new-chat-modal');
  const newChatBtn = document.getElementById('new-chat-btn');
  const cancelNewChat = document.getElementById('cancel-new-chat');
  const startNewChat = document.getElementById('start-new-chat');
  if (newChatBtn && modal) {
    newChatBtn.onclick = function() {
      modal.classList.remove('hidden');
      document.getElementById('new-chat-users').value = '';
    };
  }
  if (cancelNewChat && modal) {
    cancelNewChat.onclick = function() {
      modal.classList.add('hidden');
    };
  }
  if (startNewChat && modal) {
    startNewChat.onclick = function() {
      const names = document.getElementById('new-chat-users').value.split(',').map(n => n.trim()).filter(Boolean);
      if (!names.length) return;
      let group = names.length > 1;
      let avatar = group ? 'G' : names[0][0].toUpperCase();
      let name = group ? `Group: ${names.join(', ')}` : names[0];
      const newConv = {
        id: Date.now(),
        name,
        avatar,
        last: '',
        lastTime: 'now',
        online: true,
        group,
        messages: [],
        members: group ? names : undefined
      };
      conversations.unshift(newConv);
      currentConversation = newConv;
      renderConversations();
      renderChat();
      modal.classList.add('hidden');
    };
  }
}); 