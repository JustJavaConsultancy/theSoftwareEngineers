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

document.addEventListener('DOMContentLoaded', function() {
  renderConversations();
  renderChat();

  const chatForm = document.getElementById('chat-form');
  if (chatForm) {
    chatForm.onsubmit = function(e) {
      e.preventDefault();
      const input = document.getElementById('chat-input');
      const text = input.value.trim();
      if (!text) return;
      currentConversation.messages.push({ fromMe: true, text });
      currentConversation.last = text;
      currentConversation.lastTime = 'now';
      input.value = '';
      renderChat();
      renderConversations();
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
        messages: []
      };
      conversations.unshift(newConv);
      currentConversation = newConv;
      renderConversations();
      renderChat();
      modal.classList.add('hidden');
    };
  }
}); 