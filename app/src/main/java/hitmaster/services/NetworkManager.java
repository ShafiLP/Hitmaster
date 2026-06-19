package hitmaster.services;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkManager {
    private ServerSocket serverSocket;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isHost;
    private NetworkListener listener;

    public interface NetworkListener {
        void onObjectReceived(Object obj);
    }

    public void startAsHost(int port, NetworkListener listener) {
        this.isHost = true;
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
        this.isHost = false;
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
}
