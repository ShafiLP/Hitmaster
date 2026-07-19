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
import java.util.Properties;

import hitmaster.models.MultiplayerLobby;

public class NetworkManager {

    private ServerSocket serverSocket;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private NetworkListener listener;

    private DatagramSocket udpSocket;
    private boolean isBroadcasting = false;
    private boolean isDiscovering = false;
    private static final int DISCOVERY_PORT = 5051;

    public interface NetworkListener {
        void onObjectReceived(Object obj);
    }

    public void startLobbyBroadcast(String lobbyName, int tcpPort) {
        this.isBroadcasting = true;

        String appVersion = NetworkManager.loadVersion();

        new Thread(() -> {
            try {
                udpSocket = new DatagramSocket();
                udpSocket.setBroadcast(true);

                String message = "LOBBY:" + lobbyName + ":" + tcpPort + ":" + appVersion;
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

    public void stopLobbyBroadcast() {
        this.isBroadcasting = false;

        if (udpSocket != null && !udpSocket.isClosed())
            udpSocket.close();
    }

    public interface DiscoveryListener {
        void onLobbyFound(MultiplayerLobby lobby);
    }

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
                            int tcpPort = Integer.parseInt(parts[2]);
                            String appVersion = parts[3];
                            String hostIp = packet.getAddress().getHostAddress();

                            discoveryListener.onLobbyFound(new MultiplayerLobby(lobbyName, hostIp, appVersion, 1, 2));
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

    public void stopLobbyDiscovery() {
        this.isDiscovering = false;
    }

    public void startAsHost(int port, NetworkListener listener) {
        this.listener = listener;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                Log.Info("Waiting for players...");

                socket = serverSocket.accept();
                Log.Info("Client connected!");

                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();

                in = new ObjectInputStream(socket.getInputStream());

                this.startListening();
            }
            catch (IOException e) {
                Log.Error("Error while creating host connection: " + e.getMessage());
            }
        }).start();
    }

    public void startAsClient(String ip, int port, NetworkListener listener) throws IOException {
        this.listener = listener;

        Log.Info("Connecting to host...");
        socket = new Socket(ip, port);

        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();

        in = new ObjectInputStream(socket.getInputStream());
        Log.Info("Connected to host!");

        this.startListening();
    }

    private void startListening() {
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
                Log.Error("Connection cancelled: " + e.getMessage());
            }
            finally {
                this.closeConnection();
            }
        }).start();
    }

    public synchronized void sendObject(Object obj) {
        if (out == null) {
            Log.Error("Cannot send object, OutputStream is null.");
            return;
        }
        
        try {
            out.writeObject(obj);
            out.flush();
            out.reset(); 
        }
        catch (IOException e) {
            Log.Error("An error occured while sending an object: " + e.getMessage());
        }

    }

    public void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
            Log.Info("Closed connection.");
        }
        catch (IOException e) {
            Log.Error("An error occured while trying to close the connection: " + e.getMessage());
        }
    }

    public void setListener(NetworkListener listener) {
        this.listener = listener;
    }

    /**
     * Loads current version of app from project.proerties file.
     * @return Current app version as String.
     */
    private static String loadVersion() {
        Properties properties = new Properties();

        try (InputStream input = UpdateService.class.getClassLoader().getResourceAsStream("project.properties")) {
            if (input == null)
                return "unknown";

            properties.load(input);
            return properties.getProperty("version", "unknown");
        }
        catch (IOException e) {
            return "unknown";
        }
    }
}
