package hitmaster.views;

import java.util.List;

import hitmaster.GameLogic;
import hitmaster.design.Card;
import hitmaster.design.CardStripPane;
import hitmaster.design.ChatPane;
import hitmaster.design.ChipPane;
import hitmaster.design.DiscardPile;
import hitmaster.design.OpponentPane;
import hitmaster.design.PlayerCard;
import hitmaster.design.SongCard;
import hitmaster.design.StealOverlayPane;
import hitmaster.design.WinnerPane;
import hitmaster.models.Player;
import hitmaster.models.Song;
import hitmaster.services.Log;
import hitmaster.services.Timer;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class GameView extends Pane {

    private final GameLogic GAME;

    private Timer timerUnit;
    private SongCard currentCard;
    private PlayerCard stealCard = new PlayerCard(null);
    private boolean isStealing = false;

    // UI elements
    private final ChatPane CHAT;
    private final CardStripPane STRIP;
    private final DiscardPile DISCARD_PILE;
    private final ChipPane CHIP_PANE;
    private final TextField ARTIST;
    private final TextField TITLE;

    private final Button SUBMIT;
    private final Button PLAYPAUSE;
    private final Button BACKWARD;
    private final Button FORWARD;
    private final Button RESTART;
    private final Button SKIP;
    private final Button INSERT;

    private final ImageView PILE_IMAGE;
    private final Label REMAINING_CARDS;

    private OpponentPane OPPONENT_PANE;

    public GameView(GameLogic GAME) {
        this.GAME = GAME;

        final double CONTROLS_WIDTH = 320;
        final double GAP = 20;

        // 1) Chat Pane
        CHAT = new ChatPane(GAME);
        CHAT.addInfoMessage("Welcome to Hitmaster!");
        VBox.setVgrow(CHAT, Priority.ALWAYS);

        // 2) Card Strip Pane
        STRIP = new CardStripPane(GAME);
        STRIP.setPrefHeight(200);

        STRIP.prefWidthProperty().bind(this.widthProperty().subtract(CONTROLS_WIDTH + (GAP * 3)));
        STRIP.layoutYProperty().bind(this.heightProperty().subtract(STRIP.prefHeightProperty()).subtract(GAP));
        STRIP.setLayoutX(GAP);
        this.getChildren().add(STRIP);

        // 3) Card Pile
        BorderPane cardPile = new BorderPane();
        cardPile.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Image imageFile = new Image(getClass().getResourceAsStream("/cardDesign.png"));
        PILE_IMAGE = new ImageView(imageFile);

        double targetWidth = 150;
        double targetHeight = targetWidth * (imageFile.getHeight() / imageFile.getWidth());

        PILE_IMAGE.setFitWidth(targetWidth);
        PILE_IMAGE.setFitHeight(targetHeight);
        PILE_IMAGE.setPreserveRatio(true);
        PILE_IMAGE.setPickOnBounds(true);

        Rectangle clip = new Rectangle(targetWidth, targetHeight);
        clip.setArcWidth(20); 
        clip.setArcHeight(20);
        PILE_IMAGE.setClip(clip);

        REMAINING_CARDS = new Label("0 cards left");
        REMAINING_CARDS.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");

        VBox cardPileLayout = new VBox(5, PILE_IMAGE, REMAINING_CARDS);
        cardPileLayout.setStyle("-fx-alignment: center;");

        cardPile.setCenter(cardPileLayout);

        double totalPileHeight = targetHeight + 25;
        cardPile.setPrefSize(targetHeight, totalPileHeight);

        cardPile.layoutXProperty().bind(this.widthProperty().subtract(cardPile.prefWidthProperty()).divide(2));
        cardPile.layoutYProperty().bind( this.heightProperty().subtract(cardPile.prefHeightProperty()).divide(2));

        this.getChildren().add(cardPile);

        // 4) Discard Pile
        DISCARD_PILE = new DiscardPile();

        // 5) Text fields and button
        ARTIST = new TextField();
        ARTIST.getStyleClass().add("modern-textbox");
        ARTIST.setPromptText("Artist...");

        TITLE = new TextField();
        TITLE.getStyleClass().add("modern-textbox");
        TITLE.setPromptText("Song title...");

        SUBMIT = new Button("Submit");
        SUBMIT.getStyleClass().add("primary-button");
        SUBMIT.setMaxWidth(Double.MAX_VALUE);
        SUBMIT.setOnAction(e -> {
            if (!isStealing) {
                this.startStealTime();
            }
            else {
                this.checkStealGuess();
            }
        });

        VBox inputs = new VBox(8, ARTIST, TITLE, SUBMIT);

        // 6) Spotify media control
        BACKWARD = new Button("⏮");
        PLAYPAUSE = new Button("►");
        FORWARD = new Button("⏭");
        RESTART = new Button("↺");
        SKIP = new Button("Skip");
        INSERT = new Button("Insert");

        BACKWARD.getStyleClass().add("modern-button");
        PLAYPAUSE.getStyleClass().add("modern-button");
        FORWARD.getStyleClass().add("modern-button");
        RESTART.getStyleClass().add("modern-button");
        SKIP.getStyleClass().add("modern-button");
        INSERT.getStyleClass().add("modern-button");

        BACKWARD.setOnAction(e -> GAME.seek5secBackward());
        PLAYPAUSE.setOnAction(e -> this.playPause());
        FORWARD.setOnAction(e -> GAME.seek5secForward());
        RESTART.setOnAction(e -> GAME.restartCurrentSong());
        SKIP.setOnAction(e -> { if (GAME.getChipCountOfCurrentPlayer() >= 1) GAME.skipCurrentSong(); });
        INSERT.setOnAction(e -> { if (GAME.getChipCountOfCurrentPlayer() >= 3) GAME.insertCardIntoStrip(); });

        this.setInputsEnabled(false);

        HBox mediaRow = new HBox(15, BACKWARD, PLAYPAUSE, FORWARD, RESTART, SKIP, INSERT);
        mediaRow.setAlignment(Pos.CENTER);

        ComboBox<String> deviceDropdown = new ComboBox<>();
        deviceDropdown.getStyleClass().add("modern-dropdown");
        deviceDropdown.setPromptText("Device...");
        deviceDropdown.setPrefWidth(130);
        deviceDropdown.getItems().addAll(GAME.getAvailablePlayingDevices());
        deviceDropdown.getSelectionModel().select(GAME.getCurrentPlayingDevice());
        deviceDropdown.setOnAction(e -> { GAME.setPlayerDevice(deviceDropdown.getValue()); });

        Slider volumeSlider = new Slider(0, 100, GAME.getVolume());
        volumeSlider.getStyleClass().add("modern-slider");
        volumeSlider.setOnMouseReleased(e -> { GAME.setVolume((int) volumeSlider.getValue()); });
        HBox.setHgrow(volumeSlider, Priority.ALWAYS);

        HBox audioRow = new HBox(12, deviceDropdown, volumeSlider);
        audioRow.setAlignment(Pos.CENTER_LEFT);

        // 7) Chip panel
        CHIP_PANE = new ChipPane();

        // 8) Build full panel
        VBox controlPanel = new VBox(15, inputs, mediaRow, audioRow);

        VBox rightSidebar = new VBox(15);
        rightSidebar.setPrefWidth(CONTROLS_WIDTH);

        rightSidebar.setAlignment(Pos.CENTER_RIGHT);

        rightSidebar.layoutXProperty().bind(this.widthProperty().subtract(CONTROLS_WIDTH + GAP));
        rightSidebar.setLayoutY(GAP);
        rightSidebar.prefHeightProperty().bind(this.heightProperty().subtract(GAP * 2));

        rightSidebar.getChildren().addAll(CHAT, DISCARD_PILE, CHIP_PANE, controlPanel);
        
        this.getChildren().add(rightSidebar);
        Platform.runLater(this::requestFocus);
    }

    /**
     * Toggles play and pause for the current song.
     * Updates all play/pause buttons on the UI.
     */
    public void playPause() {
        currentCard.isPlaying = !currentCard.isPlaying;

        PLAYPAUSE.setText(currentCard.isPlaying ? "⏸" : "►");

        currentCard.togglePlayPause();
        GAME.togglePlayPause(currentCard.song, currentCard.isPlaying);
    }

    /**
     * Starts the countdown for the second player to steal the SongCard by guessing it correct.
     * If no player interrupts game continues with reveal.
     */
    private void startStealTime() {
        // 1) Lock current guess by disabling all inputs
        this.setInputsEnabled(false);
        currentCard.setDraggable(false);

        // 2) Skip if multiplayer is disabled or opponent doesn't have any chips
        if (!GAME.isMultiplayer() || GAME.getPreviousPlayer().hitmasterPoints < 1) {
            this.confirmInput();
            return;
        }

        // 3) Set Status
        CHAT.addInfoMessage(GAME.getCurrentPlayer().username + " placed their guess - Other players can now attempt to steal!");

        // 4) Enable steal action
        if (GAME.isLAN()) {
            GAME.startStealTimer();
            currentCard.startCountdown(5);
        }
        else {
            currentCard.setStealState(true);

            // 5) Start Timer
            if (timerUnit != null)
                timerUnit.stop();
            
            timerUnit = new Timer(3);
            timerUnit.start(
                () -> Platform.runLater(() -> 
                    CHAT.setRemainingTime(timerUnit.getRemainingSeconds())
                ),
                () -> Platform.runLater(() -> {
                    currentCard.setStealState(false);

                    if (!isStealing)
                        this.confirmInput();
                })
            );
        }
    }

    /**
     * Starts confirmation of player input.
     * Artist and Title Label get colored green if correct and red if incorrect.
     * Sets border color of currentCard green if correct and red if incorrect.
     * Moves to this.checkForWin() after timer finished.
     */
    public void confirmInput() {
        Platform.runLater(() -> {
            // 1) Show front of currentCard and disable dragging stealCard
            if (currentCard != null)
                currentCard.showFront();

            if (stealCard != null)
                stealCard.setDraggable(false);

            // 2) Color border of artist and title input green if correct and red if incorrect
            String artistInput = ARTIST.getText().trim().isEmpty() ? "X" : ARTIST.getText().trim();
            String titleInput = TITLE.getText().trim().isEmpty() ? "X" : TITLE.getText().trim();

            GAME.sendObject("CHAT:PLAYER_GUESS:" + artistInput + ":" + titleInput);
            this.addPlayerMessage(GAME.getLocalPlayer(), "ARTIST: \"" + artistInput + "\"\nTITLE: \"" + titleInput + "\"");

            ARTIST.setStyle(GAME.checkArtistInformation(artistInput) ? "-fx-border-color:rgb(0, 255, 0);" : "-fx-border-color:rgb(255, 0, 0);");
            TITLE.setStyle(GAME.checkTitleInformation(titleInput) ? "-fx-border-color:rgb(0, 255, 0);" : "-fx-border-color:rgb(255, 0, 0);");
            GAME.checkSongInformation(artistInput, titleInput);

            // 3) Check position of card
            boolean guess = GAME.checkSongOrder(STRIP.getSongCards());
            currentCard.setBorderColor(guess ? "rgb(0, 255, 0)" : "rgb(255, 0, 0)");

            // 4) Send result to connected player
            GAME.revealCards(STRIP.getCards(), guess);

            // 5) Set Status and start Timer
            if (guess) {
                CHAT.addSuccessMessage(GAME.getCurrentPlayer().username + " guessed right!");
            }
            else {
                CHAT.addErrorMessage(GAME.getCurrentPlayer().username + " guessed wrong!");
            }
            
            if (timerUnit != null)
                timerUnit.stop();

            timerUnit = new Timer(3);
            timerUnit.start(
                () -> Platform.runLater(() -> 
                    CHAT.setRemainingTime(timerUnit.getRemainingSeconds())
                ),
                () -> this.checkForWin(guess)
            );
        });
    }

    /**
     * If guess was correct, check for win.
     * If guess was incorrect, move card to discard pile.
     * After checking, continues game with next player.
     * @param guess
     */
    private void checkForWin(boolean guess) {
        // 1) Reset styles
        ARTIST.setStyle("");
        ARTIST.getStyleClass().add("modern-textbox");
        ARTIST.clear();
        ARTIST.setEditable(true);

        TITLE.setStyle("");
        TITLE.getStyleClass().add("modern-textbox");
        TITLE.clear();
        TITLE.setEditable(true);

        currentCard.resetBorderColor();
        if (stealCard != null) {
            stealCard.resetBorderColor();
            stealCard.setVisible(false);
        }

        // 2) Check for win if guess was correct
        if (guess) {
            GAME.addCardToPlayerSorted(GAME.getCurrentPlayer());
            if (GAME.checkForWin(STRIP.getSongCards())) {
                Platform.runLater(() -> {
                    CHAT.addSuccessMessage(GAME.getCurrentPlayer().username + " won the game!");

                    if (timerUnit != null)
                        timerUnit.stop();

                    GAME.sendObject("OPPONENT_WIN");
                    WinnerPane.winnerDialog(this, GAME.getCurrentPlayer());
                });
            }
        }

        // 3) Move card to discard pile if guess was incorrect
        else {
            Platform.runLater(() -> {
                final SongCard wrongCard = this.currentCard; 
            
                if (wrongCard == null) return;

                double sceneX = wrongCard.localToScene(0, 0).getX();
                double sceneY = wrongCard.localToScene(0, 0).getY();
                
                Point2D localPos = this.sceneToLocal(sceneX, sceneY);

                STRIP.removeCard(wrongCard, false); 

                if (wrongCard.getParent() != this) {
                    this.getChildren().add(wrongCard);
                }

                wrongCard.setLayoutX(localPos.getX());
                wrongCard.setLayoutY(localPos.getY());
                wrongCard.setTranslateX(0);
                wrongCard.setTranslateY(0);
                wrongCard.toFront();

                Platform.runLater(() -> {
                    DISCARD_PILE.discardCard(wrongCard);
                });
            });

            GAME.handleCardMove(STRIP.getCards());
            GAME.sendObject("OPPONENT_DISCARD");
        }

        // 4) Swap active player
        GAME.finishTurn();
        this.initializeNewTimer();
    }

    /**
     * Adds a song as SongCard to Card Strip.
     * Used at the beginning of the game to give players their first card.
     * @param song Song to place in Card Strip as SongCard.
     */
    public void addToCardStrip(Song song) {
        SongCard card = new SongCard(this, song);
        card.showFront();
        STRIP.addCard(card);
    }

    /**
     * Adds a new song as SongCard to stack.
     * Current player can move the card freely and place it in their Card Strip.
     * @param song
     */
    public void addToCardStack(Song song) {
        SongCard card = new SongCard(this, song);

        Platform.runLater(() -> {
            Point2D sceneCoords = PILE_IMAGE.localToScene(0, 0);
            Point2D localCoords = this.sceneToLocal(sceneCoords);

            if (localCoords != null) {
                card.setLayoutX(localCoords.getX());
                card.setLayoutY(localCoords.getY());
            }

            STRIP.registerExternalCard(card);
            currentCard = card;

            this.getChildren().add(currentCard);
            currentCard.toFront();

            // Reset border color of opponent card if LAN is active
            if (GAME.isLAN())
                this.resetOpponentCard();

            this.setInputsEnabled(true);
            this.initializeNewTimer();
        });

        //! DEBUG
        Log.Info("Artist: " + song.artists.getFirst());
        Log.Info("Name: " + song.titles.getFirst());
        Log.Info("Year: " + song.year);
    }

    private void initializeNewTimer() {
        if (timerUnit != null)
            timerUnit.stop();

        timerUnit = new Timer(GAME.getGameOptions().moveTime);
        timerUnit.start(
            () -> Platform.runLater(() -> 
                CHAT.setRemainingTime(timerUnit.getRemainingSeconds())
            ),
            () -> Platform.runLater(() -> {
                this.confirmInput();
            })
        );
    }

    public void stopTimer() {
        if (timerUnit != null)
            timerUnit.stop();
    }

    public void setRemainingCards(int remainingCards) {
        Platform.runLater(() -> REMAINING_CARDS.setText(remainingCards + " cards left"));
    }

    /**
     * Inserts a card into current player's Card Strip.
     * Automatically sorts the Card Strip by Release Year.
     */
    public void insertCardIntoStrip(boolean showFront) {
        Platform.runLater(() -> {
            STRIP.addCardSorted(currentCard, currentCard.song.year);
            currentCard.setDraggable(false);

            if (showFront)
                currentCard.showFront();

            GAME.handleCardMove(STRIP.getCards());
        });
    }

    /**
     * Adds a new Hitmaster Chip to chip pane of the current player.
     */
    public void addHitmasterChip() {
        Platform.runLater(() -> CHIP_PANE.addChip());
    }

    /**
     * Removes a Hitmaster Chip from chip pane of the current player.
     */
    public void removeHitmasterChip() {
        Platform.runLater(() -> CHIP_PANE.removeChip());
    }

    /**
     * Removes ALL Hitmaster chips from chip pane of the current player.
     */
    public void removeAllHitmasterChips() {
        while (CHIP_PANE.getActiveChipsCount() > 0) {
            CHIP_PANE.removeChip();
        }
    }

    public void skipCard() {
        if (currentCard == null)
            return;

        Platform.runLater(() -> {
            currentCard.showFront();
            currentCard.setBorderColor("rgb(255, 0, 0)");

            final SongCard wrongCard = this.currentCard; 
    
            if (wrongCard == null) return;

            double sceneX = wrongCard.localToScene(0, 0).getX();
            double sceneY = wrongCard.localToScene(0, 0).getY();
            
            Point2D localPos = this.sceneToLocal(sceneX, sceneY);

            STRIP.removeCard(wrongCard, false); 

            if (wrongCard.getParent() != this) {
                this.getChildren().add(wrongCard);
            }

            wrongCard.setLayoutX(localPos.getX());
            wrongCard.setLayoutY(localPos.getY());
            wrongCard.setTranslateX(0);
            wrongCard.setTranslateY(0);
            wrongCard.toFront();

            DISCARD_PILE.discardCard(wrongCard);
        });
    }

    public void setTimerForOpponent(int time) {
        if (timerUnit != null)
            timerUnit.stop();

        timerUnit = new Timer(time);
        timerUnit.start(
            () -> Platform.runLater(() ->
                CHAT.setRemainingTime(timerUnit.getRemainingSeconds())
            ),
            () -> Platform.runLater(() -> {
                //
            })
        );
    }

    /**
     * Enables or disables all input methods for Player that owns this view.
     * @param enabled Property if inputs should be enabled or not.
     */
    private void setInputsEnabled(boolean enabled) {
        ARTIST.setDisable(!enabled);
        TITLE.setDisable(!enabled);

        SUBMIT.setDisable(!enabled);
        PLAYPAUSE.setDisable(!enabled);
        BACKWARD.setDisable(!enabled);
        FORWARD.setDisable(!enabled);
        RESTART.setDisable(!enabled);
        SKIP.setDisable(!enabled);
        INSERT.setDisable(!enabled);
    }

    public SongCard getCurrentSongCard() {
        return currentCard;
    }

    public List<Card> getCardsFromStrip() {
        return STRIP.getCards();
    }

    public GameLogic getGameLogic() {
        return GAME;
    }


    // ==============================
    // Multiplayer methods
    // ==============================

    /**
     * Initializes OpponentPane with oponnent Card Strip, Avatar and Username.
     * @param opponent Player object of opponent.
     */
    public void initializeOpponentPane(Player opponent) {
        OPPONENT_PANE = new OpponentPane(opponent.username, opponent.img);
        OPPONENT_PANE.setLayoutX(0);
        OPPONENT_PANE.setLayoutY(20);
        OPPONENT_PANE.prefWidthProperty().bind(this.widthProperty());
        
        this.getChildren().add(OPPONENT_PANE);
    }

    /**
     * Swaps current player's Card Strip with opponent.
     */
    public void switchSideWithOpponent() {
        if (!GAME.isMultiplayer())
            return;

        Platform.runLater(() -> {
            // 1) Clear area of current player
            STRIP.clear();
            this.removeAllHitmasterChips();

            // 2) Set new player progress for current player
            for (Song song : GAME.getCurrentPlayer().songs) {
                SongCard card = new SongCard(this, song);
                card.showFront();
                STRIP.addCard(card);
            }

            for (int i = 0; i < GAME.getCurrentPlayer().hitmasterPoints; i++) {
                this.addHitmasterChip();
            }

            // 3) Set new opponent progress
            OPPONENT_PANE.updateAvatar(GAME.getPreviousPlayer().username, GAME.getPreviousPlayer().img);
            OPPONENT_PANE.setName(GAME.getPreviousPlayer().username);
            OPPONENT_PANE.setSongs(GAME.getPreviousPlayer().songs);
            OPPONENT_PANE.setChipsCount(GAME.getPreviousPlayer().hitmasterPoints);
        });
    }

    public void updateOpponentCards(List<Song> songs) {
        Platform.runLater(() -> OPPONENT_PANE.setSongs(songs));
    }

    public void addOpponentCard(Song song) {
        Platform.runLater(() -> OPPONENT_PANE.addSong(song));
    }

    public void addOpponentChip() {
        Platform.runLater(() -> OPPONENT_PANE.addChip());
    }

    public void removeOpponentChip() {
        Platform.runLater(() -> OPPONENT_PANE.removeChip());
    }

    public void removeAllOpponentChips() {
        while (OPPONENT_PANE.getChipsCount() > 0) {
            Platform.runLater(() -> OPPONENT_PANE.removeChip());
        }
    }

    public OpponentPane getOpponentPane() {
        return this.OPPONENT_PANE;
    }

    public void showStealOverlay() {
        Platform.runLater(() -> {
            StealOverlayPane overlay = new StealOverlayPane(this, 5);
            this.getChildren().add(overlay);

            // Use opponent timer since timer is already handled in overlay
            this.setTimerForOpponent(GAME.getGameOptions().stealTime);
        });
    }

    public void stealSkip() {
        GAME.sendObject("OPPONENT_STEAL_SKIP");
    }

    public void stealButtonPressed() {
        GAME.startStealAction();
    }

    /**
     * Starts a new stealing action by the opponent player.
     * Opponent gets to pick a different position than current player.
     * If opponent's guess was right, they get the SongCard instead of current player.
     * Called when button "Steal?" gets pressed.
     */
    public void startStealAction() {
        // 1) Skip if opponent doesn't have enough chips or game isn't multiplayer
        if (GAME.getPreviousPlayer().hitmasterPoints < 1 || !GAME.isMultiplayer())
            return;

        GAME.getPreviousPlayer().hitmasterPoints--;
        OPPONENT_PANE.removeChip();

        // 2) Set steal state
        isStealing = true;
        CHAT.addInfoMessage(GAME.getPreviousPlayer().username + " is attempting to steal!");

        timerUnit.stop();
        currentCard.setStealState(false);

        // 3) Set timer (Load steal duration from Game Options)
        timerUnit = new Timer(GAME.getGameOptions().stealTime);
        timerUnit.start(
            () -> Platform.runLater(() ->
                CHAT.setRemainingTime(timerUnit.getRemainingSeconds())
            ),
            () -> Platform.runLater(() -> {
                if (stealCard != null) {
                    stealCard.setVisible(true);
                    this.getChildren().remove(stealCard);

                    isStealing = false;
                    currentCard.setStealState(false);
                    this.confirmInput();
                }
            })
        );
        
        // 3) Add new SongCard to steal
        stealCard = new PlayerCard(GAME.getPreviousPlayer());
        stealCard.setVisible(true);
        stealCard.setLayoutX(500); 
        stealCard.setLayoutY(50);
        stealCard.toFront();
        STRIP.registerExternalCard(stealCard);
        this.getChildren().add(stealCard);
    }

    /**
     * Check if opponent's steal attempt was successful.
     * Returns if opponent card doesn't have a valid steal position.
     */
    private void checkStealGuess() {
        // 1) Check valid input position
        if (!this.checkValidStealPosition())
            return;

        isStealing = false;
        timerUnit.stop();

        // 2) Check position of steal card
        boolean guess = GAME.checkStealOrder(STRIP.getCards());
        if (guess && !GAME.checkSongOrder(STRIP.getSongCards())) {
            stealCard.setBorderColor("rgb(0, 255, 0)");
            GAME.addCardToPlayerSorted(GAME.getPreviousPlayer());
        }
        else {
            stealCard.setBorderColor("rgb(255, 0, 0)");
        }

        // 3) Reveal guesses
        this.confirmInput(); 
    }

    /**
     * Checks if stealCard has a valid position.
     * stealCard is not allowed to be directly next to currentCard.
     * @return Validation result.
     */
    private boolean checkValidStealPosition() {
        // 1) Get list of song cards
        List<Card> cards = STRIP.getCards();

        // 2) Look for currentCard index
        int currentCardIdx = -1;
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).equals(currentCard)) {
                currentCardIdx = i;
                break;
            }
        }

        // 3) Loook for stealCard index
        int stealCardIdx = -1;
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).equals(stealCard)) {
                stealCardIdx = i;
                break;
            }
        }

        // 4) Return if position is valid
        if (currentCardIdx == -1 || stealCardIdx == -1)
            return false;

        return (!(stealCardIdx - 1 == currentCardIdx) && !(stealCardIdx + 1 == currentCardIdx));
    }

    public void paintOpponentCard(Song currentSong, String cssColor) {
        Platform.runLater(() -> {
            OPPONENT_PANE.paintOpponentCard(currentSong, cssColor);
        });
    }

    public void resetOpponentCard() {
        Platform.runLater(() -> {
            OPPONENT_PANE.resetOpponentCard();
        });
    }

    public void addPlayerMessage(Player player, String message) {
        Platform.runLater(() -> CHAT.addPlayerMessage(player, message));
    }

    public void addInfoMessage(String message) {
        Platform.runLater(() -> CHAT.addInfoMessage(message));
    }

    public void addSuccessMessage(String message) {
        Platform.runLater(() -> CHAT.addSuccessMessage(message));
    }

    public void addErrorMessage(String message) {
        Platform.runLater(() -> CHAT.addErrorMessage(message));
    }

    /**
     * Adds a song as a SongCard to discard pile.
     * Directly shows Card front.
     * @param song Song to put in discard Pile.
     */
    public void addNewCardToDiscard(Song song) {
        Platform.runLater(() -> {
            final SongCard wrongCard = new SongCard(this, song);

            double sceneX = wrongCard.localToScene(0, 0).getX();
            double sceneY = wrongCard.localToScene(0, 0).getY();
            Point2D localPos = this.sceneToLocal(sceneX, sceneY);

            STRIP.removeCard(wrongCard, false);
            if (wrongCard.getParent() != this) {
                this.getChildren().add(wrongCard);
            }
            wrongCard.setLayoutX(localPos.getX());
            wrongCard.setLayoutY(localPos.getY());
            wrongCard.setTranslateX(0);
            wrongCard.setTranslateY(0);
            wrongCard.toFront();

            wrongCard.showFront();

            DISCARD_PILE.setDiscardedCard(wrongCard);
        });
    }
}
