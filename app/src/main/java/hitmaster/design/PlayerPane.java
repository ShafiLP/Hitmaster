package hitmaster.design;

import java.util.ArrayList;
import java.util.List;

import hitmaster.models.Player;
import hitmaster.models.Song;
import hitmaster.services.Log;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public final class PlayerPane extends HBox {

    private StackPane avatarContainer;
    private Label nameLabel;
    private ChipPane chipPane;
    private Pane cardRowPane;

    private Player player;

    private final List<Song> playerSongs = new ArrayList<>();
    private final List<MiniSongCard> miniSongCards = new ArrayList<>();
    
    private final double CARD_SIZE = 100.0; 
    private final double CARD_GAP = 10.0;

    /**
     * Constructor for PlayerPane.
     * Calls super method of HBox with 20px spacing.
     */
    public PlayerPane() {
        super(20);
    }

    /**
     * Initializes PlayerPane.
     * Creates layout and sets player name and avatar.
     * @param player Player object with name and avatar.
     */
    public void initialize(Player player) {
        this.player = player;
        if (player == null) player = new Player("notSet", "notSet");

        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(10, 20, 10, 20));
        
        setStyle("-fx-background-color: rgba(255, 255, 255, 0.03);" +
                 "-fx-border-color: rgba(255, 255, 255, 0.05);" +
                 "-fx-border-width: 0 0 1 0;");

        // ===== PLAYER INFORMATION =====
        avatarContainer = new StackPane();
        avatarContainer.setMinSize(60, 60);
        avatarContainer.setPrefSize(60, 60);
        avatarContainer.setMaxSize(60, 60);

        updateAvatar(player);

        nameLabel = new Label(player.username);
        nameLabel.getStyleClass().add("subheader");

        chipPane = new ChipPane();
        chipPane.setScaleX(0.85);
        chipPane.setScaleY(0.85);

        VBox infoContainer = new VBox(4, nameLabel, chipPane);
        infoContainer.setAlignment(Pos.CENTER);

        HBox profileSection = new HBox(12, avatarContainer, infoContainer);
        profileSection.setAlignment(Pos.CENTER_LEFT);

        // ===== SONGCARDS =====
        cardRowPane = new Pane();
        cardRowPane.setPrefHeight(CARD_SIZE);
        HBox.setHgrow(cardRowPane, Priority.ALWAYS);

        cardRowPane.widthProperty().addListener((obs, oldVal, newVal) -> refreshCardLayout());

        getChildren().addAll(profileSection, cardRowPane);
    }

    /**
     * Replaces the whole list of SongCards and draws them.
     */
    public void setSongs(List<Song> songs) {
        playerSongs.clear();
        playerSongs.addAll(songs);

        miniSongCards.clear();
        cardRowPane.getChildren().clear();

        for (Song song : songs) {
            MiniSongCard card = createMiniCard(song, CARD_SIZE);
            miniSongCards.add(card);
            cardRowPane.getChildren().add(card);
        }

        refreshCardLayout();
    }

    /**
     * Gets all songs inside list of songs and returns them.
     * @return List<Song> All songs assigned to player of this pane.
     */
    public List<Song> getSongs() {
        return playerSongs;
    }

    /**
     * Adds a single card to list of SongCards.
     * Added at end of list if list is not empty.
     */
    public void addSong(Song song) {
        playerSongs.add(song);

        MiniSongCard card = createMiniCard(song, CARD_SIZE);
        miniSongCards.add(card);
        
        cardRowPane.getChildren().add(card);

        refreshCardLayout();
    }

    public void addChip() { chipPane.addChip(); }
    public void removeChip() { chipPane.removeChip(); }
    public int getChipsCount() { return chipPane.getActiveChipsCount(); }

    /**
     * Calculates positions of smaller preview SongCards.
     */
    private void refreshCardLayout() {
        int totalCards = playerSongs.size();
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
        if (player == null) 
            return new MiniSongCard(song, size, null, Color.GRAY, "?");
        

        return new MiniSongCard(
            song, 
            size, 
            player.img, 
            player.color != null ? player.color : Color.GRAY, 
            player.initial != null ? player.initial : "?"
        );
    }

    /**
     * Updates avatar image.
     * If image is null, use username to create a new default avatar image
     * containing first letter of username and a coloured background.
     * @param player Player object containing username and image.
     */
    public void updateAvatar(Player player) {
        this.player = player;
        avatarContainer.getChildren().clear();

        if (player != null) {
            StackPane playerAvatar = player.getPlayerImage(60);
            avatarContainer.getChildren().add(playerAvatar);
        }
    }

    /**
     * Sets a new username for this PlayerPane.
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

    public void paintPlayerCard(Song song, String cssColor) {
        // 1) Search for song
        for (int i = 0; i < miniSongCards.size(); i++) {
            if (miniSongCards.get(i).getSong().id == song.id) {
                // 2) Paint border of card
                miniSongCards.get(i).paintBorder(cssColor);
                return;
            }
        }

        Log.Error("Couldn't find song with ID " + song.id + " in Player Pane. Couldn't paint border.");
    }

    public void resetPlayerCard() {
        for (int i = 0; i < miniSongCards.size(); i++) {
            miniSongCards.get(i).paintBorder("black");
        }
    }
}
