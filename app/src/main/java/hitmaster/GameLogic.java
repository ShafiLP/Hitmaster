package hitmaster;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import hitmaster.design.SongCard;
import hitmaster.models.GameOptions;
import hitmaster.models.Player;
import hitmaster.models.Song;
import hitmaster.models.SongDTO;
import hitmaster.services.Database;
import hitmaster.services.Log;
import hitmaster.services.MusicPlayer;
import hitmaster.services.NetworkManager;
import hitmaster.views.GameView;
import javafx.application.Platform;

public final class GameLogic {

    private final GameView VIEW;
    private final MusicPlayer MUSICPLAYER;
    private final GameOptions OPTIONS;
    private final Player[] PLAYERS;
    private final boolean MULTIPLAYER;
    private int currentPlayerIdx = 0;

    private final List<Song> SONGS;
    private Song currentSong;

    // LAN connection
    private NetworkManager networkManager;
    private boolean isHost;
    private String nextSongPurpose = "";

    // Regex Patterns
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[^a-z0-9 ]");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

    public GameLogic(GameOptions OPTIONS, boolean isHost, NetworkManager netManager) {
        this.MUSICPLAYER = new MusicPlayer();
        this.OPTIONS = OPTIONS;
        this.PLAYERS = OPTIONS.players;
        this.MULTIPLAYER = (PLAYERS.length > 1);

        this.isHost = isHost;
        this.networkManager = netManager;

        // 1) Read songs from DB and shuffle them
        if (isHost) {
            SONGS = this.loadSongsFromDB();
            Collections.shuffle(SONGS);
        }
        else {
            SONGS = new ArrayList<>();
        }

        // 2) Setup Game
        this.VIEW = new GameView(this);

        if (MULTIPLAYER) {
            if (isHost) {
                // ----- HOST LOGIC -----
                Song clientStartSong = SONGS.removeFirst();
                PLAYERS[1].songs.add(clientStartSong);
                VIEW.initializeOpponentPane(PLAYERS[1]);
                VIEW.addOpponentCard(clientStartSong);

                Song hostStartSong = SONGS.removeFirst();
                PLAYERS[0].songs.add(hostStartSong);
                VIEW.addToCardStrip(hostStartSong);

                // Send information to client if LAN connection is active
                if (networkManager != null) {
                    Log.Info("Starting to send objects.");
                    this.sendObject(new SongDTO(clientStartSong, "CLIENT_STRIP"));
                    Log.Info("Sent ClientStartSong.");

                    this.sendObject(new SongDTO(hostStartSong, "OPPONENT_PANE"));
                    Log.Info("Sent HostStartSong.");
                }
            }
            else {
                // ----- CLIENT LOGIC -----
                VIEW.initializeOpponentPane(PLAYERS[0]);
                this.initializeClientNetworkListener();
            }
        }
        else {
            // ----- SINGLEPLAYER LOGIC -----
            PLAYERS[0].songs.add(SONGS.getFirst());
            this.addFirstToCardStrip();
        }

        // 3) Add first card to stack
        if (isHost)
            this.addFirstToCardStack();
    }

    public List<Song> loadSongsFromDB() {
        return Database.getSongFromActiveSets();
    }

    private void addFirstToCardStrip() {
        VIEW.addToCardStrip(SONGS.getFirst());
        SONGS.removeFirst();
    }

    public void addFirstToCardStack() {
        VIEW.addToCardStack(SONGS.getFirst());
        currentSong = SONGS.getFirst();
        SONGS.removeFirst();
    }

    public void addCardToCorrectSongs() {
        PLAYERS[currentPlayerIdx].songs.add(currentSong);
    }

    public void skipCurrentSong() {
        PLAYERS[currentPlayerIdx].hitmasterPoints--;

        VIEW.removeHitmasterChip();
        VIEW.removeCurrentCard();
    }

    public void markCurrentSongAsCorrect() {
        PLAYERS[currentPlayerIdx].hitmasterPoints = 0;

        VIEW.removeAllHitmasterChips();
        VIEW.insertCardIntoStrip();
    }

    /**
     * Inserts the current song into list of songs of a given player.
     * Insertion index gets automatically calculated.
     * Prints warning in console if insertion index couldn't get calculated.
     * @param player Player to insert current song in list.
     */
    public void addCardToPlayerSorted(Player player) {
        // 1) Check if first index fits
        if (currentSong.year <= player.songs.getFirst().year) {
            player.songs.addFirst(currentSong);
            return;
        }

        // 2) Check if last index fits
        if (currentSong.year >= player.songs.getLast().year) {
            player.songs.addLast(currentSong);
            return;
        }

        // 3) Search for index
        int insertionIdx = -1;
        for (int i = 1; i < player.songs.size() - 1; i++) {
            if (player.songs.get(i - 1).year < currentSong.year && player.songs.get(i).year > currentSong.year) {
                insertionIdx = i;
                break;
            }
        }

        if (insertionIdx >= 0) {
            player.songs.add(insertionIdx, currentSong);
        }
        else {
            Log.Warning("Song \"" + currentSong.titles.getFirst() + "\" couldn't get inserted into songs of \"" + player.username + "\": No insertion index found.");
        }
    }

    public boolean checkSongOrder(List<SongCard> songCards) {
        // 1) Search for currentSong
        int idx = -1;
        for (int i = 0; i < songCards.size(); i++) {
            if (songCards.get(i).song.id == currentSong.id) {
                idx = i;
                break;
            }
        }
        if (idx == -1)
            return false;

        // 2) Check position
        if (songCards.get(idx).equals(songCards.getFirst()))
            return (songCards.get(idx + 1).song.year >= songCards.get(idx).song.year);

        if (songCards.get(idx).equals(songCards.getLast()))
            return (songCards.get(idx - 1).song.year <= songCards.get(idx).song.year);

        return (songCards.get(idx - 1).song.year <= songCards.get(idx).song.year && songCards.get(idx + 1).song.year >= songCards.get(idx).song.year);
    }

    public boolean checkStealOrder(List<SongCard> songCards, SongCard stealCard) {
        // 1) Search for steal card
        int idx = -1;
        for (int i = 0; i < songCards.size(); i++) {
            if (songCards.get(i) == stealCard) {
                idx = i;
                break;
            }
        }
        if (idx == -1)
            return false;

        // 2) Check if stealCard position is true
        songCards.get(idx).song = new Song();
        songCards.get(idx).song = SONGS.getFirst(); // TODO: Replace with debug song (For steal card functionality only)
        songCards.get(idx).song.year = currentSong.year;
        
        if (songCards.get(idx).equals(songCards.getFirst()))
            return (songCards.get(idx + 1).song.year >= songCards.get(idx).song.year);

        if (songCards.get(idx).equals(songCards.getLast()))
            return (songCards.get(idx - 1).song.year <= songCards.get(idx).song.year);

        return (songCards.get(idx - 1).song.year <= songCards.get(idx).song.year && songCards.get(idx + 1).song.year >= songCards.get(idx).song.year);
    }

    /**
     * Checks if player reached 10 correct cards.
     * @param songCards List of SongCard UI elements.
     * @return Win result.
     */
    public boolean checkForWin(List<SongCard> songCards) {
        return (songCards.size() >= 10);
    }

    /**
     * Changes currentPlayerIdx to next player in array.
     * If index reached end of array, it gets set to first.
     */
    public void switchToNextPlayer() {
        // 1) Change currentPlayerIdx
        if (currentPlayerIdx == PLAYERS.length - 1) {
            currentPlayerIdx = 0;
        }
        else {
            currentPlayerIdx++;
        }

        // 2) Update UI
        if (MULTIPLAYER && networkManager == null)
            VIEW.switchSideWithOpponent();

        // 3) Send command if online multiplayer
        if (isHost && networkManager != null)
            this.sendObject("PLAYER_NEXT");
    }

    public Player getCurrentPlayer() {
        return PLAYERS[currentPlayerIdx];
    }

    public Player getPreviousPlayer() {
        if (currentPlayerIdx > 0)
            return PLAYERS[currentPlayerIdx - 1];

        return PLAYERS[PLAYERS.length - 1];
    }

    public int getChipCountOfCurrentPlayer() {
        return PLAYERS[currentPlayerIdx].hitmasterPoints;
    }

    public GameOptions getGameOptions() {
        return OPTIONS;
    }
    
    public int getRemainingCardCount() {
        return SONGS.size();
    }

    public boolean isMultiplayer() {
        return MULTIPLAYER;
    }

    /**
     * Checks if user input of artist and song title are correct.
     * If input is correct, user receives one HM point.
     * @param artist User input of artist.
     * @param title User input of song title.
     * @return Comparison result.
     */
    public boolean checkSongInformation(String artist, String title) {
        if (compareArtist(artist, currentSong.artists) && compareTitle(title, currentSong.titles)) {
            PLAYERS[currentPlayerIdx].increaseHitmasterPoints();
            VIEW.addHitmasterChip();
            return true;
        }

        return false;
    }

    public boolean checkArtistInformation(String artist) {
        return compareArtist(artist, currentSong.artists);
    }

    public boolean checkTitleInformation(String title) {
        return compareTitle(title, currentSong.titles);
    }

    /**
     * Compares if a String matches the artist name.
     * Ignores uppercase/lowercase, special characters and additional spacings.
     * @param input String to compare to artist name and alias.
     * @param artist String with artist input.
     * @return Comparison result.
     */
    private boolean compareArtist(String input, List<String> artists) {
        if (input == null || artists == null || artists.isEmpty())
            return false;

        String normalizedInput = normalizeText(input);

        for (String artist : artists) {
            if (normalizedInput.equals(normalizeText(artist)))
                return true;
        }

        return false;
    }

    /**
     * Compares if a String matches the song title.
     * Ignores uppercase/lowercase, special characters and additional spacings.
     * @param input String to compare to song title and alias.
     * @param song String with Song information.
     * @return Comparison result.
     */
    private boolean compareTitle(String input, List<String> titles) {
        if (input == null || titles == null || titles.isEmpty())
            return false;

        String normalizedInput = normalizeText(input);

        for (String title :  titles) {
            if (normalizedInput.equals(normalizeText(title))) 
                return true;
        }

        return false;
    }

    /**
     * Removes special characters (ä, é, î) and unnecessary spaces from String.
     * @param text Input String to normalize.
     * @return Normalized String.
     */
    public static String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        // 1) To lowercase
        String lowercase = text.toLowerCase();

        // 2) Remove accents
        String normalized = Normalizer.normalize(lowercase, Normalizer.Form.NFD);

        // 3) Delete diacritics
        String noAccents = DIACRITICS.matcher(normalized).replaceAll("");

        // 4) Remove everything that's not a letter, a number or a space
        String cleanChars = SPECIAL_CHARS.matcher(noAccents).replaceAll("");

        // 5) Delete additional spacings
        return MULTIPLE_SPACES.matcher(cleanChars).replaceAll(" ").trim();
    }

    public GameView getView() {
        return VIEW;
    }

    // ==============================
    // Music Player Methods
    // ==============================

    public void togglePlayPause(Song song, boolean play) {
        if (play) {
            MUSICPLAYER.play(song);
        } else {
            MUSICPLAYER.play(song);
        }
    }

    public void seek5secForward() {
        MUSICPLAYER.seekForward5sec();
    }

    public void seek5secBackward() {
        MUSICPLAYER.seekForward5sec();
    }

    public void restartCurrentSong() {
        MUSICPLAYER.restart(currentSong);
    }

    public String[] getAvailablePlayingDevices() {
        return MUSICPLAYER.getAvailableDevices();
    }

    public String getCurrentPlayingDevice() {
        return MUSICPLAYER.getCurrentDevice();
    }

    public void setPlayerDevice(String device) {
        MUSICPLAYER.setCurrentDevice(device);
    }

    public int getVolume() {
        return MUSICPLAYER.getVolume();
    }

    public void setVolume(int volume) {
        MUSICPLAYER.setVolume(volume);
    }

    // ==============================
    // LAN connection methods
    // ==============================

    public synchronized void sendObject (Object object) {
        if (networkManager != null) {
            networkManager.sendObject(object);
        }
    }

    public void handleNetworkCommand(String command) {
        String[] parts = command.split(":");
        String action = parts[0];

        switch (action) {
            case "PLAYER_NEXT":
                this.switchToNextPlayer();
                break;

            case "UPDATE_CURRENT_SONG":
                break;

            case "ADD_CARD_TO_STACK":
                VIEW.addToCardStack(currentSong);
                break;

            case "PLAY_SONG":
                break;
        }
    }

    private void initializeClientNetworkListener() {
        if (networkManager != null) {
            networkManager.setListener(receivedObj -> {
                if (receivedObj instanceof String command) {
                    Log.Info("Received command:" + command);
                    switch (command) {
                        case "INIT_CLIENT_STRIP":
                            this.nextSongPurpose = "CLIENT_STRIP";
                            break;
                        case "INIT_OPPONENT_PANE":
                            this.nextSongPurpose = "OPPONENT_PANE";
                            break;
                        default:
                            Platform.runLater(() -> handleNetworkCommand(command));
                            break;
                    }
                } 
                else if (receivedObj instanceof SongDTO receivedDTO) {
                    Song receivedSong = receivedDTO.toSong();
                    
                    Platform.runLater(() -> {
                        switch (receivedDTO.purpose) {
                            case "CLIENT_STRIP":
                                PLAYERS[0].songs.add(receivedSong);
                                VIEW.addToCardStrip(receivedSong);
                                break;
                                
                            case "OPPONENT_PANE":
                                PLAYERS[1].songs.add(receivedSong);
                                VIEW.addOpponentCard(receivedSong);
                                break;
                                
                            default:
                                Log.Error("Received SongDTO with invalid purpose.");
                                break;
                        }
                    });
                }
            });
        }
    }
}
