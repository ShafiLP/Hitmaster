package hitmaster.design;

import hitmaster.views.GameView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class StealOverlayPane extends StackPane {

    private final Label timerLabel;
    private final Button actionButton;
    private Timeline timeline;
    private int remainingSeconds;
    private final GameView parentContainer;

    /**
     * Creates a dark overlay with timer.
     * @param parentContainer Das Container-Pane der GameView (z.B. die View selbst), aus dem sich das Overlay später wieder entfernt.
     * @param initialSeconds Die Startzeit für den Timer in Sekunden.
     */
    public StealOverlayPane(GameView parentContainer, int initialSeconds) {
        this.parentContainer = parentContainer;
        this.remainingSeconds = initialSeconds;

        // 1) Darken background
        this.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");

        this.prefWidthProperty().bind(parentContainer.widthProperty());
        this.prefHeightProperty().bind(parentContainer.heightProperty());
        
        this.setFocusTraversable(true); 

        // 2) Create UI elements
        timerLabel = new Label(String.valueOf(remainingSeconds));
        timerLabel.setFont(Font.font("System", FontWeight.BOLD, 48));
        timerLabel.setTextFill(Color.WHITE);

        actionButton = new Button("Attempt Steal");
        actionButton.setFont(Font.font("System", FontWeight.NORMAL, 18));
        actionButton.getStyleClass().add("primary-button");
        
        // Set action for button
        actionButton.setOnAction(e -> {
            this.closeOverlay();
            parentContainer.stealButtonPressed();
        });

        // 3) Bind Enter Key to Button
        this.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                actionButton.fire();
                event.consume();
            }
        });

        // 4) Layout
        VBox contentBox = new VBox(25, timerLabel, actionButton);
        contentBox.setAlignment(Pos.CENTER);
        this.getChildren().add(contentBox);

        // 5) Timer
        this.startTimer();

        Platform.runLater(this::requestFocus);
    }

    private void startTimer() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;
            timerLabel.setText(String.valueOf(remainingSeconds));

            if (remainingSeconds <= 0) {
                this.closeOverlay();
                parentContainer.stealSkip();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void closeOverlay() {
        if (timeline != null) {
            timeline.stop();
        }

        if (parentContainer != null) {
            parentContainer.getChildren().remove(this);
        }
    }
}