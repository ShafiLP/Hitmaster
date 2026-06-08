package hitmaster.views;

import hitmaster.GameLogic;
import hitmaster.design.CardStripPane;
import hitmaster.design.SongCard;
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
    private TextField artist;
    private TextField title;
    private final Label TIMER;


    public GameView(GameLogic GAME, Song firstSong) {
        this.GAME = GAME;
        currentCard = new SongCard(firstSong);

        // 2) Initialize CardStripPane
        final double CONTROLS_WIDTH = 320;
        final double GAP = 20;

        STRIP = new CardStripPane();
        STRIP.setPrefHeight(200);

        STRIP.prefWidthProperty().bind(this.widthProperty().subtract(CONTROLS_WIDTH + (GAP * 3)));
        STRIP.layoutYProperty().bind(this.heightProperty().subtract(STRIP.prefHeightProperty()).subtract(GAP));
        STRIP.setLayoutX(GAP);
        this.getChildren().add(STRIP);

        // 3) Text fields and button
        artist = new TextField();
        artist.getStyleClass().add("modern-textbox");
        artist.setPromptText("Artist...");

        title = new TextField();
        title.getStyleClass().add("modern-textbox");
        title.setPromptText("Song title...");

        Button flip = new Button("Flip");
        flip.getStyleClass().add("primary-button");
        flip.setMaxWidth(Double.MAX_VALUE);
        flip.setOnAction(e -> {
            confirmInput();
        });

        VBox inputs = new VBox(8, artist, title, flip);

        // 4) Spotify media control
        Button back = new Button("⏮");
        Button playPause = new Button("⏸");
        Button forward = new Button("⏭");
        Button restart = new Button("↺");

        back.getStyleClass().add("modern-button");
        playPause.getStyleClass().add("modern-button");
        forward.getStyleClass().add("modern-button");
        restart.getStyleClass().add("modern-button");

        back.setOnAction(e -> {
            currentCard.player.seekBackward5sec();
        });
        playPause.setOnAction(e -> {
            currentCard.player.playPause(currentCard.song);
        });
        forward.setOnAction(e -> {
            currentCard.player.seekForward5sec();
        });
        restart.setOnAction(e -> {
            currentCard.player.restart(currentCard.song);
        });

        HBox mediaRow = new HBox(15, back, playPause, forward, restart);
        mediaRow.setAlignment(Pos.CENTER);

        ComboBox<String> deviceDropdown = new ComboBox<>();
        deviceDropdown.getStyleClass().add("modern-dropdown");
        deviceDropdown.setPromptText("Device...");
        deviceDropdown.setPrefWidth(130);
        deviceDropdown.getItems().addAll(currentCard.player.getAvailableDevices());
        deviceDropdown.getSelectionModel().select(currentCard.player.getCurrentDevice());
        deviceDropdown.setOnAction(e -> {
            currentCard.player.setCurrentDevice(deviceDropdown.getValue());
        });

        Slider volumeSlider = new Slider(0, 100, currentCard.player.getVolume());
        volumeSlider.getStyleClass().add("modern-slider");
        volumeSlider.setOnMouseReleased(e -> {
            currentCard.player.setVolume((int) volumeSlider.getValue());
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

        // 6) Build full panel
        VBox controlPanel = new VBox(15, inputs, mediaRow, audioRow);
        controlPanel.setPrefWidth(CONTROLS_WIDTH);
        
        controlPanel.layoutXProperty().bind(this.widthProperty().subtract(CONTROLS_WIDTH + GAP));
        controlPanel.layoutYProperty().bind(this.heightProperty().subtract(controlPanel.heightProperty().add(GAP)));
        
        this.getChildren().add(controlPanel);
        Platform.runLater(this::requestFocus);
    }

    private void confirmInput() {
        // 1) Flip card
        if (currentCard != null)
            currentCard.showFront();

        // 2) Check artist and title guess
        if (GAME.checkSongInformation(artist.getText(), title.getText())) {
            artist.setStyle("-fx-border-color:rgb(0, 255, 0);");
            title.setStyle("-fx-border-color:rgb(0, 255, 0);");
        }
        else {
            artist.setStyle("-fx-border-color:rgb(255, 0, 0);");
            title.setStyle("-fx-border-color:rgb(255, 0, 0);");
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

        artist.setStyle("");
        artist.getStyleClass().add("modern-textbox");
        artist.clear();

        title.setStyle("");
        title.getStyleClass().add("modern-textbox");
        title.clear();

        currentCard.resetBorderColor();

        // 2) Move card to failure stack if false
        if (!guess) {
            Platform.runLater(() -> {
                STRIP.removeCard(currentCard);
                this.getChildren().remove(currentCard);
                GAME.addFirstToCardStack();
            });
        }

        // 3) Check for win if true
        else {
            if (GAME.checkForWin(STRIP.getCards())) {
                Log.Info("WON.");
            }
            else {
                Platform.runLater(() -> {
                    GAME.addFirstToCardStack();
                });
            }
        }
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
        SongCard card = new SongCard(song);
        card.showFront();
        STRIP.addCard(card);
    }

    public void addToCardStack(Song song) {
        SongCard card = new SongCard(song);
        card.setLayoutX(500); 
        card.setLayoutY(50);

        STRIP.registerExternalCard(card);

        currentCard = card;

        this.getChildren().add(currentCard);
        currentCard.toFront();
    }
}