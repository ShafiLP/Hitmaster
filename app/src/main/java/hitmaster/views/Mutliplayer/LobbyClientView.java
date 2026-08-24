package hitmaster.views.Mutliplayer;

import hitmaster.GameLogic;
import hitmaster.models.GameOptions;
import hitmaster.models.Player;
import hitmaster.services.NetworkManager;
import hitmaster.services.ThemeManager;
import hitmaster.views.MainMenu;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LobbyClientView {

    private final MainMenu PARENT;
    private final Stage STAGE;

    private Player hostPlayer;
    private Player ownPlayer;
    private final NetworkManager NETMANAGER;
    private GameOptions options;

    private final Label MOVETIME_LABEL;
    private final Label STEALTIME_LABEL;
    private final Label MAXPLAYERS_LABEL;
    private final Label ACTIVESETS_LABEL;

    private final ListView<String> PLAYERLIST_VIEW;

    /**
     * Constructor for "LobbyClientView".
     * Initializes UI.
     * @param parent MainMenu that started this view. Used to set stage later.
     * @param lobbyName Name of this lobby.
     */
    public LobbyClientView(MainMenu parent, String lobbyName, String password, Player user, NetworkManager netManager) {
        this.PARENT = parent;
        this.NETMANAGER = netManager;
        this.ownPlayer = user;

        STAGE = new Stage();
        STAGE.setTitle("Lobby: " + lobbyName);

        // =========================
        // ROOT LAYOUT
        // =========================
        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        // =========================
        // HEADER
        // =========================
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleContainer = new VBox(5);
        Label title = new Label("Lobby: " + lobbyName);
        title.getStyleClass().add("header");

        Label subtitle = new Label("Waiting for host to start...");
        subtitle.getStyleClass().add("header-description");
        titleContainer.getChildren().addAll(title, subtitle);

        HBox.setHgrow(titleContainer, Priority.ALWAYS);
        header.getChildren().addAll(titleContainer);

        // =========================
        // CONTENT
        // =========================
        HBox content = new HBox(20);
        content.setFillHeight(true);
        VBox.setVgrow(content, Priority.ALWAYS);

        // Player List
        VBox leftColumn = new VBox(8);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        Label playersTitle = new Label("Players in Lobby:");
        playersTitle.getStyleClass().add("subheader");
        
        PLAYERLIST_VIEW = new ListView<>();
        PLAYERLIST_VIEW.getStyleClass().add("modern-listview");
        PLAYERLIST_VIEW.setPrefHeight(180);
        PLAYERLIST_VIEW.getItems().add((hostPlayer != null ? hostPlayer.username : "null") + " (Host)");
        PLAYERLIST_VIEW.getItems().add(user.username + " (You)");
        leftColumn.getChildren().addAll(playersTitle, PLAYERLIST_VIEW);

        // Settings
        VBox rightColumn = new VBox(12);
        rightColumn.setAlignment(Pos.TOP_LEFT);
        rightColumn.setPrefWidth(180);
        rightColumn.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 10; -fx-background-radius: 5;");

        Label settingsTitle = new Label("Lobby Settings:");
        settingsTitle.getStyleClass().add("description");

        MAXPLAYERS_LABEL = new Label("• Max. Players: 2");
        MAXPLAYERS_LABEL.getStyleClass().add("description");

        MOVETIME_LABEL = new Label("• Move Time: " + (this.options != null ? this.options.moveTime : 0) + "s");
        MOVETIME_LABEL.getStyleClass().add("description");

        STEALTIME_LABEL = new Label("• Steal Time: " + (this.options != null ? this.options.stealTime : 0) + "s");
        STEALTIME_LABEL.getStyleClass().add("description");

        // TODO: Implement ACTIVESETS_LABEL
        ACTIVESETS_LABEL = new Label("• Active Sets: " + "/");
        ACTIVESETS_LABEL.getStyleClass().add("description");

        rightColumn.getChildren().addAll(settingsTitle, MAXPLAYERS_LABEL, MOVETIME_LABEL, STEALTIME_LABEL);

        content.getChildren().addAll(leftColumn, rightColumn);

        // =========================
        // FOOTER (Settings, Cancel & Start)
        // =========================
        Button cancelBtn = new Button("Leave");
        cancelBtn.getStyleClass().add("modern-button");
        cancelBtn.setPrefWidth(120);
        cancelBtn.setOnAction(e -> {
            STAGE.close();
        });

        HBox rightButtons = new HBox(10, cancelBtn);
        rightButtons.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(rightButtons, Priority.ALWAYS);

        HBox footer = new HBox(10, rightButtons);
        footer.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(header, content, footer);

        Scene scene = new Scene(root, 580, 390);
        ThemeManager.getInstance().registerScene(scene);

        STAGE.setScene(scene);
        STAGE.initModality(Modality.APPLICATION_MODAL);
    }

    /**
     * Updates game options and labels that display game options.
     * Called when host makes a change on game options.
     * @param options New game options to replace the old object with.
     */
    public void updateGameOptions(GameOptions options) {
        this.options = options;

        Platform.runLater(() -> {
            PLAYERLIST_VIEW.getItems().clear();
            PLAYERLIST_VIEW.getItems().add(options.players[0].username + " (Host)");
            for (int i = 1; i < options.players.length; i++) {
                PLAYERLIST_VIEW.getItems().add(options.players[i].username + (options.players[i].username.equals(ownPlayer.username) ? " (You)" : ""));
            }
            MOVETIME_LABEL.setText("• Move Time: " + this.options.moveTime + "s");
            STEALTIME_LABEL.setText("• Steal Time: " + this.options.stealTime + "s");
        });
    }

    /**
     * Shows the already initialized stage.
     * Centeres stage based on position of stage of previous frame.
     * @param parent Stage of previous frame.
     */
    public void show(Stage parent) {
        // Place centered to parent
        STAGE.setOnShowing(e -> {
            Platform.runLater(() -> {
                double ownerX = parent.getX();
                double ownerY = parent.getY();
                double ownerWidth = parent.getWidth();
                double ownerHeight = parent.getHeight();

                double newWidth = parent.getWidth();
                double newHeight = parent.getHeight();

                double centerX = ownerX + (ownerWidth / 2.0) - (newWidth / 2.0);
                double centerY = ownerY + (ownerHeight / 2.0) - (newHeight / 2.0);

                STAGE.setX(centerX);
                STAGE.setY(centerY);
            });
        });

        STAGE.show();
    }

    public void close() {
        STAGE.close();
    }

    public void startGame() {
        GameLogic gameLogic = new GameLogic(options, false, NETMANAGER);
        PARENT.setStage(gameLogic.getView(), true);
    }
}
