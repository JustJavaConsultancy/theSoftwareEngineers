/**
 * Register an event at the document for the specified selector,
 * so events are still catched after DOM changes.
 */
function handleEvent(eventType, selector, handler) {
  document.addEventListener(eventType, function(event) {
    if (event.target.matches(selector + ', ' + selector + ' *')) {
      handler.apply(event.target.closest(selector), arguments);
    }
  });
}

handleEvent('click', '.js-file-delete', function(event) {
  const $fileDiv = event.target.parentElement;
  const $fileRow = $fileDiv.previousElementSibling;
  $fileRow.removeAttribute('disabled');
  $fileRow.classList.remove('d-none');
  $fileDiv.remove();
});

document.addEventListener('DOMContentLoaded', function() {
            const menuToggle = document.getElementById('menu-toggle');
            const sidebar = document.querySelector('.sidebar');
            const sidebarOverlay = document.querySelector('.sidebar-overlay');

            menuToggle.addEventListener('click', function() {
                sidebar.classList.toggle('active');
                sidebarOverlay.classList.toggle('active');
            });

            sidebarOverlay.addEventListener('click', function() {
                sidebar.classList.remove('active');
                sidebarOverlay.classList.remove('active');
            });

            document.addEventListener('click', function(event) {
                const isClickInsideSidebar = sidebar.contains(event.target);
                const isClickOnMenuToggle = menuToggle.contains(event.target);

                if (!isClickInsideSidebar && !isClickOnMenuToggle && window.innerWidth < 768) {
                    sidebar.classList.remove('active');
                    sidebarOverlay.classList.remove('active');
                }
            });
            const dropdownToggles = document.querySelectorAll('.dropdown-toggle');

                        dropdownToggles.forEach(toggle => {
                            toggle.addEventListener('click', function(e) {
                                e.stopPropagation();
                                const dropdownMenu = this.nextElementSibling;
                                const isOpen = dropdownMenu.classList.contains('show');

                                // Close all other open dropdowns
                                document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
                                    if (menu !== dropdownMenu) {
                                        menu.classList.remove('show');
                                    }
                                });

                                // Toggle current dropdown
                                dropdownMenu.classList.toggle('show', !isOpen);
                            });
                        });

                        // Close dropdowns when clicking outside
                        document.addEventListener('click', function() {
                            document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
                                menu.classList.remove('show');
                            });
                        });

                        // Close dropdowns when pressing Escape key
                        document.addEventListener('keydown', function(e) {
                            if (e.key === 'Escape') {
                                document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
                                    menu.classList.remove('show');
                                });
                            }
                        });


                        // Chatbox
                         // Chat functionality
                         const supportBtn = document.getElementById('support-chat-btn');
                                 const chatBox = document.getElementById('chat-box');
                                 const closeChatBtn = document.getElementById('close-chat-btn');
                                 const chatInput = document.getElementById('chat-input');
                                 const sendBtn = document.getElementById('send-message-btn');
                                 const chatMessages = document.getElementById('chat-messages');
                                 const chatHeader = document.getElementById('chat-header');

                                 // State management
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

                                 // Predefined responses for demo purposes
                                 const botResponses = [
                                     "Thank you for reaching out! How can I help you with TaskMaster today?",
                                     "I'd be happy to assist you with that. Can you provide more details?",
                                     "That's a great question! Let me help you find the right solution.",
                                     "I understand your concern. Our support team will look into this for you.",
                                     "Is there anything specific about the process workflow you'd like to know?",
                                     "I'm here to help! Feel free to ask any questions about TaskMaster features."
                                 ];

                                 let messageCount = 0;

                                 // Show chat box
                                 function showChatBox() {
                                     chatBox.classList.remove('hidden');
                                     chatBox.classList.add('flex');
                                     supportBtn.style.display = 'none';

                                     // Reset position to center
                                     chatBox.style.left = '50%';
                                     chatBox.style.top = '50%';
                                     chatBox.style.transform = 'translate(-50%, -50%)';
                                     chatState.xOffset = 0;
                                     chatState.yOffset = 0;

                                     // Add entrance animation
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

                                 // Hide chat box
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

                                 // Drag functionality
                                 function handleDragStart(e) {
                                     // Only start drag if clicking on header (not close button)
                                     if (!chatHeader.contains(e.target) || closeChatBtn.contains(e.target)) {
                                         return;
                                     }

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

                                     // Prevent text selection during drag
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

                                     // Mark as moved if significant movement
                                     if (Math.abs(chatState.currentX) > 5 || Math.abs(chatState.currentY) > 5) {
                                         chatState.hasMoved = true;
                                     }

                                     // Calculate new position
                                     const rect = chatBox.getBoundingClientRect();
                                     const newLeft = (window.innerWidth / 2) + chatState.currentX;
                                     const newTop = (window.innerHeight / 2) + chatState.currentY;

                                     // Boundary checking
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

                                     // Small delay to prevent immediate click outside trigger
                                     setTimeout(() => {
                                         chatState.hasMoved = false;
                                     }, 50);
                                 }

                                 // Click outside to close
                                 function handleDocumentClick(e) {
                                     // Don't close if:
                                     // - Chat is hidden
                                     // - Click is inside chatbox
                                     // - Click is on support button
                                     // - Currently dragging or just finished dragging
                                     // - Click is on close button (handled separately)
                                     if (chatBox.classList.contains('hidden') ||
                                         chatBox.contains(e.target) ||
                                         supportBtn.contains(e.target) ||
                                         chatState.isDragging ||
                                         chatState.hasMoved) {
                                         return;
                                     }

                                     closeChatBox();
                                 }

                                 // Event listeners
                                 supportBtn.addEventListener('click', showChatBox);
                                 closeChatBtn.addEventListener('click', closeChatBox);

                                 // Drag events
                                 chatHeader.addEventListener('mousedown', handleDragStart);
                                 document.addEventListener('mousemove', handleDragMove);
                                 document.addEventListener('mouseup', handleDragEnd);

                                 // Touch events for mobile
                                 chatHeader.addEventListener('touchstart', handleDragStart, { passive: false });
                                 document.addEventListener('touchmove', handleDragMove, { passive: false });
                                 document.addEventListener('touchend', handleDragEnd);

                                 // Click outside to close - use capture phase to handle before other events
                                 document.addEventListener('click', handleDocumentClick, true);

                                 // Prevent chatbox content clicks from bubbling
                                 chatBox.addEventListener('click', function(e) {
                                     e.stopPropagation();
                                 });

                                 // Get current time
                                 function getCurrentTime() {
                                     const now = new Date();
                                     return now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                                 }

                                 // Send message function
                                 function sendMessage() {
                                     const message = chatInput.value.trim();
                                     if (message) {
                                         // Disable send button temporarily
                                         sendBtn.disabled = true;

                                         // Add user message
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

                                         // Clear input
                                         chatInput.value = '';

                                         // Scroll to bottom
                                         chatMessages.scrollTop = chatMessages.scrollHeight;

                                         // Show typing indicator
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

                                         // Simulate bot response after a delay
                                         setTimeout(function() {
                                             // Remove typing indicator
                                             const typingIndicator = document.getElementById('typing-indicator');
                                             if (typingIndicator) {
                                                 typingIndicator.remove();
                                             }

                                             // Add bot response
                                             const botMessageDiv = document.createElement('div');
                                             botMessageDiv.className = 'flex items-start space-x-2';
                                             const randomResponse = botResponses[Math.floor(Math.random() * botResponses.length)];
                                             botMessageDiv.innerHTML = `
                                                 <div class="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center flex-shrink-0">
                                                     <span class="material-icons text-white text-sm">support_agent</span>
                                                 </div>
                                                 <div class="bg-slate-700 text-white p-3 rounded-lg rounded-tl-none max-w-xs">
                                                     <p class="text-sm">${randomResponse}</p>
                                                     <span class="text-xs text-slate-400 mt-1 block">${getCurrentTime()}</span>
                                                 </div>
                                             `;
                                             chatMessages.appendChild(botMessageDiv);
                                             chatMessages.scrollTop = chatMessages.scrollHeight;

                                             // Re-enable send button
                                             sendBtn.disabled = false;
                                             messageCount++;
                                         }, 1500 + Math.random() * 1000);
                                     }
                                 }

                                 // Send message on button click
                                 sendBtn.addEventListener('click', sendMessage);

                                 // Send message on Enter key press
                                 chatInput.addEventListener('keypress', function(e) {
                                     if (e.key === 'Enter' && !sendBtn.disabled) {
                                         sendMessage();
                                     }
                                 });

                                 // Auto-resize chat input
                                 chatInput.addEventListener('input', function() {
                                     sendBtn.disabled = this.value.trim() === '';
                                 });

                                 // Escape key to close
                                 document.addEventListener('keydown', function(e) {
                                     if (e.key === 'Escape' && !chatBox.classList.contains('hidden')) {
                                         closeChatBox();
                                     }
                                 });

                                 // Initial state
                                 sendBtn.disabled = true;


           // Simple modal functionality
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
                              // Wait for animation to finish before hiding
                              modal.addEventListener('animationend', function handleAnimationEnd() {
                                modal.classList.add('hidden');
                                modal.classList.remove('hide');
                                modal.removeEventListener('animationend', handleAnimationEnd);
                              });
                              document.body.style.overflow = 'auto';
                            }

                            document.querySelectorAll('.process-modal-toggle').forEach(button => {
                                  button.addEventListener('click', function(e) {
                                    e.preventDefault();
                                    e.stopPropagation();
                                    openProcessModal();
                                  });
                                });

                                // Handle close button
                                const closeButton = document.getElementById('closeModal');
                                if (closeButton) {
                                  closeButton.addEventListener('click', closeProcessModal);
                                }

                                // Close modal when clicking backdrop
                                const modal = document.getElementById('processModal');
                                if (modal) {
                                  modal.addEventListener('click', function(e) {
                                    if (e.target === modal) {
                                      closeProcessModal();
                                    }
                                  });
                                }

                                // Close modal with Escape key
                                document.addEventListener('keydown', function(e) {
                                  if (e.key === 'Escape') {
                                    const modal = document.getElementById('processModal');
                                    if (modal && !modal.classList.contains('hidden')) {
                                      closeProcessModal();
                                    }
                                  }
                                })

            // Dark/Light mode toggle logic
            const modeToggleBtn = document.getElementById('mode-toggle');
            const body = document.body;
            const MODE_KEY = 'colorMode';
            const ICON_MOON = 'nightlight_round';
            const ICON_SUN = 'wb_sunny';

            function setMode(mode) {
                if (mode === 'light') {
                    body.classList.add('light-mode');
                } else {
                    body.classList.remove('light-mode');
                }
                // Change icon if possible
                if (modeToggleBtn) {
                    const icon = modeToggleBtn.querySelector('.material-icons');
                    if (icon) {
                        icon.textContent = mode === 'light' ? ICON_SUN : ICON_MOON;
                    }
                }
            }

            // On load, apply saved mode
            const savedMode = localStorage.getItem(MODE_KEY);
            if (savedMode === 'light' || savedMode === 'dark') {
                setMode(savedMode);
            }

            // Toggle on button click
            if (modeToggleBtn) {
                modeToggleBtn.addEventListener('click', function() {
                    const isLight = body.classList.toggle('light-mode');
                    const newMode = isLight ? 'light' : 'dark';
                    localStorage.setItem(MODE_KEY, newMode);
                    setMode(newMode);
                });
            }
        });