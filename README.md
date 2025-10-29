💬 Java Multi-Client Chat Application

A modern desktop chat system built using Java Sockets and Java Swing.
This project supports multiple clients, private messaging (DM), file transfer, emoji support, and dark mode UI — demonstrating concepts of socket programming, multi-threading, and GUI design.

🚀 Features
🖥️ Server
Accepts multiple client connections concurrently
Tracks online users and prevents duplicate usernames
Broadcasts join/leave events

Supports:
✅ Broadcast messages
🔒 Direct (private) messages
📁 File transfers via Base64
🟢 User status updates (Online / Busy / Away)
Thread-safe collections for managing clients and statuses

💻 Client (Java Swing GUI)
Modern, responsive design with chat bubbles
Dark mode and emoji picker
File attach button for easy file transfer
Displays user status and supports private chats
Auto-scroll, timestamps, and message formatting

🧩 Tools & Technologies
Language: Java SE (JDK 8+ recommended; tested on JDK 11 & 17)
GUI: Java Swing (Nimbus Look & Feel)
Networking: java.net.Socket, ServerSocket
Concurrency: java.util.concurrent, synchronized collections
File Transfer: java.util.Base64
IDE (optional): IntelliJ IDEA / Eclipse / VS Code
OS Support: Windows / macOS / Linux

⚙️ Message Protocol
Type	Format	Description
Broadcast	username: message	Normal chat message
Private Message	DM::<recipient>::<message>	Sent only to target user
File Transfer	FILE::<filename>::<base64>	Sends encoded file
Status	STATUS::<status>	Updates user status
User Event	USER_EVENT::joined::username / left::username	Notifies all users
🏗️ Project Architecture
+-------------+        +-------------------------+
| ChatClient  | <----> |      ChatServer         |
| (Swing GUI) |        | (ServerSocket, Threads) |
+-------------+        +-------------------------+


Server: Handles multiple clients using threads and synchronized lists/maps
Client: Connects via socket, runs background listener thread, updates Swing UI safely using SwingUtilities.invokeLater()

🧠 Key Classes
🧩 ChatServer
Accepts connections
Spawns ClientHandler threads
Manages usernames, broadcasts, and statuses

🧩 ChatClient

GUI using JFrame
Handles message sending, file sharing, and emoji insertion
Runs receiver thread for incoming messages

🧪 How to Run
 ---  
1️⃣ Compile
javac ChatServer.java
javac ChatClient.java

2️⃣ Run Server
java ChatServer


(Default port: 1234)

3️⃣ Run Client
java ChatClient


Enter username, host (default: 127.0.0.1), and port
Open multiple clients to test multi-user chat

outpot snapppits


🧾 Sample Test Scenarios
Test	Action	Expected Result
1	Two users join	Both see each other in online list
2	Send broadcast	All users receive message
3	Send private message	Only target user sees message
4	Send file	All users prompted to save file
5	Change status	User list updates with new status
6	Duplicate username	Server rejects and prompts retry
🧵 Concurrency & Safety

Each client runs in its own thread
Uses synchronized collections for shared data
GUI updates via Event Dispatch Thread (EDT) for Swing safety

⚠️ Limitations

Not encrypted (plaintext TCP)
One thread per client (not scalable to thousands)
Large files not chunked — may cause memory issues
No persistent storage or authentication

🔮 Future Enhancements
🔐 SSL/TLS encrypted sockets
👤 Login & authentication system
💾 Database for message history
🌐 WebSocket version for browser-based chat

🧱 Group chats and message delivery receipts

🏁 Conclusion
This project showcases end-to-end Java development — from networking and concurrency to modern GUI design.
It’s a complete demonstration of client-server communication, thread safety, and user-friendly interaction using Java Swing.
