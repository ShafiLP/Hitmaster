package hitmaster.design;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import hitmaster.models.Song;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public final class OpponentPane extends HBox {

    private final StackPane avatarContainer;
    private final ImageView avatarView;
    private final Label avatarLetterLabel;
    private final Circle avatarCircle;
    private final Label nameLabel;
    private final ChipPane chipPane;
    private final Pane cardRowPane;

    private final List<Song> opponentSongs = new ArrayList<>();
    
    private final double CARD_SIZE = 100.0; 
    private final double CARD_GAP = 10.0;

    private final Color COLOR;

    public OpponentPane(String opponentName, Image avatarImage) {
        super(20);
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(10, 20, 10, 20));
        
        this.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03);" +
                      "-fx-border-color: rgba(255, 255, 255, 0.05);" +
                      "-fx-border-width: 0 0 1 0;");

        COLOR = PastelColor.random();

        // ==========================================
        // PLAYER INFORMATION
        // ==========================================
        avatarView = new ImageView();
        avatarView.setFitWidth(60);
        avatarView.setFitHeight(60);
        avatarView.setPreserveRatio(true);
        
        Rectangle avatarClip = new Rectangle(60, 60);
        avatarClip.setArcWidth(60);
        avatarClip.setArcHeight(60);
        avatarView.setClip(avatarClip);

        avatarCircle = new Circle(30);
        avatarLetterLabel = new Label();
        avatarLetterLabel.setStyle("""
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;"
        """); 
        
        avatarContainer = new StackPane();
        avatarContainer.setMinSize(60, 60);
        avatarContainer.setPrefSize(60, 60);
        avatarContainer.setMaxSize(60, 60);

        this.updateAvatar(opponentName, avatarImage);

        nameLabel = new Label(opponentName);
        nameLabel.getStyleClass().add("subheader");

        chipPane = new ChipPane();
        chipPane.setScaleX(0.85);
        chipPane.setScaleY(0.85);

        VBox infoContainer = new VBox(4, nameLabel, chipPane);
        infoContainer.setAlignment(Pos.CENTER);

        HBox profileSection = new HBox(12, avatarContainer, infoContainer);
        profileSection.setAlignment(Pos.CENTER_LEFT);

        // ==========================================
        // SONGCARDS
        // ==========================================
        cardRowPane = new Pane();
        cardRowPane.setPrefHeight(CARD_SIZE);
        HBox.setHgrow(cardRowPane, Priority.ALWAYS);

        cardRowPane.widthProperty().addListener((obs, oldVal, newVal) -> refreshCardLayout());

        this.getChildren().addAll(profileSection, cardRowPane);
    }

    /**
     * Replaces the whole list of SongCards and draws them.
     */
    public void setSongs(List<Song> songs) {
        this.opponentSongs.clear();
        this.opponentSongs.addAll(songs);
        this.refreshCardLayout();
    }

    public List<Song> getSongs() {
        return this.opponentSongs;
    }

    /**
     * Adds a single card to list of SongCards.
     */
    public void addSong(Song song) {
        this.opponentSongs.add(song);
        this.refreshCardLayout();
    }

    public void addChip() { chipPane.addChip(); }
    public void removeChip() { chipPane.removeChip(); }
    public int getChipsCount() { return chipPane.getActiveChipsCount(); }

    /**
     * Calculates positions of smaller preview SongCards.
     */
    private void refreshCardLayout() {
        cardRowPane.getChildren().clear();
        int totalCards = opponentSongs.size();
        if (totalCards == 0) return;

        double availableWidth = cardRowPane.getWidth();
        double neededWidth = totalCards * CARD_SIZE + (totalCards - 1) * CARD_GAP;
        
        double currentSize = CARD_SIZE;
        double currentGap = CARD_GAP;

        if (neededWidth > availableWidth && availableWidth > 0) {
            double scale = availableWidth / (neededWidth + 10);
            currentSize = CARD_SIZE * scale;
            currentGap = CARD_GAP * scale;
        }

        double startX = 0; 

        for (int i = 0; i < totalCards; i++) {
            Song song = opponentSongs.get(i);
            
            StackPane miniCard = createMiniCard(song, currentSize);
            miniCard.setLayoutX(startX + i * (currentSize + currentGap));
            miniCard.setLayoutY((cardRowPane.getHeight() - currentSize) / 2.0);
            
            cardRowPane.getChildren().add(miniCard);
        }
    }

    /**
     * Creates a smaller preview version of SongCard.
     * Completely static without drag & drop handler.
     */
    private StackPane createMiniCard(Song song, double size) {
        StackPane cardRoot = new StackPane();
        cardRoot.setPrefSize(size, size);
        
        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        cardRoot.setClip(clip);

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(2));

        if (song == null || song.artists == null || song.artists.isEmpty()) {
            layout.setStyle(
                "-fx-background-color: #111111;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-border-color: #333333;" +
                "-fx-border-width: 1.5px;"
            );

            StackPane miniAvatarContainer = new StackPane();
            double miniAvatarSize = size * 0.55;
            miniAvatarContainer.setMinSize(miniAvatarSize, miniAvatarSize);
            miniAvatarContainer.setPrefSize(miniAvatarSize, miniAvatarSize);
            miniAvatarContainer.setMaxSize(miniAvatarSize, miniAvatarSize);

            if (avatarView.getImage() != null) {
                ImageView miniAvatarView = new ImageView(avatarView.getImage());
                miniAvatarView.setFitWidth(miniAvatarSize);
                miniAvatarView.setFitHeight(miniAvatarSize);
                miniAvatarView.setPreserveRatio(true);

                Rectangle miniClip = new Rectangle(miniAvatarSize, miniAvatarSize);
                miniClip.setArcWidth(miniAvatarSize);
                miniClip.setArcHeight(miniAvatarSize);
                miniAvatarView.setClip(miniClip);

                miniAvatarContainer.getChildren().add(miniAvatarView);
            } else {
                Circle miniCircle = new Circle(miniAvatarSize / 2.0);
                miniCircle.setFill(avatarCircle.getFill());

                Label miniLetterLabel = new Label(avatarLetterLabel.getText());
                miniLetterLabel.setStyle("-fx-font-size: " + (miniAvatarSize * 0.45) + "px; -fx-font-weight: bold; -fx-text-fill: white;");

                miniAvatarContainer.getChildren().addAll(miniCircle, miniLetterLabel);
            }

            layout.setCenter(miniAvatarContainer);
        }
        else {
            layout.setStyle(String.format(
                "-fx-background-color: rgb(%d,%d,%d);" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-border-color: black;" +
                "-fx-border-width: 1px;",
                (int)(COLOR.getRed() * 255),
                (int)(COLOR.getGreen() * 255),
                (int)(COLOR.getBlue() * 255)
            ));

            Label artistLabel = new Label(song.artists.getFirst());
            artistLabel.setStyle("-fx-font-size: " + (size * 0.09) + "px; -fx-text-fill: black;");
            artistLabel.setWrapText(false);
            StackPane top = new StackPane(artistLabel);

            Label yearLabel = new Label(String.valueOf(song.year));
            yearLabel.setStyle("-fx-font-size: " + (size * 0.25) + "px; -fx-font-weight: bold; -fx-text-fill: black;");
            
            Label titleLabel = new Label(song.titles.getFirst());
            titleLabel.setStyle("-fx-font-size: " + (size * 0.09) + "px; -fx-text-fill: black;");
            titleLabel.setWrapText(false);
            StackPane bottom = new StackPane(titleLabel);

            layout.setTop(top);
            layout.setCenter(yearLabel);
            layout.setBottom(bottom);
        }

        cardRoot.getChildren().add(layout);
        
        return cardRoot;
    }

    /**
     * Updates avatar image.
     * If image is null, use username to create a new default avatar image
     * containing first letter of username and a coloured background.
     * @param username Username to use if image is null.
     * @param image Image to set avatar.
     */
    public void updateAvatar(String username, Image image) {
        avatarContainer.getChildren().clear();

        if (image != null) {
            avatarView.setImage(image);
            avatarContainer.getChildren().add(avatarView);
        }
        else {
            String initial = "?";

            if (username != null && !username.isBlank())
                initial = username.substring(0, 1).toUpperCase();

            avatarLetterLabel.setText(initial);
            avatarCircle.setFill(PastelColor.random());

            avatarContainer.getChildren().addAll(avatarCircle, avatarLetterLabel);
        }
    }

    /**
     * Sets a new username for this OpponentPane.
     * @param newName New username.
     */
    public void setName(String newName) {
        nameLabel.setText(newName);
    }

    /**
     * Removes all chips from chip pane and sets a new count of chips.
     * @param count New count of chips.
     */
    public void setChipsCount(int count) {
        while (chipPane.getActiveChipsCount() > 0) {
            chipPane.removeChip();
        }

        for (int i = 0; i < count; i++) {
            chipPane.addChip();
        }
    }
    
    private static class PastelColor {
        public static Color random() {
            Random rand = new Random();
            double r = (rand.nextInt(50) + 150) / 255.0;
            double g = (rand.nextInt(50) + 150) / 255.0;
            double b = (rand.nextInt(50) + 150) / 255.0;
            return Color.color(r, g, b);
        }
    }
}