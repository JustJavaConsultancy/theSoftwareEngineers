const currentUser = document.getElementById('currentUserId').value;
       const currentUserName = document.getElementById('currentUserName').value;
       let stompClient = null;
       const socket = new SockJS('/ws');
       stompClient = Stomp.over(socket);

       // Variables to store current chat info
       let currentChat = {
           type: null,
           id: null,
           name: null,
           userIds: []
       };

       // Store all conversations data
       let allConversations = {};

       // Connect to WebSocket
       stompClient.connect({}, function (frame) {
           console.log('Connected:', frame);

           // Subscribe to user messages
           stompClient.subscribe(`/user/${currentUser}/queue/messages`, function (message) {
               handleIncomingMessage(message);
           });

           // Subscribe to group messages
           stompClient.subscribe(`/topic/group/${currentUser}`, function (message) {
               handleIncomingMessage(message);
           });

       }, function (error) {
           console.error('WebSocket connection error:', error);
       });

       function handleIncomingMessage(message) {
           try {
               const receivedMessage = JSON.parse(message.body);
               console.log('Received message:', receivedMessage);

               // Determine if this is a group message
               const isGroupMessage = !!receivedMessage.groupId;
               const conversationId = isGroupMessage ? receivedMessage.groupId :
                                     (receivedMessage.senderId === currentUser ? receivedMessage.receiverId : receivedMessage.senderId);

               // Create or update conversation data
               if (!allConversations[conversationId]) {
                   allConversations[conversationId] = {
                       conversationId: conversationId,
                       userId: isGroupMessage ? null : (receivedMessage.senderId === currentUser ? receivedMessage.receiverId : receivedMessage.senderId),
                       userName: isGroupMessage ? receivedMessage.groupName :
                                (receivedMessage.senderId === currentUser ? receivedMessage.receiverName : receivedMessage.senderName),
                       userInitials: isGroupMessage ? getGroupInitials(receivedMessage.groupName) :
                                    (receivedMessage.senderId === currentUser ?
                                     receivedMessage.receiverName.split(' ').map(n => n[0]).join('') :
                                     receivedMessage.senderName.split(' ').map(n => n[0]).join('')),
                       isOnline: false,
                       isGroup: isGroupMessage,
                       groupId: isGroupMessage ? receivedMessage.groupId : null,
                       groupName: isGroupMessage ? receivedMessage.groupName : null,
                       messages: []
                   };
               }

               // Add the message to conversation
               const isSent = receivedMessage.senderId === currentUser;
               allConversations[conversationId].messages.push({
                   content: receivedMessage.content,
                   timestamp: receivedMessage.timestamp || new Date().toISOString(),
                   isSent: isSent,
                   senderName: isSent ? null : receivedMessage.senderName
               });

               // Update UI if this is the current chat
               if (currentChat.id &&
                   ((currentChat.type === 'user' && receivedMessage.senderId === currentChat.id) ||
                    (currentChat.type === 'group' && receivedMessage.groupId === currentChat.id))) {
                   displayMessage(receivedMessage.content, isSent, isSent ? null : receivedMessage.senderName);
               }

               // Update conversation list
               updateConversationList(conversationId);

           } catch (error) {
               console.error('Error parsing message:', error);
           }
       }

       function updateConversationList(conversationId) {
           const conversation = allConversations[conversationId];
           if (!conversation) return;

           const conversationsContainer = document.querySelector('.conversations-container');
           if (!conversationsContainer) return;

           const lastMessage = conversation.messages[conversation.messages.length - 1];
           const existingItem = document.querySelector(`[data-conversation-id="${conversationId}"]`);

           if (existingItem) {
               // Update existing conversation item
               existingItem.querySelector('.text-sm.text-gray-400 span').textContent =
                   `${lastMessage.isSent ? 'You: ' : (conversation.isGroup && !lastMessage.isSent ? lastMessage.senderName + ': ' : '')}${lastMessage.content}`;
               existingItem.querySelector('.text-xs.text-gray-400').textContent =
                   formatTime(lastMessage.timestamp);

               // Move to top with animation
               existingItem.classList.add('new-conversation');
               existingItem.style.transition = 'all 0.3s ease';
               existingItem.style.opacity = '0';
               existingItem.style.transform = 'translateY(20px)';

               // Remove from current position
               existingItem.remove();

               // Insert at top
               conversationsContainer.insertBefore(existingItem, conversationsContainer.firstChild);

               // Animate in
               setTimeout(() => {
                   existingItem.style.opacity = '1';
                   existingItem.style.transform = 'translateY(0)';
               }, 10);
           } else {
               // Create new conversation item
               const conversationItem = document.createElement('div');
               conversationItem.className = 'chat-item flex items-center p-3 cursor-pointer hover:bg-gray-700 transition-colors new-conversation';
               conversationItem.dataset.conversationId = conversationId;

               if (conversation.isGroup) {
                   conversationItem.innerHTML = `
                       <div class="group-name-avatar mr-3">${conversation.userInitials}</div>
                       <div class="flex-1">
                           <div class="font-medium">${conversation.groupName}</div>
                           <div class="text-sm text-gray-400 flex items-center">
                               <span class="truncate">${lastMessage.isSent ? 'You: ' : (lastMessage.senderName ? lastMessage.senderName + ': ' : '')}${lastMessage.content}</span>
                           </div>
                       </div>
                       <div class="text-xs text-gray-400">${formatTime(lastMessage.timestamp)}</div>
                   `;
                   conversationItem.dataset.chatType = 'group';
                   conversationItem.dataset.chatId = conversation.groupId;
                   conversationItem.dataset.chatName = conversation.groupName;
               } else {
                   conversationItem.innerHTML = `
                       <div class="relative">
                           <div class="w-10 h-10 rounded-full mr-3 flex items-center justify-center bg-blue-500 text-white font-medium">${conversation.userInitials}</div>
                           <span class="absolute bottom-0 right-2 w-2 h-2 rounded-full border-2 border-gray-800 ${conversation.isOnline ? 'bg-green-500' : 'bg-gray-500'}"></span>
                       </div>
                       <div class="flex-1">
                           <div class="font-medium">${conversation.userName}</div>
                           <div class="text-sm text-gray-400 flex items-center">
                               <span class="truncate">${lastMessage.isSent ? 'You: ' : ''}${lastMessage.content}</span>
                           </div>
                       </div>
                       <div class="text-xs text-gray-400">${formatTime(lastMessage.timestamp)}</div>
                   `;
                   conversationItem.dataset.chatType = 'user';
                   conversationItem.dataset.chatId = conversation.userId;
                   conversationItem.dataset.chatName = conversation.userName;
                   conversationItem.dataset.userInitials = conversation.userInitials;
                   conversationItem.dataset.isOnline = conversation.isOnline;
               }

               conversationItem.addEventListener('click', function() {
                   loadChatFromConversation(this);
               });

               // Hide "no conversations" message
               const noConversationsDiv = document.querySelector('.no-conversations');
               if (noConversationsDiv) {
                   noConversationsDiv.style.display = 'none';
               }

               // Add with slide-up animation
               conversationItem.style.opacity = '0';
               conversationItem.style.transform = 'translateY(20px)';
               conversationsContainer.insertBefore(conversationItem, conversationsContainer.firstChild);

               // Trigger animation
               setTimeout(() => {
                   conversationItem.style.opacity = '1';
                   conversationItem.style.transform = 'translateY(0)';
               }, 10);
           }
       }

       function clearChatDisplay() {
           const messagesContainer = document.getElementById('messagesContainer');
           messagesContainer.innerHTML = `
               <div class="empty-chat-state">
                   <span class="empty-chat-icon material-icons">sentiment_satisfied_alt</span>
                   <h3 class="empty-chat-title">No messages yet</h3>
                   <p class="empty-chat-subtitle">Select a chat to start messaging or create a new conversation</p>
               </div>
           `;
       }

       function displayMessage(message, isOutgoing = true, senderName = null) {
           const messagesContainer = document.getElementById('messagesContainer');
           const emptyState = messagesContainer.querySelector('.empty-chat-state');

           if (emptyState && !emptyState.classList.contains('hidden')) {
               emptyState.classList.add('hidden');
           }

           const messageElement = document.createElement('div');
           messageElement.className = `message ${isOutgoing ? 'message-out' : 'message-in'}`;


           const contentElement = document.createElement('div');
           contentElement.textContent = message;
           messageElement.appendChild(contentElement);

           const timestamp = document.createElement('div');
           timestamp.className = 'text-xs mt-1 text-right text-gray-400';
           timestamp.textContent = new Date().toLocaleTimeString();
           messageElement.appendChild(timestamp);

           messagesContainer.appendChild(messageElement);
           messagesContainer.scrollTop = messagesContainer.scrollHeight;
       }

       function sendMessageOverWebSocket() {
           const messageInput = document.getElementById('messageInput');
           const messageContent = messageInput.value.trim();

           if (!messageContent) {
               console.warn('Message cannot be empty');
               return;
           }

           if (!currentChat.type) {
               console.warn('No chat selected to send message to');
               return;
           }

           const messageData = {
               senderName: currentUserName,
               senderId: currentUser,
               content: messageContent,
               timestamp: new Date().toISOString()
           };

           if (currentChat.type === 'user') {
               messageData.receiverId = currentChat.id;
               messageData.receiverName = currentChat.name;
           } else if (currentChat.type === 'group') {
               messageData.groupId = currentChat.id;
               messageData.groupName = currentChat.name;
           }

           if (stompClient && stompClient.connected) {
               console.log('Sending message:', messageData);
               stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(messageData));

               // Add message to local conversation immediately
               const conversationId = currentChat.id;
               if (!allConversations[conversationId]) {
                   allConversations[conversationId] = {
                       conversationId: conversationId,
                       userId: currentChat.type === 'user' ? currentChat.id : null,
                       userName: currentChat.name,
                       userInitials: currentChat.type === 'user' ?
                                    currentChat.name.split(' ').map(n => n[0]).join('') :
                                    getGroupInitials(currentChat.name),
                       isOnline: false,
                       isGroup: currentChat.type === 'group',
                       groupId: currentChat.type === 'group' ? currentChat.id : null,
                       groupName: currentChat.type === 'group' ? currentChat.name : null,
                       messages: []
                   };
               }

               allConversations[conversationId].messages.push({
                   content: messageContent,
                   timestamp: messageData.timestamp,
                   isSent: true,
                   senderName: null
               });

               // Update UI
               displayMessage(messageContent, true);
               updateConversationList(conversationId);
           } else {
               console.warn('WebSocket is not connected.');
           }

           messageInput.value = '';
       }

       function getGroupInitials(name) {
           const words = name.replace(/[^\w\s]/g, '').split(/\s+/);
           if (words.length === 0) return '';
           if (words.length === 1) return words[0].charAt(0).toUpperCase();
           return words[0].charAt(0).toUpperCase() +
                  words[words.length - 1].charAt(0).toUpperCase();
       }

       function formatTime(timestamp) {
           const date = new Date(timestamp);
           return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
       }

       function loadChatFromConversation(conversationElement) {
           const conversationId = conversationElement.dataset.conversationId;
           const conversation = allConversations[conversationId];
           if (!conversation) return;

           currentChat = {
               type: conversation.isGroup ? 'group' : 'user',
               id: conversationId,
               name: conversation.isGroup ? conversation.groupName : conversation.userName,
               userIds: conversation.isGroup ? [] : [conversation.userId]
           };

           clearChatDisplay();

           chatHeaderDefault.classList.add('hidden');

           if (!conversation.isGroup) {
               chatHeaderGroup.classList.add('hidden');
               chatHeaderUser.classList.remove('hidden');

               userChatName.textContent = currentChat.name;
               userAvatarContainer.textContent = conversation.userInitials;

               if (conversation.isOnline) {
                   userChatStatus.textContent = "Online";
                   userChatStatus.classList.remove('text-gray-400');
                   userChatStatus.classList.add('text-green-400');
                   userOnlineStatus.classList.remove('hidden');
               } else {
                   userChatStatus.textContent = "Offline";
                   userChatStatus.classList.remove('text-green-400');
                   userChatStatus.classList.add('text-gray-400');
                   userOnlineStatus.classList.add('hidden');
               }
           } else {
               chatHeaderUser.classList.add('hidden');
               chatHeaderGroup.classList.remove('hidden');

               groupChatName.textContent = currentChat.name;
               groupNameAvatar.textContent = conversation.userInitials;
               groupMembersCount.textContent = `${conversation.memberCount || 2} members`;
           }

           document.querySelectorAll('.chat-item').forEach(item => {
               item.classList.remove('active-chat');
           });
           conversationElement.classList.add('active-chat');

           // Display all messages in the conversation
           conversation.messages.forEach(message => {
               displayMessage(
                   message.content,
                   message.isSent,
                   !message.isSent ? (conversation.isGroup ? message.senderName : null) : null
               );
           });
       }

       // Initialize the page
       document.addEventListener('DOMContentLoaded', function() {
           // Hide the "no conversations" message initially
           const noConversationsDiv = document.querySelector('.no-conversations');
           if (noConversationsDiv) {
               noConversationsDiv.style.display = 'none';
           }

           // Create a container for conversations
           const conversationsContainer = document.createElement('div');
           conversationsContainer.className = 'conversations-container flex-1 overflow-y-auto';
           const sidebar = document.querySelector('.chat-sidebar');
           if (sidebar) {
               sidebar.insertBefore(conversationsContainer, noConversationsDiv);
           }

           // Set up message input to send on Enter key
           document.getElementById('messageInput').addEventListener('keypress', function(e) {
               if (e.key === 'Enter') {
                   sendMessageOverWebSocket();
               }
           });
       });


       // Modal elements and event listeners
       const startChatModal = document.getElementById('startChatModal');
       const startChatButton = document.getElementById('startChatButton');
       const cancelStartChat = document.getElementById('cancelStartChat');
       const confirmStartChat = document.getElementById('confirmStartChat');
       const userSearchStart = document.getElementById('userSearchStart');

       const createGroupModal = document.getElementById('createGroupModal');
       const createGroupButton = document.getElementById('createGroupButton');
       const cancelCreateGroup = document.getElementById('cancelCreateGroup');
       const confirmCreateGroup = document.getElementById('confirmCreateGroup');
       const userSearchGroup = document.getElementById('userSearchGroup');
       const groupName = document.getElementById('groupName');
       const groupNameError = document.getElementById('groupNameError');
       const userCheckboxes = document.querySelectorAll('.user-checkbox');

       // Header elements
       const chatHeaderDefault = document.getElementById('chatHeaderDefault');
       const chatHeaderUser = document.getElementById('chatHeaderUser');
       const chatHeaderGroup = document.getElementById('chatHeaderGroup');
       const userChatName = document.getElementById('userChatName');
       const userChatStatus = document.getElementById('userChatStatus');
       const userOnlineStatus = document.getElementById('userOnlineStatus');
       const userAvatarContainer = document.getElementById('userAvatarContainer');
       const groupChatName = document.getElementById('groupChatName');
       const groupMembersCount = document.getElementById('groupMembersCount');
       const groupNameAvatar = document.getElementById('groupNameAvatar');

       // Start Chat Modal functionality
       startChatButton.addEventListener('click', function() {
           startChatModal.classList.add('active');
       });

       cancelStartChat.addEventListener('click', function() {
           startChatModal.classList.remove('active');
       });

       confirmStartChat.addEventListener('click', function() {
           startChatModal.classList.remove('active');
       });

       // User search functionality for start chat
       userSearchStart.addEventListener('input', function() {
           const searchTerm = this.value.toLowerCase();
           const userItems = document.querySelectorAll('.user-list-start .user-item');

           userItems.forEach(item => {
               const userName = item.querySelector('.font-medium').textContent.toLowerCase();
               if (userName.includes(searchTerm)) {
                   item.style.display = 'flex';
               } else {
                   item.style.display = 'none';
               }
           });
       });

       // Create Group Modal functionality
       createGroupButton.addEventListener('click', function() {
           createGroupModal.classList.add('active');
       });

       cancelCreateGroup.addEventListener('click', function() {
           createGroupModal.classList.remove('active');
           resetGroupForm();
       });

       // Validate group name on input
       groupName.addEventListener('input', function() {
           validateGroupForm();
       });

       // Update Create Group button state based on selections
       function updateCreateGroupButton() {
           const checkedBoxes = document.querySelectorAll('.user-checkbox:checked');
           const isGroupNameValid = groupName.value.trim() !== '';

           if (checkedBoxes.length >= 2 && isGroupNameValid) {
               confirmCreateGroup.disabled = false;
               confirmCreateGroup.classList.remove('btn-disabled');
               confirmCreateGroup.classList.add('hover:bg-blue-700');
           } else {
               confirmCreateGroup.disabled = true;
               confirmCreateGroup.classList.add('btn-disabled');
               confirmCreateGroup.classList.remove('hover:bg-blue-700');
           }
       }

       // Validate group form
       function validateGroupForm() {
           if (groupName.value.trim() === '') {
               groupName.classList.add('required-field');
               groupNameError.style.display = 'block';
           } else {
               groupName.classList.remove('required-field');
               groupNameError.style.display = 'none';
           }
           updateCreateGroupButton();
       }

       // Reset group form
       function resetGroupForm() {
           groupName.value = '';
           groupName.classList.remove('required-field');
           groupNameError.style.display = 'none';
           userCheckboxes.forEach(checkbox => {
               checkbox.checked = false;
           });
           updateCreateGroupButton();
       }

       // Add event listeners to checkboxes
       userCheckboxes.forEach(checkbox => {
           checkbox.addEventListener('change', updateCreateGroupButton);
       });

       confirmCreateGroup.addEventListener('click', function() {
           if (groupName.value.trim() === '') {
               groupName.classList.add('required-field');
               groupNameError.style.display = 'block';
               return;
           }

           const name = groupName.value.trim();
           const checkedBoxes = document.querySelectorAll('.user-checkbox:checked');
           const selectedUserIds = Array.from(checkedBoxes).map(cb => cb.value);

           const selectedUsers = Array.from(document.querySelectorAll('.user-list-group .user-item'))
               .filter(item => {
                   const checkbox = item.querySelector('.user-checkbox');
                   return checkbox.checked;
               })
               .map(item => {
                   return {
                       id: item.querySelector('.user-id').value,
                       firstName: item.querySelector('.user-firstname').value,
                       lastName: item.querySelector('.user-lastname').value,
                       initials: item.querySelector('.rounded-full').textContent
                   };
               });

           clearChatDisplay();

           const groupId = 'group-' + Math.random().toString(36).substr(2, 9);

           currentChat = {
               type: 'group',
               id: groupId,
               name: name,
               userIds: selectedUserIds
           };

           chatHeaderDefault.classList.add('hidden');
           chatHeaderUser.classList.add('hidden');
           chatHeaderGroup.classList.remove('hidden');

           groupChatName.textContent = name;
           groupMembersCount.textContent = `${selectedUserIds.length} members`;

           groupNameAvatar.textContent = getGroupInitials(name);

           createGroupModal.classList.remove('active');
           resetGroupForm();
       });

       // User search functionality for group chat
       userSearchGroup.addEventListener('input', function() {
           const searchTerm = this.value.toLowerCase();
           const userItems = document.querySelectorAll('.user-list-group .user-item');

           userItems.forEach(item => {
               const userName = item.querySelector('.font-medium').textContent.toLowerCase();
               if (userName.includes(searchTerm)) {
                   item.style.display = 'flex';
               } else {
                   item.style.display = 'none';
               }
           });
       });

       // Close modals when clicking outside
       startChatModal.addEventListener('click', function(e) {
           if (e.target === startChatModal) {
               startChatModal.classList.remove('active');
           }
       });

       createGroupModal.addEventListener('click', function(e) {
           if (e.target === createGroupModal) {
               createGroupModal.classList.remove('active');
               resetGroupForm();
           }
       });

       // User selection in start chat modal
       document.querySelectorAll('.select-user').forEach(user => {
           user.addEventListener('click', function() {
               const userId = this.querySelector('.user-id').value;
               const firstName = this.querySelector('.user-firstname').value;
               const lastName = this.querySelector('.user-lastname').value;
               const online = this.querySelector('.user-online').value === 'true';
               const initials = this.querySelector('.rounded-full').textContent;

               clearChatDisplay();

               currentChat = {
                   type: 'user',
                   id: userId,
                   name: `${firstName} ${lastName}`,
                   userIds: [userId]
               };

               chatHeaderDefault.classList.add('hidden');
               chatHeaderGroup.classList.add('hidden');
               chatHeaderUser.classList.remove('hidden');

               userChatName.textContent = currentChat.name;
               userAvatarContainer.textContent = initials;

               if (online) {
                   userChatStatus.textContent = "Online";
                   userChatStatus.classList.remove('text-gray-400');
                   userChatStatus.classList.add('text-green-400');
                   userOnlineStatus.classList.remove('hidden');
               } else {
                   userChatStatus.textContent = "Offline";
                   userChatStatus.classList.remove('text-green-400');
                   userChatStatus.classList.add('text-gray-400');
                   userOnlineStatus.classList.add('hidden');
               }

               startChatModal.classList.remove('active');
           });
       });