package hitmaster.design;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class CardStripPane extends Pane {

    private final List<SongCard> cards = new ArrayList<>();
    private int originalIndex = -1;
    private int insertIndex = -1;

    private final double CARDWIDTH = 150; 
    private final double CARDHEIGHT = 150;
    private final double HGAP = 20;
    private final Line marker = new Line();

    public CardStripPane() {
        widthProperty().addListener((obs, oldVal, newVal) -> layoutCards());
        heightProperty().addListener((obs, oldVal, newVal) -> layoutCards());

        marker.setStroke(Color.WHITE);
        marker.setStrokeWidth(5);
        marker.getStrokeDashArray().addAll(6.0, 4.0);
        marker.setVisible(false);
        this.getChildren().add(marker);

        this.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        this.getStyleClass().add("card-strip");
    }

    public void addCard(SongCard card) {
        if (!cards.contains(card)) cards.add(card);
        if (!getChildren().contains(card)) getChildren().add(card);
        registerExternalCard(card);
        layoutCards();
    }

    public void registerExternalCard(SongCard card) {
        card.setOnDragStarted(() -> {
            originalIndex = cards.indexOf(card);
            if (originalIndex >= 0) {
                cards.remove(card);
            }
            marker.setVisible(false);
            layoutCards();
        });

        card.setOnDragged((sceneX, sceneY) -> {
            Point2D localPoint = sceneToLocal(sceneX, sceneY);
            
            if (isWithinStripBounds(localPoint.getX(), localPoint.getY())) {
                insertIndex = calculateInsertionIndex(localPoint.getX());
                updateMarkerPosition(insertIndex);
            } else {
                insertIndex = -1;
                marker.setVisible(false);
            }
        });

        card.setOnDragFinished(() -> {
            marker.setVisible(false);
            
            if (insertIndex >= 0) {
                if (card.getParent() != this) {
                    if (card.getParent() != null) {
                        ((Pane) card.getParent()).getChildren().remove(card);
                    }
                    getChildren().add(card);
                }
                cards.add(insertIndex, card);
            } else {
                if (originalIndex >= 0) {
                    Point2D scenePoint = card.localToScene(0, 0);
                    Pane gameView = (Pane) getParent();
                    
                    getChildren().remove(card);
                    
                    if (gameView != null && !gameView.getChildren().contains(card)) {
                        gameView.getChildren().add(card);
                        Point2D localRoot = gameView.sceneToLocal(scenePoint);
                        card.setLayoutX(localRoot.getX());
                        card.setLayoutY(localRoot.getY());
                    }
                }
            }
            insertIndex = -1;
            originalIndex = -1;
            layoutCards();
        });
    }

    private boolean isWithinStripBounds(double localX, double localY) {
        return localX >= 0 && localX <= getWidth() && localY >= 0 && localY <= getHeight();
    }

    private int calculateInsertionIndex(double localX) {
        int totalCards = cards.size();
        if (totalCards == 0) return 0;

        double totalWidth = totalCards * CARDWIDTH + (totalCards - 1) * HGAP;
        double startX = (getWidth() - totalWidth) / 2.0;

        for (int i = 0; i < totalCards; i++) {
            double cardCenterX = startX + i * (CARDWIDTH + HGAP) + (CARDWIDTH / 2.0);
            if (localX < cardCenterX) return i;
        }
        return totalCards;
    }

    private void updateMarkerPosition(int index) {
        int totalCards = cards.size();
        double totalWidth = totalCards * CARDWIDTH + (totalCards - 1) * HGAP;
        double startX = (getWidth() - totalWidth) / 2.0;
        double stripY = (getHeight() - CARDHEIGHT) / 2.0;

        double markerX;
        if (totalCards == 0) {
            markerX = getWidth() / 2.0;
        } else if (index < totalCards) {
            markerX = startX + index * (CARDWIDTH + HGAP) - (HGAP / 2.0);
        } else {
            markerX = startX + totalCards * (CARDWIDTH + HGAP) - (HGAP / 2.0) + (HGAP / 2.0);
        }

        marker.setStartX(markerX);
        marker.setStartY(stripY - 10);
        marker.setEndX(markerX);
        marker.setEndY(stripY + CARDHEIGHT + 10);
        marker.toFront();
        marker.setVisible(true);
    }

    private void layoutCards() {
        int totalCards = cards.size();
        double totalWidth = totalCards * CARDWIDTH + (totalCards - 1) * HGAP;
        double startX = (getWidth() - totalWidth) / 2.0;
        double stripY = (getHeight() - CARDHEIGHT) / 2.0;

        for (int i = 0; i < totalCards; i++) {
            SongCard card = cards.get(i);
            double x = startX + i * (CARDWIDTH + HGAP);
            card.relocate(x, stripY);
        }
    }
}