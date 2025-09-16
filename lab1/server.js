const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const path = require('path');

const app = express();
const server = http.createServer(app);
const io = socketIo(server);

// Serve static files
app.use(express.static(path.join(__dirname, 'public')));

// Store active users and rooms
const rooms = new Map();

// Route to serve the main page
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Socket.IO connection handling
io.on('connection', (socket) => {
    console.log('A user connected:', socket.id);

    // Handle user joining a room
    socket.on('join room', (data) => {
        const { username, room } = data;
        
        // Leave previous room if any
        if (socket.currentRoom) {
            socket.leave(socket.currentRoom);
            removeUserFromRoom(socket.currentRoom, socket.username);
            socket.to(socket.currentRoom).emit('admin message', {
                message: `${socket.username} has left!`,
                timestamp: new Date().toLocaleTimeString()
            });
            socket.to(socket.currentRoom).emit('user left', {
                username: socket.username,
                users: getRoomUsers(socket.currentRoom)
            });
        }

        // Join new room
        socket.join(room);
        socket.currentRoom = room;
        socket.username = username;

        // Add user to room
        addUserToRoom(room, username, socket.id);

        // Send room joined confirmation
        socket.emit('room joined', {
            room: room,
            username: username,
            users: getRoomUsers(room)
        });

        // Welcome message to the user
        socket.emit('admin message', {
            message: `Welcome, ${username}!`,
            timestamp: new Date().toLocaleTimeString()
        });

        // Notify other users in the room
        socket.to(room).emit('admin message', {
            message: `${username} has joined!`,
            timestamp: new Date().toLocaleTimeString()
        });

        // Update users list for all users in the room
        socket.to(room).emit('user joined', {
            username: username,
            users: getRoomUsers(room)
        });

        console.log(`${username} joined room: ${room}`);
    });

    // Handle chat messages
    socket.on('chat message', (message) => {
        if (socket.currentRoom && socket.username) {
            const messageData = {
                username: socket.username,
                message: message,
                timestamp: new Date().toLocaleTimeString(),
                socketId: socket.id
            };

            // Broadcast message to all users in the room
            io.to(socket.currentRoom).emit('chat message', messageData);
            
            console.log(`Message from ${socket.username} in ${socket.currentRoom}: ${message}`);
        }
    });

    // Handle user leaving room
    socket.on('leave room', () => {
        if (socket.currentRoom && socket.username) {
            // Remove user from room
            removeUserFromRoom(socket.currentRoom, socket.username);
            
            // Notify other users
            socket.to(socket.currentRoom).emit('admin message', {
                message: `${socket.username} has left!`,
                timestamp: new Date().toLocaleTimeString()
            });

            socket.to(socket.currentRoom).emit('user left', {
                username: socket.username,
                users: getRoomUsers(socket.currentRoom)
            });

            // Leave the room
            socket.leave(socket.currentRoom);
            
            console.log(`${socket.username} left room: ${socket.currentRoom}`);
            
            socket.currentRoom = null;
            socket.username = null;
        }
    });

    // Handle disconnection
    socket.on('disconnect', () => {
        if (socket.currentRoom && socket.username) {
            removeUserFromRoom(socket.currentRoom, socket.username);
            
            socket.to(socket.currentRoom).emit('admin message', {
                message: `${socket.username} has disconnected!`,
                timestamp: new Date().toLocaleTimeString()
            });

            socket.to(socket.currentRoom).emit('user left', {
                username: socket.username,
                users: getRoomUsers(socket.currentRoom)
            });
        }
        
        console.log('User disconnected:', socket.id);
    });
});

// Helper functions for room management
function addUserToRoom(roomName, username, socketId) {
    if (!rooms.has(roomName)) {
        rooms.set(roomName, new Map());
    }
    rooms.get(roomName).set(username, socketId);
}

function removeUserFromRoom(roomName, username) {
    if (rooms.has(roomName)) {
        rooms.get(roomName).delete(username);
        if (rooms.get(roomName).size === 0) {
            rooms.delete(roomName);
        }
    }
}

function getRoomUsers(roomName) {
    if (rooms.has(roomName)) {
        return Array.from(rooms.get(roomName).keys());
    }
    return [];
}

// Start server
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
    console.log(`Visit http://localhost:${PORT} to access the chat`);
});