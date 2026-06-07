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
import se.michaelthelin.spotify.requests.data.player.GetUsersAvailableDevicesRequest;

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
                    System.out.println("REQUEST: " + exchange.getRequestURI());
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
     * Connects to linked Spotify account from "spotify.properties" and plays a song on user's spotify device.
     * @param Link Link to spotify song (unformatted URI).
     * @return Success result.
     */
    public static boolean playSpotifyLink(SpotifyApi connection, String Link) {
        try {
            // 1) Find user devices
            // TODO: User sets speaker device themself
            GetUsersAvailableDevicesRequest request = connection.getUsersAvailableDevices().build();
            Device[] devices = request.execute();

            // 2) Play song
            JsonArray uris = new JsonArray();
            uris.add(toSpotifyUri(Link));

            connection.startResumeUsersPlayback()
                .uris(uris)
                .device_id(devices[0].getId())
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
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean pauseCurrentSong(SpotifyApi connection) {
        try {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean rewindCurrentSong5sec(SpotifyApi connection) {
        try {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean forwardCurrentSong5sec(SpotifyApi connection) {
        try {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Formats a spotify link to a spotify URI.
     * @param url Unformatted spotify link.
     * @return Formatted spotify URI "spotify:track:{trackId}".
     */
    private static String toSpotifyUri(String url) {
        // 1) Remove parameters
        String clean = url.split("\\?")[0];

        // 2) Extract track ID 
        String trackId = clean.substring(clean.lastIndexOf("/") + 1);

        // 3) Build URI
        return "spotify:track:" + trackId;
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
