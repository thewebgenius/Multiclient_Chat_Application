import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 1234;
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static final Set<String> usernames = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, String> statuses = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        int port = PORT;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                System.out.println("Invalid port argument, using default port " + PORT);
                port = PORT;
            }
        }

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
            System.out.println("Java runtime: " + System.getProperty("java.version") + " (home=" + System.getProperty("java.home") + ")");
            while (true) {
                System.out.println("Waiting for client connection...");
                Socket socket = serverSocket.accept();
                System.out.println("New client connected from: " + socket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (BindException be) {
            System.err.println("Failed to bind to port " + port + ". It may already be in use or you may not have permission.");
                System.err.println(String.format("On Windows you can run: netstat -ano | findstr \":%d\" to find the PID, then taskkill /F /PID <PID> to terminate it.", port));
            be.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            System.err.println("I/O error while running server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        private void broadcastUserList() {
            StringBuilder userList = new StringBuilder("Online users: ");
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    if (client.username != null) {
                        String status = statuses.getOrDefault(client.username, "Online");
                        // format: username|status
                        userList.append(client.username).append("|").append(status).append(", ");
                    }
                }
            }
            broadcast(userList.toString());
        }

        @Override
        public void run() {
            try {
                System.out.println("Setting up streams for client...");
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);
                
                System.out.println("Requesting username...");
                out.println("Enter your username:");
                username = in.readLine();
                
                if (username == null || username.trim().isEmpty()) {
                    System.out.println("Client disconnected without providing username");
                    return;
                }

                synchronized (usernames) {
                    if (usernames.contains(username)) {
                        out.println("Username already taken. Please try again.");
                        return;
                    }
                    usernames.add(username);
                }

                System.out.println("New user joined: " + username);
                // Send a structured user event so clients can show it in the online users box
                broadcast("USER_EVENT::joined::" + username);
                broadcastUserList();

                String message;
                while ((message = in.readLine()) != null) {
                    if (message == null) break;
                    if (message.startsWith("STATUS::")) {
                        String newStatus = message.substring("STATUS::".length()).trim();
                        statuses.put(username, newStatus);
                        broadcastUserList();
                        continue;
                    }
                    if (message.startsWith("FILE::")) {
                        // message format: FILE::filename::base64
                        String rest = message.substring("FILE::".length());
                        // rebroadcast with sender info: FILE_FROM::username::filename::base64
                        broadcast("FILE_FROM::" + username + "::" + rest);
                    } else if (message.startsWith("DM::")) {
                        // Format: DM::recipient::message
                        String[] parts = message.substring("DM::".length()).split("::", 2);
                        if (parts.length == 2) {
                            String recipient = parts[0];
                            String dmContent = parts[1];
                            // Find recipient's handler and send only to them (and back to sender)
                            boolean found = false;
                            synchronized(clients) {
                                for (ClientHandler client : clients) {
                                    if (client.username != null && 
                                        (client.username.equals(recipient) || client.username.equals(username))) {
                                        client.out.println("PRIVATE::" + username + ": " + dmContent);
                                        if (client.username.equals(recipient)) found = true;
                                    }
                                }
                            }
                            if (!found) {
                                // Let sender know if recipient not found
                                out.println("System: User '" + recipient + "' not found or offline.");
                            }
                        }
                    } else if (!message.trim().isEmpty()) {
                        broadcast(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error handling client " + username + ": " + e.getMessage());
            } finally {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
                
                if (username != null) {
                    usernames.remove(username);
                    clients.remove(this);
                    // Send a structured user event so clients can show it in the online users box
                    broadcast("USER_EVENT::left::" + username);
                    broadcastUserList();
                }
            }
        }

        private void broadcast(String message) {
            for (ClientHandler client : clients) {
                client.out.println(message);
            }
        }
    }
}