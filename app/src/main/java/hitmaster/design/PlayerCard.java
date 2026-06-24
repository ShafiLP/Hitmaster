package hitmaster.design;

import hitmaster.models.Player;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

public class PlayerCard extends Card {

    private final Player PLAYER;

    /**
     * Constructor for PlayerCard.
     * Sets Card background and places player profile picture as well as their username.
     * @param player Player object with profile picture and username to place on Card.
     */
    public PlayerCard(Player player) {
        super();
        this.PLAYER = player;

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
        double imgSize = 75;

        if (PLAYER != null && PLAYER.img != null) {
            ImageView profileImage = new ImageView(PLAYER.img);
            profileImage.setFitWidth(imgSize);
            profileImage.setFitHeight(imgSize);
            profileImage.setPreserveRatio(true);

            // Kreisförmig ausschneiden
            Circle clip = new Circle(imgSize / 2, imgSize / 2, imgSize / 2);
            profileImage.setClip(clip);

            StackPane.setAlignment(profileImage, Pos.CENTER);
            layout.getChildren().add(profileImage);
        } 
        else {
            String initial = "?";
            if (PLAYER != null && PLAYER.username != null && !PLAYER.username.isBlank()) {
                initial = PLAYER.username.substring(0, 1).toUpperCase();
            }

            Circle avatarCircle = new Circle(imgSize / 2);
            avatarCircle.setFill(hitmaster.design.PastelColor.random()); 

            Label avatarLetterLabel = new Label(initial);
            avatarLetterLabel.setStyle("""
                -fx-font-size: 28px;
                -fx-text-fill: white;
                -fx-font-weight: bold;
            """);

            StackPane.setAlignment(avatarCircle, Pos.CENTER);
            StackPane.setAlignment(avatarLetterLabel, Pos.CENTER);

            layout.getChildren().addAll(avatarCircle, avatarLetterLabel);
        }

        // 4) Username Text
        Label displayText = new Label(player != null ? player.username : "");
        displayText.setStyle("""
            -fx-font-size: 16px;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        """);

        // 5) Set Layout
        StackPane.setAlignment(displayText, Pos.CENTER);
        displayText.setTranslateY((imgSize / 2) + 25); 
        layout.getChildren().add(displayText);

        this.getChildren().clear();
        this.getChildren().add(layout);
    }

    /**
     * Gets the Player who owns the card and returns it.
     * @return Player that owns the card.
     */
    public Player getCardOwner() {
        return PLAYER;
    }
}
