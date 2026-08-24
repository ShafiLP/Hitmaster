package hitmaster;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import hitmaster.design.Card;
import hitmaster.design.PlayerCard;
import hitmaster.design.SongCard;
import hitmaster.design.WinnerPane;
import hitmaster.models.GameOptions;
import hitmaster.models.Player;
import hitmaster.models.Song;
import hitmaster.models.SongDTO;
import hitmaster.services.Database;
import hitmaster.services.Log;
import hitmaster.services.MusicPlayer;
import hitmaster.services.NetworkManager;
import hitmaster.views.GameView;
import hitmaster.views.StealView;
import javafx.application.Platform;
import javafx.stage.Stage;

public final class GameLogic {

    private final GameView VIEW;
    private final MusicPlayer MUSICPLAYER;
    private final GameOptions OPTIONS;
    private final Player[] PLAYERS;
    private final boolean MULTIPLAYER;
    private int currentPlayerIdx = 0;
    private boolean hasPlayer = false;

    private final List<Song> SONGS;
    private Song currentSong;
    private int remaining_cards;

    // LAN connection
    private NetworkManager networkManager;
    private boolean isHost;

    private final AtomicBoolean clientReady = new AtomicBoolean(false);
    private final AtomicBoolean hostReady = new AtomicBoolean(false);

    // Regex Patterns
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[^a-z0-9 ]");

    /**
     * Constructor for GameLogic class.
     * Creates a new GameView and waits for LAN connection if LAN game is enabled.
     * Host player / player 1 starts with the game.
     * @param OPTIONS GameOptions containing move time, steal time and players.
     * @param isHost Boolean if player with this GameLogic instance is hosting/owning the game.
     * @param netManager LAN connection (if null, game runs offline).
     */
    public GameLogic(GameOptions OPTIONS, boolean isHost, NetworkManager netManager) {
        this.MUSICPLAYER = new MusicPlayer();
        hasPlayer = MUSICPLAYER.validateConnection();

        this.OPTIONS = OPTIONS;
        this.PLAYERS = OPTIONS.players;
        this.MULTIPLAYER = (PLAYERS.length > 1);

        this.isHost = isHost;
        this.networkManager = netManager;

        // 1) Show GameView
        // ----- HOST & SINGLEPLAYER LOGIC -----
        if (isHost) {
            this.VIEW = new GameView(this);
            if (MULTIPLAYER)
                VIEW.initializeOpponentPane(PLAYERS[1]);
        }

        // ----- CLIENT LOGIC -----
        else {
            this.VIEW = new GameView(this);
            VIEW.initializeOpponentPane(PLAYERS[0]);
        }


        // 2) Wait for LAN connection
        if (networkManager != null) {
            this.initializeNetworkListener();

            // ----- HOST LOGIC -----
            if (isHost) {
                Log.Info("Waiting for Client...");
                while (!clientReady.get() && netManager != null) {
                    // Wait...
                }
                this.sendObject("HOST_READY");
            }

            // ----- CLIENT LOGIC -----
            else {
                Log.Info("Waiting for Host...");
                do {
                    this.sendObject("CLIENT_READY");
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        //
                    }
                } while (!hostReady.get());
            }
        }

        // 3) Initialize Database
        // ----- HOST & SINGLEPLAYER LOGIC -----
        if (isHost) {
            SONGS = Database.getInstance().getSongFromActiveSets();
            Collections.shuffle(SONGS);

            remaining_cards = SONGS.size();
            this.sendObject("UPDATE_STACK:" + remaining_cards);
        }

        // ----- CLIENT LOGIC -----
        else {
            SONGS = new ArrayList<>();
        }

        // 4) Initialize Starting Cards for all players
        if (MULTIPLAYER) {
            if (isHost) {
                // ----- HOST LOGIC -----
                Song clientStartSong = SONGS.removeFirst();
                PLAYERS[1].songs.add(clientStartSong);
                VIEW.addOpponentCard(clientStartSong);

                Song hostStartSong = SONGS.removeFirst();
                PLAYERS[0].songs.add(hostStartSong);
                VIEW.addToCardStrip(hostStartSong);

                // Send Starting Cards to Client
                if (networkManager != null) {
                    this.sendObject(new SongDTO(clientStartSong, "CLIENT_STRIP"));
                    this.sendObject(new SongDTO(hostStartSong, "OPPONENT_PANE"));
                }
            } 
        }
        else {
            // ----- SINGLEPLAYER LOGIC -----
            Song startSong = SONGS.removeFirst();
            PLAYERS[0].songs.add(startSong);
            VIEW.addToCardStrip(startSong);
        }

        // 5) Add first song to Card Stack (Host begins)
        if (isHost)
            this.addFirstToCardStack();

        VIEW.addInfoMessage(this.getCurrentPlayer().username + " is making their guess!");
    }

    /**
     * Adds first song from global list SONGS to GameView of the current player.
     * Updates count of remaining cards on GameView.
     * Returns if method is called by client.
     */
    public void addFirstToCardStack() {
        if (!isHost)
            return;

        currentSong = SONGS.removeFirst();
        this.sendObject(new SongDTO(currentSong, "CURRENT_SONG"));

        remaining_cards--;
        VIEW.setRemainingCards(remaining_cards);

        if (PLAYERS[currentPlayerIdx].role.equals(Player.Role.CLIENT)) {
            this.sendObject(new SongDTO(currentSong, "ADD_CARD_TO_STACK"));
            VIEW.setTimerForOpponent(OPTIONS.moveTime);
            return;
        }

        VIEW.addToCardStack(currentSong);
        this.sendObject("OPPONENT_MOVE_TIMER");
    }

    /**
     * Skip song fur current player and place new card on theyr GameView.
     * Removes a Hitmaster Chip from current player.
     */
    public void skipCurrentSong() {
        this.sendObject("OPPONENT_SKIP");
        VIEW.addInfoMessage(this.getCurrentPlayer().username + " skipped their song!");

        PLAYERS[currentPlayerIdx].hitmasterPoints--;

        VIEW.removeHitmasterChip();
        VIEW.skipCard();
        this.addFirstToCardStack();
    }

    /**
     * Inserts a SongCard on GameView into the CardStripPane of current player correctly sorted.
     * Removes all Hitmaster Chips from current player.
     */
    public void insertCardIntoStrip() {
        PLAYERS[currentPlayerIdx].hitmasterPoints = 0;

        this.sendObject("OPPONENT_REMOVE_CHIPS");

        VIEW.removeAllHitmasterChips();
        VIEW.insertCardIntoStrip(false);
    }

    /**
     * Inserts the current song into list of songs of a given player.
     * Insertion index gets automatically calculated.
     * Prints warning in console if insertion index couldn't get calculated.
     * @param player Player to insert current song in list.
     */
    public void addCardToPlayerSorted(Player player) {
        if (player.songs.isEmpty()) {
            Log.Error("List of player \"" + player.username + "\" is empty!");
            return;
        }

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
        for (int i = 1; i < player.songs.size(); i++) {
            if (player.songs.get(i - 1).year <= currentSong.year && player.songs.get(i).year >= currentSong.year) {
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

    public boolean checkStealOrder(List<Card> cardList) {
        // 1) Search for steal card (PlayerCard)
        int idx = -1;

        for (int i = 0; i < cardList.size(); i++) {
            if (cardList.get(i) instanceof PlayerCard) {
                idx = i;
                break;
            }
        }

        if (idx == -1) {
            Log.Error("No PlayerCard found while checking steal order!");
            return false;
        }

        // 2) Check if stealCard position is true
        if (cardList.get(idx).equals(cardList.getFirst()) && cardList.getFirst() instanceof SongCard songCard)
            return (songCard.song.year >= currentSong.year);

        if (cardList.get(idx).equals(cardList.getLast()) && cardList.getLast() instanceof SongCard songCard)
            return (songCard.song.year <= currentSong.year);

        if (cardList.get(idx - 1) instanceof SongCard songCardBefore && cardList.get(idx - 1) instanceof SongCard songCardAfter)
            return (songCardBefore.song.year <= currentSong.year && songCardAfter.song.year >= currentSong.year);

        Log.Error("Cards next to PlayerCard aren't Song Cards!");
        return false;
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

        VIEW.addInfoMessage(this.getCurrentPlayer().username + " is making their guess!");
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

    public Song getCurrentSong() {
        return currentSong;
    }

    public GameOptions getGameOptions() {
        return OPTIONS;
    }

    public boolean isMultiplayer() {
        return MULTIPLAYER;
    }

    public Player getLocalPlayer() {
        if (this.isLAN()) {
            if (isHost)
                return PLAYERS[0];

            return PLAYERS[1];
        }

        return PLAYERS[currentPlayerIdx];
    }

    public Player getOpponentPlayer() {
        if (this.isLAN()) {
            if (isHost)
                return PLAYERS[1];

            return PLAYERS[0];
        }

        return this.getPreviousPlayer();
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
            this.sendObject("OPPONENT_ADD_CHIP");
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

        // 5) Delete all spacings
        return cleanChars.replaceAll(" ", "");
    }

    public GameView getView() {
        return VIEW;
    }

    // ==============================
    // Music Player Methods
    // ==============================

    public void togglePlayPause(Song song, boolean play) {
        if (!hasPlayer) return;

        if (play) {
            MUSICPLAYER.play(song);
        } else {
            MUSICPLAYER.pause();
        }
    }

    public void seek5secForward() {
        if (!hasPlayer) return;
        
        MUSICPLAYER.seekForward5sec();
        this.sendObject(new SongDTO(currentSong, "SONG:FORWARD"));
    }

    public void seek5secBackward() {
        if (!hasPlayer) return;
        
        MUSICPLAYER.seekForward5sec();
        this.sendObject(new SongDTO(currentSong, "SONG:BACKWARD"));
    }

    public void restartCurrentSong() {
        if (!hasPlayer) return;
        
        MUSICPLAYER.restart(currentSong);
        this.sendObject(new SongDTO(currentSong, "SONG:RESTART"));
    }

    public String[] getAvailablePlayingDevices() {
        if (!hasPlayer) return null;
        
        return MUSICPLAYER.getAvailableDevices();
    }

    public String getCurrentPlayingDevice() {
        if (!hasPlayer) return null;
        
        return MUSICPLAYER.getCurrentDevice();
    }

    public void setPlayerDevice(String device) {
        if (!hasPlayer) return;
        
        MUSICPLAYER.setCurrentDevice(device);
    }

    public int getVolume() {
        if (!hasPlayer) return -1;
        
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
            if (object instanceof SongDTO dto) {
                Log.Info("Sending object: " + object + " with purpose " + dto.purpose);
                networkManager.sendObject(object);
                return;
            }

            Log.Info("Sending object: " + object);
            networkManager.sendObject(object);
        }
    }

    public void handleNetworkCommand(String command) {
        String[] parts = command.split(":");
        String action = parts[0];

        switch (action) {

            // Signal that Host is ready: Sets flag
            case "HOST_READY":
                hostReady.set(true);
            break;

            // Signal that Client is ready: Sets flag
            case "CLIENT_READY":
                clientReady.set(true);
            break;

            case "PLAYER_NEXT":
                this.switchToNextPlayer();
                if (isHost)
                    this.addFirstToCardStack();
            break;

            // Update count of remaining cards in GameView
            // Called by host when initializing the game
            case "UPDATE_STACK":
                try {
                    remaining_cards = Integer.parseInt(parts[1]);
                    VIEW.setRemainingCards(remaining_cards);
                }
                catch (NumberFormatException e) {
                    Log.Error("Command \"UPDATE_STACK\" contains unvalid integer.");
                }
            break;

            case "OPPONENT_MOVE_TIMER":
                VIEW.setTimerForOpponent(OPTIONS.moveTime);
            break;

            // Displays current song in Opponent fan with a green border
            case "OPPONENT_RIGHT":
                VIEW.addSuccessMessage(this.getCurrentPlayer().username + " guessed right!");
                VIEW.paintOpponentCard(currentSong, "rgb(0, 255, 0)");
            break;

            // Displays current song in Opponent fan with a red border
            case "OPPONENT_WRONG":
                VIEW.addErrorMessage(this.getCurrentPlayer().username + " guessed wrong!");
                VIEW.paintOpponentCard(currentSong, "rgb(255, 0, 0)");
            break;

            // Skips the current card and places a new card to stack
            // If host, update card of client
            case "OPPONENT_SKIP":
                VIEW.addInfoMessage(this.getCurrentPlayer().username + " skipped their song!");

                PLAYERS[currentPlayerIdx].hitmasterPoints--;
                VIEW.removeOpponentChip();

                VIEW.paintOpponentCard(currentSong, "rgb(255, 0, 0)");
                VIEW.addNewCardToDiscard(currentSong);
                this.addFirstToCardStack(); 
            break;

            // Move card of opponent to discard pile
            case "OPPONENT_DISCARD":
                VIEW.addNewCardToDiscard(currentSong);
            break;

            // Add Hitmaster Chip to opponent pane
            case "OPPONENT_ADD_CHIP":
                PLAYERS[currentPlayerIdx].increaseHitmasterPoints();
                VIEW.addOpponentChip();
            break;

            // Decrase Hitmaster Chips in opponent pane by one
            case "OPPONENT_DECREASE_CHIP":
                PLAYERS[currentPlayerIdx].hitmasterPoints--;
                VIEW.removeOpponentChip();
            break;

            // Remove all Hitsmaster Chips in opponent pane
            case "OPPONENT_REMOVE_CHIPS":
                PLAYERS[currentPlayerIdx].hitmasterPoints = 0;
                VIEW.removeAllOpponentChips();
            break;

            // Shows the overlay pane to start a steal attempt on GameView
            case "OPPONENT_ASK_STEAL":
                VIEW.addInfoMessage(this.getCurrentPlayer().username + " placed their guess - Other players can now attempt to steal!");
                VIEW.showStealOverlay();
            break;

            // If steal action was available for opponent but they skipped,
            // continue with confirmation of own input.
            case "OPPONENT_STEAL_SKIP":
                VIEW.confirmInput();
            break;

            // Sets timer for opponent steal attempt and waits for opponent.
            case "OPPONENT_STEAL_START":
                this.getPreviousPlayer().hitmasterPoints--;
                VIEW.removeOpponentChip();
                VIEW.setTimerForOpponent(OPTIONS.stealTime);
            break;

            case "OPPONENT_STEAL_CORRECT":
                // TODO: Visual feedback and continue game
                Platform.runLater(() -> {
                    VIEW.confirmInput();
                    this.addCardToPlayerSorted(this.getPreviousPlayer());
                });
            break;

            case "OPPONENT_STEAL_FALSE":
                // TODO: Visual feedback and continue game
                VIEW.confirmInput();
            break;

            case "OPPONENT_WIN":
                Platform.runLater(() -> WinnerPane.winnerDialog(VIEW, getCurrentPlayer()));
            break;

            case "CLIENT_FINISH_TURN":
                this.finishTurn();
            break;

            case "SONG":
                switch (parts[1]) {
                    case "PLAY":
                        this.togglePlayPause(currentSong, true);
                    break;

                    case "PAUSE":
                        this.togglePlayPause(currentSong, false);
                    break;

                    case "FORWARD":
                        this.seek5secForward();
                    break;

                    case "BACKWARD":
                        this.seek5secBackward();
                    break;

                    case "RESTART":
                        this.restartCurrentSong();
                    break;
                }
            break;

            case "CHAT":
                switch (parts[1]) {
                    case "PLAYER_MESSAGE":
                        String playerMessage = parts[2];
                        VIEW.addPlayerMessage(this.getOpponentPlayer(), playerMessage);
                    break;

                    case "PLAYER_GUESS":
                        String artistGuess = (parts.length > 2 && !parts[2].isEmpty())
                            ? "\"" + parts[2] + "\""
                            : "X";

                        String titleGuess = (parts.length > 3 && !parts[3].isEmpty())
                            ? "\"" + parts[3] + "\""
                            : "X";

                        VIEW.addPlayerMessage(this.getOpponentPlayer(), "ARTIST: " + artistGuess + "\nTITLE: " + titleGuess);
                    break;
                }
            break;
        }
    }

    private void initializeNetworkListener() {
        if (networkManager != null) {
            networkManager.setListener(receivedObj -> {
                if (receivedObj instanceof String command) {
                    Log.Info("Received " + command);
                    handleNetworkCommand(command);
                    return;
                } 

                if (receivedObj instanceof SongDTO receivedDTO) {
                    
                    Log.Info("Received " + receivedDTO.toSong() + " with purpose " + receivedDTO.purpose);

                    String[] parts = receivedDTO.purpose.split(":");
                    String action = parts[0];

                    switch (action) {
                        case "OPPONENT":
                            switch (parts[1]) {
                                case "REVEAL":
                                    if (parts[2].equals("CORRECT")) {
                                        Platform.runLater(() -> {
                                            PLAYERS[currentPlayerIdx].songs.clear();

                                            for (SongDTO dto : receivedDTO.songList) {
                                                PLAYERS[currentPlayerIdx].songs.add(dto.toSong());
                                            }

                                            VIEW.updateOpponentCards(PLAYERS[currentPlayerIdx].songs);

                                            VIEW.addSuccessMessage(this.getCurrentPlayer().username + " guessed right!");
                                            VIEW.paintOpponentCard(currentSong, "rgb(0, 255, 0)");
                                        });
                                        return;
                                    }
                                    else if (parts[2].equals("WRONG")) {
                                        Platform.runLater(() -> {
                                            PLAYERS[currentPlayerIdx].songs.clear();

                                            for (SongDTO dto : receivedDTO.songList) {
                                                PLAYERS[currentPlayerIdx].songs.add(dto.toSong());
                                            }

                                            VIEW.updateOpponentCards(PLAYERS[currentPlayerIdx].songs);

                                            VIEW.addErrorMessage(this.getCurrentPlayer().username + " guessed wrong!");
                                            VIEW.paintOpponentCard(currentSong, "rgb(255, 0, 0)");
                                        });
                                        return;
                                    }
                                break;
                                    
                                case "MOVE":
                                    Platform.runLater(() -> {
                                        PLAYERS[currentPlayerIdx].songs.clear();

                                        for (SongDTO dto : receivedDTO.songList) {
                                            PLAYERS[currentPlayerIdx].songs.add(dto.toSong());
                                        }

                                        VIEW.updateOpponentCards(PLAYERS[currentPlayerIdx].songs);
                                    });
                                return;
                            }
                        break;
                    }

                    Song receivedSong = receivedDTO.toSong();
                    Log.Info("Received " + receivedSong + " with purpose " + receivedDTO.purpose);
                    
                    Platform.runLater(() -> {
                        switch (receivedDTO.purpose) {
                            case "CLIENT_STRIP":
                                PLAYERS[0].songs.add(receivedSong); //TODO: Check if idx is correct
                                VIEW.addToCardStrip(receivedSong);
                                break;
                                
                            case "OPPONENT_PANE":
                                PLAYERS[1].songs.add(receivedSong);
                                VIEW.addOpponentCard(receivedSong);
                                break;

                            case "CURRENT_SONG":
                                currentSong = receivedSong;
                                remaining_cards--;
                                VIEW.setRemainingCards(remaining_cards);
                                break;

                            case "ADD_CARD_TO_STACK":
                                currentSong = receivedSong;
                                VIEW.addToCardStack(currentSong);
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

    public void handleCardMove(List<Card> updatedSongCards) {
        if (networkManager != null) {
            List<Song> songsFromCards = new ArrayList<>();

            for (Card card : updatedSongCards) {
                if (card instanceof SongCard songCard) {
                    if (!songCard.isShowingFront) {
                        songsFromCards.add(new Song());
                    }
                    else {
                        songsFromCards.add(songCard.song);
                    }
                }
                else {
                    songsFromCards.add(new Song());
                }
            }

            this.sendObject(new SongDTO(songsFromCards, "OPPONENT:MOVE"));
        }
    }

    public void revealCards(List<Card> revealedSongCards, boolean correct) {
        List<Song> songsFromCards = new ArrayList<>();

        for (Card card : revealedSongCards) {
            if (card instanceof SongCard songCard) {
                if (!songCard.isShowingFront) {
                    songsFromCards.add(new Song());
                }
                else {
                    songsFromCards.add(songCard.song);
                }
            }
            else {
                songsFromCards.add(new Song());
            }
        }

        this.sendObject(new SongDTO(songsFromCards, "OPPONENT:REVEAL:" + (correct ? "CORRECT" : "WRONG")));
    }

    // ========== STEAL ACTIONS ==========

    /**
     * Sends a command to start steal timer to the opponent player.
     * If opponent doesn't have enough Hitmaster chips to attempt a steal,
     * continues with reveal of the guess instead.
     */
    public void startStealTimer() {
        if (MULTIPLAYER && this.isLAN() && this.getPreviousPlayer().hitmasterPoints > 0) {
            this.sendObject("OPPONENT_ASK_STEAL");
        }
        else {
            Log.Info("Not enough points!");
            VIEW.confirmInput();
        }
    }

    /**
     * Starts a steal action for the currently inactive player.
     * If inactive player doesn't have any hitmaster points the action gets skipped.
     * Removes a chip from the inactive player and opens the steal window.
     */
    public void startStealAction() {
        // 1) Check if player got enough points to attempt a steal
        if (this.getPreviousPlayer().hitmasterPoints < 1) {
            this.sendObject("STEAL_SKIP");
            return;
        }

        // 2) Remove a hitmaster chip
        this.getPreviousPlayer().hitmasterPoints--;
        VIEW.removeHitmasterChip();

        // 3) Send command to other player
        this.sendObject("OPPONENT_STEAL_START");

        // 4) Open steal view
        Platform.runLater(() -> {
            StealView stealView = new StealView(this, this.getPreviousPlayer());
            stealView.setStripCards(VIEW.getOpponentPane().getSongs());
            stealView.showAndWait((Stage) VIEW.getScene().getWindow());
        });
    }

    public void confirmStealAction(List<Card> cards, int placedIdx) {
        // 1) Check if guess was made
        if (placedIdx == -1) {
            this.sendObject("OPPONENT_STEAL_FALSE");
            return;
        }

        this.handleCardMove(VIEW.getCardsFromStrip());
        boolean correctSteal;

        // 2) Check position of steal guess
        if (placedIdx == 0 && cards.get(placedIdx + 1) instanceof SongCard songCard) {
            correctSteal = (currentSong.year <= songCard.song.year);
        }
        else if (placedIdx == cards.size() - 1 && cards.get(placedIdx - 1) instanceof SongCard songCard) {
            correctSteal = (currentSong.year >= songCard.song.year);
        }
        else if (cards.get(placedIdx - 1) instanceof SongCard songCardBefore && cards.get(placedIdx + 1) instanceof SongCard songCardAfter) {
            correctSteal = (currentSong.year >= songCardBefore.song.year) && (currentSong.year <= songCardAfter.song.year);
        }
        else {
            Log.Error("Couldn't check result of steal card placement.");
            correctSteal = false;
        }

        // 4) Add song to player's song list if guess was correct
        //! BUG: Both could be correct (if song was 2005 and both place it next to 2005)
        if (correctSteal) {
            this.addCardToPlayerSorted(this.getPreviousPlayer());
            Platform.runLater(() -> {
                VIEW.addToCardStack(currentSong);
                VIEW.insertCardIntoStrip(true);
            });
        }

        // 3) Send result to opponent
        this.sendObject(correctSteal ? "OPPONENT_STEAL_CORRECT" : "OPPONENT_STEAL_FALSE");

        // 4) Display result on own view
        // TODO
    }

    /**
     * End the current player's turn.
     * Switches to the next player in Player array and adds a new card to their GameView.
     * If LAN game is active, sends commands to the connected player to switch player, add a new card and finish the turn.
     * Called when player made their guess or player run out of time.
     */
    public void finishTurn() {
        VIEW.stopTimer();

        // ----- HOST & SINGLEPLAYER LOGIC -----
        if (isHost) {
            // 1) Update Card Strip
            this.handleCardMove(VIEW.getCardsFromStrip());

            // 2) Change active player
            this.switchToNextPlayer();
            this.sendObject("PLAYER_NEXT");

            // 3) Place new SongCard on GameView
            this.addFirstToCardStack(); // Method sends an Object
        }

        // ----- CLIENT LOGIC -----
        else {
            if (PLAYERS[currentPlayerIdx].role.equals(Player.Role.CLIENT)) {
                this.handleCardMove(VIEW.getCardsFromStrip());
                this.sendObject("CLIENT_FINISH_TURN");
            }
        }
    }

    public boolean isLAN() {
        return networkManager != null;
    }
}
