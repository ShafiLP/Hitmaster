package hitmaster.views;

import java.util.List;
import java.util.Random;

import hitmaster.design.CardStripPane;
import hitmaster.design.SongCard;
import hitmaster.models.Song;
import hitmaster.services.Database;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GameView extends Pane {

    private SongCard currentCard;

    public GameView() {
        // 1) Stylesheets laden
        this.getStylesheets().add(
            getClass().getResource("/styles/app.css").toExternalForm()
        );
        this.getStyleClass().add("app-background");

        // 2) Initialize CardStripPane
        final double CONTROLS_WIDTH = 320;
        final double GAP = 20;

        CardStripPane strip = new CardStripPane();
        strip.setPrefHeight(200);

        strip.prefWidthProperty().bind(this.widthProperty().subtract(CONTROLS_WIDTH + (GAP * 3)));
        strip.layoutYProperty().bind(this.heightProperty().subtract(strip.prefHeightProperty()).subtract(GAP));
        strip.setLayoutX(GAP);
        this.getChildren().add(strip);

        // 3) Load songs from DB
        List<Song> songs = Database.getAllSongs();
        Random random = new Random();
        Song randomSong = songs.get(random.nextInt(songs.size()));

        currentCard = new SongCard(randomSong);
        currentCard.setLayoutX(500); 
        currentCard.setLayoutY(50);

        strip.registerExternalCard(currentCard);

        // 4) Text fields and button
        TextField artist = new TextField();
        artist.getStyleClass().add("modern-textbox");
        artist.setPromptText("Artist...");

        TextField title = new TextField();
        title.getStyleClass().add("modern-textbox");
        title.setPromptText("Song title...");

        Button flip = new Button("Flip");
        flip.getStyleClass().add("primary-button");
        flip.setMaxWidth(Double.MAX_VALUE);
        flip.setOnAction(e -> {
            if (currentCard != null)
                currentCard.showFront();
        });

        VBox inputs = new VBox(8, artist, title, flip);

        // 5) Spotify media control
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

        // 6) Build full panel
        VBox controlPanel = new VBox(15, inputs, mediaRow, audioRow);
        // controlPanel.getStyleClass().add();
        controlPanel.setPrefWidth(CONTROLS_WIDTH);
        
        controlPanel.layoutXProperty().bind(this.widthProperty().subtract(CONTROLS_WIDTH + GAP));
        controlPanel.layoutYProperty().bind(this.heightProperty().subtract(controlPanel.heightProperty().add(GAP)));
        
        this.getChildren().add(controlPanel);

        //! 7) DEBUG: Add three cards to strip
        for (int i = 0; i < 3; i++) {
            SongCard tempCard = new SongCard(songs.get(i));
            tempCard.showFront();
            strip.addCard(tempCard);
        }

        this.getChildren().addAll(currentCard);
        Platform.runLater(this::requestFocus);
    }
}