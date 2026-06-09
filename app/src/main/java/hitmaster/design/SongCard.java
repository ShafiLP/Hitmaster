package hitmaster.design;

import hitmaster.models.Song;
import hitmaster.services.MusicPlayer;
import hitmaster.views.GameView;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
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

    private final GameView VIEW;
    public final MusicPlayer PLAYER;

    private Runnable dragStarted;
    private DragListener dragListener;
    private Runnable dragFinished;

    private Button playPause;

    public interface DragListener {
        void onDrag(double sceneX, double sceneY);
    }

    public SongCard(GameView view, Song song) {
        this.VIEW = view;
        this.song = song;
        PLAYER = new MusicPlayer();

        color = PastelColor.random();

        this.setPrefSize(150, 150);

        Rectangle clip = new Rectangle(150, 150);
        clip.setArcWidth(24);
        clip.setArcHeight(24);

        this.setClip(clip);

        this.getStyleClass().add("song-card");

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

        playPause = new Button("►");
        playPause.getStyleClass().add("control-button");
        playPause.setStyle("""
            -fx-font-size: 28px;
        """);

        playPause.setOnAction(e -> {
            VIEW.playPause();
        });

        // TODO: Add background

        VBox controls = new VBox(5, playPause);
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
        Label artist = new Label(song.artist);
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

    public void setBorderColor(String color) {
        this.setStyle("-fx-border-radius: 12; -fx-border-color:" + color + ";");
    }

    public void resetBorderColor() {
        this.setStyle("-fx-border-radius: 12; -fx-border-color:black;");
    }

    public void togglePlayPause() {
        if (playPause.getText().equals("⏸")) {
            playPause.setText("►");
            PLAYER.pause();
        } else {
            playPause.setText("⏸");
            PLAYER.play(song);
        }
    }
}

