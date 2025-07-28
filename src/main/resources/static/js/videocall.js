// Video Call Module using ZegoCloud
class VideoCallManager {
    constructor() {
        this.appID = 382757591;
        this.serverSecret = "a3a34788c4745c28fa325ebae4e8b0f5";
        this.currentCall = null;
        this.isCallActive = false;
        this.userInfo = null;
        
        this.initializeEventListeners();
        this.loadUserInfo();
    }
    
    async loadUserInfo() {
        try {
            const response = await fetch('/api/chat/video-call/user-info');
            if (response.ok) {
                this.userInfo = await response.json();
                console.log('Video call user info loaded:', this.userInfo);
            } else {
                console.warn('Failed to load user info, using fallback');
                this.userInfo = {
                    userId: 'user_' + Math.floor(Math.random() * 10000),
                    userName: 'User'
                };
            }
        } catch (error) {
            console.error('Error loading user info:', error);
            this.userInfo = {
                userId: 'user_' + Math.floor(Math.random() * 10000),
                userName: 'User'
            };
        }
    }
    
    initializeEventListeners() {
        // Video call button click
        document.addEventListener('click', (e) => {
            if (e.target.closest('#video-call-btn')) {
                e.preventDefault();
                this.startVideoCall();
            }
            
            if (e.target.closest('#end-video-call')) {
                e.preventDefault();
                this.endVideoCall();
            }
        });
        
        // Close modal when clicking outside
        document.addEventListener('click', (e) => {
            const modal = document.getElementById('video-call-modal');
            if (modal && e.target === modal && !this.isCallActive) {
                this.closeVideoCallModal();
            }
        });
        
        // Handle escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && !this.isCallActive) {
                this.closeVideoCallModal();
            }
        });
    }
    
    async startVideoCall() {
        // Check if we have a current conversation
        if (!currentConversation) {
            this.showError('Please select a conversation to start a video call');
            return;
        }
        
        // Ensure user info is loaded
        if (!this.userInfo) {
            await this.loadUserInfo();
        }
        
        if (currentConversation.group) {
            // For group calls, include all members
            this.initiateCall(currentConversation.id, currentConversation.name, true);
        } else {
            // For individual calls
            this.initiateCall(currentConversation.id, currentConversation.name, false);
        }
    }
    
    initiateCall(conversationId, participantName, isGroup) {
        try {
            // Generate room ID based on conversation
            const roomID = this.generateRoomId(conversationId);
            
            // Get current user info from loaded data
            const userID = this.getUserId();
            const userName = this.getUserName();
            
            // Show video call modal
            this.showVideoCallModal(participantName, isGroup);
            
            // Generate token
            const kitToken = ZegoUIKitPrebuilt.generateKitTokenForTest(
                this.appID, 
                this.serverSecret, 
                roomID, 
                userID, 
                userName
            );
            
            // Create ZegoCloud instance
            const zp = ZegoUIKitPrebuilt.create(kitToken);
            
            // Configure and join room
            const callConfig = this.getCallConfiguration(isGroup);
            
            zp.joinRoom({
                container: document.querySelector("#video-call-container"),
                ...callConfig
            });
            
            this.currentCall = zp;
            this.isCallActive = true;
            
            // Update UI
            this.updateCallStatus('Connected');
            
            // Send call notification (optional - you might want to notify other participants)
            this.notifyParticipants(conversationId, roomID, participantName, isGroup);
            
        } catch (error) {
            console.error('Error starting video call:', error);
            this.showError('Failed to start video call. Please try again.');
        }
    }
    
    getCallConfiguration(isGroup) {
        const baseUrl = window.location.protocol + '//' + window.location.host;
        const roomId = this.generateRoomId(currentConversation.id);
        
        return {
            sharedLinks: [{
                name: 'Join Call',
                url: `${baseUrl}/chat?roomID=${roomId}`,
            }],
            scenario: {
                mode: ZegoUIKitPrebuilt.VideoConference,
            },
            turnOnMicrophoneWhenJoining: false,
            turnOnCameraWhenJoining: true,
            showMyCameraToggleButton: true,
            showMyMicrophoneToggleButton: true,
            showAudioVideoSettingsButton: true,
            showScreenSharingButton: true,
            showTextChat: !isGroup, // Hide text chat for group calls since we have the main chat
            showUserList: isGroup,
            maxUsers: isGroup ? 50 : 2,
            layout: "Auto",
            showLayoutButton: isGroup,
            onJoinRoom: () => {
                console.log('Joined ZegoCloud room successfully');
                this.isCallActive = true;
                this.updateCallStatus('Call Active');
            },
            onLeaveRoom: () => {
                console.log('Left ZegoCloud room - cleaning up...');
                // Only cleanup if we're not already in the process of ending the call
                if (this.isCallActive) {
                    this.isCallActive = false;
                    this.currentCall = null;
                    this.closeVideoCallModal();
                    
                    // Clear the video container
                    const container = document.getElementById('video-call-container');
                    if (container) {
                        container.innerHTML = '';
                    }
                    
                    this.showSuccess('Call ended');
                }
            },
            onUserJoin: (users) => {
                console.log('User joined call:', users);
                this.updateCallStatus(`${users.length + 1} participant(s) in call`);
            },
            onUserLeave: (users) => {
                console.log('User left call:', users);
                if (users.length === 0) {
                    this.updateCallStatus('Waiting for others to join...');
                } else {
                    this.updateCallStatus(`${users.length + 1} participant(s) in call`);
                }
            }
        };
    }
    
    generateRoomId(conversationId) {
        // Create a room ID based on conversation ID
        return `room_${conversationId}`.replace(/[^a-zA-Z0-9_]/g, '_');
    }
    
    getUserId() {
        return this.userInfo?.userId || 'user_' + Math.floor(Math.random() * 10000);
    }
    
    getUserName() {
        return this.userInfo?.userName || 'User';
    }
    
    showVideoCallModal(participantName, isGroup) {
        const modal = document.getElementById('video-call-modal');
        const participantElement = document.getElementById('video-call-with');
        const statusElement = document.getElementById('video-call-status');
        
        if (modal && participantElement) {
            participantElement.textContent = participantName;
            modal.classList.remove('hidden');
            
            // Show connecting status
            if (statusElement) {
                statusElement.classList.remove('hidden');
            }
            
            // Disable body scroll
            document.body.style.overflow = 'hidden';
        }
    }
    
    closeVideoCallModal() {
        const modal = document.getElementById('video-call-modal');
        const statusElement = document.getElementById('video-call-status');
        
        if (modal) {
            modal.classList.add('hidden');
            
            // Hide status
            if (statusElement) {
                statusElement.classList.add('hidden');
            }
            
            // Re-enable body scroll
            document.body.style.overflow = 'auto';
        }
    }
    
    updateCallStatus(status) {
        const statusElement = document.getElementById('video-call-status');
        if (statusElement) {
            if (status === 'Connected' || status === 'Call Active') {
                statusElement.classList.add('hidden');
            } else {
                statusElement.innerHTML = `
                    <div class="text-slate-600 dark:text-slate-400">
                        <span class="material-icons animate-pulse">videocam</span>
                        <span class="ml-2">${status}</span>
                    </div>
                `;
                statusElement.classList.remove('hidden');
            }
        }
    }
    
    endVideoCall() {
        try {
            console.log('Ending video call...');
            
            // Prevent multiple calls to end
            if (!this.isCallActive && !this.currentCall) {
                console.log('Call already ended');
                return;
            }
            
            // Show ending status to user
            this.showCallEndingStatus();
            
            // Set flags first to prevent callback loops
            this.isCallActive = false;
            
            if (this.currentCall) {
                try {
                    console.log('Attempting to leave ZegoCloud room...');
                    
                    // Try to leave the room using the correct ZegoCloud method
                    if (typeof this.currentCall.leaveRoom === 'function') {
                        this.currentCall.leaveRoom();
                        console.log('Successfully called leaveRoom()');
                    } else if (typeof this.currentCall.destroy === 'function') {
                        this.currentCall.destroy();
                        console.log('Successfully called destroy()');
                    } else {
                        console.warn('No standard leave method found, forcing cleanup');
                    }
                    
                    // Small delay to let ZegoCloud clean up
                    setTimeout(() => {
                        this.performUICleanup();
                        this.showSuccess('Call ended successfully');
                    }, 500);
                    
                } catch (zegoError) {
                    console.error('Error with ZegoCloud cleanup:', zegoError);
                    // Continue with cleanup even if ZegoCloud fails
                    this.performUICleanup();
                    this.showSuccess('Call ended (forced cleanup)');
                }
                
                this.currentCall = null;
            } else {
                // No active call, just cleanup UI
                this.performUICleanup();
                this.showSuccess('Call ended');
            }
            
        } catch (error) {
            console.error('Error ending video call:', error);
            
            // Force cleanup
            this.isCallActive = false;
            this.currentCall = null;
            this.performUICleanup();
            
            this.showError('Call ended (with some issues)');
        }
    }
    
    // Separate method for UI cleanup to avoid duplication
    performUICleanup() {
        try {
            console.log('Performing UI cleanup...');
            
            // Reset the end call button
            this.resetCallEndButton();
            
            // Clear the video container
            const container = document.getElementById('video-call-container');
            if (container) {
                container.innerHTML = '';
                console.log('Cleared video container');
            }
            
            // Force stop any remaining media streams
            this.stopAllMediaStreams();
            
            // Close the modal last
            setTimeout(() => {
                this.closeVideoCallModal();
                console.log('UI cleanup completed');
            }, 200);
            
        } catch (error) {
            console.error('Error during UI cleanup:', error);
            // Ensure modal closes even if there's an error
            this.closeVideoCallModal();
        }
    }
    
    // Helper method to force stop all media streams
    stopAllMediaStreams() {
        try {
            console.log('Stopping all media streams...');
            
            // Method 1: Stop streams from video elements in the container
            const videoElements = document.querySelectorAll('#video-call-container video, #video-call-container audio');
            let streamsStopped = 0;
            
            videoElements.forEach(element => {
                if (element.srcObject) {
                    const stream = element.srcObject;
                    const tracks = stream.getTracks();
                    tracks.forEach(track => {
                        if (track.readyState === 'live') {
                            track.stop();
                            streamsStopped++;
                            console.log(`Stopped ${track.kind} track`);
                        }
                    });
                    element.srcObject = null;
                }
            });
            
            // Method 2: Try to get current active streams and stop them
            if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
                // Find all active streams
                navigator.mediaDevices.enumerateDevices()
                    .then(devices => {
                        console.log('Available devices:', devices.length);
                    })
                    .catch(err => console.log('Could not enumerate devices:', err));
            }
            
            // Method 3: Force stop any remaining tracks (more aggressive)
            if (window.stream) {
                const tracks = window.stream.getTracks();
                tracks.forEach(track => {
                    track.stop();
                    streamsStopped++;
                    console.log(`Force stopped global ${track.kind} track`);
                });
                window.stream = null;
            }
            
            console.log(`Total media tracks stopped: ${streamsStopped}`);
            
            // Give a small delay to let the browser fully release the resources
            setTimeout(() => {
                console.log('Media stream cleanup completed');
            }, 100);
            
        } catch (error) {
            console.error('Error stopping media streams:', error);
        }
    }
    
    // Add method to show call termination status
    showCallEndingStatus() {
        const endButton = document.getElementById('end-video-call');
        if (endButton) {
            endButton.disabled = true;
            endButton.innerHTML = '<span class="material-icons animate-spin">sync</span>';
            endButton.title = 'Ending call...';
        }
        
        this.updateCallStatus('Ending call...');
    }
    
    // Reset the end button state
    resetCallEndButton() {
        const endButton = document.getElementById('end-video-call');
        if (endButton) {
            endButton.disabled = false;
            endButton.innerHTML = '<span class="material-icons">call_end</span>';
            endButton.title = 'End Call';
        }
    }
    
    async notifyParticipants(conversationId, roomID, participantName, isGroup) {
        try {
            // You can implement this to send a message to the chat about the video call
            const baseUrl = window.location.protocol + '//' + window.location.host;
            const callMessage = isGroup 
                ? `🎥 Video call started - Click to join: ${baseUrl}/chat?roomID=${roomID}`
                : `🎥 ${this.getUserName()} started a video call`;
            
            console.log('Video call notification:', callMessage);
            
            // Optional: Send this as a chat message to notify other participants
            // You could integrate this with your existing message sending system
            
        } catch (error) {
            console.error('Error notifying participants:', error);
        }
    }
    
    showError(message) {
        // Create a simple error notification
        const errorDiv = document.createElement('div');
        errorDiv.className = 'fixed top-4 right-4 bg-red-500 text-white px-4 py-2 rounded-lg shadow-lg z-50 transition-all duration-300';
        errorDiv.innerHTML = `
            <div class="flex items-center">
                <span class="material-icons mr-2 text-sm">error</span>
                <span class="text-sm">${message}</span>
                <button onclick="this.parentElement.parentElement.remove()" class="ml-2 text-white hover:text-red-200">
                    <span class="material-icons text-sm">close</span>
                </button>
            </div>
        `;
        
        document.body.appendChild(errorDiv);
        
        // Remove after 5 seconds
        setTimeout(() => {
            if (document.body.contains(errorDiv)) {
                document.body.removeChild(errorDiv);
            }
        }, 5000);
    }
    
    showSuccess(message) {
        // Create a success notification
        const successDiv = document.createElement('div');
        successDiv.className = 'fixed top-4 right-4 bg-green-500 text-white px-4 py-2 rounded-lg shadow-lg z-50 transition-all duration-300';
        successDiv.innerHTML = `
            <div class="flex items-center">
                <span class="material-icons mr-2 text-sm">check_circle</span>
                <span class="text-sm">${message}</span>
                <button onclick="this.parentElement.parentElement.remove()" class="ml-2 text-white hover:text-green-200">
                    <span class="material-icons text-sm">close</span>
                </button>
            </div>
        `;
        
        document.body.appendChild(successDiv);
        
        // Remove after 3 seconds
        setTimeout(() => {
            if (document.body.contains(successDiv)) {
                document.body.removeChild(successDiv);
            }
        }, 3000);
    }
    
    // Method to join a call from a room ID (for when someone clicks a call link)
    async joinCallFromRoomId(roomID) {
        if (!roomID) return;
        
        try {
            // Ensure user info is loaded
            if (!this.userInfo) {
                await this.loadUserInfo();
            }
            
            const userID = this.getUserId();
            const userName = this.getUserName();
            
            const kitToken = ZegoUIKitPrebuilt.generateKitTokenForTest(
                this.appID, 
                this.serverSecret, 
                roomID, 
                userID, 
                userName
            );
            
            const zp = ZegoUIKitPrebuilt.create(kitToken);
            
            this.showVideoCallModal('Joining Call...', false);
            
            zp.joinRoom({
                container: document.querySelector("#video-call-container"),
                ...this.getCallConfiguration(false)
            });
            
            this.currentCall = zp;
            this.isCallActive = true;
            
            this.showSuccess('Joined video call successfully!');
            
        } catch (error) {
            console.error('Error joining call:', error);
            this.showError('Failed to join video call');
        }
    }
}

// Initialize video call manager when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.videoCallManager = new VideoCallManager();
    
    // Check if there's a roomID in URL parameters (for joining calls)
    const urlParams = new URLSearchParams(window.location.search);
    const roomID = urlParams.get('roomID');
    if (roomID && roomID !== 'secret') {
        // Auto-join call if roomID is provided
        setTimeout(() => {
            window.videoCallManager.joinCallFromRoomId(roomID);
        }, 1000); // Small delay to ensure chat is loaded
    }
}); 