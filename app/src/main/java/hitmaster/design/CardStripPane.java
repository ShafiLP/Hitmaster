package hitmaster.design;

import java.util.ArrayList;
import java.util.List;

import hitmaster.views.GameView;
import javafx.animation.TranslateTransition;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;

public class CardStripPane extends Pane {
    
    private final GameView PARENT;
    private final List<SongCard> cards = new ArrayList<>();
    private int originalIndex = -1;
    private int insertIndex = -1;

    private final double BASE_CARDWIDTH = 150;
    private final double CARDHEIGHT = 150;
    private final double BASE_HGAP = 20;
    
    private double currentCardWidth = BASE_CARDWIDTH;
    private double currentHGap = BASE_HGAP;

    private final Line marker = new Line();

    public CardStripPane(GameView PARENT) {
        this.PARENT = PARENT;

        this.widthProperty().addListener((obs, oldVal, newVal) -> layoutCards());
        this.heightProperty().addListener((obs, oldVal, newVal) -> layoutCards());

        marker.setStroke(Color.WHITE);
        marker.setStrokeWidth(5);
        marker.getStrokeDashArray().addAll(6.0, 4.0);
        marker.setVisible(false);
        this.getChildren().add(marker);

        this.getStyleClass().add("card-strip");
    }

    public void addCard(SongCard card) {
        if (!cards.contains(card)) {
            cards.add(card);
        }
        layoutCards();
    }

    public void registerExternalCard(SongCard card) {
        card.setOnDragStarted(() -> {
            originalIndex = cards.indexOf(card);
            if (originalIndex >= 0) {
                cards.remove(card);
            }
            marker.setVisible(false);
            this.layoutCards();
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
                    
                    this.getChildren().remove(card);
                    
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
            this.layoutCards();

            PARENT.getGameLogic().handleCardMove(cards);
        });
    }

    public void removeCard(SongCard card, boolean removeFromParent) {
        if (card == null) return;

        cards.remove(card);

        this.getChildren().remove(card);
        
        if (removeFromParent && getParent() != null && getParent() instanceof Pane) {
            ((Pane) getParent()).getChildren().remove(card);
        }

        originalIndex = -1;
        insertIndex = -1;
        marker.setVisible(false);

        requestLayout(); 
        layoutCards();
    }

    private boolean isWithinStripBounds(double localX, double localY) {
        return localX >= 0 && localX <= getWidth() && localY >= 0 && localY <= getHeight();
    }

    private int calculateInsertionIndex(double localX) {
        int totalCards = cards.size();
        if (totalCards == 0) return 0;

        double totalWidth = totalCards * currentCardWidth + (totalCards - 1) * currentHGap;
        double startX = (getWidth() - totalWidth) / 2.0;

        for (int i = 0; i < totalCards; i++) {
            double cardCenterX = startX + i * (currentCardWidth + currentHGap) + (currentCardWidth / 2.0);
            if (localX < cardCenterX) return i;
        }
        return totalCards;
    }

    /**
     * Adds a card automatically at the index where it fits according to song.year
     * and animates it moving to that indexed position.
     */
    public void addCardSorted(SongCard card) {
        if (card == null) return;
        
        // 1) Calculate index
        int targetIndex = 0;
        int newCardYear = card.song.year;

        for (int i = 0; i < cards.size(); i++) {
            if (newCardYear >= cards.get(i).song.year) {
                targetIndex = i + 1;
            } else {
                break;
            }
        }

        // 2) Calculate coordinates
        double initialSceneX = card.localToScene(0, 0).getX();
        double initialSceneY = card.localToScene(0, 0).getY();

        if (card.getParent() != null && card.getParent() != this) {
            ((Pane) card.getParent()).getChildren().remove(card);
        }

        if (!cards.contains(card)) {
            cards.add(targetIndex, card);
        }
        
        layoutCards();

        // 3) Animate movement to CardStripPane
        animateCardToPosition(card, initialSceneX, initialSceneY);
    }

    /**
     * Moves a card into CardStripPane smoothly.
     */
    private void animateCardToPosition(SongCard card, double fromSceneX, double fromSceneY) {
        double targetX = card.getLayoutX();
        double targetY = card.getLayoutY();

        Point2D localStart = sceneToLocal(fromSceneX, fromSceneY);

        card.setTranslateX(localStart.getX() - targetX);
        card.setTranslateY(localStart.getY() - targetY);

        TranslateTransition transition = new TranslateTransition(Duration.millis(500), card);
        transition.setToX(0);
        transition.setToY(0);
        transition.play();
    }

    private void updateMarkerPosition(int index) {
        int totalCards = cards.size();
        double totalWidth = totalCards * currentCardWidth + (totalCards - 1) * currentHGap;
        double startX = (getWidth() - totalWidth) / 2.0;
        double stripY = (getHeight() - CARDHEIGHT) / 2.0;

        double markerX;
        if (totalCards == 0) {
            markerX = getWidth() / 2.0;
        } else if (index < totalCards) {
            markerX = startX + index * (currentCardWidth + currentHGap) - (currentHGap / 2.0);
        } else {
            markerX = startX + totalCards * (currentCardWidth + currentHGap) - (currentHGap / 2.0) + (currentHGap / 2.0);
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
        if (totalCards == 0) return;

        double availableWidth = getWidth();
        double neededWidthIfNormal = totalCards * BASE_CARDWIDTH + (totalCards - 1) * BASE_HGAP;

        if (neededWidthIfNormal > availableWidth && availableWidth > 0) {
            double scaleFactor = availableWidth / (neededWidthIfNormal + 20);
            currentCardWidth = BASE_CARDWIDTH * scaleFactor;
            currentHGap = BASE_HGAP * scaleFactor;
        } else {
            currentCardWidth = BASE_CARDWIDTH;
            currentHGap = BASE_HGAP;
        }

        double totalWidth = totalCards * currentCardWidth + (totalCards - 1) * currentHGap;
        double startX = (availableWidth - totalWidth) / 2.0;
        double stripY = (getHeight() - CARDHEIGHT) / 2.0;

        for (int i = 0; i < totalCards; i++) {
            SongCard card = cards.get(i);

            card.setPrefSize(currentCardWidth, CARDHEIGHT);
            
            if (card.getClip() instanceof javafx.scene.shape.Rectangle rectangle) {
                rectangle.setWidth(currentCardWidth);
            }

            double x = startX + i * (currentCardWidth + currentHGap);
            card.relocate(x, stripY);

            if (!this.getChildren().contains(card)) {
                this.getChildren().add(card);
            }
        }

        marker.toFront();
    }

    public void clear() {
        this.getChildren().removeAll(cards);
        
        cards.clear();
        
        originalIndex = -1;
        insertIndex = -1;
        
        marker.setVisible(false);
        
        currentCardWidth = BASE_CARDWIDTH;
        currentHGap = BASE_HGAP;

        this.requestLayout();
    }

    public List<SongCard> getCards() {
        return cards;
    }
}