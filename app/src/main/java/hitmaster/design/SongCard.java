package hitmaster.design;

import java.util.List;

import hitmaster.models.Player;
import hitmaster.models.Set;
import hitmaster.models.Song;
import hitmaster.views.GameView;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public final class SongCard extends StackPane {

    public Song song;
    public Color color;
    public boolean isPlaying = false;
    public boolean isDraggable = true;

    private double mouseX;
    private double mouseY;

    private final GameView VIEW;

    private Runnable dragStarted;
    private DragListener dragListener;
    private Runnable dragFinished;

    private Button playPause;
    private Button stealButton;

    public interface DragListener {
        void onDrag(double sceneX, double sceneY);
    }

    public SongCard(GameView view, Song song) {
        this.VIEW = view;
        this.song = song;

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
     * Steal Button is shown here when playing multiplayer.
     */
    public void showBack() {
        // 1) Prepare Layout
        BorderPane layout = new BorderPane();
        layout.setPrefSize(this.getPrefWidth(), this.getPrefHeight());
        layout.setStyle(
            "-fx-background-color: black;" + 
            "-fx-padding: 10px;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;"
        );

        // 2) Set background image
        ImageView backgroundImage = new ImageView(new Image(getClass().getResourceAsStream("/cardDesignBlank.png")));
        backgroundImage.setPreserveRatio(false);

        backgroundImage.fitWidthProperty().bind(layout.prefWidthProperty());
        backgroundImage.fitHeightProperty().bind(layout.prefHeightProperty());

        layout.getChildren().add(backgroundImage);

        // 3) Play/Pause Button
        playPause = new Button("►");
        playPause.getStyleClass().add("control-button");
        playPause.setStyle("""
            -fx-font-size: 28px;
        """);

        playPause.setOnAction(e -> {
            VIEW.playPause();
        });

        // 4) Steal Button
        stealButton = new Button("Steal?");
        stealButton.getStyleClass().add("secondary-button");
        stealButton.setPrefWidth(this.getPrefWidth() / 2);
        stealButton.setMinWidth(Region.USE_PREF_SIZE);
        stealButton.setPrefHeight(30);
        stealButton.setVisible(false);

        stealButton.setOnAction(e -> {
            VIEW.startStealAction();
        });

        // 5) Set Layout
        StackPane controls = new StackPane();
        StackPane.setAlignment(playPause, Pos.CENTER);
        StackPane.setAlignment(stealButton, Pos.CENTER);
        
        stealButton.setTranslateY(45);
        controls.getChildren().addAll(playPause, stealButton);

        layout.setCenter(controls);

        this.getChildren().clear();
        this.getChildren().add(layout);
    }

    /**
     * Displays front side of the song card.
     * Front side contains artist, year and title.
     */
    public void showFront() {
        this.isDraggable = false;

        // 1) Prepare Layout
        StackPane root = new StackPane();
        root.setPrefSize(this.getPrefWidth(), this.getPrefHeight());
        root.setStyle(String.format(
            "-fx-background-color: rgb(%d,%d,%d);" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-padding: 5px;",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255)
        ));

        VBox textBox = new VBox(2);
        textBox.setAlignment(Pos.CENTER);
        textBox.setStyle("-fx-padding: 0 6px 0 6px;"); 
        textBox.setPrefWidth(this.getPrefWidth());
        StackPane.setAlignment(textBox, Pos.CENTER);

        // ARTIST
        Label artist = new Label(song.artists.getFirst());
        artist.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        artist.setWrapText(true);
        artist.setMaxWidth(Double.MAX_VALUE);
        artist.setAlignment(Pos.CENTER);
        artist.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // YEAR
        Label year = new Label(String.valueOf(song.year));
        year.setStyle("-fx-font-size: 38px; -fx-font-weight: bold;");
        year.setMaxWidth(Double.MAX_VALUE);
        year.setAlignment(Pos.CENTER);

        // TITLE
        Label title = new Label(song.titles.getFirst());
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);
        title.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        title.setMinHeight(Region.USE_PREF_SIZE);
        title.setMaxHeight(title.getFont().getSize() * 2.6);

        title.setOnMouseEntered(e -> {
            if (title.getText().length() > 20) {
                Tooltip.install(title, new Tooltip(title.getText()));
            }
        });

        textBox.getChildren().addAll(artist, year, title);
        root.getChildren().add(textBox);

        // =========================
        // ICON OVERLAY (TOP LEFT)
        // =========================
        List<Set> songSets = song.getActiveSongSets();

        if (!songSets.isEmpty()) {
            VBox iconLayout = new VBox(1);
            iconLayout.setAlignment(Pos.TOP_CENTER);
            iconLayout.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            iconLayout.setMouseTransparent(false);

            iconLayout.setTranslateX(2);
            iconLayout.setTranslateY(2);

            ImageView icon = songSets.getFirst().getIcon();
            icon.setFitWidth(18);
            icon.setFitHeight(18);
            icon.setPreserveRatio(true);
            iconLayout.getChildren().add(icon);

            if (songSets.size() > 1) {
                Label moreSets = new Label("+" + (songSets.size() - 1));
                moreSets.setStyle("""
                    -fx-font-size: 9px;
                    -fx-font-weight: bold;
                    -fx-text-fill: white;
                    -fx-alignment: center;
                """);
                iconLayout.getChildren().add(moreSets);
            }

            String allSets = songSets.stream()
                .map(s -> s.name != null ? s.name : "")
                .collect(java.util.stream.Collectors.joining("\n"));

            Tooltip tooltip = new Tooltip(allSets);
            tooltip.setStyle("-fx-font-size: 11px;"); 
            Tooltip.install(iconLayout, tooltip);

            StackPane.setAlignment(iconLayout, Pos.TOP_LEFT);
            root.getChildren().add(iconLayout);
        }

        this.getChildren().setAll(root);
    }

    /**
     * Turns the SongCard into a steal card.
     * Displays a players profile picture and username.
     * Used for opponent to steal a card.
     * @param player Player object containing profile picture and username.
     */
    public void showStealInfo(Player player) {
        // 1) Base Container
        StackPane layout = new StackPane();
        layout.setPrefSize(this.getPrefWidth(), this.getPrefHeight());

        // 2) Set background image
        ImageView backgroundImage = new ImageView(new Image(getClass().getResourceAsStream("/cardDesignBlank.png")));
        backgroundImage.setPreserveRatio(false);

        backgroundImage.fitWidthProperty().bind(layout.prefWidthProperty());
        backgroundImage.fitHeightProperty().bind(layout.prefHeightProperty());

        layout.getChildren().add(backgroundImage);

        // 3) Initialize Profile Picture
        ImageView profileImage = new ImageView(player.img);
        double imgSize = 75; // Image size
        profileImage.setFitWidth(imgSize);
        profileImage.setFitHeight(imgSize);
        profileImage.setPreserveRatio(true);
        Circle clip = new Circle(imgSize / 2, imgSize / 2, imgSize / 2);
        profileImage.setClip(clip);
        StackPane.setAlignment(profileImage, Pos.CENTER);
        layout.getChildren().add(profileImage);

        // 3) "IS STEALING" Text
        Label displayText = new Label(player.username + " IS STEALING!");
        displayText.setStyle("""
            -fx-font-size: 16px;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        """);

        // 4) Set Layout
        StackPane.setAlignment(displayText, Pos.CENTER);
        displayText.setTranslateY((imgSize / 2) + 25); 
        layout.getChildren().add(displayText);

        this.getChildren().clear();
        this.getChildren().add(layout);
    }

    public void setStealState(boolean state) {
        stealButton.setVisible(state);
    }
    
    /**
     * Enables drag and drop for this object.
     */
    private void enableDragging() {
        this.setOnMousePressed((MouseEvent e) -> {
            if (!isDraggable)
                return;

            mouseX = e.getX();
            mouseY = e.getY();

            this.toFront();

            if (dragStarted != null)
                dragStarted.run();
        });

        this.setOnMouseDragged((MouseEvent e) -> {
            if (!isDraggable)
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
            if (!isDraggable)
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

    public void setDraggable(boolean draggable) {
        this.isDraggable = draggable;
    }

    public void setBorderColor(String color) {
        this.setStyle("-fx-border-radius: 12; -fx-border-color:" + color + ";");
    }

    public void resetBorderColor() {
        this.setStyle("-fx-border-radius: 12; -fx-border-color:black;");
    }

    public void togglePlayPause() {
        playPause.setText(playPause.getText().equals("⏸") ? "►" : "⏸");
    }
}

