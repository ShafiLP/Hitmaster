package hitmaster.services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import hitmaster.models.GameSet;
import hitmaster.models.Song;
import hitmaster.models.User;

public class Database {

    public static final String URL = "jdbc:sqlite:hitmaster.db";

    private static final Gson gson = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<String>>(){}.getType();

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static boolean initializeDatabase() {
        // 1) Delete existing database to initialize new one
        deleteDatabase();

        // 2) Initialize new database
        try (Connection conn = Database.connect()) {
            Statement stmt = conn.createStatement();

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    picture TEXT,
                    provider TEXT
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    img TEXT,
                    icon TEXT,
                    csv TEXT,
                    is_active INTEGER DEFAULT 1
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS songs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    titles TEXT NOT NULL,
                    artists TEXT NOT NULL,
                    year INTEGER,
                    spotify TEXT
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS set_songs (
                    set_id INTEGER,
                    song_id INTEGER,
                    PRIMARY KEY (set_id, song_id),
                    FOREIGN KEY (set_id) REFERENCES sets(id) ON DELETE CASCADE,
                    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
                );
            """);

            //! DEBUG
            Database.insertJsonIntoSongs("songs.json");

            Database.addSetToDatabase(new GameSet("Hitster - UK", "hitster-uk.jpg", "hitster-uk.png", "hitster-uk.csv", true));
            addSongsToSetFromCsv("hitster-uk.csv", 1);

            Database.addSetToDatabase(new GameSet("Hitster - DE", "hitster-de.jpg", "hitster-de.png", "hitster-de.csv", false));
            addSongsToSetFromCsv("hitster-de.csv", 2);

            Database.addSetToDatabase(new GameSet("Rock & Metal - DE", "rock-de.jpg", "rock-de.png", "rock-de.csv", false));
            addSongsToSetFromCsv("rock-de.csv", 3);

            Database.addSetToDatabase(new GameSet("Guilty Pleasures - DE", "guilty-de.png", "guilty-de.png", "guilty-de.csv", false));
            addSongsToSetFromCsv("guilty-de.csv", 4);

            Database.addSetToDatabase(new GameSet("Bayern1 Expansion", "bavaria-ex.png", "bavaria-ex.png", "bavaria-ex.csv", false));
            addSongsToSetFromCsv("bavaria-ex.csv", 5);

            Database.addSetToDatabase(new GameSet("Rock & Metal - Nordics", "rock-nordics.jpg", "rock-nd.png", "rock-nordics.csv", false));
            addSongsToSetFromCsv("rock-nordics.csv", 6);

            Database.addSetToDatabase(new GameSet("Punk Expansion", "punk-ex.png", "punk-ex.png", "punk-expansion.csv", false));
            addSongsToSetFromCsv("punk-expansion.csv", 7);

            Database.addSetToDatabase(new GameSet("Deutschrock Expansion", "deutschrock-ex.png", "deutschrock-ex.png", "deutschrock-ex.csv", false));
            addSongsToSetFromCsv("deutschrock-ex.csv", 8);

            return true;
        }
        catch (SQLException e) {
            Log.Error("Error while initialising database: " + e.getMessage());
            return false;
        }
    }

    private static boolean deleteDatabase() {
        try (Connection conn = Database.connect()) {
            Statement stmt = conn.createStatement();

            stmt.execute("DROP TABLE IF EXISTS sets");
            stmt.execute("DROP TABLE IF EXISTS songs");
            stmt.execute("DROP TABLE IF EXISTS set_songs");

            Log.Success("Deleted all data from tables.");
            return true;
        }
        catch (SQLException e) {
            Log.Error("Error while deleting data from database: " + e.getMessage());
            return false;
        }
    }

    public static boolean addSongsToSetFromCsv(String csvFileName, int setId) {
        try {
            InputStream is = Database.class.getResourceAsStream("/csv/" + csvFileName);
            if (is == null) {
                Log.Error("CSV file not found: /csv/" + csvFileName);
                return false;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            List<String> lines = reader.lines()
                .filter(line -> line != null && !line.trim().isEmpty())
                .toList();

            if (lines.isEmpty()) {
                Log.Error("CSV file is empty: " + csvFileName);
                return false;
            }

            try (Connection conn = Database.connect();
                PreparedStatement ps = conn.prepareStatement("""
                    INSERT OR IGNORE INTO set_songs (set_id, song_id)
                    VALUES (?, ?)
                """)) {

                // i = 1 to skip head row
                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    try {
                        int songId = Integer.parseInt(line);
                        
                        ps.setInt(1, setId);
                        ps.setInt(2, songId);
                        ps.addBatch();
                    } catch (NumberFormatException e) {
                        Log.Error("Skipped invalid ID in CSV: " + line);
                    }
                }

                ps.executeBatch();
                Log.Success("Succesfully added " + (lines.size() - 1) + " songs to set with ID " + setId + ".");
                return true;
            }
        }
        catch (SQLException e) {
            Log.Error("Error while reading set CSV: " + e.getMessage());
            return false;
        }
    }

    public static boolean insertJsonIntoSongs(String jsonFileName) {
        try {
            // 1) JSON aus dem Ressourcen-Pfad laden
            InputStream is = Database.class.getResourceAsStream("/json/" + jsonFileName);
            if (is == null) {
                Log.Error("JSON file not found: /json/" + jsonFileName);
                return false;
            }

            // 2) Gson liest direkt aus dem Reader in ein JsonArray
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            JsonArray songsArray = JsonParser.parseReader(reader).getAsJsonArray();

            // 3) In die Datenbank schreiben
            try (Connection conn = Database.connect();
                PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO songs (titles, artists, year, spotify)
                    VALUES (?, ?, ?, ?)
                """)) {

                for (JsonElement element : songsArray) {
                    JsonObject songObj = element.getAsJsonObject();

                    // Die inneren Arrays konvertieren wir wieder zu Strings für die DB-Spalten
                    ps.setString(1, songObj.getAsJsonArray("titles").toString());
                    ps.setString(2, songObj.getAsJsonArray("artists").toString());
                    ps.setInt(3, songObj.get("year").getAsInt());
                    ps.setString(4, songObj.get("spotify_id").getAsString());

                    ps.addBatch();
                }

                ps.executeBatch();
                Log.Success("Successfully inserted " + songsArray.size() + " songs from \"" + jsonFileName + "\" into 'songs'.");
                return true;
            }
        }
        catch (JsonIOException | JsonSyntaxException | UnsupportedEncodingException | SQLException e) {
            Log.Error("Error while inserting data from \"" + jsonFileName + "\" into table \"songs\": " + e.getMessage());
            return false;
        }
    }

    private static boolean createDefaultUser() {
        try (Connection conn = Database.connect();) {
            Statement stmt = conn.createStatement();

            stmt.execute("INSERT INTO user (username) VALUES ('New User')");
            return true;
        }
        catch (Exception e) {
            Log.Error("Error while creating default user: " + e.getMessage());
            return false;
        }
    }


    // ==============================
    // GET OPERATIONS
    // ==============================

    public static User getCurrentUser() {
        try (Connection conn = Database.connect()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("""
                SELECT *
                FROM user
                WHERE id = 1
            """);

            if (rs.next()) {
                return mapUser(rs);
            }

            // Enters this section if no user exists
            Database.createDefaultUser();
            
            rs = stmt.executeQuery("""
                SELECT *
                FROM user
                WHERE id = 1
            """);

            if (rs.next()) {
                return mapUser(rs);
            }

            throw new Error("No user found in database. Couldn't create new default user.");
        }
        catch (SQLException e) {
            Log.Error("Error while fetching user from database: " + e.getMessage());
            return new User();
        }
    }

    public static List<GameSet> getAllSets() {
        try (Connection conn = Database.connect()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("""
                SELECT *
                FROM sets
            """);

            List<GameSet> sets = new ArrayList<>();
            while (rs.next()) {
                sets.add(mapSet(rs));
            }

            return sets;
        }
        catch (SQLException e) {
            Log.Error("Error while fetching sets from database: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static List<Song> getAllSongs() {
        try (Connection conn = Database.connect()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("""
                SELECT *
                FROM songs
            """);

            List<Song> songs = new ArrayList<>();
            while (rs.next()) {
                songs.add(mapSong(rs));
            }

            return songs;
        }
        catch (SQLException e) {
            Log.Error("Error while fetching songs from database: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static List<Song> getSongFromActiveSets() {
        try (Connection conn = Database.connect()) {
            Statement stmt = conn.createStatement();

            String sql = """
                SELECT DISTINCT s.* FROM songs s
                INNER JOIN set_songs ss ON s.id = ss.song_id
                INNER JOIN sets o ON ss.set_id = o.id
                WHERE o.is_active = 1
            """;
            ResultSet rs = stmt.executeQuery(sql);

            List<Song> songs = new ArrayList<>();
            while (rs.next()) {
                songs.add(mapSong(rs));
            }

            return songs;
        }
        catch (SQLException e) {
            Log.Error("Error while fetching songs from database: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static Song getSongById(int songId) {
        try (Connection conn = Database.connect()) {
            String sqlExecute = """
                SELECT *
                FROM songs
                WHERE song_id = ?
            """;
            PreparedStatement ps = conn.prepareStatement(sqlExecute);
            ps.setInt(1, songId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapSong(rs);
            }

            Log.Error("Error while fetching songs from database: Couldn't find match.");
            return new Song();
        }
        catch (SQLException e) {
            Log.Error("Error while fetching songs from database: " + e.getMessage());
            return new Song();
        }
    }

    public static List<Song> getSongsBySetId(int setId) {
        try (Connection conn = Database.connect()) {
            String sqlExecute = """
                SELECT s.* FROM songs s
                INNER JOIN set_songs ss ON s.id = ss.song_id
                WHERE ss.set_id = ?
            """;
            PreparedStatement ps = conn.prepareStatement(sqlExecute);
            ps.setInt(1, setId);

            ResultSet rs = ps.executeQuery();

            List<Song> songs = new ArrayList<>();
            while(rs.next()) {
                songs.add(mapSong(rs));
            }

            return songs;
        }
        catch (SQLException e) {
            Log.Error("Error while fetching songs from database: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static List<GameSet> getSetsBySongId(int songId) {
        try (Connection conn = Database.connect()) {
            String sql = """
                SELECT s.*
                FROM sets s
                INNER JOIN set_songs ss ON s.id = ss.set_id
                WHERE ss.song_id = ?
            """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, songId);

            ResultSet rs = ps.executeQuery();

            List<GameSet> sets = new ArrayList<>();
            while (rs.next()) {
                sets.add(mapSet(rs));
            }

            return sets;
        }
        catch (SQLException e) {
            Log.Error("Error while fetching sets by song ID: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static List<GameSet> getActiveSetsBySongId(int songId) {
        try (Connection conn = Database.connect()) {
            String sql = """
                SELECT s.*
                FROM sets s
                INNER JOIN set_songs ss ON s.id = ss.set_id
                WHERE ss.song_id = ?
                AND s.is_active = 1
            """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, songId);

            ResultSet rs = ps.executeQuery();

            List<GameSet> sets = new ArrayList<>();
            while (rs.next()) {
                sets.add(mapSet(rs));
            }

            return sets;
        }
        catch (SQLException e) {
            Log.Error("Error while fetching active sets by song ID: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==============================
    // SET OPERATIONS
    // ==============================

    public static boolean updateUser(User user) {
        try (Connection conn = Database.connect()) {

            PreparedStatement stmt = conn.prepareStatement("""
                UPDATE user
                SET username = ?, picture = ?, provider = ?
                WHERE id = ?
            """);

            stmt.setString(1, user.username);
            stmt.setString(2, user.picture);
            stmt.setString(3, user.provider);
            stmt.setInt(4, user.id);

            stmt.executeUpdate();

            Log.Success("User updated successfully.");
            return true;
        }
        catch (Exception e) {
            Log.Error("Error while updating user: " + e.getMessage());
            return false;
        }
    }

    public static boolean addSetToDatabase(GameSet set) {
        try (Connection conn = Database.connect()) {

            PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO sets (name, img, icon, csv, is_active)
                VALUES (?, ?, ?, ?, ?)
            """);

            stmt.setString(1, set.name);
            stmt.setString(2, set.img);
            stmt.setString(3, set.icon);
            stmt.setString(4, set.csv);
            stmt.setInt(5, set.isActive ? 1 : 0);

            stmt.executeUpdate();

            Log.Success("Set insertion of \"" + set.name + "\" successful.");
            return true;
        }
        catch (Exception e) {
            Log.Error("Error while inserting set \"" + set.name + "\": " + e.getMessage());
            return false;
        }
    }

    public static boolean updateSetStatus(int setId, boolean isActive) {
        try (Connection conn = Database.connect()) {

            PreparedStatement stmt = conn.prepareStatement("""
                UPDATE sets
                SET is_active = ?
                WHERE id = ?
            """);

            stmt.setInt(1, isActive ? 1 : 0);
            stmt.setInt(2, setId);

            stmt.executeUpdate();

            Log.Success("Set updated successfully.");
            return true;
        }
        catch (Exception e) {
            Log.Error("Error while updating set: " + e.getMessage());
            return false;
        }
    }

    // ==============================
    // MAPPERS
    // ==============================

    private static User mapUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.id = rs.getInt("id");
        user.username = rs.getString("username");
        user.picture = rs.getString("picture");
        user.provider = rs.getString("provider");

        return user;
    }

    private static GameSet mapSet(ResultSet rs) throws SQLException {
        GameSet set = new GameSet();

        set.id = rs.getInt("id");
        set.name= rs.getString("name");
        set.img = rs.getString("img");
        set.icon = rs.getString("icon");
        set.csv = rs.getString("csv"); 
        set.isActive = rs.getInt("is_active") == 1;

        return set;
    }

    private static Song mapSong(ResultSet rs) throws SQLException {
        Song song = new Song();

        song.id = rs.getInt("id");
        song.year = rs.getInt("year");
        song.spotify = rs.getString("spotify");

        // Gson wandelt den JSON-String direkt in eine List<String> um
        song.titles = gson.fromJson(rs.getString("titles"), LIST_TYPE);
        song.artists = gson.fromJson(rs.getString("artists"), LIST_TYPE);

        return song;
    }
}
