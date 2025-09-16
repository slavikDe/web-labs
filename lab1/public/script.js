// Initialize Socket.IO connection
const socket = io();

// Global variables
let currentUser = null;
let currentRoom = null;

// DOM Elements
const loginContainer = document.getElementById('loginContainer');
const chatContainer = document.getElementById('chatContainer');
const usernameInput = document.getElementById('usernameInput');
const roomInput = document.getElementById('roomInput');
const messageInput = document.getElementById('messageInput');
const messagesContainer = document.getElementById('messages');
const usersList = document.getElementById('usersList');
const roomNameDisplay = document.getElementById('roomName');
const userNameDisplay = document.getElementById('userName');

// Socket Event Listeners
socket.on('connect', () => {
    console.log('Connected to server');
});

socket.on('disconnect', () => {
    console.log('Disconnected from server');
    showNotification('Disconnected from server', 'error');
});

socket.on('room joined', (data) => {
    currentRoom = data.room;
    currentUser = data.username;
    
    roomNameDisplay.textContent = `Room: ${data.room}`;
    userNameDisplay.textContent = `Username: ${data.username}`;
    
    updateUsersList(data.users);
    showChatInterface();
    showNotification(`Joined room: ${data.room}`, 'success');
});

socket.on('admin message', (data) => {
    addMessage('admin', 'Admin', data.message, data.timestamp);
});

socket.on('chat message', (data) => {
    const isCurrentUser = data.socketId === socket.id;
    const messageType = isCurrentUser ? 'current-user' : 'user';
    addMessage(messageType, data.username, data.message, data.timestamp);
});

socket.on('user joined', (data) => {
    updateUsersList(data.users);
    showNotification(`${data.username} joined the room`, 'info');
});

socket.on('user left', (data) => {
    updateUsersList(data.users);
    showNotification(`${data.username} left the room`, 'info');
});

socket.on('connect_error', (error) => {
    console.error('Connection error:', error);
    showNotification('Connection error. Please try again.', 'error');
});

// Main Functions
function joinRoom() {
    const username = usernameInput.value.trim();
    const room = roomInput.value.trim();

    if (!username || !room) {
        showNotification('Please enter both username and room name', 'error');
        return;
    }

    if (username.length < 2 || username.length > 20) {
        showNotification('Username must be between 2 and 20 characters', 'error');
        return;
    }

    if (room.length < 2 || room.length > 20) {
        showNotification('Room name must be between 2 and 20 characters', 'error');
        return;
    }

    // Sanitize input
    const sanitizedUsername = sanitizeInput(username);
    const sanitizedRoom = sanitizeInput(room);

    if (sanitizedUsername !== username || sanitizedRoom !== room) {
        showNotification('Please use only letters, numbers, and basic symbols', 'error');
        return;
    }

    socket.emit('join room', { username: sanitizedUsername, room: sanitizedRoom });
}

function leaveRoom() {
    if (currentRoom && currentUser) {
        socket.emit('leave room');
        showLoginForm();
        clearMessages();
        clearUsersList();
        showNotification(`Left room: ${currentRoom}`, 'info');
        currentRoom = null;
        currentUser = null;
    }
}

function sendMessage() {
    const message = messageInput.value.trim();

    if (!message) {
        return;
    }

    if (message.length > 500) {
        showNotification('Message too long. Maximum 500 characters.', 'error');
        return;
    }

    if (!currentRoom || !currentUser) {
        showNotification('You must be in a room to send messages', 'error');
        return;
    }

    const sanitizedMessage = sanitizeInput(message);
    socket.emit('chat message', sanitizedMessage);
    messageInput.value = '';
}

function handleKeyPress(event) {
    if (event.key === 'Enter') {
        event.preventDefault();
        sendMessage();
    }
}

// UI Helper Functions
function showChatInterface() {
    loginContainer.style.display = 'none';
    chatContainer.style.display = 'flex';
    messageInput.focus();
}

function showLoginForm() {
    loginContainer.style.display = 'flex';
    chatContainer.style.display = 'none';
    usernameInput.value = '';
    roomInput.value = '';
    usernameInput.focus();
}

function addMessage(type, username, message, timestamp) {
    const messageElement = document.createElement('div');
    messageElement.className = `message ${type}`;

    if (type === 'admin') {
        messageElement.innerHTML = `
            <div class="message-content">${escapeHtml(message)}</div>
        `;
    } else {
        messageElement.innerHTML = `
            <div class="message-header">
                ${escapeHtml(username)} 
                <span class="message-time">${escapeHtml(timestamp)}</span>
            </div>
            <div class="message-content">${escapeHtml(message)}</div>
        `;
    }

    messagesContainer.appendChild(messageElement);
    scrollToBottom();
    
    // Limit number of messages in DOM to prevent memory issues
    if (messagesContainer.children.length > 100) {
        messagesContainer.removeChild(messagesContainer.firstChild);
    }
}

function updateUsersList(users) {
    usersList.innerHTML = '';
    
    users.forEach(user => {
        const userItem = document.createElement('li');
        userItem.className = 'user-item';
        userItem.textContent = user;
        
        // Highlight current user
        if (user === currentUser) {
            userItem.style.fontWeight = 'bold';
            userItem.style.color = '#3b82f6';
        }
        
        usersList.appendChild(userItem);
    });
}

function clearMessages() {
    messagesContainer.innerHTML = '';
}

function clearUsersList() {
    usersList.innerHTML = '';
}

function scrollToBottom() {
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

// Utility Functions
function sanitizeInput(input) {
    // Remove potential harmful characters
    return input.replace(/[<>]/g, '').trim();
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    
    // Style the notification
    Object.assign(notification.style, {
        position: 'fixed',
        top: '20px',
        right: '20px',
        padding: '12px 20px',
        borderRadius: '8px',
        color: 'white',
        fontWeight: '500',
        zIndex: '1000',
        minWidth: '250px',
        textAlign: 'center',
        animation: 'slideInRight 0.3s ease-out'
    });

    // Set background color based on type
    switch (type) {
        case 'success':
            notification.style.background = 'linear-gradient(135deg, #10b981, #059669)';
            break;
        case 'error':
            notification.style.background = 'linear-gradient(135deg, #ef4444, #dc2626)';
            break;
        case 'info':
        default:
            notification.style.background = 'linear-gradient(135deg, #3b82f6, #2563eb)';
            break;
    }

    // Add to document
    document.body.appendChild(notification);

    // Remove after 3 seconds
    setTimeout(() => {
        if (notification.parentNode) {
            notification.style.animation = 'slideOutRight 0.3s ease-in';
            setTimeout(() => {
                if (notification.parentNode) {
                    document.body.removeChild(notification);
                }
            }, 300);
        }
    }, 3000);
}

// Add CSS animations for notifications
const style = document.createElement('style');
style.textContent = `
    @keyframes slideInRight {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOutRight {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// Initialize app when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Focus on username input
    usernameInput.focus();
    
    // Add event listeners for input validation
    usernameInput.addEventListener('input', function() {
        this.value = this.value.replace(/[<>]/g, '');
    });
    
    roomInput.addEventListener('input', function() {
        this.value = this.value.replace(/[<>]/g, '');
    });
    
    messageInput.addEventListener('input', function() {
        if (this.value.length > 500) {
            this.value = this.value.substring(0, 500);
            showNotification('Message length limit reached (500 characters)', 'error');
        }
    });

    // Handle form submission with Enter key
    usernameInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            roomInput.focus();
        }
    });

    roomInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            joinRoom();
        }
    });
});

// Handle page visibility changes
document.addEventListener('visibilitychange', function() {
    if (document.hidden) {
        console.log('Page is hidden');
    } else {
        console.log('Page is visible');
        // Scroll to bottom when page becomes visible
        if (currentRoom) {
            scrollToBottom();
        }
    }
});

// Handle beforeunload to clean up
window.addEventListener('beforeunload', function() {
    if (currentRoom && currentUser) {
        socket.emit('leave room');
    }
});