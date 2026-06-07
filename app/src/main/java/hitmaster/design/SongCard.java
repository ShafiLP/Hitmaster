package hitmaster.design;

import hitmaster.models.Song;
import hitmaster.services.MusicPlayer;
import javafx.geometry.Point2D;
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
    public boolean isPlaying = false;
    public boolean isFlipped = false;

    private double mouseX;
    private double mouseY;

    public final MusicPlayer player;

    private Runnable dragStarted;
    private DragListener dragListener;
    private Runnable dragFinished;

    public interface DragListener {
        void onDrag(double sceneX, double sceneY);
    }

    public SongCard(Song song) {
        this.song = song;
        player = new MusicPlayer();

        this.getStylesheets().add(
            getClass().getResource("/styles/app.css").toExternalForm()
        );

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

        this.showBack();
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

        rewind5.getStyleClass().add("control-button");
        playPause.getStyleClass().add("control-button");
        forward5.getStyleClass().add("control-button");
        restart.getStyleClass().add("control-button");

        // --- Play/Pause Toggle Logic ---
        playPause.setOnAction(e -> {
            isPlaying = !isPlaying;

            if (isPlaying) {
                playPause.setText("⏸");
                player.play(song);
            } else {
                playPause.setText("►");
                player.pause();
            }
        });
 
        rewind5.setOnAction(e -> player.seekBackward5sec());
        forward5.setOnAction(e -> player.seekForward5sec());
        restart.setOnAction(e -> player.restart(song));

        // --- Layout ---
        VBox bottomControls = new VBox(8);
        bottomControls.setStyle("-fx-alignment: center;");

        bottomControls.getChildren().addAll(playPause, restart);

        HBox topControlls = new HBox(10);
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
    public void showFront() {
        this.isFlipped = true;

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

        // TODO: Set Icon 
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
        this.setOnMousePressed((MouseEvent e) -> {
            if (isFlipped)
                return;

            mouseX = e.getX();
            mouseY = e.getY();

            this.toFront();

            if (dragStarted != null)
                dragStarted.run();
        });

        this.setOnMouseDragged((MouseEvent e) -> {
            if (isFlipped)
                return;

            if (getParent() != null) {
                Point2D localParam = getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
                setLayoutX(localParam.getX() - mouseX);
                setLayoutY(localParam.getY() - mouseY);
            }

            if (dragListener != null) {
                dragListener.onDrag(e.getSceneX(), e.getSceneY());
            }
        });

        this.setOnMouseReleased(e -> {
            if (isFlipped)
                return;

            if (dragFinished != null)
                dragFinished.run();
        });
    }

    public void setOnDragStarted(Runnable dragStarted) {
        this.dragStarted = dragStarted;
    }

    public void setOnDragged(DragListener dragListener) {
        this.dragListener = dragListener;
    }

    public void setOnDragFinished(Runnable dragFinished) {
        this.dragFinished = dragFinished;
    }
}

