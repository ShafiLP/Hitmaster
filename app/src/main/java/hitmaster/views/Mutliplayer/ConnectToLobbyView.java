package hitmaster.views.Mutliplayer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import hitmaster.GameLogic;
import hitmaster.design.StyleDialog;
import hitmaster.models.GameOptionsDTO;
import hitmaster.models.JoinRequest;
import hitmaster.models.MultiplayerLobby;
import hitmaster.models.Player;
import hitmaster.models.User;
import hitmaster.services.Database;
import hitmaster.services.Log;
import hitmaster.services.NetworkManager;
import hitmaster.services.ThemeManager;
import hitmaster.services.UpdateService;
import hitmaster.views.MainMenu;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ConnectToLobbyView {
    
    private final MainMenu PARENT;
    private final Stage STAGE;
    private final Stage PREV_STAGE;

    private final Player PLAYER;
    private final String APP_VERSION;

    private NetworkManager discoveryNetManager;
    private final Set<String> foundLobbiesTracker = new HashSet<>(); // Prevents duplicates

    private LobbyClientView clientView;

    /**
     * Constructor for ConnectToLobbyView.
     * Initializes UI and starts looking for available lobbies.
     * @param parent MainMenu that started this view (Used to set stage later).
     * @param prevStage Previous stage (Used to show view on top of it).
     */
    public ConnectToLobbyView(MainMenu parent, Stage prevStage) {
        this.PARENT = parent;
        this.PREV_STAGE = prevStage;

        APP_VERSION = ConnectToLobbyView.loadAppVersion();

        STAGE = new Stage();
        STAGE.setTitle("Connect To Host");

        User user = Database.getCurrentUser();
        PLAYER = new Player(user.username, user.picture);

        // =========================
        // ROOT LAYOUT
        // =========================
        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        // =========================
        // HEADER
        // =========================
        Label title = new Label("Connect To Host");
        title.getStyleClass().add("header");

        Label description = new Label("Select a lobby from list or enter host's IP-adress to connect to game.");
        description.getStyleClass().add("header-description");

        VBox header = new VBox(5, title, description);
        header.setAlignment(Pos.TOP_LEFT);

        // =========================
        // CONTENT
        // =========================
        VBox content = new VBox(15);
        content.setFillWidth(true);

        Label statusLabel = new Label("Searching for lobbies...");

        ListView<MultiplayerLobby> lobbyListView = new ListView<>();
        lobbyListView.setPrefHeight(120);
        lobbyListView.getStyleClass().add("modern-listview");

        lobbyListView.setCellFactory(lv -> new ListCell<>() {
            private final HBox layout = new HBox();
            private final Label nameLabel = new Label();
            private final Label lockLabel = new Label("🔒");
            private final Label countLabel = new Label("1/2 👤");

            {
                layout.setAlignment(Pos.CENTER_LEFT);
                countLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 13px;");
                lockLabel.setStyle("-fx-font-size: 13px;");
                
                HBox.setHgrow(nameLabel, Priority.ALWAYS);
                nameLabel.setMaxWidth(Double.MAX_VALUE);
                
                layout.getChildren().addAll(nameLabel, lockLabel, countLabel);
            }

            @Override
            protected void updateItem(MultiplayerLobby item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                }
                else {
                    nameLabel.setText(item.name);
                    nameLabel.textFillProperty().bind(this.textFillProperty()); 

                    lockLabel.setVisible(item.hasPassword);
                    lockLabel.setManaged(item.hasPassword);

                    setGraphic(layout);
                }
            }
        });
        
        TextField ipInput = new TextField();
        ipInput.getStyleClass().add("modern-textbox");
        ipInput.setPrefWidth(180);
        ipInput.setPromptText("Enter IP manually (e.g. 192.168.1.1)");

        content.getChildren().addAll(statusLabel, lobbyListView, ipInput);

        // =========================
        // FOOTER (Cancel & Start)
        // =========================
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("modern-button");
        cancel.setCancelButton(true);
        cancel.setPrefWidth(120);
        cancel.setOnAction(e -> {
            STAGE.close();
        });

        Button connectToGame = new Button("Connect");
        connectToGame.getStyleClass().add("primary-button");
        connectToGame.setPrefWidth(140);
        connectToGame.setOnAction(e -> {
            String ip = ipInput.getText().trim();

            if (ip.isEmpty()) {
                ipInput.setStyle("-fx-border-color: red;");
                return;
            }

            MultiplayerLobby selectedLobby = lobbyListView.getSelectionModel().getSelectedItem();

            if (selectedLobby != null && selectedLobby.hasPassword) {
                String password = this.showPasswordInputDialog(selectedLobby.name);

                if (password == null) 
                    return;

                this.connectToHost(ip, password, connectToGame, lobbyListView, ipInput, statusLabel);
            }
            else {
                this.connectToHost(ip, "", connectToGame, lobbyListView, ipInput, statusLabel);
            }
        });

        lobbyListView.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                MultiplayerLobby selected = lobbyListView.getSelectionModel().getSelectedItem();
                
                if (selected != null) {
                    // Check if app version of host and client are equal
                    if (!selected.appVersion.equals(APP_VERSION)) {
                        StyleDialog.errorDialog("Version Error", "Couldn't connect to host:\nApp versions are not equal.\nHost Version: " + selected.appVersion + "\nYour Version: " + APP_VERSION);
                        return;
                    }

                    ipInput.setText(selected.ip);
                    connectToGame.fire();
                }
            }
        });

        HBox footer = new HBox(10, cancel, connectToGame);
        footer.setAlignment(Pos.BOTTOM_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        // =========================
        // ROOT ASSEMBLY & SCENE
        // =========================
        root.getChildren().addAll(header, content, footer);

        Scene scene = new Scene(root, 520, 360);
        ThemeManager.getInstance().registerScene(scene);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                connectToGame.fire();
                event.consume(); 
            }
        });

        STAGE.setScene(scene);
        STAGE.setOnCloseRequest(e -> this.stopDiscovery());

        Platform.runLater(STAGE::requestFocus);
        this.startDiscovery(lobbyListView, statusLabel);
    }

    private void connectToHost(String ip, String password, Button connectBtn, ListView<MultiplayerLobby> lobbyListView, TextField ipInput, Label statusLabel) {
        this.stopDiscovery();

        connectBtn.setDisable(true);
        connectBtn.setText("Connecting...");
        lobbyListView.setDisable(true);
        ipInput.setEditable(false);
        ipInput.setStyle("");
        statusLabel.setText("Connecting to " + ip + "...");

        new Thread(() -> {
            try {
                NetworkManager netManager = new NetworkManager();
                
                netManager.startAsClient(ip, 5050, receivedObj -> {
                    if (receivedObj instanceof GameOptionsDTO hostOptions) {
                        switch (hostOptions.purpose) {
                            case "START_GAME":
                                Platform.runLater(() -> {
                                    clientView.close();
                                    STAGE.close();

                                    for (Player player : hostOptions.players) {
                                        if (player != null) {
                                            player.decodeImage();
                                        }
                                    }
                                    
                                    GameLogic gameLogic = new GameLogic(hostOptions.getGameOptions(), false, netManager);
                                    PARENT.setStage(gameLogic.getView(), true);
                                    
                                    Log.Success("GameOptions received! Starting Game...");
                                });
                            break;

                            case "OPTIONS_UPDATE":
                                // 1) Check if clientView is initialized
                                if (this.clientView == null) {
                                    Log.Error("Received \"" + hostOptions.purpose + "\" with clientView being null");
                                    return;
                                }

                                // 2) Update game options on UI
                                clientView.updateGameOptions(hostOptions.getGameOptions());
                            break;
                        }
                        
                    }
                    else if ("JOIN_SUCCESS".equals(receivedObj)) {
                        Log.Success("Successfully joined lobby!");
                        Platform.runLater(() -> statusLabel.setText("Joined! Waiting for Host to start..."));

                        // Show lobby to client
                        Platform.runLater(() -> {
                            clientView = new LobbyClientView(PARENT, ip, password, PLAYER, netManager);
                            clientView.show(STAGE);
                        });
                    }
                    else if ("REJECTED_PASSWORD".equals(receivedObj)) {
                        Log.Error("Rejected connection: Wrong password!");
                        Platform.runLater(() -> {
                            StyleDialog.errorDialog("Access Denied", "Incorrect password entered for this lobby.");
                            this.resetUiAfterConnectionError(connectBtn, lobbyListView, ipInput, statusLabel);
                        });
                    }
                });

                netManager.sendObject(new JoinRequest(PLAYER, password));
            }
            catch (IOException ex) {
                Log.Error("Couldn't connect to host: " + ex.getMessage());
                
                Platform.runLater(() -> {
                    StyleDialog.errorDialog("Connection Error", "Failed to connect to host at " + ip);
                    this.resetUiAfterConnectionError(connectBtn, lobbyListView, ipInput, statusLabel);
                });
            }
        }).start();
    }

    private void resetUiAfterConnectionError(Button connectBtn, ListView<MultiplayerLobby> lobbyListView, TextField ipInput, Label statusLabel) {
        connectBtn.setDisable(false);
        connectBtn.setText("Connect");

        lobbyListView.setDisable(false);
        ipInput.setStyle("-fx-border-color: red;");
        ipInput.setEditable(true);

        this.startDiscovery(lobbyListView, statusLabel);
    }

    private String showPasswordInputDialog(String lobbyName) {
        Stage stage = new Stage();
        stage.setTitle("Protected Lobby");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(STAGE);

        Label headerLabel = new Label("Password Required");
        headerLabel.getStyleClass().add("header");

        Label descriptionLabel = new Label("The lobby \"" + lobbyName + "\" is protected with a password.");
        descriptionLabel.getStyleClass().add("description");
        descriptionLabel.setWrapText(true);

        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(15);
        inputGrid.setVgap(12);
        inputGrid.setPadding(new Insets(10, 0, 10, 0));

        Label passwordPrompt = new Label("Password: ");
        passwordPrompt.getStyleClass().add("subheader");

        PasswordField passwordInput = new PasswordField();
        passwordInput.getStyleClass().add("modern-textbox");
        passwordInput.setPromptText("Enter Password");
        passwordInput.setPrefWidth(200);

        inputGrid.add(passwordPrompt, 0, 0);
        inputGrid.add(passwordInput, 1, 0);

        final String[] result = new String[1]; // Aufnahme des Rückgabewerts

        Button joinButton = new Button("Join");
        joinButton.getStyleClass().add("primary-button");
        joinButton.setOnAction(e -> {
            result[0] = passwordInput.getText().trim();
            stage.close();
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("error-button");
        cancelButton.setOnAction(e -> {
            result[0] = null;
            stage.close();
        });

        HBox buttonBox = new HBox(15, cancelButton, joinButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(15, headerLabel, descriptionLabel, inputGrid, buttonBox);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        ThemeManager.getInstance().registerScene(scene);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                joinButton.fire();
                event.consume();
            }
        });

        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.showAndWait();

        return result[0];
    }

    /**
     * Starts searching for open lobbies.
     * When lobby gets found, adds it to list of available lobbies.
     * @param lobbyListView ListView of available lobbies to put new found lobbies at.
     * @param statusLabel Status label to put current status on.
     */
    private void startDiscovery(ListView<MultiplayerLobby> lobbyListView, Label statusLabel) {
        if (discoveryNetManager == null) {
            foundLobbiesTracker.clear();
            lobbyListView.getItems().clear();
            
            discoveryNetManager = new NetworkManager();
            discoveryNetManager.startLobbyDiscovery((lobby) -> {
                if (!foundLobbiesTracker.contains(lobby.ip)) {
                    foundLobbiesTracker.add(lobby.ip);
                    Platform.runLater(() -> {
                        lobbyListView.getItems().add(lobby);
                        statusLabel.setText("Lobbies found in your network (Double-click to join):");
                    });
                }
            });
        }
    }

    /**
     * Stops searching for open lobbies.
     * Called when connecting to a lobby or when closing view.
     */
    private void stopDiscovery() {
        if (discoveryNetManager != null) {
            discoveryNetManager.stopLobbyDiscovery();
            discoveryNetManager = null;
            Log.Info("Stopped LAN Discovery thread.");
        }
    }

    /**
     * Shows already initialized UI and waits.
     * Shows UI right above previous stage.
     */
    public void show() {
        // 1) Place view on top of previous stage
        STAGE.setOnShowing(e -> {
            Platform.runLater(() -> {
                double ownerX = PREV_STAGE.getX();
                double ownerY = PREV_STAGE.getY();
                double ownerWidth = PREV_STAGE.getWidth();
                double ownerHeight = PREV_STAGE.getHeight();

                double newWidth = STAGE.getWidth();
                double newHeight = STAGE.getHeight();

                double centerX = ownerX + (ownerWidth / 2.0) - (newWidth / 2.0);
                double centerY = ownerY + (ownerHeight / 2.0) - (newHeight / 2.0);

                STAGE.setX(centerX);
                STAGE.setY(centerY);

                if (PREV_STAGE != null)
                    PREV_STAGE.close();
            });
        });
        
        // 2) Show view and wait
        STAGE.initModality(Modality.APPLICATION_MODAL);
        STAGE.showAndWait();
    }

    /**
     * Loads current version of app from project.proerties file.
     * @return Current app version as String.
     */
    private static String loadAppVersion() {
        Properties properties = new Properties();

        try (InputStream input = UpdateService.class.getClassLoader().getResourceAsStream("project.properties")) {
            if (input == null)
                return "unknown";

            properties.load(input);
            return properties.getProperty("version", "unknown");
        }
        catch (IOException e) {
            return "unknown";
        }
    }
}
