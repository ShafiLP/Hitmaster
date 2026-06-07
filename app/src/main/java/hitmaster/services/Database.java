package hitmaster.services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hitmaster.models.Artist;
import hitmaster.models.Song;
import hitmaster.models.User;

/**
 * TODO:
 * - Song can have multiple artists (song_artist table?)
 */

public class Database {
    public static final String URL = "jdbc:sqlite:hitmaster.db";

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

            //! DEBUG
            stmt.execute("INSERT INTO user (username) VALUES('Testuser');");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    img TEXT
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS artists (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    alias TEXT
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS songs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    artist_id INTEGER,
                    year INTEGER,
                    spotify TEXT,
                    set_id INTEGER,
                    FOREIGN KEY (artist_id) REFERENCES artists(id),
                    FOREIGN KEY (set_id) REFERENCES sets(id)
                );
            """);

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

            //stmt.execute("DELETE FROM user");
            //stmt.execute("DELETE FROM sets");
            //stmt.execute("DELETE FROM artists");
            //stmt.execute("DELETE FROM songs");
            stmt.execute("DROP TABLE IF EXISTS user");
            stmt.execute("DROP TABLE IF EXISTS sets");
            stmt.execute("DROP TABLE IF EXISTS artists");
            stmt.execute("DROP TABLE IF EXISTS songs");

            Log.Success("Deleted all data from tables.");
            return true;
        }
        catch (SQLException e) {
            Log.Error("Error while deleting data from database: " + e.getMessage());
            return false;
        }
    }

    public static boolean insertCsvIntoDatabase(String tableName, String fileName) {
        try {
            // 1) Get CSV from path
            InputStream is = Database.class.getResourceAsStream("/csv/" + fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            List<String> lines = reader.lines()
                .filter(line -> line != null && !line.trim().isEmpty())
                .toList();

            // 2) Build prompt from CSV
            String[] columns = lines.get(0).split(";");
            String placeholders = String.join(",", Collections.nCopies(columns.length, "?"));
            String sqlExecute = String.format(
                "INSERT INTO %s (%s) VALUES (%s)",
                tableName,
                String.join(",", columns),
                placeholders);

            // 3) Execute command
            Connection conn = Database.connect();
            PreparedStatement ps = conn.prepareStatement(sqlExecute);

            // Assuming heading row
            for (int i = 1; i < lines.size(); i++) {
                String[] values = lines.get(i).split(";");

                for (int j = 0; j < columns.length; j++) {
                    ps.setString(j + 1, j < values.length ? values[j] : null);
                }

                ps.addBatch();
            }

            ps.executeBatch();

            Log.Success("Inserted data from \"" + fileName + "\" into " + tableName + ".");
            return true;
        }
        catch (SQLException e) {
            Log.Error("Error while inserting data from \"" + fileName + "\" into table \"" + tableName + "\": " + e.getMessage());
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

            throw new Error("No user found.");
        }
        catch (SQLException e) {
            Log.Error("Error while fetching user from database: " + e.getMessage());
            return new User();
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
                Song song = new Song();

                song.id = rs.getInt("id");
                song.title = rs.getString("title");
                song.artist_id = rs.getInt("artist_id");
                song.year = rs.getInt("year");
                song.spotify = rs.getString("spotify");
                song.set_id = rs.getInt("set_id");

                return song;
            }

            Log.Error("Error while fetching songs from database: Couldn't find match.");
            return new Song();
        }
        catch (SQLException e) {
            Log.Error("Error while fetching songs from database: " + e.getMessage());
            return new Song();
        }
    }

    public static Artist getArtistById(int artistId) {
        try (Connection conn = Database.connect()) {
            String sqlExecute = """
                SELECT *
                FROM artists
                WHERE id = ?
            """;
            PreparedStatement ps = conn.prepareStatement(sqlExecute);
            ps.setInt(1, artistId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Artist artist = new Artist();

                artist.id = rs.getInt("id");
                artist.name = rs.getString("name");
                artist.alias = rs.getString("alias");

                return artist;
            }

            Log.Error("Error while fetching artists from database: Couldn't find match.");
            return new Artist();
        }
        catch (SQLException e) {
            Log.Error("Error while fetching artists from database: " + e.getMessage());
            return new Artist();
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

    private static Song mapSong(ResultSet rs) throws SQLException {
        Song song = new Song();

        song.id = rs.getInt("id");
        song.title = rs.getString("title");
        song.artist_id = rs.getInt("artist_id");
        song.year = rs.getInt("year");
        song.spotify = rs.getString("spotify");
        song.set_id = rs.getInt("set_id");

        return song;
    }
}
