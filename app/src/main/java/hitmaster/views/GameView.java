package hitmaster.views;

import java.util.List;

import hitmaster.GameLogic;
import hitmaster.design.CardStripPane;
import hitmaster.design.ChipPane;
import hitmaster.design.DiscardPile;
import hitmaster.design.OpponentPane;
import hitmaster.design.SongCard;
import hitmaster.models.Player;
import hitmaster.models.Song;
import hitmaster.services.Log;
import hitmaster.services.Timer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GameView extends Pane {

    private final GameLogic GAME;

    private SongCard currentCard;

    // UI elements
    private final CardStripPane STRIP;
    private final DiscardPile DISCARD_PILE;
    private final ChipPane CHIP_PANE;
    private final Label TIMER;
    private final TextField ARTIST;
    private final TextField TITLE;
    private final Button PLAYPAUSE;

    private OpponentPane OPPONENT_PANE;


    public GameView(GameLogic GAME, Song firstSong) {
        this.GAME = GAME;
        currentCard = new SongCard(this, firstSong);

        // 1) Initialize CardStripPane
        final double CONTROLS_WIDTH = 320;
        final double GAP = 20;

        STRIP = new CardStripPane();
        STRIP.setPrefHeight(200);

        STRIP.prefWidthProperty().bind(this.widthProperty().subtract(CONTROLS_WIDTH + (GAP * 3)));
        STRIP.layoutYProperty().bind(this.heightProperty().subtract(STRIP.prefHeightProperty()).subtract(GAP));
        STRIP.setLayoutX(GAP);
        this.getChildren().add(STRIP);

        // 2) Initilize discard pile
        DISCARD_PILE = new DiscardPile();
        DISCARD_PILE.setLayoutX(GAP);
        DISCARD_PILE.layoutYProperty().bind(
            this.heightProperty()
                 .subtract(DISCARD_PILE.prefHeightProperty())
                 .divide(2)
        );
        this.getChildren().add(DISCARD_PILE);

        // 3) Text fields and button
        ARTIST = new TextField();
        ARTIST.getStyleClass().add("modern-textbox");
        ARTIST.setPromptText("Artist...");

        TITLE = new TextField();
        TITLE.getStyleClass().add("modern-textbox");
        TITLE.setPromptText("Song title...");

        Button flip = new Button("Flip");
        flip.getStyleClass().add("primary-button");
        flip.setMaxWidth(Double.MAX_VALUE);
        flip.setOnAction(e -> {
            confirmInput();
        });

        VBox inputs = new VBox(8, ARTIST, TITLE, flip);

        // 4) Spotify media control
        Button back = new Button("⏮");
        PLAYPAUSE = new Button("►");
        Button forward = new Button("⏭");
        Button restart = new Button("↺");
        Button skip = new Button("Skip");
        Button insert = new Button("Insert");

        back.getStyleClass().add("modern-button");
        PLAYPAUSE.getStyleClass().add("modern-button");
        forward.getStyleClass().add("modern-button");
        restart.getStyleClass().add("modern-button");
        skip.getStyleClass().add("modern-button");
        insert.getStyleClass().add("modern-button");

        back.setOnAction(e -> {
            currentCard.PLAYER.seekBackward5sec();
        });
        PLAYPAUSE.setOnAction(e -> {
            playPause();
        });
        forward.setOnAction(e -> {
            currentCard.PLAYER.seekForward5sec();
        });
        restart.setOnAction(e -> {
            currentCard.PLAYER.restart(currentCard.song);
        });
        skip.setOnAction(e -> {
            if (GAME.getChipCountOfCurrentPlayer() >= 1)
                GAME.skipCurrentSong();
        });
        insert.setOnAction(e -> {
            if (GAME.getChipCountOfCurrentPlayer() >= 3)
                GAME.markCurrentSongAsCorrect();
        });

        HBox mediaRow = new HBox(15, back, PLAYPAUSE, forward, restart, skip, insert);
        mediaRow.setAlignment(Pos.CENTER);

        ComboBox<String> deviceDropdown = new ComboBox<>();
        deviceDropdown.getStyleClass().add("modern-dropdown");
        deviceDropdown.setPromptText("Device...");
        deviceDropdown.setPrefWidth(130);
        deviceDropdown.getItems().addAll(currentCard.PLAYER.getAvailableDevices());
        deviceDropdown.getSelectionModel().select(currentCard.PLAYER.getCurrentDevice());
        deviceDropdown.setOnAction(e -> {
            currentCard.PLAYER.setCurrentDevice(deviceDropdown.getValue());
        });

        Slider volumeSlider = new Slider(0, 100, currentCard.PLAYER.getVolume());
        volumeSlider.getStyleClass().add("modern-slider");
        volumeSlider.setOnMouseReleased(e -> {
            currentCard.PLAYER.setVolume((int) volumeSlider.getValue());
        });
        HBox.setHgrow(volumeSlider, Priority.ALWAYS);

        HBox audioRow = new HBox(12, deviceDropdown, volumeSlider);
        audioRow.setAlignment(Pos.CENTER_LEFT);

        // 5) Timer Label
        TIMER = new Label();
        TIMER.getStyleClass().add("modern-label");
        TIMER.setVisible(false);
        TIMER.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            TIMER.setLayoutX((this.getWidth() - newBounds.getWidth()) / 2);
        });

        this.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            TIMER.setLayoutX((newWidth.doubleValue() - TIMER.getWidth()) / 2);
        });

        TIMER.setLayoutY(10);
        this.getChildren().add(TIMER);

        // 6) Chip panel
        CHIP_PANE = new ChipPane();

        // 7) Build full panel
        VBox controlPanel = new VBox(15, CHIP_PANE, inputs, mediaRow, audioRow);
        controlPanel.setPrefWidth(CONTROLS_WIDTH);
        
        controlPanel.layoutXProperty().bind(this.widthProperty().subtract(CONTROLS_WIDTH + GAP));
        controlPanel.layoutYProperty().bind(this.heightProperty().subtract(controlPanel.heightProperty().add(GAP)));
        
        this.getChildren().add(controlPanel);
        Platform.runLater(this::requestFocus);
    }

    public void playPause() {
        currentCard.isPlaying = !currentCard.isPlaying;

        if (currentCard.isPlaying) {
            PLAYPAUSE.setText("⏸");
            currentCard.togglePlayPause();
        } else {
            PLAYPAUSE.setText("►");
            currentCard.togglePlayPause();
        }
    }

    private void confirmInput() {
        // 1) Flip card
        if (currentCard != null)
            currentCard.showFront();

        // 2) Check artist and title guess
        if (GAME.checkSongInformation(ARTIST.getText(), TITLE.getText())) {
            ARTIST.setStyle("-fx-border-color:rgb(0, 255, 0);");
            TITLE.setStyle("-fx-border-color:rgb(0, 255, 0);");
        }
        else {
            ARTIST.setStyle("-fx-border-color:rgb(255, 0, 0);");
            TITLE.setStyle("-fx-border-color:rgb(255, 0, 0);");
        }

        // 3) Check position of card
        boolean guess = GAME.checkSongOrder(STRIP.getCards());
        if (guess) {
            currentCard.setBorderColor("rgb(0, 255, 0)");
        }
        else {
            currentCard.setBorderColor("rgb(255, 0, 0)");
        }

        Timer timer = new Timer(3);
        timer.start(
            () -> Platform.runLater(() -> 
                setTimer(timer.getRemainingSeconds())
            ),
            () -> checkForWin(guess)
        );
    }

    private void checkForWin(boolean guess) {
        // 1) Reset styles
        hideTimer();

        ARTIST.setStyle("");
        ARTIST.getStyleClass().add("modern-textbox");
        ARTIST.clear();

        TITLE.setStyle("");
        TITLE.getStyleClass().add("modern-textbox");
        TITLE.clear();

        currentCard.resetBorderColor();

        // 2) Move card to failure stack if false
        if (!guess) {
            Platform.runLater(() -> {
                final SongCard wrongCard = this.currentCard; 
            
                if (wrongCard == null) return;

                double sceneX = wrongCard.localToScene(0, 0).getX();
                double sceneY = wrongCard.localToScene(0, 0).getY();
                
                javafx.geometry.Point2D localPos = this.sceneToLocal(sceneX, sceneY);

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
                
                GAME.addFirstToCardStack();
            });
        }

        // 3) Check for win if true
        else {
            GAME.addCurrentSongToCurrentPlayer();
            if (GAME.checkForWin(STRIP.getCards())) {
                Log.Info("WON.");
            }
            else {
                Platform.runLater(() -> {
                    GAME.addFirstToCardStack();
                });
            }
        }

        // 4) Swap active player
        GAME.switchToNextPlayer();
    }

    private void setTimer(int seconds) {
        // TODO: Minutes if 60+ seconds
        TIMER.setVisible(true);
        TIMER.setText("⏱ " + (seconds + 1) + "s");
    }

    private void hideTimer() {
        TIMER.setVisible(false);
    }

    public void addToCardStrip(Song song) {
        SongCard card = new SongCard(this, song);
        card.showFront();
        STRIP.addCard(card);
    }

    public void addToCardStack(Song song) {
        SongCard card = new SongCard(this, song);
        card.setLayoutX(500); 
        card.setLayoutY(50);

        STRIP.registerExternalCard(card);

        currentCard = card;

        this.getChildren().add(currentCard);
        currentCard.toFront();
    }

    public void insertCardIntoStrip() {
        STRIP.addCardSorted(currentCard);
    }

    public void addHitmasterChip() {
        CHIP_PANE.addChip();
    }

    public void removeHitmasterChip() {
        CHIP_PANE.removeChip();
    }

    public void removeAllHitmasterChips() {
        while (CHIP_PANE.getActiveChipsCount() > 0) {
            CHIP_PANE.removeChip();
        }
    }

    public void removeCurrentCard() {
        currentCard.showFront();
        currentCard.setBorderColor("rgb(255, 0, 0)");

        Timer timer = new Timer(3);
        timer.start(
            () -> Platform.runLater(() -> 
                setTimer(timer.getRemainingSeconds())
            ),
            () -> Platform.runLater(() -> {
                hideTimer();
                
                final SongCard wrongCard = this.currentCard;
                if (wrongCard != null) {
                    double sceneX = wrongCard.localToScene(0, 0).getX();
                    double sceneY = wrongCard.localToScene(0, 0).getY();
                    javafx.geometry.Point2D localPos = this.sceneToLocal(sceneX, sceneY);

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
                }
                
                GAME.addFirstToCardStack();
            })
        );
    }

    // ==============================
    // Multiplayer methods
    // ==============================

    public void initializeOpponentPane(Player opponent) {
        OPPONENT_PANE = new OpponentPane(opponent.username, opponent.img);
        OPPONENT_PANE.setLayoutX(0);
        OPPONENT_PANE.setLayoutY(0);
        OPPONENT_PANE.prefWidthProperty().bind(this.widthProperty());
        
        this.getChildren().add(OPPONENT_PANE);
    }

    public void switchSideWithOpponent() {
        Platform.runLater(() -> {
            // 1) Clear area of current player
            STRIP.clear();
            removeAllHitmasterChips();

            // 2) Set new player progress for current player
            for (Song song : GAME.getCurrentPlayer().songs) {
                SongCard card = new SongCard(this, song);
                card.showFront();
                STRIP.addCard(card);
            }

            for (int i = 0; i < GAME.getCurrentPlayer().hitmasterPoints; i++) {
                addHitmasterChip();
            }

            // 3) Set new opponent progress
            OPPONENT_PANE.setName(GAME.getPreviousPlayer().username);
            OPPONENT_PANE.setAvatar(GAME.getPreviousPlayer().img);
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

    public void addOpponentChip(Song song) {
        Platform.runLater(() -> OPPONENT_PANE.addChip());
    }

    public void removeOpponentChip(Song song) {
        Platform.runLater(() -> OPPONENT_PANE.removeChip());
    }
}