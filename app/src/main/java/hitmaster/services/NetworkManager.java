package hitmaster.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import hitmaster.models.JoinRequest;
import hitmaster.models.MultiplayerLobby;

public class NetworkManager {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private NetworkListener listener;

    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private String hostPassword = "";

    private DatagramSocket udpSocket;
    private boolean isBroadcasting = false;
    private boolean isDiscovering = false;
    private static final int DISCOVERY_PORT = 5051;

    // ==========================================
    // #region LAN DISCOVERY & BROADCAST
    // ==========================================

    public interface DiscoveryListener {
        void onLobbyFound(MultiplayerLobby lobby);
    }

    /**
     * Starts broadcasting active lobby to other users in this network.
     * Sends lobby information every two seconds, containting name and boolean has password.
     * @param lobbyName Name of lobby.
     * @param hasPassword Boolean if lobby is password protected.
     */
    public void startLobbyBroadcast(String lobbyName, boolean hasPassword) {
        this.isBroadcasting = true;

        String appVersion = NetworkManager.loadVersion();

        new Thread(() -> {
            try {
                udpSocket = new DatagramSocket();
                udpSocket.setBroadcast(true);

                String message = "LOBBY:" + lobbyName + ":" + appVersion + ":" + hasPassword;
                byte[] buffer = message.getBytes();

                InetAddress broadcastAdress = InetAddress.getByName("255.255.255.255"); // 255.255.255.255 gets all devices from current subnet
                Log.Info("Starting LAN Broadcast...");

                while (isBroadcasting) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAdress, DISCOVERY_PORT);
                    udpSocket.send(packet);

                    Thread.sleep(2000);
                }
            }
            catch (IOException | InterruptedException e) {
                Log.Error("Broadcast Error: " + e.getMessage());
            }
            finally {
                if (udpSocket != null && !udpSocket.isClosed())
                    udpSocket.close();
            }
        }).start();
    }

    /**
     * Stops broadcasting lobby to other users in this network.
     */
    public void stopLobbyBroadcast() {
        this.isBroadcasting = false;

        if (udpSocket != null && !udpSocket.isClosed())
            udpSocket.close();
    }

    /**
     * Starts looking for broadcastet LAN lobbies in this network.
     * Triggers onLobbyFound() method for given DiscoveryListener when lobby was found.
     * @param discoveryListener DiscoveryListener with onLobbyFound() method.
     */
    public void startLobbyDiscovery(DiscoveryListener discoveryListener) {
        this.isDiscovering = true;

        new Thread(() -> {
            try (DatagramSocket receiveSocket = new DatagramSocket(null)) {
                receiveSocket.setReuseAddress(true);
                receiveSocket.bind(new InetSocketAddress(DISCOVERY_PORT));

                receiveSocket.setSoTimeout(3000);
                byte[] buffer = new byte[1024];

                Log.Info("Searching for LAN lobbies...");
                while(isDiscovering) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    try {
                        receiveSocket.receive(packet);
                        String message = new String(packet.getData(), 0, packet.getLength()).trim();

                        if (message.startsWith("LOBBY:")) {
                            String[] parts = message.split(":");
                            String lobbyName = parts[1];
                            String appVersion = parts[2];
                            boolean hasPassword = Boolean.parseBoolean(parts[3]);
                            String hostIp = packet.getAddress().getHostAddress();

                            MultiplayerLobby foundLobby = new MultiplayerLobby(lobbyName, hostIp, appVersion, 1, 2);
                            foundLobby.hasPassword = hasPassword;

                            discoveryListener.onLobbyFound(foundLobby);
                        }
                    }
                    catch (IOException | NumberFormatException e) {
                        //
                    }
                }
            }
            catch (Exception e) {
                Log.Error("Discovery Error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Stops looking for broadcastet lobbies.
     */
    public void stopLobbyDiscovery() {
        this.isDiscovering = false;
    }

    // #endregion

    // ==========================================
    // #region HOST MODE (Multiplayer & Password Check)
    // ==========================================

    public interface NetworkListener {
        void onObjectReceived(Object obj);
    }

    /**
     * Starts hosting a lobby and waits for other players to join.
     * @param port Port to host the lobby at.
     * @param password Password of hosted lobby. Blank if none.
     * @param listener NetworkListener with onObjectReceived() method.
     */
    public void startAsHost(int port, String password, NetworkListener listener) {
        this.listener = listener;
        this.hostPassword = password != null ? password : "";

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                Log.Info("Host started. Waiting for players on port " + port + "...");

                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    Log.Info("Client connecting from: " + socket.getInetAddress().getHostAddress());

                    new Thread(() -> this.handleIncomingClient(socket)).start();
                }
            }
            catch (IOException e) {
                Log.Error("Error while hosting: " + e.getMessage());
            }
        }).start();
    }

    private void handleIncomingClient(Socket socket) {
        try {
            ObjectOutputStream clientOut = new ObjectOutputStream(socket.getOutputStream());
            clientOut.flush();
            ObjectInputStream clientIn = new ObjectInputStream(socket.getInputStream());

            Object firstObject = clientIn.readObject();

            if (firstObject instanceof JoinRequest joinRequest) {

                // Check password
                if (!this.hostPassword.isEmpty() && !this.hostPassword.equals(joinRequest.password)) {
                    Log.Warning("Client tried to join with wrong password!");
                    clientOut.writeObject("REJECTED_PASSWORD");
                    clientOut.flush();
                    socket.close();
                    return;
                }

                ClientHandler handler = new ClientHandler(socket, clientIn, clientOut);
                connectedClients.add(handler);

                clientOut.writeObject("JOIN_SUCCESS");
                Log.Info("Client joined successfully! Total clients: " + connectedClients.size());

                if (listener != null) {
                    listener.onObjectReceived(joinRequest);
                }

                new Thread(handler::listen).start();
            }
            else {
                socket.close();
            }

        } 
        catch (IOException | ClassNotFoundException e) {
            Log.Error("Error during client connection setup: " + e.getMessage());
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // #endregion

    // ==========================================
    // #region CLIENT MODE
    // ==========================================

    public void startAsClient(String ip, int port, NetworkListener listener) throws IOException {
        this.listener = listener;

        Log.Info("Connecting to host at " + ip + ":" + port + "...");
        clientSocket = new Socket(ip, port);

        out = new ObjectOutputStream(clientSocket.getOutputStream());
        out.flush();

        in = new ObjectInputStream(clientSocket.getInputStream());

        Log.Info("Connected to socket, starting incoming listener...");
        this.startListeningClient();
    }

    private void startListeningClient() {
        new Thread(() -> {
            try {
                Object obj;
                while ((obj = in.readObject()) != null) {
                    if (listener != null) {
                        listener.onObjectReceived(obj);
                    }
                }
            }
            catch (IOException | ClassNotFoundException e) {
                Log.Error("Client connection lost: " + e.getMessage());
            }
            finally {
                this.closeConnection();
            }
        }).start();
    }

    // #endregion

    // ==========================================
    // #region SEND & BROADCAST
    // ==========================================

    public synchronized void sendObject(Object obj) {
        if (out == null) {
            if (!connectedClients.isEmpty()) {
                this.broadcastObject(obj);
                return;
            }

            if (out != null) {
                try {
                    out.writeObject(obj);
                    out.flush();
                    out.reset();
                } 
                catch (IOException e) {
                    Log.Error("An error occurred while sending an object: " + e.getMessage());
                }
                return;
            }

            Log.Error("Cannot send object: No active connection or connected clients.");
        }

        try {
            out.writeObject(obj);
            out.flush();
            out.reset();
        } 
        catch (IOException e) {
            Log.Error("An error occurred while sending an object: " + e.getMessage());
        }
    }

    public void broadcastObject(Object obj) {
        for (ClientHandler client : connectedClients) {
            client.sendObject(obj);
        }
    }

    // #endregion

    // ==========================================
    // #region CLEANUP & CLOSING
    // ==========================================

    public void closeConnection() {
        try {
            for (ClientHandler client : connectedClients) {
                client.close();
            }
            connectedClients.clear();

            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();

            Log.Info("All connections closed.");
        } 
        catch (IOException e) {
            Log.Error("An error occurred while closing connections: " + e.getMessage());
        }
    }

    public void setListener(NetworkListener listener) {
        this.listener = listener;
    }

    private static String loadVersion() {
        Properties properties = new Properties();
        try (InputStream input = UpdateService.class.getClassLoader().getResourceAsStream("project.properties")) {
            if (input == null) return "unknown";
            properties.load(input);
            return properties.getProperty("version", "unknown");
        } 
        catch (IOException e) {
            return "unknown";
        }
    }

    // #endregion

    // ==========================================
    // #region INNER CLASS: CLIENT HANDLER (HOST SIDE)
    // ==========================================

    /**
     * Handles the permanent connection to a single connected player from host side.
     */
    private class ClientHandler {
        private final Socket socket;
        private final ObjectInputStream in;
        private final ObjectOutputStream out;

        public ClientHandler(Socket socket, ObjectInputStream in, ObjectOutputStream out) {
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        public void listen() {
            try {
                Object obj;
                while ((obj = in.readObject()) != null) {
                    if (listener != null) {
                        listener.onObjectReceived(obj);
                    }
                }
            } 
            catch (IOException | ClassNotFoundException e) {
                Log.Info("Client disconnected: " + socket.getInetAddress().getHostAddress());
            } 
            finally {
                close();
                connectedClients.remove(this);
            }
        }

        public synchronized void sendObject(Object obj) {
            if (out != null) {
                try {
                    out.writeObject(obj);
                    out.flush();
                    out.reset();
                } 
                catch (IOException e) {
                    Log.Error("An error occurred while sending an object: " + e.getMessage());
                }
                return;
            }
        }

        public void close() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } 
            catch (IOException ignored) {}
        }
    }
}
