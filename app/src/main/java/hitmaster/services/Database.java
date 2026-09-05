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

    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<String>>(){}.getType();

    private static Database instance;

    private Database() {}

    public static synchronized Database getInstance() {
        if (instance == null)
            instance = new Database();

        return instance;
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public boolean initializeDatabase() {
        // 1) Initialize new database
        try (Connection conn = Database.connect()) {

            Statement stmt = conn.createStatement();

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    picture TEXT,
                    provider TEXT,
                    theme TEXT DEFAULT light,
                    autoCheckUpdate INTEGER DEFAULT 1
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    region TEXT,
                    desc TEXT,
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

            // 2) Insert Songs into Database
            this.insertJsonIntoSongs("songs.json");

            // 3) Insert GameSets into Database
            this.addSetToDatabase(new GameSet(1,"Hitster - UK", "EN", 
                "The ultimate party game: Turn any gathering into an unforgettable experience with Hitster Original - a thrilling party game for adults that combines music and fun!",
                "hitster-uk_standard.png", "hitster-uk.png", "hitster-uk.csv", true));
            this.addSongsToSetFromCsv("hitster-uk.csv", 1);

            this.addSetToDatabase(new GameSet(2, "Hitster - DE", "DE",
                "Feier die ultimative Party mit Hitster! Mit über 300 der größten Hits der letzten 100 Jahre ist Hitster das perfekte Partyspiel für einen Abend voller Lachen, Singen, Tanzen und gemeinsamen Erinnerungen.",
                "hitster-de_standard.png", "hitster-de.png", "hitster-de.csv", false));
            this.addSongsToSetFromCsv("hitster-de.csv", 2);

            this.addSetToDatabase(new GameSet(3, "Rock - DE", "DE",
                "Tauche ein in die elektrisierende Welt der Rockmusik mit HITSTER Rock Radio BOB! Diese Ausgabe bietet eine speziell zusammengestellte Sammlung legendärer Rocksongs aus verschiedenen Epochen.",
                "hitster-de_rock.png", "rock-de.png", "rock-de.csv", false));
            this.addSongsToSetFromCsv("rock-de.csv", 3);

            this.addSetToDatabase(new GameSet(4, "Guilty Pleasures - DE", "DE",
                "Die Party geht weiter mit Hitster Guilty Pleasures! Das lustigste Partyspiel kommt mit einer neuen Ausgabe, diesmal mit den Hits die angeblich keiner kennt und trotzdem jeder mitsingen kann.",
                "hitster-de_guilty.png", "guilty-de.png", "guilty-de.csv", false));
            this.addSongsToSetFromCsv("guilty-de.csv", 4);

            this.addSetToDatabase(new GameSet(5, "Summer Party - DE", "DE",
                "Alle Sommerhits der vergangenen Jahrzehnte kommen in dieser Variante von Hitster zusammen! Feiert eure ultimative Sommerparty mit internationalen und deutschen Hits, die jeder kennt und begebt euch auf die Zeitreise.",
                "hitster-de_summer.png", "summer-de.png", "summer-de.csv", false));
            this.addSongsToSetFromCsv("summer-de.csv", 5);

            this.addSetToDatabase(new GameSet(6, "Schlager Party - DE", "DE",
                "Hitster Schlagerparty ist das ultimative Partyspiel für alle, die bereit sind, sich durch die Nächte zuschlagen. Diese Variante des beliebten Partyspiels enthält die größten Schlagerhits der vergangenen Jahrzehnte.",
                "hitster-de_schlager.png", "schlager-de.png", "schlager-de.csv", false));
            this.addSongsToSetFromCsv("schlager-de.csv", 6);

            this.addSetToDatabase(new GameSet(7, "Bayern1 Expansion", "DE",
                "Bayern 1 Hitliste: Tauche ein in eine Sammlung von über 150 Karten mit den größten Radiohits der vergangenen Jahrzehnte.",
                "hitster-de_bavaria.png", "bavaria-ex.png", "bavaria-ex.csv", false));
            this.addSongsToSetFromCsv("bavaria-ex.csv", 7);

            this.addSetToDatabase(new GameSet(8, "Rock & Metal - Nordics", "INT", null, "rock-nordics.jpg", "rock-nd.png", "rock-nordics.csv", false));
            this.addSongsToSetFromCsv("rock-nordics.csv", 8);

            this.addSetToDatabase(new GameSet(9, "Punk Expansion", "INT", null, "punk-ex.png", "punk-ex.png", "punk-expansion.csv", false));
            this.addSongsToSetFromCsv("punk-expansion.csv", 9);

            this.addSetToDatabase(new GameSet(10, "Deutschrock Expansion", "DE", null, "deutschrock-ex.png", "deutschrock-ex.png", "deutschrock-ex.csv", false));
            this.addSongsToSetFromCsv("deutschrock-ex.csv", 10);

            Log.Success("Succesfully initialized Database.");
            return true;
        }
        catch (SQLException e) {
            Log.Error("Error while initialising database: " + e.getMessage());
            return false;
        }
    }

    // ==============================
    // #region USER OPERATIONS
    // ==============================

    public User getCurrentUser() {
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
            this.createDefaultUser();
            
            rs = stmt.executeQuery("""
                SELECT *
                FROM user
                WHERE id = 1
            """);

            if (rs.next()) {
                return this.mapUser(rs);
            }

            throw new Error("No user found in database. Couldn't create new default user.");
        }
        catch (SQLException e) {
            Log.Error("Error while fetching user from database: " + e.getMessage());
            return new User();
        }
    }

    private boolean createDefaultUser() {
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

    public boolean updateUser(User user) {
        try (Connection conn = Database.connect()) {

            PreparedStatement stmt = conn.prepareStatement("""
                UPDATE user
                SET username = ?, picture = ?, provider = ?, theme = ?, autoCheckUpdate = ?
                WHERE id = ?
            """);

            stmt.setString(1, user.username);
            stmt.setString(2, user.picture);
            stmt.setString(3, user.provider);
            stmt.setString(4, user.theme.equals(ThemeManager.Theme.DARK) ? "dark" : "light");
            stmt.setBoolean(5, user.autoCheckUpdate);
            stmt.setInt(6, user.id);

            stmt.executeUpdate();

            Log.Success("User updated successfully.");
            return true;
        }
        catch (Exception e) {
            Log.Error("Error while updating user: " + e.getMessage());
            return false;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.id = rs.getInt("id");
        user.username = rs.getString("username");
        user.picture = rs.getString("picture");
        user.provider = rs.getString("provider");
        user.theme = rs.getString("theme").equals("dark") ? ThemeManager.Theme.DARK : ThemeManager.Theme.LIGHT;
        user.autoCheckUpdate = rs.getBoolean("autoCheckUpdate");

        return user;
    }

    // #endregion


    // ==============================
    // #region SONG OPERATIONS
    // ==============================

    public boolean insertJsonIntoSongs(String jsonFileName) {
        try {
            // 1) Load JSON from Ressource Path
            InputStream is = Database.class.getResourceAsStream("/json/" + jsonFileName);
            if (is == null) {
                Log.Error("JSON file not found: /json/" + jsonFileName);
                return false;
            }

            // 2) GSON converts JSON to JsonArray
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            JsonArray songsArray = JsonParser.parseReader(reader).getAsJsonArray();

            // 3) Check if number of songs in JSON is equal to Database
            int dbCount = this.getSongCount();
            if (dbCount == songsArray.size()) {
                Log.Info("Database already contains " + dbCount + " songs: Skipping update.");
                return true;
            }

            // 4) Write songs into Database
            try (Connection conn = Database.connect()) {

                conn.setAutoCommit(false);

                // Delete old Data
                try (Statement deleteStmt = conn.createStatement()) {
                    deleteStmt.executeUpdate("DELETE FROM songs");
                    deleteStmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name='songs'");
                    Log.Success("Succesfully deleted all songs from Database.");
                }

                PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO songs (titles, artists, year, spotify)
                    VALUES (?, ?, ?, ?)
                """);

                for (JsonElement element : songsArray) {
                    JsonObject songObj = element.getAsJsonObject();

                    ps.setString(1, songObj.getAsJsonArray("titles").toString());
                    ps.setString(2, songObj.getAsJsonArray("artists").toString());
                    ps.setInt(3, songObj.get("year").getAsInt());
                    ps.setString(4, songObj.get("spotify_id").getAsString());

                    ps.addBatch();
                }

                ps.executeBatch();
                conn.commit();

                Log.Success("Successfully inserted " + songsArray.size() + " songs from \"" + jsonFileName + "\" into 'songs'.");
                return true;
            }
        }
        catch (JsonIOException | JsonSyntaxException | UnsupportedEncodingException | SQLException e) {
            Log.Error("Error while inserting data from \"" + jsonFileName + "\" into table \"songs\": " + e.getMessage());
            return false;
        }
    }

    public int getSongCount() {
        String sql = "SELECT COUNT(*) FROM songs";

        try (Connection conn = Database.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        catch (SQLException e) {
            Log.Error("Error while counting songs in database: " + e.getMessage());
        }

        return -1;
    }

    public List<Song> getAllSongs() {
        try (Connection conn = Database.connect()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("""
                SELECT *
                FROM songs
            """);

            List<Song> songs = new ArrayList<>();
            while (rs.next()) {
                songs.add(this.mapSong(rs));
            }

            return songs;
        }
        catch (SQLException e) {
            Log.Error("Error while fetching songs from database: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Song> getSongFromActiveSets() {
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
                songs.add(this.mapSong(rs));
            }

            return songs;
        }
        catch (SQLException e) {
            Log.Error("Error while fetching songs from database: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Song getSongById(int songId) {
        try (Connection conn = Database.connect()) {

            String sqlExecute = """
                SELECT *
                FROM songs
                WHERE id = ?
            """;

            PreparedStatement ps = conn.prepareStatement(sqlExecute);
            ps.setInt(1, songId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return this.mapSong(rs);
            }

            Log.Error("Error while fetching songs from database: Couldn't find match.");
            return new Song();
        }
        catch (SQLException e) {
            Log.Error("Error while fetching songs from database: " + e.getMessage());
            return new Song();
        }
    }

    public List<Song> getSongsBySetId(int setId) {
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
                songs.add(this.mapSong(rs));
            }

            return songs;
        }
        catch (SQLException e) {
            Log.Error("Error while fetching songs from database: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private Song mapSong(ResultSet rs) throws SQLException {
        Song song = new Song();

        song.id = rs.getInt("id");
        song.year = rs.getInt("year");
        song.spotify = rs.getString("spotify");

        // Gson wandelt den JSON-String direkt in eine List<String> um
        song.titles = GSON.fromJson(rs.getString("titles"), LIST_TYPE);
        song.artists = GSON.fromJson(rs.getString("artists"), LIST_TYPE);

        return song;
    }

    // #endregion

    // ==============================
    // #region GAME SET OPERATIONS
    // ==============================

    public boolean addSongsToSetFromCsv(String csvFileName, int setId) {
        try {
            // 1) Locate CSV file
            InputStream is = Database.class.getResourceAsStream("/csv/" + csvFileName);
            if (is == null) {
                Log.Error("CSV file not found: /csv/" + csvFileName);
                return false;
            }

            // 2) Read CSV and check if file contains data
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            List<String> lines = reader.lines()
                .filter(line -> line != null && !line.trim().isEmpty())
                .toList();

            if (lines.isEmpty()) {
                Log.Error("CSV file is empty: " + csvFileName);
                return false;
            }

            // 3) Check if amount of songs in CSV and Database are equal
            int csvSongCount = lines.size() - 1;

            int dbSongCount = getSongCountForSet(setId);
            if (dbSongCount == csvSongCount) {
                Log.Info("Set with ID " + setId + " already contains " + dbSongCount + " songs. Skipping update.");
                return true;
            }

            // 4) Connect songs to set in table "set_songs" with set ID and song ID
            try (Connection conn = Database.connect()) {

                conn.setAutoCommit(false);

                // Reset set_songs for this set
                try (PreparedStatement deletePs = conn.prepareStatement("DELETE FROM set_songs WHERE set_id = ?")) {
                    deletePs.setInt(1, setId);
                    deletePs.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO set_songs (set_id, song_id)
                    VALUES (?, ?)
                """)) {
                    for (int i = 1; i < lines.size(); i++) {
                        String line = lines.get(i).trim();
                        try {
                            int songId = Integer.parseInt(line);
                            
                            ps.setInt(1, setId);
                            ps.setInt(2, songId);
                            ps.addBatch();
                        }
                        catch (NumberFormatException e) {
                            Log.Error("Skipped invalid ID in CSV: " + line);
                        }
                    }
                    ps.executeBatch();
                }

                conn.commit();
                Log.Success("Successfully added " + csvSongCount + " songs to set with ID " + setId + ".");
                return true;
            }
        }
        catch (SQLException e) {
            Log.Error("Error while reading set CSV: " + e.getMessage());
            return false;
        }
    }

    public int getSongCountForSet(int setId) {
        String sql = """
            SELECT COUNT(*) 
            FROM set_songs ss
            INNER JOIN songs s ON ss.song_id = s.id
            WHERE ss.set_id = ?
        """;

        try (Connection conn = Database.connect();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, setId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        catch (SQLException e) {
            Log.Error("Error while counting valid songs for set ID " + setId + ": " + e.getMessage());
        }

        return -1;
    }

    public boolean addSetToDatabase(GameSet set) {
        // 1) Check if GameSet already exists in Database
        if (checkIfSetExists(set)) {
            Log.Info("GameSet \"" + set.name + "\" already exists in Database: Skipping insertion to Database.");
            return true;
        }

        // 2) Delete all sets after parameter set's ID to prevent wrong order
        try (Connection conn = Database.connect()) {

            PreparedStatement deleteStmt = conn.prepareStatement("""
                DELETE FROM sets
                WHERE id >= ?;
            """);

            deleteStmt.setInt(1, set.id);
            deleteStmt.executeUpdate();
            
            // 3) Insert set if doesn't already exist in Database
            PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO sets (id, name, region, desc, img, icon, csv, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """);

            stmt.setInt(1, set.id);
            stmt.setString(2, set.name);
            stmt.setString(3, set.region);
            stmt.setString(4, set.desc);
            stmt.setString(5, set.img);
            stmt.setString(6, set.icon);
            stmt.setString(7, set.csv);
            stmt.setInt(8, set.isActive ? 1 : 0);

            stmt.executeUpdate();

            Log.Success("Set insertion of \"" + set.name + "\" successful.");
            return true;
        }
        catch (Exception e) {
            Log.Error("Error while inserting set \"" + set.name + "\": " + e.getMessage());
            return false;
        }
    }

    private boolean checkIfSetExists(GameSet set) {
        try (Connection conn = Database.connect()) {
            
            PreparedStatement checkStmt = conn.prepareStatement("""
                SELECT COUNT(*) FROM sets WHERE name = ? AND id = ?
            """);

            checkStmt.setString(1, set.name);
            checkStmt.setInt(2, set.id);

            ResultSet rs = checkStmt.executeQuery();
            return (rs.next() && rs.getInt(1) > 0);
        }
        catch (Exception e) {
            Log.Error("Error while checking existance of GameSet \"" + set.name + "\" in Database: " + e.getMessage());
            return false;
        }
    }

    public boolean updateSetStatus(int setId, boolean isActive) {
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

    public List<GameSet> getAllSets() {
        try (Connection conn = Database.connect()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("""
                SELECT *
                FROM sets
            """);

            List<GameSet> sets = new ArrayList<>();
            while (rs.next()) {
                sets.add(this.mapSet(rs));
            }

            return sets;
        }
        catch (SQLException e) {
            Log.Error("Error while fetching sets from database: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<GameSet> getActiveSetsBySongId(int songId) {
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
                sets.add(this.mapSet(rs));
            }

            return sets;
        }
        catch (SQLException e) {
            Log.Error("Error while fetching active sets by song ID: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<GameSet> getSetsBySongId(int songId) {
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
                sets.add(this.mapSet(rs));
            }

            return sets;
        }
        catch (SQLException e) {
            Log.Error("Error while fetching sets by song ID: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private GameSet mapSet(ResultSet rs) throws SQLException {
        GameSet set = new GameSet();

        set.id = rs.getInt("id");
        set.name= rs.getString("name");
        set.region = rs.getString("region");
        set.desc = rs.getString("desc");
        set.img = rs.getString("img");
        set.icon = rs.getString("icon");
        set.csv = rs.getString("csv"); 
        set.isActive = rs.getInt("is_active") == 1;

        return set;
    }

    // #endregion
}
