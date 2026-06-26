package hitmaster.design;

import hitmaster.GameLogic;
import hitmaster.models.Player;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ChatPane extends VBox {

    private final GameLogic GAME;
    private final Player PLAYER;
    
    private final Label timerLabel;
    private final VBox chatMessageContainer;
    private final ScrollPane scrollPane;
    private final TextField inputField;

    public ChatPane(GameLogic game) {
        this.GAME = game;
        this.PLAYER = GAME.getLocalPlayer();

        this.getStyleClass().add("chat-pane");
        this.setPadding(new Insets(10));
        this.setSpacing(10);
        this.setPrefSize(350, 400);

        // TIMER
        timerLabel = new Label("00");
        timerLabel.getStyleClass().add("timer");

        HBox timerWrapper = new HBox(timerLabel);
        timerWrapper.setAlignment(Pos.CENTER);
        timerWrapper.setStyle("-fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 0 0 1 0; -fx-padding: 0 0 5 0;");

        // CHAT MESSAGE CONTAINER
        chatMessageContainer = new VBox(8);
        chatMessageContainer.getStyleClass().add("chat-messages");
        
        scrollPane = new ScrollPane(chatMessageContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Automatically scroll for new messages
        chatMessageContainer.heightProperty().addListener((obs, oldVal, newVal) -> scrollPane.setVvalue(1.0));

        // 3) INPUT USER MESSAGES
        inputField = new TextField();
        inputField.setPromptText("Enter message...");
        
        // Confirm with Enter
        inputField.setOnAction(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                this.addPlayerMessage(PLAYER, text);
                inputField.clear();

                GAME.sendObject("CHAT:PLAYER_MESSAGE:" + text);
            }
        });

        this.getChildren().addAll(timerWrapper, scrollPane, inputField);
    }

    /**
     * Set remaining time for timer label.
     * Automatically converts time into minutes and seconds.
     * @param seconds Seconds remaining for the timer.
     */
    public void setRemainingTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;

        Platform.runLater(() -> timerLabel.setText(String.format("%02d:%02d", minutes, secs) + " ⏱"));
    }

    private void addSystemMessage(String text, String hexColor) {
        Platform.runLater(() -> {
            Label msgLabel = new Label("[SYSTEM]: " + text);
            msgLabel.setWrapText(true);
            msgLabel.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold; -fx-font-size: 12px;");
            appendRow(msgLabel);
        });
    }

    public void addInfoMessage(String text) {
        this.addSystemMessage(text, "#ffffff");
    }

    public void addSuccessMessage(String text) {
        this.addSystemMessage(text, "#00ff00");
    }

    public void addWarningMessage(String text) {
        this.addSystemMessage(text, "#ffff00");
    }

    public void addErrorMessage(String text) {
        this.addSystemMessage(text, "#ff5050");
    }

    public void addDebugMessage(String text) {
        Platform.runLater(() -> {
            Label msgLabel = new Label("[DEBUG] " + text);
            msgLabel.setWrapText(true);
            msgLabel.setStyle("-fx-text-fill: #a4b0be; -fx-font-family: 'Courier New'; -fx-font-size: 11px;");
            appendRow(msgLabel);
        });
    }

    public void addPlayerMessage(Player player, String text) {
        Platform.runLater(() -> {
            HBox row = new HBox(8);
            row.setAlignment(Pos.TOP_LEFT);

            Node avatarNode = null;
            double pfpSize = 32;

            if (player != null)
                avatarNode = player.getPlayerImage(pfpSize);

            VBox textBubble = new VBox(2);
            Label nameLabel = new Label(player != null ? player.username : "Unknown");
            nameLabel.getStyleClass().add("message-username");
            
            Label contentLabel = new Label(text);
            contentLabel.setWrapText(true);
            contentLabel.getStyleClass().add("message-content");

            textBubble.getStyleClass().add("message-bubble");
            textBubble.getChildren().addAll(nameLabel, contentLabel);
            
            row.getChildren().addAll(avatarNode, textBubble);
            this.appendRow(row);
        });
    }

    private void appendRow(Node node) {
        chatMessageContainer.getChildren().add(node);
    }
}
