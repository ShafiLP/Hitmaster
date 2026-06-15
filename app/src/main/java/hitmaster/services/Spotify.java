package hitmaster.services;

import java.awt.Desktop;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.apache.hc.core5.http.ParseException;

import com.google.gson.JsonArray;
import com.sun.net.httpserver.HttpServer;

import hitmaster.models.User;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.scene.media.Track;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import se.michaelthelin.spotify.model_objects.miscellaneous.Device;

public class Spotify {

    public static String CLIENT_ID;
    public static String CLIENT_SECRET;;

    /**
     * Creates a new connection to user's spotify account.
     * User gets redirected to authentification screen.
     * After confirming user properties get saved to "spotify.properties".
     * @return Success result.
     */
    public static boolean createSpotifyConnection() {
        loadEnvValues();
        try {
            // 1) Spotify Login
            SpotifyApi spotify = new SpotifyApi.Builder()
                    .setClientId(CLIENT_ID)
                    .setClientSecret(CLIENT_SECRET)
                    .setRedirectUri(URI.create("http://127.0.0.1:8888/callback"))
                    .build();

            URI uri = spotify.authorizationCodeUri()
                .scope("user-modify-playback-state user-read-playback-state")
                .state(UUID.randomUUID().toString())
                .show_dialog(true)
                .build()
                .execute();

            // 2) Replace code with token
            HttpServer server = HttpServer.create(new InetSocketAddress(8888), 0);
            CountDownLatch latch = new CountDownLatch(1);
            String[] code = new String[1];

            server.createContext("/callback", exchange -> {
                try {
                    String query = exchange.getRequestURI().getQuery();

                    if (query != null) {
                        for (String param : query.split("&")) {
                            String[] pair = param.split("=");
                            if (pair.length == 2 && pair[0].equals("code")) {
                                code[0] = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                            }
                        }
                    }
                    
                    String response = "Login erfolreich, du kannst dieses Fenster schließen.";
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }

                    exchange.close();
                }
                finally {
                    latch.countDown();
                }
            });

            server.start();
            Desktop.getDesktop().browse(uri);
            latch.await();
            server.stop(0);

            AuthorizationCodeCredentials credentials = null;
            try {
                System.out.println("Exchanging code: " + code[0]);

                credentials =
                    spotify.authorizationCode(code[0])
                        .build()
                        .execute();

                Log.Success("Token OK.");

            } catch (IOException | ParseException | SpotifyWebApiException e) {
                throw new Error("Coudldn't fetch valid token: " + e.getMessage());
            }

            String accessToken = credentials.getAccessToken();
            String refreshToken = credentials.getRefreshToken();

            spotify.setAccessToken(accessToken);
            spotify.setRefreshToken(refreshToken);

            // 3) Save properties
            Properties props = new Properties();
            props.setProperty("accessToken", accessToken);
            props.setProperty("refreshToken", refreshToken);

            try (FileOutputStream out = new FileOutputStream("spotify.properties")) {
                props.store(out, "Spotify Tokens");
            }

            User user = Database.getCurrentUser();
            user.provider = "spotify";
            Database.updateUser(user);
            
            Log.Success("Connection to Spotify was successful.\nTokens saved in \"spotify.properties\".");
            return true;
        }
        catch (IOException | InterruptedException e) {
            Log.Error("Connection to Spotify failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads spotify properties from "spotify.properties" and builds a connection to user's linked spotify account.
     * @return SpotifyApi object with linked user account.
     */
    public static SpotifyApi requestSpotifyConnection() {
        loadEnvValues();
        try {
            // 1) Load properties from file
            Properties props = new Properties();

            try (FileInputStream in = new FileInputStream("spotify.properties")) {
                props.load(in);
            }

            // 2) Build connection to Spotify
            SpotifyApi spotify = new SpotifyApi.Builder()
                .setClientId(CLIENT_ID)
                .setClientSecret(CLIENT_SECRET)
                .setRedirectUri(URI.create("http://127.0.0.1:8888/callback"))
                .build();

            spotify.setAccessToken(props.getProperty("accessToken"));
            spotify.setRefreshToken(props.getProperty("refreshToken"));

            Log.Success("Connection to Spotify was successful.");
            return spotify;
        }
        catch (IOException e) {
            Log.Error("Connection to Spotify failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Fetches the current playing device from Spotify.
     * @param connection SpotifyApi connection.
     * @return Name of the current playing device.
     */
    public static boolean checkConnectionStatus(SpotifyApi connection) {
        try {
            CurrentlyPlayingContext context = connection
                .getInformationAboutUsersCurrentPlayback()
                .build()
                .execute();

            Device currentDevice = context != null ? context.getDevice() : null;

            return currentDevice != null;
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            Log.Error("Error occured while checking connection status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Connects to linked Spotify account from "spotify.properties" and plays a song on user's spotify device.
     * @param TrackId Spotify song ID.
     * @return Success result.
     */
    public static boolean playSpotifyLink(SpotifyApi connection, String TrackId) {
        try {
            JsonArray uris = new JsonArray();
            uris.add("spotify:track:" + TrackId);

            connection.startResumeUsersPlayback()
                .uris(uris)
                .build()
                .execute();

            CurrentlyPlayingContext playback = connection
                .getInformationAboutUsersCurrentPlayback()
                .build()
                .execute();

            if (playback != null && playback.getItem() instanceof Track)
                Log.Info("Now playing song \"" + playback.getItem().getName() + "\" on Spotify.");

            return true;
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            Log.Error("Playing Spotify link failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean restartSong(SpotifyApi connection, String Link) {
        try {
            connection.seekToPositionInCurrentlyPlayingTrack(0)
                .build()
                .execute();

            Log.Info("Restarted current song.");
            return true;
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            Log.Error("Restarting current Spotify song failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Pause the song that is currently playing on the user's spotify account.
     * @param connection SpotifyApi connection.
     * @return Success result.
     */
    public static boolean pauseCurrentSong(SpotifyApi connection) {
        try {
            connection.pauseUsersPlayback()
                .build()
                .execute();

            if (!isSongPlaying(connection))
                return true;

            throw new Exception("Song is still playing");
        }
        catch (Exception e) {
            Log.Error("Couldn't pause song: " + e.getMessage());
            return false;
        }
    }

    public static boolean rewindCurrentSong5sec(SpotifyApi connection) {
        try {
            CurrentlyPlayingContext context = connection
                .getInformationAboutUsersCurrentPlayback()
                .build()
                .execute();

            if (context == null || context.getProgress_ms() == null)
                throw new Exception("No song is currently playing.");

            int newPosition = Math.max(0, context.getProgress_ms() - 5000);

            connection.seekToPositionInCurrentlyPlayingTrack(newPosition)
                .build()
                .execute();

            Log.Info("Rewinded 5sec of currently playing song.");
            return true;
        }
        catch (Exception e) {
            Log.Error("Couldn't rewind song: " + e.getMessage());
            return false;
        }
    }

    public static boolean forwardCurrentSong5sec(SpotifyApi connection) {
        try {
            CurrentlyPlayingContext context = connection
                .getInformationAboutUsersCurrentPlayback()
                .build()
                .execute();

            if (context == null || context.getProgress_ms() == null)
                throw new Exception("No song is currently playing.");

            
            int newPosition = context.getProgress_ms() + 5000;

            connection.seekToPositionInCurrentlyPlayingTrack(newPosition)
                .build()
                .execute();

            Log.Info("Forwarded 5sec of currently playing song.");
            return true;
        }
        catch (Exception e) {
            Log.Error("Couldn't forward song: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fetches the current available devices from Spotify.
     * @param connection SpotifyApi connection.
     * @return Name of all availably devices.
     */
    public static String[] getAllDevices(SpotifyApi connection) {
        try {
            Device[] devices = connection
                .getUsersAvailableDevices()
                .build()
                .execute();

            if (devices != null) {
                String[] deviceNames = new String[devices.length];
                for (int i = 0; i < devices.length; i++) {
                    deviceNames[i] = devices[i].getName();
                }
                return deviceNames;
            }

            throw new Error("No devices found.");
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            Log.Error("Error occured while fetching devices: " + e.getMessage());
            return null;
        }
    }

    /**
     * Fetches the current playing device from Spotify.
     * @param connection SpotifyApi connection.
     * @return Name of the current playing device.
     */
    public static String getCurrentDevice(SpotifyApi connection) {
        try {
            CurrentlyPlayingContext context = connection
                .getInformationAboutUsersCurrentPlayback()
                .build()
                .execute();

            Device currentDevice = context != null ? context.getDevice() : null;

            if (currentDevice != null)
                return currentDevice.getName();
            
            throw new Error("No device found (currentDevice is null).");
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            Log.Error("Error occured while fetching devices: " + e.getMessage());
            return null;
        }
    }

    /**
     * Search for a device by name and continues playback on that device.
     * @param connection SpotifyApi connection.
     * @param deviceName Name of the device to continue playback on.
     * @return Success result.
     */
    public static boolean setCurrentDevice(SpotifyApi connection, String deviceName) {
        try {
            Device[] devices = connection
                .getUsersAvailableDevices()
                .build()
                .execute();

            String deviceId = null;

            for (Device d: devices) {
                if (d.getName().equalsIgnoreCase(deviceName)) {
                    deviceId = d.getId();
                    break;
                }
            }

            if (deviceId != null) {
                connection.startResumeUsersPlayback()
                    .device_id(deviceId)
                    .build()
                    .execute();

                return true;
            }

            throw new Error("No matching devide found for deviceName \"" + deviceName + "\".");
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            Log.Error("Error occured while setting current player device: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the current volume for the opened Spotify player.
     * @param connection SpotifyApi connection.
     * @return Current volume in percent.
     */
    public static int getVolume(SpotifyApi connection) {
        try {
            CurrentlyPlayingContext context = connection
            .getInformationAboutUsersCurrentPlayback()
            .build()
            .execute();

            if (context != null) {
                Device device = context.getDevice();

                if (device != null)
                    return device.getVolume_percent();
            }

            throw new Error("Context or Device is null.");
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            Log.Error("Error occured while changing volume of current user playback: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Sets the current volume for the opened Spotify player.
     * @param connection SpotifyApi connection.
     * @param volume New volume in percent.
     * @return Success result.
     */
    public static boolean setVolume(SpotifyApi connection, int volume) {
        try {
            connection.setVolumeForUsersPlayback(volume)
                .build()
                .execute();

            return true;
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            Log.Error("Error occured while changing volume of current user playback: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cheks if user's connected Spotify account is currently playing a song.
     * @param connection SpotifyApi connection.
     * @return Playback state (playing = true).
     */
    public static boolean isSongPlaying(SpotifyApi connection) {
        try {
            CurrentlyPlayingContext context = connection
                .getInformationAboutUsersCurrentPlayback()
                .build()
                .execute();

            if (context != null && context.getIs_playing() != null)
                return context.getIs_playing();

            throw new Error("context or context.getIs_playing() is null.");
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            Log.Error("Error occured while checking current playback state: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads CLIENT_ID and CLIENT_SECRET from .env file
     */
    private static void loadEnvValues() {
        Dotenv dotenv = Dotenv.load();
        CLIENT_ID = dotenv.get("CLIENT_ID");
        CLIENT_SECRET = dotenv.get("CLIENT_SECRET");
    }
}
