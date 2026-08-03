package hitmaster.views.Mutliplayer;

import java.net.InetAddress;
import java.net.UnknownHostException;

import hitmaster.GameLogic;
import hitmaster.models.GameOptions;
import hitmaster.models.GameOptionsDTO;
import hitmaster.models.JoinRequest;
import hitmaster.models.Player;
import hitmaster.models.User;
import hitmaster.services.Database;
import hitmaster.services.Log;
import hitmaster.services.NetworkManager;
import hitmaster.services.ThemeManager;
import hitmaster.views.MainMenu;
import hitmaster.views.OnlineMultiplayerSettings;
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

public class LobbyHostView {

    private final MainMenu PARENT;
    private final Stage STAGE;

    private final Player HOST_PLAYER;
    private NetworkManager netManager;
    private GameOptions options;

    private final Label MOVETIME_LABEL;
    private final Label STEALTIME_LABEL;
    private final Label MAXPLAYERS_LABEL;
    private final Label ACTIVESETS_LABEL;

    private final ListView<String> PLAYERLIST_VIEW;

    /**
     * Constructor for "LobbyHostView".
     * Initializes UI and starts lobby broadcasting.
     * @param parent MainMenu that started this view. Used to set stage later.
     * @param lobbyName Name of this lobby.
     * @param tcpPort Port to start the lobby broadcasting at.
     */
    public LobbyHostView(MainMenu parent, String lobbyName, String password, int tcpPort) {
        this.PARENT = parent;

        STAGE = new Stage();
        STAGE.setTitle("Lobby: " + lobbyName);

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
        playersTitle.getStyleClass().add("subheader");
        
        PLAYERLIST_VIEW = new ListView<>();
        PLAYERLIST_VIEW.getStyleClass().add("modern-listview");
        PLAYERLIST_VIEW.setPrefHeight(180);
        PLAYERLIST_VIEW.getItems().add(HOST_PLAYER.username + " (Host) (You)");
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

        MOVETIME_LABEL = new Label("• Move Time: " + this.options.moveTime + "s");
        MOVETIME_LABEL.getStyleClass().add("description");

        STEALTIME_LABEL = new Label("• Steal Time: " + this.options.stealTime + "s");
        STEALTIME_LABEL.getStyleClass().add("description");

        // TODO: Implement ACTIVESETS_LABEL
        ACTIVESETS_LABEL = new Label("• Active Sets: " + "/");
        ACTIVESETS_LABEL.getStyleClass().add("description");

        rightColumn.getChildren().addAll(settingsTitle, MAXPLAYERS_LABEL, MOVETIME_LABEL, STEALTIME_LABEL);

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
            
            netManager.sendObject(new GameOptionsDTO(this.options, "START_GAME")); 
            
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

        Scene scene = new Scene(root, 580, 390);
        ThemeManager.getInstance().registerScene(scene);

        STAGE.setScene(scene);
        STAGE.setOnCloseRequest(e -> this.stopServer());

        this.startNetworking(tcpPort, lobbyName, password, startGameBtn);
        
        STAGE.initModality(Modality.APPLICATION_MODAL);
    }

    /**
     * Starts lobby as host and waits for a player to join.
     * When player object received, displays player in player list and unlocks play button.
     * @param port Port to wait for player object.
     * @param lobbyName Name of lobby (used for lobby broadcast).
     * @param password Passowrd to join lobby. Blank if none.
     * @param startGameBtn Reference to "Start Game" button so it can be enabled when player connects.
     */
    private void startNetworking(int port, String lobbyName, String password, Button startGameBtn) {
        netManager = new NetworkManager();

        PARENT.getStage().setOnCloseRequest(event -> {
            netManager.closeConnection();

            Platform.exit();
            System.exit(0);
        });
        
        new Thread(() -> {
            netManager.startAsHost(port, password, receivedObj -> {
                if (receivedObj instanceof JoinRequest request) {
                    Player clientPlayer = request.player;
                    clientPlayer.decodeImage();
                    this.options.players[1] = clientPlayer;

                    Log.Info("Player joined host lobby: " + clientPlayer.username);

                    Platform.runLater(() -> {
                        PLAYERLIST_VIEW.getItems().add(clientPlayer.username);
                        startGameBtn.setDisable(false);
                    });

                    netManager.sendObject(new GameOptionsDTO(this.options, "OPTIONS_UPDATE"));
                }
            });
        }).start();

        netManager.startLobbyBroadcast(lobbyName, !password.isEmpty());
    }

    /**
     * Opens settings view for this lobby's game.
     * Labels of settings get updated when closing settings view.
     */
    private void openLobbySettingsDialog() {
        OnlineMultiplayerSettings settingsView = new OnlineMultiplayerSettings(options);
        settingsView.showAndWait(STAGE);

        netManager.sendObject(new GameOptionsDTO(options, "OPTIONS_UPDATE"));
        this.updateSettingsLabels();
    }

    /**
     * Updates all texts for settings labels.
     * Called after GameOptions object was updated.
     */
    private void updateSettingsLabels() {
        Platform.runLater(() -> {
            MOVETIME_LABEL.setText("• Move Time: " + this.options.moveTime + "s");
            STEALTIME_LABEL.setText("• Steal Time: " + this.options.stealTime + "s");
        });
    }

    /**
     * Stops broadcasting lobby to other players without closing connection.
     * Called when lobby is full.
     */
    private void stopServerBroadcastOnly() {
        if (netManager != null) {
            netManager.stopLobbyBroadcast();
        }
    }

    /**
     * Stops server connection fully.
     * Closes lobby and stops server broadcasting.
     * Used when game gets cancelled instead of started.
     */
    private void stopServer() {
        this.stopServerBroadcastOnly();
        netManager.closeConnection();
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
}
