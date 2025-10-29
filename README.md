<h1 align="center">💬 Java Multi-Client Chat Application</h1>

<p align="center">
A modern desktop chat system built using <b>Java Sockets</b> and <b>Java Swing</b>.<br>
Supports multiple clients, private messaging (DM), file transfer, emoji support, and dark mode — demonstrating <b>socket programming</b>, <b>multi-threading</b>, and <b>GUI design</b>.
</p>

---

<h2>🚀 Features</h2>

<h3>🖥️ Server</h3>
<ul>
  <li>Accepts multiple clients concurrently</li>
  <li>Tracks online users and prevents duplicate usernames</li>
  <li>Broadcasts user join/leave events</li>
  <li>Supports:
    <ul>
      <li>✅ Broadcast messages</li>
      <li>🔒 Direct (Private) messages</li>
      <li>📁 File transfers via Base64</li>
      <li>🟢 User status updates (Online / Busy / Away)</li>
    </ul>
  </li>
  <li>Thread-safe collections for managing clients and statuses</li>
</ul>

<h3>💻 Client (Java Swing GUI)</h3>
<ul>
  <li>Modern, responsive chat-bubble design</li>
  <li>🌙 Dark mode & 😃 Emoji picker</li>
  <li>📎 File attach button for easy transfer</li>
  <li>Displays user statuses & supports private chats</li>
  <li>Auto-scroll, timestamps, and styled message formatting</li>
</ul>

---

<h2>🧩 Tools & Technologies</h2>

<table>
<tr><td>💻 Language</td><td>Java SE (JDK 8+ recommended; tested on JDK 11 & 17)</td></tr>
<tr><td>🎨 GUI</td><td>Java Swing (Nimbus Look & Feel)</td></tr>
<tr><td>🌐 Networking</td><td>java.net.Socket, java.net.ServerSocket</td></tr>
<tr><td>⚙️ Concurrency</td><td>java.util.concurrent, synchronized collections</td></tr>
<tr><td>📁 File Transfer</td><td>java.util.Base64</td></tr>
<tr><td>🧠 IDE (optional)</td><td>IntelliJ IDEA / Eclipse / VS Code</td></tr>
<tr><td>🖥️ OS</td><td>Windows / macOS / Linux</td></tr>
</table>

---

<h2>⚙️ Message Protocol</h2>

| Type | Format | Description |
|------|---------|-------------|
| 💬 Broadcast | `username: message` | Normal chat message |
| 🔒 Private Message | `DM::recipient::message` | Sent only to target user |
| 📁 File Transfer | `FILE::filename::base64` | Sends encoded file |
| 🟢 Status | `STATUS::status` | Updates user status |
| 👥 User Event | `USER_EVENT::joined::username / left::username` | Notifies all users |

---

<h2>🏗️ Project Architecture</h2>
+-------------+ +--------------------------+
| ChatClient | <--TCP--> | ChatServer |
| (Swing GUI) | | (ServerSocket, Threads) |
+-------------+ +--------------------------+


- **Server:** Handles multiple clients using threads & synchronized data structures  
- **Client:** Connects via sockets, listens on a background thread, and updates Swing UI safely using `SwingUtilities.invokeLater()`

---

<h2>🧠 Key Classes</h2>

<h3>🧩 ChatServer</h3>
<ul>
  <li>Accepts client connections</li>
  <li>Spawns <b>ClientHandler</b> threads</li>
  <li>Manages usernames, broadcasts, and user statuses</li>
</ul>

<h3>🧩 ChatClient</h3>
<ul>
  <li>Builds the GUI using JFrame</li>
  <li>Handles message sending, file sharing, and emoji insertion</li>
  <li>Runs background thread for receiving messages</li>
</ul>

---

<h2>🧪 How to Run</h2>

```bash
# 1️⃣ Compile
javac ChatServer.java
javac ChatClient.java

# 2️⃣ Run Server (default port: 1234)
java ChatServer

# 3️⃣ Run Client
java ChatClient
```

<h2>📸 Output Snapshots</h2> <p align="center"> 🖼️ <i>(Add screenshots of your UI and terminal output here)</i> </p>

<h2>🧾 Sample Test Scenarios</h2>
#	Action	Expected Result
1️⃣	Two users join	Both see each other in online list
2️⃣	Send broadcast	All users receive message
3️⃣	Send private message	Only target user sees message
4️⃣	Send file	All users prompted to save file
5️⃣	Change status	User list updates
6️⃣	Duplicate username	Server rejects and prompts retry

<h2>🧵 Concurrency & Safety</h2>
Each client runs in its own thread
Shared data structures are synchronized

GUI updates handled on Event Dispatch Thread (EDT) for Swing safety

<h2>⚠️ Limitations</h2>
Not encrypted (plain TCP)
One thread per client (not scalable for large systems)
No chunked file transfer (large files may cause memory issues)
No persistent storage or authentication

<h2>🔮 Future Enhancements</h2>
🔐 SSL/TLS encrypted sockets
👤 Login & authentication system
💾 Database for chat history
🌐 WebSocket version for browser chat
💬 Group chats & delivery receipts

<h2>🏁 Conclusion</h2>
This project showcases full-stack Java desktop development — covering networking, concurrency, file transfer, and modern UI design.
It demonstrates how client-server communication and thread-safe GUI interaction can be integrated into a single, polished application.

<h2>📚 References</h2>
Oracle Java SE Documentation — java.net, java.io, java.nio.file
Oracle Tutorials: Networking & Swing
Base64 Encoding — java.util.Base64
