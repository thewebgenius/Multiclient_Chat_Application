import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.BiConsumer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

class BubbleBorder extends AbstractBorder {
    private Color color;
    private int thickness;
    private int radius;
    private Insets insets;
    private BasicStroke stroke;
    private int strokePad;
    private RenderingHints hints;

    public BubbleBorder(Color color, int thickness, int radius) {
        this.color = color;
        this.thickness = thickness;
        this.radius = radius;
        
        stroke = new BasicStroke(thickness);
        strokePad = thickness / 2;
        hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int pad = radius + strokePad;
        insets = new Insets(pad, pad, pad, pad);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return insets;
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        return getBorderInsets(c);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHints(hints);

        int bottomLineY = height - thickness;
        RoundRectangle2D bubble = new RoundRectangle2D.Float(
            strokePad, strokePad,
            width - thickness, height - thickness,
            radius, radius);

        // Create a soft shadow effect
        g2.setColor(new Color(0, 0, 0, 30));
        g2.setStroke(new BasicStroke(thickness));
        g2.translate(2, 2);
        g2.draw(bubble);
        g2.translate(-2, -2);

        // Draw the main border
        g2.setColor(color);
        g2.setStroke(stroke);
        g2.draw(bubble);
    }
}

public class ChatClient extends JFrame {
    private JTextArea chatArea;
    private JPanel messagePanel;
    private JScrollPane chatScroll;
    private JTextField messageField;
    private JButton sendButton;
    private JButton emojiButton;
    private JToggleButton darkModeToggle;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JLabel userEventLabel;
    private JLabel chatTargetLabel;
    private JComboBox<String> statusCombo;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private JPopupMenu emojiMenu;
    private static final HashMap<String, String> EMOJIS = new HashMap<String, String>() {{
        put("\uD83D\uDE0A", ":smile:"); // 😊
        put("\uD83D\uDE02", ":joy:"); // 😂
        put("\u2764\uFE0F", ":heart:"); // ❤️
        put("\uD83D\uDC4D", ":thumbsup:"); // 👍
        put("\uD83D\uDE0E", ":cool:"); // 😎
        put("\uD83C\uDF89", ":party:"); // 🎉
        put("\uD83E\uDD14", ":thinking:"); // 🤔
        put("\uD83D\uDC4B", ":wave:"); // 👋
        put("\uD83C\uDF1F", ":star:"); // 🌟
        put("\uD83D\uDD25", ":fire:"); // 🔥
        put("\uD83D\uDE18", ":kiss:"); // 😘
        put("\uD83D\uDE0D", ":heart_eyes:"); // 😍
        put("\uD83D\uDE04", ":grin:"); // 😄
        put("\uD83D\uDC4C", ":ok:"); // �
        put("\u2728", ":sparkles:"); // ✨
    }};

    public ChatClient() {
        // Apply modern Nimbus look-and-feel
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.put("nimbusBase", new Color(0, 123, 255)); // Blue accent
            UIManager.put("control", Color.WHITE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Modern Chat App");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

    // Top bar: dark mode toggle
    JPanel topBar = new JPanel(new BorderLayout());
    topBar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
    topBar.setBackground(Color.WHITE);
    darkModeToggle = new JToggleButton("🕶️ Dark");
    darkModeToggle.setFocusPainted(false);
    darkModeToggle.addActionListener(e -> applyTheme(darkModeToggle.isSelected()));
    JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    rightBox.setOpaque(false);
    rightBox.add(darkModeToggle);
    // Status selector
    statusCombo = new JComboBox<>(new String[]{"Online", "Away", "Busy"});
    statusCombo.setSelectedIndex(0);
    statusCombo.setFocusable(false);
    statusCombo.addActionListener(e -> {
        String s = (String) statusCombo.getSelectedItem();
        if (out != null) out.println("STATUS::" + s);
    });
    rightBox.add(Box.createRigidArea(new Dimension(8,0)));
    rightBox.add(statusCombo);
    topBar.add(rightBox, BorderLayout.EAST);
    // Add a left-side area to show current chat target
    JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    leftBox.setOpaque(false);
    chatTargetLabel = new JLabel("Chatting with: All");
    chatTargetLabel.setFont(new Font("Arial", Font.BOLD, 12));
    leftBox.add(chatTargetLabel);
    topBar.add(leftBox, BorderLayout.WEST);

    add(topBar, BorderLayout.NORTH);

        // Left panel: Online users (like a sidebar)
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Arial", Font.PLAIN, 12));
        userList.setBackground(new Color(240, 240, 240));
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String sel = userList.getSelectedValue();
                if (sel == null || sel.trim().isEmpty()) {
                    chatTargetLabel.setText("Chatting with: All");
                } else {
                    // display is like "username (Online)" — strip status
                    String name = sel.split(" ")[0];
                    chatTargetLabel.setText("Chatting with: " + name);
                }
            }
        });
    JScrollPane userScroll = new JScrollPane(userList);
    userScroll.setBorder(BorderFactory.createTitledBorder("Online Users"));
    userScroll.setPreferredSize(new Dimension(150, 0));

    // Create a panel to hold the user list and a small event label below it
    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.setPreferredSize(new Dimension(170, 0));
    leftPanel.add(userScroll, BorderLayout.CENTER);

    userEventLabel = new JLabel("");
    userEventLabel.setFont(new Font("Arial", Font.ITALIC, 12));
    userEventLabel.setBorder(BorderFactory.createEmptyBorder(6,8,6,8));
    userEventLabel.setForeground(new Color(80,80,80));
    leftPanel.add(userEventLabel, BorderLayout.SOUTH);

    add(leftPanel, BorderLayout.WEST);

    // Center: messages panel with chat-bubble UI
    messagePanel = new JPanel();
    messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
    messagePanel.setBackground(Color.WHITE);

    chatScroll = new JScrollPane(messagePanel);
    chatScroll.setBorder(BorderFactory.createTitledBorder("Chat"));
    add(chatScroll, BorderLayout.CENTER);

    // Use a font that supports emojis for bubble labels
    Font bubbleFont;
        try {
            // Try using system emoji fonts in order of preference
            String[] emojiFonts = {"Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", "Noto Emoji"};
            Font emojiFont = null;
            for (String fontName : emojiFonts) {
                if (Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()).contains(fontName)) {
                    emojiFont = new Font(fontName, Font.PLAIN, 14);
                    break;
                }
            }
            if (emojiFont != null) {
                bubbleFont = emojiFont;
            } else {
                bubbleFont = new Font("Dialog", Font.PLAIN, 14);
            }
        } catch (Exception e) {
            bubbleFont = new Font("Dialog", Font.PLAIN, 14);
        }
    // chatScroll already has a titled border

        // Bottom: Input panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.setBackground(Color.WHITE);

    messageField = new JTextField();
        // Use the same emoji font for consistency
        try {
            String[] emojiFonts = {"Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", "Noto Emoji"};
            Font emojiFont = null;
            for (String fontName : emojiFonts) {
                if (Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()).contains(fontName)) {
                    emojiFont = new Font(fontName, Font.PLAIN, 14);
                    break;
                }
            }
            if (emojiFont != null) {
                messageField.setFont(emojiFont);
            } else {
                messageField.setFont(new Font("Dialog", Font.PLAIN, 14));
            }
        } catch (Exception e) {
            messageField.setFont(new Font("Dialog", Font.PLAIN, 14));
        }
        messageField.setBorder(BorderFactory.createCompoundBorder(
            new BubbleBorder(new Color(214, 219, 229), 1, 20),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        messageField.setPreferredSize(new Dimension(0, 42));
        messageField.setBackground(new Color(250, 251, 252));
        messageField.setCaretColor(new Color(33, 150, 243));

    // Create emoji button with modern styling
    emojiButton = new JButton("\uD83D\uDE0A"); // Unicode for 😊
        // Try to find the best emoji font available
        Font emojiFont = null;
        String[] emojiFonts = {
            "Segoe UI Emoji", // Windows
            "Apple Color Emoji", // macOS
            "Noto Color Emoji", // Linux
            "Segoe UI Symbol", // Windows fallback
            "Noto Emoji"
        };
        for (String fontName : emojiFonts) {
            try {
                if (Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()).contains(fontName)) {
                    emojiFont = new Font(fontName, Font.PLAIN, 18);
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (emojiFont == null) {
            emojiFont = new Font("Dialog", Font.PLAIN, 18);
        }
        emojiButton.setFont(emojiFont);
        emojiButton.setBackground(new Color(245, 247, 250));
        emojiButton.setForeground(new Color(52, 73, 94));
        emojiButton.setFocusPainted(false);
        emojiButton.setBorder(BorderFactory.createCompoundBorder(
            new BubbleBorder(new Color(214, 219, 229), 1, 15),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        emojiButton.setPreferredSize(new Dimension(45, 40));

        // Create emoji menu with modern styling
        emojiMenu = new JPopupMenu();
        emojiMenu.setBorder(BorderFactory.createCompoundBorder(
            new BubbleBorder(new Color(214, 219, 229), 1, 10),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        emojiMenu.setBackground(new Color(250, 251, 252));

        // Use a grid layout for the emoji menu
        JPanel emojiGrid = new JPanel(new GridLayout(0, 5, 2, 2));
        emojiGrid.setBackground(new Color(250, 251, 252));
        
        // Get the same emoji font we used for the button but larger
        Font menuEmojiFont = emojiButton.getFont().deriveFont(22f);
        
        for (String emoji : EMOJIS.keySet()) {
            JButton item = new JButton(emoji);
            item.setFont(menuEmojiFont);
            item.setBackground(new Color(250, 251, 252));
            item.setForeground(new Color(52, 73, 94));
            item.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            item.setFocusPainted(false);
            item.addActionListener(e -> {
                insertEmoji(emoji);
                emojiMenu.setVisible(false);
            });
            
            // Add hover effect
            item.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    item.setBackground(new Color(240, 242, 245));
                }
                public void mouseExited(MouseEvent e) {
                    item.setBackground(new Color(250, 251, 252));
                }
            });
            
            emojiGrid.add(item);
        }
        
        emojiMenu.add(emojiGrid);

        emojiButton.addActionListener(e -> {
            emojiMenu.show(emojiButton, 0, -emojiMenu.getPreferredSize().height);
        });

        // Attach (file) button
        JButton attachButton = new JButton("📎");
        attachButton.setFont(new Font("Dialog", Font.PLAIN, 16));
        attachButton.setBackground(new Color(240, 240, 240));
        attachButton.setFocusPainted(false);
        attachButton.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        attachButton.setPreferredSize(new Dimension(48, 40));
        attachButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int res = chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                new Thread(() -> sendFile(selected)).start();
            }
        });

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.setBackground(new Color(33, 150, 243)); // Material Blue
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createCompoundBorder(
            new BubbleBorder(new Color(25, 118, 210), 1, 15),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        sendButton.setPreferredSize(new Dimension(85, 42));
        
        // Add hover effect
        sendButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                sendButton.setBackground(new Color(25, 118, 210));
            }
            public void mouseExited(MouseEvent e) {
                sendButton.setBackground(new Color(33, 150, 243));
            }
        });

    JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
    inputPanel.setBackground(Color.WHITE);
    JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    leftControls.setOpaque(false);
    leftControls.add(emojiButton);
    leftControls.add(attachButton);
    inputPanel.add(leftControls, BorderLayout.WEST);
    inputPanel.add(messageField, BorderLayout.CENTER);
    inputPanel.add(sendButton, BorderLayout.EAST);

        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Helper to add a message bubble
        final Font finalBubbleFont = bubbleFont;
        Runnable ensureScroll = () -> SwingUtilities.invokeLater(() -> {
            JScrollBar v = chatScroll.getVerticalScrollBar();
            v.setValue(v.getMaximum());
        });

        // method: addMessage
        BiConsumer<String, String> addOwnMessage = (sender, text) -> {
            JPanel bubble = createBubble(sender, text, true, finalBubbleFont);
            messagePanel.add(bubble);
            messagePanel.add(Box.createVerticalStrut(6));
            ensureScroll.run();
            messagePanel.revalidate();
        };
        BiConsumer<String, String> addOtherMessage = (sender, text) -> {
            JPanel bubble = createBubble(sender, text, false, finalBubbleFont);
            messagePanel.add(bubble);
            messagePanel.add(Box.createVerticalStrut(6));
            ensureScroll.run();
            messagePanel.revalidate();
        };

        // Event listeners
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

    // Apply initial (light) theme
    applyTheme(false);

        // Ask for username (require non-empty). If user cancels, exit gracefully.
        while (true) {
            username = JOptionPane.showInputDialog(this, "Enter your username:", "Login", JOptionPane.PLAIN_MESSAGE);
            if (username == null) {
                // user cancelled
                dispose();
                return;
            }
            username = username.trim();
            if (!username.isEmpty()) break;
        }

        boolean connected = connectToServer();
        if (!connected) {
            // user chose not to retry / failed to connect — close UI gracefully
            dispose();
            return;
        }

        new Thread(this::receiveMessages).start();
    }

    /**
     * Try to connect to server, prompting for host/port and allowing retries.
     * @return true if connected successfully, false if the user cancelled / chose not to retry
     */
    private boolean connectToServer() {
        String host = "127.0.0.1";
        int port = 1234;

        while (true) {
            try {
                String hostInput = (String) JOptionPane.showInputDialog(this, "Server host:", "Connect", JOptionPane.PLAIN_MESSAGE, null, null, host);
                if (hostInput == null) return false; // user cancelled
                String portInput = (String) JOptionPane.showInputDialog(this, "Server port:", "Connect", JOptionPane.PLAIN_MESSAGE, null, null, Integer.toString(port));
                if (portInput == null) return false;

                host = hostInput.trim();
                port = Integer.parseInt(portInput.trim());

                System.out.println("Attempting to connect to server at " + host + ":" + port + " ...");
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 5000); // 5s timeout

                System.out.println("Connected to server");
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));

                // Read the server's username prompt (if any) and send username
                try {
                    socket.setSoTimeout(3000);
                    String prompt = in.readLine();
                    System.out.println("Server says: " + prompt);
                } catch (IOException ignored) {
                    // server may not send a prompt; continue
                } finally {
                    try { socket.setSoTimeout(0); } catch (SocketException ignored) {}
                }

                out.println(username);
                System.out.println("Sent username: " + username);
                return true;
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Invalid port number. Please enter a numeric port.");
                continue;
            } catch (IOException e) {
                e.printStackTrace();
                int option = JOptionPane.showConfirmDialog(this, "Unable to connect to server: " + e.getMessage() + "\nRetry?", "Connection failed", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                if (option != JOptionPane.YES_OPTION) return false;
                // else loop and prompt again
            }
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            String selectedUser = userList.getSelectedValue();
            if (selectedUser != null && !selectedUser.trim().isEmpty()) {
                // Strip (status) from display name if present
                String recipient = selectedUser.split(" ")[0];
                // Send as direct message
                out.println("DM::" + recipient + "::" + message);
            } else {
                // Broadcast to all
                out.println(message);
            }
            messageField.setText("");
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                final String finalMessage = message;
                SwingUtilities.invokeLater(() -> {
                    if (finalMessage.startsWith("Online users:")) {
                        updateUserList(finalMessage);
                    } else if (finalMessage.startsWith("USER_EVENT::")) {
                        // Format: USER_EVENT::action::username  (action = joined|left)
                        String[] parts = finalMessage.split("::", 3);
                        if (parts.length == 3) {
                            String action = parts[1];
                            String who = parts[2];
                            String text = who + ("joined".equalsIgnoreCase(action) ? " joined" : " left");
                            showUserEvent(text);
                        }
                    } else if (finalMessage.startsWith("FILE_FROM::")) {
                        // Format: FILE_FROM::sender::filename::base64
                        String[] parts = finalMessage.split("::", 4);
                        if (parts.length == 4) {
                            String sender = parts[1];
                            String filename = parts[2];
                            String base64 = parts[3];
                            addMessage(sender, "sent a file: " + filename, false);
                            // Prompt to save
                            int choice = JOptionPane.showConfirmDialog(this, sender + " sent a file: " + filename + "\nSave to disk?", "File received", JOptionPane.YES_NO_OPTION);
                            if (choice == JOptionPane.YES_OPTION) {
                                try {
                                    byte[] data = Base64.getDecoder().decode(base64);
                                    Path outPath = Paths.get(System.getProperty("user.home"), "Downloads", filename);
                                    Files.createDirectories(outPath.getParent());
                                    Files.write(outPath, data);
                                    JOptionPane.showMessageDialog(this, "Saved to: " + outPath.toString());
                                } catch (IOException ex) {
                                    JOptionPane.showMessageDialog(this, "Failed to save file: " + ex.getMessage());
                                }
                            }
                        }
                    } else if (finalMessage.startsWith("PRIVATE::")) {
                        // Handle private messages with special styling
                        String content = finalMessage.substring("PRIVATE::".length());
                        String timestamp = new SimpleDateFormat("HH:mm").format(new Date());
                        String msg = content;
                        String sender = "";
                        String body = msg;
                        int idx = msg.indexOf(": ");
                        if (idx > 0) {
                            sender = msg.substring(0, idx);
                            body = msg.substring(idx + 2);
                        }
                        boolean isSelf = sender != null && !sender.isEmpty() && sender.equals(username);
                        addPrivateMessage(sender, body, isSelf);
                    } else {
                        // Expect normal chat broadcasts in format: "username: message"
                        String timestamp = new SimpleDateFormat("HH:mm").format(new Date());
                        String msg = finalMessage;
                        String sender = "";
                        String body = msg;
                        int idx = msg.indexOf(": ");
                        if (idx > 0) {
                            sender = msg.substring(0, idx);
                            body = msg.substring(idx + 2);
                        }

                        boolean isSelf = sender != null && !sender.isEmpty() && sender.equals(username);
                        // If there's no explicit sender (server/system message), show as left-side info bubble
                        if (sender == null || sender.isEmpty()) {
                            addMessage("", "[" + timestamp + "] " + body, false);
                        } else {
                            addMessage(sender, body, isSelf);
                        }
                    }
                });
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> addMessage("", "Disconnected from server.", false));
        }
    }

    private void showUserEvent(String text) {
        if (userEventLabel == null) return;
        userEventLabel.setText(text);
        // clear after 5 seconds
        Timer t = new Timer(5000, e -> userEventLabel.setText(""));
        t.setRepeats(false);
        t.start();
    }

    private void sendFile(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            // Send in format: FILE::filename::base64
            out.println("FILE::" + file.getName() + "::" + base64);
            SwingUtilities.invokeLater(() -> addMessage(username, "Sent file: " + file.getName(), true));
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to send file: " + e.getMessage()));
        }
    }

    private void updateUserList(String message) {
        userListModel.clear();
        String[] users = message.replace("Online users: ", "").split(", ");
        for (String user : users) {
            if (!user.trim().isEmpty()) {
                // expected format: username|status
                String token = user.trim();
                String display = token;
                if (token.contains("|")) {
                    String[] parts = token.split("\\|", 2);
                    String uname = parts[0];
                    String st = parts.length > 1 ? parts[1] : "Online";
                    display = uname + " (" + st + ")";
                }
                userListModel.addElement(display);
            }
        }
    }

    // UI: create a chat bubble panel
    private JPanel createBubble(String sender, String text, boolean isSelf, Font font) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        
        // WhatsApp-inspired gradient colors
        Color startColor = isSelf ? new Color(0, 184, 148) : new Color(255, 255, 255);
        Color endColor = isSelf ? new Color(0, 168, 132) : new Color(240, 242, 245);
        Color borderColor = isSelf ? new Color(0, 150, 120) : new Color(220, 222, 225);
        
        bubble.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(2, 2, 2, 2),
            BorderFactory.createCompoundBorder(
                new BubbleBorder(borderColor, 1, 15),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            )
        ));
        
        // Use modern messenger gradient background
        bubble.setBackground(startColor);

        JLabel nameLabel = new JLabel(sender == null || sender.isEmpty() ? "" : sender);
        nameLabel.setFont(font.deriveFont(Font.BOLD, 13f));
        nameLabel.setForeground(isSelf ? Color.WHITE : new Color(33, 33, 33));

    // Use an HTML JLabel for the message body so wrapping and preferred size are calculated reliably
    String escaped = escapeHtml(text);
    // approxWidth will be computed below and inserted into the HTML to control wrapping
    JLabel msgLabel = new JLabel();
    msgLabel.setFont(font);
    msgLabel.setOpaque(false);
    msgLabel.setForeground(isSelf ? Color.WHITE : Color.BLACK);

    if (!nameLabel.getText().isEmpty()) bubble.add(nameLabel);
    bubble.add(msgLabel);

        // Adaptive sizing: choose max width based on scroll viewport width
        int maxWidth = 400; // larger fallback for better text display
        if (chatScroll != null) {
            JViewport vp = chatScroll.getViewport();
            Dimension viewSize = vp.getExtentSize();
            if (viewSize != null && viewSize.width > 0) {
                // allow bubble up to ~75% of viewport width for better text display
                maxWidth = Math.max(200, (int)(viewSize.width * 0.75));
            }
        }

        // For long messages, always use maxWidth to ensure proper wrapping
        FontMetrics fm = msgLabel.getFontMetrics(msgLabel.getFont());
        int approxCharWidth = fm.charWidth('a') > 0 ? fm.charWidth('a') : 7;
        int textWidth = text.length() * approxCharWidth;
        // If text is longer than maxWidth, use maxWidth for proper wrapping
        int approxWidth = textWidth > maxWidth ? maxWidth : Math.max(80, Math.min(maxWidth, textWidth + 20));
        String html = "<html><div style='width: " + approxWidth + "px; word-wrap: break-word;'>" + escaped + "</div></html>";
        msgLabel.setText(html);
        msgLabel.setMaximumSize(new Dimension(approxWidth, Integer.MAX_VALUE));

        // align bubble left or right
        if (isSelf) {
            wrapper.add(bubble, BorderLayout.EAST);
        } else {
            wrapper.add(bubble, BorderLayout.WEST);
        }

        return wrapper;
    }

    private void addPrivateMessage(String sender, String text, boolean isSelf) {
        Font font = messageField.getFont();
        // Create private message bubble with Messenger/Telegram-inspired colors
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        
        // Modern private message styling
        Color bgStart = isSelf ? new Color(0, 132, 255) : new Color(244, 241, 255);
        Color bgEnd = isSelf ? new Color(0, 120, 232) : new Color(236, 233, 250);
        Color borderColor = isSelf ? new Color(0, 108, 210) : new Color(220, 215, 245);
        
        bubble.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(2, 2, 2, 2),
            BorderFactory.createCompoundBorder(
                new BubbleBorder(borderColor, 1, 15),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            )
        ));
        bubble.setBackground(bgStart);
        
        // Add a small lock icon to indicate private message
        JLabel nameLabel = new JLabel(sender + " 🔒");
        nameLabel.setFont(font.deriveFont(Font.BOLD, 13f));
        nameLabel.setForeground(isSelf ? Color.WHITE : new Color(88, 86, 214));
        
        // Use HTML JLabel for message text
        String escaped = escapeHtml(text);
        JLabel msgLabel = new JLabel();
        msgLabel.setFont(font);
        msgLabel.setOpaque(false);
        msgLabel.setForeground(Color.BLACK);
        
        // Adaptive sizing based on viewport width
        int maxWidth = 300;
        if (chatScroll != null) {
            JViewport vp = chatScroll.getViewport();
            Dimension viewSize = vp.getExtentSize();
            if (viewSize != null && viewSize.width > 0) {
                maxWidth = Math.max(120, (int)(viewSize.width * 0.6));
            }
        }
        
        FontMetrics fm = msgLabel.getFontMetrics(msgLabel.getFont());
        int approxCharWidth = fm.charWidth('a') > 0 ? fm.charWidth('a') : 7;
        int textWidth = text.length() * approxCharWidth;
        // If text is longer than maxWidth, use maxWidth for proper wrapping
        int approxWidth = textWidth > maxWidth ? maxWidth : Math.max(80, Math.min(maxWidth, textWidth + 20));
        String html = "<html><div style='width: " + approxWidth + "px; word-wrap: break-word;'>" + escaped + "</div></html>";
        msgLabel.setText(html);
        msgLabel.setMaximumSize(new Dimension(approxWidth, Integer.MAX_VALUE));
        
        bubble.add(nameLabel);
        bubble.add(msgLabel);
        
        if (messagePanel != null && chatScroll != null) {
            JPanel holder = new JPanel(new BorderLayout());
            holder.setOpaque(false);
            if (isSelf) {
                holder.add(bubble, BorderLayout.EAST);
            } else {
                holder.add(bubble, BorderLayout.WEST);
            }
            messagePanel.add(holder);
            messagePanel.add(Box.createVerticalStrut(6));
            messagePanel.revalidate();
            
            // Enhanced auto-scroll mechanism
            SwingUtilities.invokeLater(() -> {
                // First scroll attempt
                JScrollBar v = chatScroll.getVerticalScrollBar();
                v.setValue(v.getMaximum());
                
                // Second scroll attempt after a short delay to ensure layout is complete
                Timer timer = new Timer(50, e -> {
                    v.setValue(v.getMaximum());
                    ((Timer)e.getSource()).stop();
                    
                    // Final scroll attempt to handle any dynamic content
                    Timer finalTimer = new Timer(100, e2 -> {
                        v.setValue(v.getMaximum());
                        ((Timer)e2.getSource()).stop();
                    });
                    finalTimer.setRepeats(false);
                    finalTimer.start();
                });
                timer.setRepeats(false);
                timer.start();
            });
            return;
        }
    }

    private void addMessage(String sender, String text, boolean isSelf) {
        Font font = messageField.getFont();
        JPanel bubble = createBubble(sender, text, isSelf, font);

        if (messagePanel != null && chatScroll != null) {
            // Put the bubble inside a holder panel to left/right align consistently
            JPanel holder = new JPanel(new BorderLayout());
            holder.setOpaque(false);
            if (isSelf) {
                holder.add(bubble, BorderLayout.EAST);
            } else {
                holder.add(bubble, BorderLayout.WEST);
            }
            messagePanel.add(holder);
            messagePanel.add(Box.createVerticalStrut(6));
            messagePanel.revalidate();
            
            // Enhanced auto-scroll mechanism
            SwingUtilities.invokeLater(() -> {
                // First scroll attempt
                JScrollBar v = chatScroll.getVerticalScrollBar();
                v.setValue(v.getMaximum());
                
                // Second scroll attempt after a short delay to ensure layout is complete
                Timer timer = new Timer(50, e -> {
                    v.setValue(v.getMaximum());
                    ((Timer)e.getSource()).stop();
                    
                    // Final scroll attempt to handle any dynamic content
                    Timer finalTimer = new Timer(100, e2 -> {
                        v.setValue(v.getMaximum());
                        ((Timer)e2.getSource()).stop();
                    });
                    finalTimer.setRepeats(false);
                    finalTimer.start();
                });
                timer.setRepeats(false);
                timer.start();
            });
            return;
        }

        // Fallback: try to find the scroll pane (legacy behavior)
        JScrollPane sp = null;
        for (Component c : getContentPane().getComponents()) {
            if (c instanceof JScrollPane) { sp = (JScrollPane) c; break; }
        }
        if (sp == null) return;
        JViewport vp = sp.getViewport();
        Component view = vp.getView();
        if (!(view instanceof JPanel)) return;
        JPanel mp = (JPanel) view;
        mp.add(bubble);
        mp.add(Box.createVerticalStrut(6));
        mp.revalidate();
        JScrollBar v = sp.getVerticalScrollBar();
        v.setValue(v.getMaximum());
    }

    private void insertEmoji(String emoji) {
        String currentText = messageField.getText();
        int caretPosition = messageField.getCaretPosition();
        String newText = currentText.substring(0, caretPosition) + emoji + currentText.substring(caretPosition);
        messageField.setText(newText);
        messageField.setCaretPosition(caretPosition + emoji.length());
        messageField.requestFocusInWindow();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        String out = s.replace("&", "&amp;")
                      .replace("<", "&lt;")
                      .replace(">", "&gt;")
                      .replace("\n", "<br/>");
        return out;
    }

    // Apply dark or light theme to the UI
    private void applyTheme(boolean dark) {
        Color bg, panelBg, inputBg, fg, accent, borderColor;
        if (dark) {
            bg = new Color(34, 34, 34);
            panelBg = new Color(48, 48, 48);
            inputBg = new Color(60, 60, 60);
            fg = new Color(230, 230, 230);
            accent = new Color(10, 132, 255);
            borderColor = new Color(80, 80, 80);
            darkModeToggle.setText("🌞 Light");
        } else {
            bg = Color.WHITE;
            panelBg = new Color(240, 240, 240);
            inputBg = Color.WHITE;
            fg = Color.BLACK;
            accent = new Color(0, 123, 255);
            borderColor = new Color(200, 200, 200);
            darkModeToggle.setText("🕶️ Dark");
        }

        getContentPane().setBackground(bg);
        // Chat area / message panel
        if (chatArea != null) {
            chatArea.setBackground(panelBg);
            chatArea.setForeground(fg);
            chatArea.setCaretColor(fg);
        } else {
            // find message panel inside scroll pane
            for (Component c : getContentPane().getComponents()) {
                if (c instanceof JScrollPane) {
                    JViewport vp = ((JScrollPane) c).getViewport();
                    Component view = vp.getView();
                    if (view instanceof JPanel) {
                        view.setBackground(panelBg);
                        // update child bubbles foreground where applicable
                        for (Component child : ((JPanel) view).getComponents()) {
                            // best-effort: update labels and text areas
                            if (child instanceof JPanel) {
                                for (Component inner : ((JPanel) child).getComponents()) {
                                    if (inner instanceof JLabel) inner.setForeground(fg);
                                    if (inner instanceof JTextArea) inner.setForeground(fg);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Message field
        messageField.setBackground(inputBg);
        messageField.setForeground(fg);
        messageField.setCaretColor(fg);
        messageField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        // User list
        userList.setBackground(panelBg);
        userList.setForeground(fg);

        // Buttons
        sendButton.setBackground(accent);
        sendButton.setForeground(Color.WHITE);
        emojiButton.setBackground(panelBg);
        emojiButton.setForeground(fg);

        // Toggle styling
        darkModeToggle.setBackground(panelBg);
        darkModeToggle.setForeground(fg);

        // Force repaint
        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient().setVisible(true));
    }
}
