package hitmaster.views;

import java.util.List;

import hitmaster.GameLogic;
import hitmaster.design.CardStripPane;
import hitmaster.design.SongCard;
import hitmaster.design.StatusBar;
import hitmaster.design.StyleDialog;
import hitmaster.models.Song;
import hitmaster.services.ThemeManager;
import hitmaster.services.Timer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class StealView extends Stage {

    private final GameLogic GAME;

    private final CardStripPane cardStripPane;
    private final StatusBar statusBar;
    private final Button confirmButton;
    private final SongCard targetCard;
    private Timer timerUnit;
    
    private boolean confirmed = false;

    public StealView(GameLogic game) {
        this.GAME = game;
        this.targetCard = new SongCard(null, GAME.getCurrentSong());
        this.targetCard.setDraggable(true);

        this.setTitle("Steal Attempt!");
        this.initModality(Modality.APPLICATION_MODAL);
        this.setMinWidth(700);
        this.setMinHeight(400);

        BorderPane root = new BorderPane();

        // 1) Add status bar to top
        statusBar = new StatusBar();
        statusBar.setInfoText("Place your guess at the correct area of the strip!");
        root.setTop(statusBar);

        // 2) Add CardStripPane to center
        AnchorPane centerLayout = new AnchorPane();
        centerLayout.setPadding(new Insets(20));

        cardStripPane = new CardStripPane(game);
        cardStripPane.setPrefHeight(180);

        AnchorPane.setTopAnchor(cardStripPane, 20.0);
        AnchorPane.setLeftAnchor(cardStripPane, 20.0);
        AnchorPane.setRightAnchor(cardStripPane, 20.0);

        targetCard.setLayoutX(275); 
        targetCard.setLayoutY(260);

        cardStripPane.registerExternalCard(targetCard);

        centerLayout.getChildren().addAll(cardStripPane, targetCard);
        root.setCenter(centerLayout);

        // 3) Confirmation button at bottom
        confirmButton = new Button("Confirm placement");
        confirmButton.getStyleClass().add("primary-button");
        
        confirmButton.setOnAction(e -> handleConfirmation());

        VBox bottomLayout = new VBox(confirmButton);
        bottomLayout.setAlignment(Pos.CENTER);
        bottomLayout.setPadding(new Insets(15));
        root.setBottom(bottomLayout);

        // 4) Set scene
        Scene scene = new Scene(root);
        ThemeManager.getInstance().registerScene(scene);
        
        this.setScene(scene);
    }

    /**
     * Checks if song card was placed, sets the flag and closes the window.
     * Shows warning if card is not in strip pane.
     */
    private void handleConfirmation() {
        if (isCardPlaced()) {
            confirmed = true;
            this.close();

            if (timerUnit != null)
                timerUnit.stop();
            
            GAME.confirmStealAction(cardStripPane.getCards(), this.getPlacedPosition());
        }
        else {
            StyleDialog.warningDialog("Place Card!", "Place the song card inside the strip before confirming your input!");
        }
    }

    /**
     * Sets songs for CardStripPane in window.
     * Clears SongCards in CardStripPane and sets a new list of song.
     * @param songs List of songs to insert into CardStripPane.
     */
    public void setStripCards(List<Song> songs) {
        cardStripPane.clear();

        for (Song song : songs) {
            SongCard cardFromSong = new SongCard(null, song);
            cardFromSong.setDraggable(false);

            if (cardFromSong.song.titles.isEmpty()) {
                cardFromSong.showBack();
                // TODO: replace with opponent pfp
            }
            else {
                cardFromSong.showFront();
            }

            cardStripPane.addCard(cardFromSong);
        }
    }

    /**
     * Prüft, ob die zu platzierende Karte aktuell im CardStripPane liegt.
     */
    public boolean isCardPlaced() {
        return cardStripPane.getCards().contains(targetCard);
    }

    /**
     * Gibt den finalen Index der platzierten Karte zurück.
     * @return Index im Strip, oder -1 falls nicht platziert oder abgebrochen.
     */
    public int getPlacedPosition() {
        if (!confirmed) return -1;
        return cardStripPane.getCards().indexOf(targetCard);
    }

    /**
     * Erlaubt den Zugriff auf die StatusBar von außen (z.B. für Timer-Updates).
     */
    public StatusBar getStatusBar() {
        return statusBar;
    }

    public void showAndWait(Stage parent) {
        this.setOnShowing(e -> {
            Platform.runLater(() -> {
                double ownerX = parent.getX();
                double ownerY = parent.getY();
                double ownerWidth = parent.getWidth();
                double ownerHeight = parent.getHeight();

                double newWidth = this.getWidth();
                double newHeight = this.getHeight();

                double centerX = ownerX + (ownerWidth / 2.0) - (newWidth / 2.0);
                double centerY = ownerY + (ownerHeight / 2.0) - (newHeight / 2.0);

                this.setX(centerX);
                this.setY(centerY);
            });
        });

        timerUnit = new Timer(GAME.getGameOptions().stealTime);
        timerUnit.start(
            () -> Platform.runLater(() -> {
                statusBar.setRemainingTime(timerUnit.getRemainingSeconds());
            }),
            () -> {
                GAME.confirmStealAction(cardStripPane.getCards(), this.getPlacedPosition());
                this.close();
            }
        );

        super.showAndWait();
    }
}