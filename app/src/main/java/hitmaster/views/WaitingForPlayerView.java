package hitmaster.views;

import java.net.InetAddress;
import java.net.UnknownHostException;

import hitmaster.GameLogic;
import hitmaster.models.GameOptions;
import hitmaster.models.Player;
import hitmaster.models.User;
import hitmaster.services.Database;
import hitmaster.services.Log;
import hitmaster.services.NetworkManager;
import hitmaster.services.ThemeManager;
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

public class WaitingForPlayerView {

    private final MainMenu PARENT;
    private final Stage STAGE;

    private final Player HOST_PLAYER;
    private NetworkManager netManager;
    private GameOptions options;

    private ListView<String> playerListView;

    private Label moveTimeLabel;
    private Label stealTimeLabel;
    private Label maxPlayersLabel;
    private Label activeSetsLabel;

    public WaitingForPlayerView(MainMenu parent, String lobbyName, int tcpPort) {
        this.PARENT = parent;

        STAGE = new Stage();
        STAGE.setTitle("Waiting for Players");

        User user = Database.getCurrentUser();
        HOST_PLAYER = new Player(user.username, user.picture);
        HOST_PLAYER.role = Player.Role.HOST;

        // Default Settings
        this.options = new GameOptions();
        this.options.players = new Player[2];
        this.options.players[0] = HOST_PLAYER;

        this.options.moveTime = 300;
        this.options.stealTime = 30;

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

        Label subtitle = new Label("Waiting for players to join...");
        subtitle.getStyleClass().add("header-description");
        titleContainer.getChildren().addAll(title, subtitle);

        String localIp = "Unknown";
        try {
            localIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            Log.Error("Could not determine local IP");
        }
        Label ipLabel = new Label("Your IP: " + localIp);
        ipLabel.getStyleClass().add("modern-label"); 
        ipLabel.setStyle("-fx-text-fill: gray; -fx-background-color: transparent; -fx-font-size: 13px;");

        HBox.setHgrow(titleContainer, Priority.ALWAYS);
        header.getChildren().addAll(titleContainer, ipLabel);

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
        playersTitle.setStyle("-fx-font-weight: bold;");
        
        playerListView = new ListView<>();
        playerListView.getStyleClass().add("modern-listview");
        playerListView.setPrefHeight(180);
        playerListView.getItems().add(HOST_PLAYER.username + " (Host)");
        leftColumn.getChildren().addAll(playersTitle, playerListView);

        // Settings
        VBox rightColumn = new VBox(12);
        rightColumn.setAlignment(Pos.TOP_LEFT);
        rightColumn.setPrefWidth(180);
        rightColumn.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 10; -fx-background-radius: 5;");

        Label settingsTitle = new Label("Lobby Settings:");
        settingsTitle.getStyleClass().add("description");

        maxPlayersLabel = new Label("• Max. Players: 2");
        maxPlayersLabel.getStyleClass().add("description");

        moveTimeLabel = new Label("• Move Time: " + this.options.moveTime + "s");
        moveTimeLabel.getStyleClass().add("description");

        stealTimeLabel = new Label("• Steal Time: " + this.options.stealTime + "s");
        stealTimeLabel.getStyleClass().add("description");

        // TODO: Implement activeSetsLabel
        activeSetsLabel = new Label("• Active Sets: " + "/");
        activeSetsLabel.getStyleClass().add("description");

        rightColumn.getChildren().addAll(settingsTitle, maxPlayersLabel, moveTimeLabel, stealTimeLabel);

        content.getChildren().addAll(leftColumn, rightColumn);

        // =========================
        // FOOTER (Settings, Cancel & Start)
        // =========================

        Button openSettingsBtn = new Button("⚙ Options");
        openSettingsBtn.getStyleClass().add("modern-button");
        openSettingsBtn.setPrefWidth(120);
        openSettingsBtn.setOnAction(e -> this.openLobbySettingsDialog());

        Button cancelBtn = new Button("Close Lobby");
        cancelBtn.getStyleClass().add("modern-button");
        cancelBtn.setPrefWidth(120);
        cancelBtn.setOnAction(e -> {
            this.stopServer();
            STAGE.close();
        });

        Button startGameBtn = new Button("Start Game");
        startGameBtn.getStyleClass().add("primary-button");
        startGameBtn.setPrefWidth(140);
        startGameBtn.setDisable(true);
        startGameBtn.setOnAction(e -> {
            this.stopServerBroadcastOnly(); 
            
            netManager.sendObject(this.options); 
            
            GameLogic gameLogic = new GameLogic(this.options, true, netManager);
            PARENT.setStage(gameLogic.getView(), true);
            STAGE.close();
        });

        HBox rightButtons = new HBox(10, cancelBtn, startGameBtn);
        rightButtons.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(rightButtons, Priority.ALWAYS);

        HBox footer = new HBox(10, openSettingsBtn, rightButtons);
        footer.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(header, content, footer);

        Scene scene = new Scene(root, 580, 390); // Leicht angepasst für die Einstellungsbox
        ThemeManager.getInstance().registerScene(scene);

        STAGE.setScene(scene);
        STAGE.setOnCloseRequest(e -> this.stopServer());

        this.startNetworking(tcpPort, lobbyName, startGameBtn);
        
        STAGE.initModality(Modality.APPLICATION_MODAL);
    }

    private void startNetworking(int port, String lobbyName, Button startGameBtn) {
        netManager = new NetworkManager();
        
        new Thread(() -> {
            netManager.startAsHost(port, receivedObj -> {
                if (receivedObj instanceof Player clientPlayer) {
                    clientPlayer.decodeImage();
                    this.options.players[1] = clientPlayer;

                    Platform.runLater(() -> {
                        playerListView.getItems().add(clientPlayer.username);
                        startGameBtn.setDisable(false);
                        subtitleLabelUpdate("Player connected! Ready to start.");
                    });
                }
            });
        }).start();

        netManager.startLobbyBroadcast(lobbyName, port);
    }

    private void subtitleLabelUpdate(String text) {
        Log.Info(text);
    }

    private void openLobbySettingsDialog() {
        OnlineMultiplayerSettings settingsView = new OnlineMultiplayerSettings(options);
        settingsView.showAndWait(STAGE);

        this.updateSettingsLabels();
    }

    private void updateSettingsLabels() {
        Platform.runLater(() -> {
            moveTimeLabel.setText("• Move Time: " + this.options.moveTime + "s");
            stealTimeLabel.setText("• Steal Time: " + this.options.stealTime + "s");
        });
    }

    private void stopServerBroadcastOnly() {
        if (netManager != null) {
            netManager.stopLobbyBroadcast();
        }
    }

    private void stopServer() {
        if (netManager != null) {
            netManager.stopLobbyBroadcast();
        }
    }

    public void show(Stage parent) {
        STAGE.showAndWait();
    }
}
