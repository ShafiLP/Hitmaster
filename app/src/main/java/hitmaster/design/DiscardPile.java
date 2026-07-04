package hitmaster.design;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class DiscardPile extends StackPane {

    private final StackPane cardContainer;

    public DiscardPile() {
        this.setStyle("-fx-border-color: rgba(255, 0, 0, 0.4); " +
                      "-fx-border-dash-array: 6 6; " +
                      "-fx-border-width: 2; " +
                      "-fx-background-color: rgba(255, 0, 0, 0.03); " +
                      "-fx-border-radius: 12; " +
                      "-fx-background-radius: 12;");
        
        this.setPrefHeight(200);
        this.setMinHeight(Region.USE_PREF_SIZE);
        this.setMaxHeight(Region.USE_PREF_SIZE);

        this.setMaxWidth(Double.MAX_VALUE);

        Label label = new Label("DISCARD PILE");
        label.setStyle("-fx-text-fill: rgba(255, 0, 0, 0.6); " +
                      "-fx-font-size: 11px; " +
                      "-fx-font-weight: bold; " +
                      "-fx-font-family: 'Arial'; " +
                      "-fx-letter-spacing: 1px;");

        StackPane.setAlignment(label, Pos.TOP_CENTER);
        StackPane.setMargin(label, new Insets(8, 0, 0, 0));

        cardContainer = new StackPane();
        cardContainer.setAlignment(Pos.CENTER);

        this.getChildren().addAll(label, cardContainer);
    }

    /**
     * Animates the movement of a card to discard pile smoothly.
     * Old card gets deleted after animation.
     */
    public void discardCard(SongCard card) {
        if (card == null) return;

        // 1) Calculates target destination from current SongCard position
        double cardWidth = card.getBoundsInLocal().getWidth() > 0 ? card.getBoundsInLocal().getWidth() : 140;
        double cardHeight = card.getBoundsInLocal().getHeight() > 0 ? card.getBoundsInLocal().getHeight() : 200;

        double pileSceneX = this.localToScene(0, 0).getX();
        double pileSceneY = this.localToScene(0, 0).getY();

        Point2D pileLocalInGameView = card.getParent().sceneToLocal(pileSceneX, pileSceneY);

        double targetX = (pileLocalInGameView.getX() + (this.getWidth() - cardWidth) / 2) - card.getLayoutX();
        double targetY = (pileLocalInGameView.getY() + (this.getHeight() - cardHeight) / 2) - card.getLayoutY();

        // 2) Animate
        TranslateTransition transition = new TranslateTransition(Duration.millis(500), card);
        transition.setToX(targetX);
        transition.setToY(targetY);

        // 3) Clear after animation
        transition.setOnFinished(e -> {
            card.setTranslateX(0);
            card.setTranslateY(0);

            card.setMaxSize(cardWidth, cardHeight);
            card.setPrefSize(cardWidth, cardHeight);

            cardContainer.getChildren().clear();
            cardContainer.getChildren().add(card);
        });

        transition.play();
    }

    /**
     * Animates the movement of a card to discard pile smoothly.
     * Old card gets deleted after animation.
     */
    public void setDiscardedCard(Card card) {
        if (card == null)
            return;

        double cardWidth = card.getBoundsInLocal().getWidth() > 0 ? card.getBoundsInLocal().getWidth() : 140;
        double cardHeight = card.getBoundsInLocal().getHeight() > 0 ? card.getBoundsInLocal().getHeight() : 150;

        card.setTranslateX(0);
        card.setTranslateY(0);

        card.setManaged(false);

        card.resize(cardWidth, cardHeight);

        card.setLayoutX((this.getWidth() - cardWidth) / 2);
        card.setLayoutY((this.getHeight() - cardHeight) / 2);
        
        cardContainer.getChildren().clear();
        cardContainer.getChildren().add(card);
    }
}