package hitmaster.design;

import hitmaster.models.Song;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class SongCard extends StackPane {
    public Song song;
    public Color color;
    public boolean isFlipped = false;

    private double mouseX;
    private double mouseY;

    public SongCard(Song song) {
        this.song = song;

        color = PastelColor.random();

        this.setPrefSize(150, 150);

        Rectangle clip = new Rectangle(150, 150);
        clip.setArcWidth(24);
        clip.setArcHeight(24);

        this.setClip(clip);

        this.setStyle(
            "-fx-background-color: black;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: black;"
        );

        this.showFront();
        this.enableDragging();
    }

    /**
     * Displays back side of the song card.
     * Back side contains UI to control the playing song.
     */
    private void showBack() {
        BorderPane layout = new BorderPane();
        layout.setPrefSize(this.getPrefWidth(), this.getPrefHeight());
        layout.setStyle(
            "-fx-background-color: black;" + 
            "-fx-padding: 10px;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;"
        );

        // --- Buttons (nur Icons) ---
        Button rewind5 = new Button("<<");
        Button forward5 = new Button(">>");
        Button playPause = new Button("►");
        Button restart = new Button("↺");

        String buttonStyle = """
            -fx-background-color: transparent;
            -fx-text-fill: white;
            -fx-font-size: 11px;
        """;

        rewind5.setStyle(buttonStyle);
        playPause.setStyle(buttonStyle);
        forward5.setStyle(buttonStyle);
        restart.setStyle(buttonStyle);

        // --- Play/Pause Toggle Logic ---
        final boolean[] isPlaying = {false};

        playPause.setOnAction(e -> {
            isPlaying[0] = !isPlaying[0];

            if (isPlaying[0]) {
                playPause.setText("⏸"); // pause symbol
                //songPlayer.play();
            } else {
                playPause.setText("►"); // play symbol
                //songPlayer.pause();
            }
        });

        // TODO: actions
        //rewind5.setOnAction(e -> songPlayer.seekBackward(5));
        //forward5.setOnAction(e -> songPlayer.seekForward(5));
        //playPause.setOnAction(e -> songPlayer.togglePlayPause());
        //restart.setOnAction(e -> songPlayer.restart());

        // --- Layout ---
        VBox bottomControls = new VBox(8);
        bottomControls.setStyle("-fx-alignment: center;");

        bottomControls.getChildren().addAll(playPause, restart);

        HBox topControlls = new HBox(15);
        topControlls.setStyle("-fx-alignment: center;");
        topControlls.getChildren().addAll(rewind5, playPause, forward5);

        VBox controls = new VBox(5, topControlls, bottomControls);
        controls.setStyle("-fx-alignment: center;");

        layout.setCenter(controls);

        this.getChildren().clear();
        this.getChildren().add(layout);
    }

    /**
     * Displays front side of the song card.
     * Front side contains artist, year and title.
     */
    private void showFront() {
        BorderPane layout = new BorderPane();
        layout.setPrefSize(this.getPrefWidth(), this.getPrefHeight());
        layout.setStyle(String.format(
            "-fx-background-color: rgb(%d,%d,%d);" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-padding: 5px;",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255)
        ));

        Label artist = new Label(song.getArtist().name);
        Label year = new Label(String.valueOf(song.year));
        Label title = new Label(song.title);

        artist.setStyle("-fx-font-weight: bold;");
        year.setStyle("-fx-font-size: 40px; -fx-font-weight: bold;");

        StackPane top = new StackPane(artist);
        StackPane bottom = new StackPane(title);

        layout.setTop(top);
        layout.setCenter(year);
        layout.setBottom(bottom);

        this.getChildren().clear();
        this.getChildren().add(layout);
    }
    
    /**
     * Enables drag and drop for this object.
     */
    private void enableDragging() {
        setOnMousePressed((MouseEvent e) -> {
            mouseX = e.getSceneX() - getLayoutX();
            mouseY = e.getSceneY() - getLayoutY();
        });

        setOnMouseDragged((MouseEvent e) -> {
            setLayoutX(e.getSceneX() - mouseX);
            setLayoutY(e.getSceneY() - mouseY);
        });

        //! DEBUG:
        setOnMouseClicked((MouseEvent e) -> {
            if (isFlipped) {
                showBack();
                isFlipped = !isFlipped;
            }
            else {
                showFront();
                isFlipped = !isFlipped;
            }
        });
    }
}
