// Video Call Module using ZegoCloud
class VideoCallManager {
    constructor() {
        this.appID = 382757591;
        this.serverSecret = "a3a34788c4745c28fa325ebae4e8b0f5";
        this.userInfo = null;
        
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
    
    async startVideoCall() {
        // Check if we have a current chat (from chat.js)
        if (typeof currentChat === 'undefined' || !currentChat || !currentChat.id) {
            alert('Please select a conversation to start a video call');
            return;
        }

        // Wait for user info to be loaded
        if (!this.userInfo) {
            await this.loadUserInfo();
        }

        // Generate room ID and meeting URL
        const roomId = this.generateRoomId(currentChat.id);
        const baseUrl = window.location.protocol + '//' + window.location.host;
        const meetingUrl = `${baseUrl}/videocall?roomID=${roomId}&userName=${encodeURIComponent(this.userInfo.userName)}&userID=${encodeURIComponent(this.userInfo.userId)}`;
        
        // Open in new tab
        window.open(meetingUrl, '_blank');
    }
    
    generateRoomId(chatId) {
        // Generate a clean room ID based on chat ID
        return `room_${chatId}`.replace(/[^a-zA-Z0-9_]/g, '_');
    }
    
    generateToken(roomId, userId) {
        // For production, this should be generated server-side
        // This is a simplified client-side token generation for demo purposes
        const payload = {
            iss: this.appID,
            exp: Math.floor(Date.now() / 1000) + 7200, // 2 hours from now
        };
        
        // In a real implementation, you'd call your backend to generate this token
        // For now, we'll use ZegoCloud's built-in token generation
        return window.ZegoUIKitPrebuilt.generateKitTokenForTest(
            this.appID,
            this.serverSecret,
            roomId,
            userId,
            this.userInfo.userName
        );
    }
}

// Initialize the video call manager
window.videoCallManager = new VideoCallManager();

// Initialize ZegoCloud video call if we're on the videocall page
if (window.location.pathname === '/videocall') {
    document.addEventListener('DOMContentLoaded', function() {
        const urlParams = new URLSearchParams(window.location.search);
        const roomID = urlParams.get('roomID');
        const meetingName = urlParams.get('meetingName') || 'Video Call';
        let userName = urlParams.get('userName');
        let userID = urlParams.get('userID');
        
        // If no user info provided (shared link), prompt for name
        if (!userName || !userID) {
            userName = prompt('Enter your name to join the video call:') || 'Guest';
            userID = 'guest_' + Math.floor(Math.random() * 100000);
        }
        
        if (roomID && userName) {
            const appID = 382757591;
            const serverSecret = "a3a34788c4745c28fa325ebae4e8b0f5";
            
            // Update page title
            document.title = `${meetingName} - ProcessManager`;
            
            // Generate token for this call
            const token = ZegoUIKitPrebuilt.generateKitTokenForTest(
                appID,
                serverSecret,
                roomID,
                userID,
                userName
            );
            
            // Create ZegoCloud instance
            const zp = ZegoUIKitPrebuilt.create(token);
            
            // Join the call
            zp.joinRoom({
                container: document.querySelector('#video-call-container'),
                scenario: {
                    mode: ZegoUIKitPrebuilt.VideoConference,
                },
                showPreJoinView: true, // Show prejoin view for shared links
                showRoomTimer: true,
                showUserCount: true,
                maxUsers: 50, // Increased for shared meetings
                layout: "Auto",
                showLayoutButton: true,
                showScreenSharingButton: true,
                showAudioVideoSettingsButton: true,
                showTextChat: true,
                showUserList: true,
                lowerLeftNotification: {
                    showUserJoinAndLeave: true,
                    showTextChat: true,
                },
                branding: {
                    logoURL: "",
                },
                onJoinRoom: () => {
                    console.log('Joined video call room:', roomID);
                },
                onLeaveRoom: () => {
                    console.log('Left video call room:', roomID);
                    // Close the tab when leaving the call
                    window.close();
                },
                onUserJoin: (users) => {
                    console.log('Users joined:', users);
                },
                onUserLeave: (users) => {
                    console.log('Users left:', users);
                }
            });
        } else if (!roomID) {
            document.querySelector('#video-call-container').innerHTML = '<div class="text-center text-red-500 p-8">Invalid room ID</div>';
        } else {
            document.querySelector('#video-call-container').innerHTML = '<div class="text-center text-red-500 p-8">Name is required to join the call</div>';
        }
    });
}