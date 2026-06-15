package hitmaster.design;

import java.util.ArrayList;
import java.util.List;

import hitmaster.models.Song;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class OpponentPane extends HBox {

    private final ImageView avatarView;
    private final Label nameLabel;
    private final ChipPane chipPane;
    private final Pane cardRowPane;

    private final List<Song> opponentSongs = new ArrayList<>();
    
    // Einstellungen für die kleinere Kartenreihe des Gegners
    private final double CARD_SIZE = 100.0; 
    private final double CARD_GAP = 10.0;

    public OpponentPane(String opponentName, Image avatarImage) {
        super(20); // Abstand zwischen Profil-Sektion und Kartenreihe
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(10, 20, 10, 20));
        
        // Hintergrund-Styling passend zum Modern-Look
        this.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03);" +
                      "-fx-border-color: rgba(255, 255, 255, 0.05);" +
                      "-fx-border-width: 0 0 1 0;"); // Trennlinie nach unten

        // ==========================================
        // PROFIL- & SPIELERINFO-SEKTION (Links)
        // ==========================================
        avatarView = new ImageView();
        avatarView.setFitWidth(60);
        avatarView.setFitHeight(60);
        avatarView.setPreserveRatio(true);
        
        // Rundes Profilbild via Clip erzwingen
        Rectangle avatarClip = new Rectangle(60, 60);
        avatarClip.setArcWidth(60);
        avatarClip.setArcHeight(60);
        avatarView.setClip(avatarClip);
        
        try {
            // Falls du Bilder als Resource oder URL lädst:
            avatarView.setImage(avatarImage);
        } catch (Exception e) {
            // Fallback, falls kein Bild gefunden wird (grauer Kreis)
            avatarView.setStyle("-fx-background-color: #444444;");
        }

        nameLabel = new Label(opponentName);
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        chipPane = new ChipPane();
        // Optionale optische Anpassung: Da der Gegner oben sitzt, Chips leicht verkleinern
        chipPane.setScaleX(0.85);
        chipPane.setScaleY(0.85);

        VBox infoContainer = new VBox(4, nameLabel, chipPane);
        infoContainer.setAlignment(Pos.CENTER_LEFT);

        HBox profileSection = new HBox(12, avatarView, infoContainer);
        profileSection.setAlignment(Pos.CENTER_LEFT);

        // ==========================================
        // KARTENREIHE-SEKTION (Rechts, dynamisch)
        // ==========================================
        cardRowPane = new Pane();
        cardRowPane.setPrefHeight(CARD_SIZE);
        HBox.setHgrow(cardRowPane, Priority.ALWAYS);

        // Listener für dynamischen Zeilen-Layout bei Fenstergrößenänderung
        cardRowPane.widthProperty().addListener((obs, oldVal, newVal) -> refreshCardLayout());

        // Alles zusammenfügen
        this.getChildren().addAll(profileSection, cardRowPane);
    }

    /**
     * Setzt die komplette Kartenreihe neu und zeichnet sie.
     */
    public void setSongs(List<Song> songs) {
        this.opponentSongs.clear();
        this.opponentSongs.addAll(songs);
        refreshCardLayout();
    }

    /**
     * Fügt eine einzelne Karte hinzu (wichtig für die "nacheinander dran" Funktionen)
     */
    public void addSong(Song song) {
        this.opponentSongs.add(song);
        refreshCardLayout();
    }

    public void addChip() { chipPane.addChip(); }
    public void removeChip() { chipPane.removeChip(); }
    public int getChipsCount() { return chipPane.getActiveChipsCount(); }

    /**
     * Berechnet die Positionen der kleineren Vorschaukarten (ohne Interaktivität)
     */
    private void refreshCardLayout() {
        cardRowPane.getChildren().clear();
        int totalCards = opponentSongs.size();
        if (totalCards == 0) return;

        double availableWidth = cardRowPane.getWidth();
        double neededWidth = totalCards * CARD_SIZE + (totalCards - 1) * CARD_GAP;
        
        double currentSize = CARD_SIZE;
        double currentGap = CARD_GAP;

        // Wenn der Platz eng wird, skalieren wir wie in deiner CardStripPane die Karten kleiner
        if (neededWidth > availableWidth && availableWidth > 0) {
            double scale = availableWidth / (neededWidth + 10);
            currentSize = CARD_SIZE * scale;
            currentGap = CARD_GAP * scale;
        }

        double startX = 0; // Linksbündig in der Reihe neben dem Profil

        for (int i = 0; i < totalCards; i++) {
            Song song = opponentSongs.get(i);
            
            // Wir bauen eine vereinfachte, nicht-interaktive SongCard-Vorschau
            StackPane miniCard = createMiniCard(song, currentSize);
            miniCard.setLayoutX(startX + i * (currentSize + currentGap));
            miniCard.setLayoutY((cardRowPane.getHeight() - currentSize) / 2.0);
            
            cardRowPane.getChildren().add(miniCard);
        }
    }

    /**
     * Erzeugt eine visuelle, kleinere Repräsentation einer SongCard
     * Komplett statisch, ohne Drag-and-Drop Event-Handler.
     */
    private StackPane createMiniCard(Song song, double size) {
        StackPane cardRoot = new StackPane();
        cardRoot.setPrefSize(size, size);
        
        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        cardRoot.setClip(clip);

        // Simuliert das Aussehen der Vorderseite (aufgedeckt)
        VBox layout = new VBox();
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(2));
        
        // Nutzt zufällige Pastellfarbe wie die echte SongCard
        Color cardColor = PastelColor.random(); 
        layout.setStyle(String.format(
            "-fx-background-color: rgb(%d,%d,%d);" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;" +
            "-fx-border-color: black;" +
            "-fx-border-width: 1px;",
            (int)(cardColor.getRed() * 255),
            (int)(cardColor.getGreen() * 255),
            (int)(cardColor.getBlue() * 255)
        ));

        Label yearLabel = new Label(String.valueOf(song.year));
        // Schriftgröße dynamisch an die Kartengröße anpassen
        yearLabel.setStyle("-fx-font-size: " + (size * 0.25) + "px; -fx-font-weight: bold; -fx-text-fill: black;");
        
        Label titleLabel = new Label(song.titles.getFirst());
        titleLabel.setStyle("-fx-font-size: " + (size * 0.09) + "px; -fx-text-fill: black;");
        titleLabel.setWrapText(false);

        layout.getChildren().addAll(yearLabel, titleLabel);
        cardRoot.getChildren().add(layout);
        
        return cardRoot;
    }

    public void setName(String newName) {
        nameLabel.setText(newName);
    }

    public void setAvatar(Image newAvatar) {
        try {
            avatarView.setImage(newAvatar);
        }
        catch (Exception e) {
            avatarView.setStyle("-fx-background-color: #444444;");
        }
    }

    public void setChipsCount(int count) {
        while (chipPane.getActiveChipsCount() > 0) {
            chipPane.removeChip();
        }
        for (int i = 0; i < count; i++) {
            chipPane.addChip();
        }
    }
    
    // Lokale Hilfsklasse für Farben, falls PastelColor nicht statisch zugänglich ist
    private static class PastelColor {
        public static Color random() {
            java.util.Random rand = new java.util.Random();
            double r = (rand.nextInt(50) + 150) / 255.0;
            double g = (rand.nextInt(50) + 150) / 255.0;
            double b = (rand.nextInt(50) + 150) / 255.0;
            return Color.color(r, g, b);
        }
    }
}