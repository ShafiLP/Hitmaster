package hitmaster.design;

import java.util.ArrayList;
import java.util.List;

import hitmaster.models.Song;
import hitmaster.services.Log;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
    private final List<MiniSongCard> miniSongCards = new ArrayList<>();
    
    private final double CARD_SIZE = 100.0; 
    private final double CARD_GAP = 10.0;

    public OpponentPane(String opponentName, Image avatarImage) {
        super(20);
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(10, 20, 10, 20));
        
        this.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03);" +
                      "-fx-border-color: rgba(255, 255, 255, 0.05);" +
                      "-fx-border-width: 0 0 1 0;");

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

        miniSongCards.clear();
        cardRowPane.getChildren().clear();

        for (Song song : songs) {
            MiniSongCard card = createMiniCard(song, CARD_SIZE);
            miniSongCards.add(card);
            cardRowPane.getChildren().add(card);
        }

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

        MiniSongCard card = this.createMiniCard(song, CARD_SIZE);
        this.miniSongCards.add(card);
        
        cardRowPane.getChildren().add(card);

        this.refreshCardLayout();
    }

    public void addChip() { chipPane.addChip(); }
    public void removeChip() { chipPane.removeChip(); }
    public int getChipsCount() { return chipPane.getActiveChipsCount(); }

    /**
     * Calculates positions of smaller preview SongCards.
     */
    private void refreshCardLayout() {
        int totalCards = opponentSongs.size();
        if (totalCards == 0) return;
        
        double currentSize = CARD_SIZE;
        double currentGap = CARD_GAP;

        for (int i = 0; i < totalCards; i++) {
            MiniSongCard card = miniSongCards.get(i);

            card.setLayoutX(i * (currentSize + currentGap));
            card.setLayoutY((cardRowPane.getHeight() - currentSize) / 2.0);
        }
    }

    /**
     * Creates a smaller preview version of SongCard.
     * Completely static without drag & drop handler.
     */
    private MiniSongCard createMiniCard(Song song, double size) {
        return new MiniSongCard(song, size, avatarView.getImage(), avatarCircle.getFill(), avatarLetterLabel.getText());
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

    public void paintOpponentCard(Song song, String cssColor) {
        // 1) Search for song
        for (int i = 0; i < miniSongCards.size(); i++) {
            if (miniSongCards.get(i).getSong().id == song.id) {
                // 2) Paint border of card
                miniSongCards.get(i).paintBorder(cssColor);
                return;
            }
        }

        Log.Error("Couldn't find song with ID " + song.id + " in Opponent Pane. Couldn't paint border.");
    }

    public void resetOpponentCard() {
        for (int i = 0; i < miniSongCards.size(); i++) {
            miniSongCards.get(i).paintBorder("black");
        }
    }
}
