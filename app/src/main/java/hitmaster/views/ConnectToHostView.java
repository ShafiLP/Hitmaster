package hitmaster.views;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ConnectToHostView {
    
    private final MainMenu PARENT;
    private final Stage STAGE;
    private final Stage PREV_STAGE;

    private final Player PLAYER;

    private NetworkManager discoveryNetManager;
    private final Set<String> foundLobbiesTracker = new HashSet<>(); // Prevents duplicates

    public ConnectToHostView(MainMenu parent, Stage prevStage) {
        this.PARENT = parent;
        this.PREV_STAGE = prevStage;

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

        Label description = new Label("Enter host's IP-adress to connect to game.");
        description.getStyleClass().add("header-description");

        VBox header = new VBox(5, title, description);
        header.setAlignment(Pos.TOP_LEFT);

        // =========================
        // SETTINGS CONTENT
        // =========================
        VBox content = new VBox(15);
        content.setFillWidth(true);

        Label statusLabel = new Label("Searching for lobbies...");

        ListView<String> lobbyListView = new ListView<>();
        lobbyListView.setPrefHeight(120);
        lobbyListView.getStyleClass().add("modern-listview");
        
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

            this.stopDiscovery();

            connectToGame.setDisable(true);
            connectToGame.setText("Connecting...");
            lobbyListView.setDisable(true);
            ipInput.setEditable(false);

            new Thread(() -> {
                try {
                    int port = 5050; // Same port as host!
                    NetworkManager netManager = new NetworkManager();
                    
                    // Start connecting
                    netManager.startAsClient(ip, port, receivedObj -> {
                        if (receivedObj instanceof GameOptions hostOptions) {
                            Platform.runLater(() -> {
                                STAGE.close(); 

                                for (Player player : hostOptions.players) {
                                    player.decodeImage();
                                }
                                
                                GameLogic gameLogic = new GameLogic(hostOptions, false, netManager);
                                PARENT.setStage(gameLogic.getView(), true);
                                
                                Log.Success("GameOptions received! Starting Game...");
                            });
                        }
                        Log.Info("Received by host: " + receivedObj);
                    });

                    Log.Info("Sending player");
                    netManager.sendObject(PLAYER);
                    Log.Info("Sent player");
                }
                catch (IOException ex) {
                    Log.Error("Couldn't connect to host: " + ex.getMessage());
                    
                    Platform.runLater(() -> {
                        connectToGame.setDisable(false);
                        connectToGame.setText("Connect");

                        lobbyListView.setDisable(false);
                        this.startDiscovery(lobbyListView, statusLabel);

                        ipInput.setStyle("-fx-border-color: red;");
                        ipInput.setEditable(true);
                    });

                    
                }
            }).start();
        });

        lobbyListView.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                String selected = lobbyListView.getSelectionModel().getSelectedItem();
                
                if (selected != null && selected.contains("(") && selected.contains(")")) {
                    String ip = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
                    ipInput.setText(ip);
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

    private void startDiscovery(ListView<String> lobbyListView, Label statusLabel) {
        if (discoveryNetManager == null) {
            foundLobbiesTracker.clear();
            lobbyListView.getItems().clear();
            
            discoveryNetManager = new NetworkManager();
            discoveryNetManager.startLobbyDiscovery((lobbyName, ipAddress, port) -> {
                String entry = lobbyName + " (" + ipAddress + ")";
                
                if (!foundLobbiesTracker.contains(entry)) {
                    foundLobbiesTracker.add(entry);
                    Platform.runLater(() -> {
                        lobbyListView.getItems().add(entry);
                        statusLabel.setText("Lobbies found in your network (Double-click to join):");
                    });
                }
            });
        }
    }

    private void stopDiscovery() {
        if (discoveryNetManager != null) {
            discoveryNetManager.stopLobbyDiscovery();
            discoveryNetManager = null;
            Log.Info("Stopped LAN Discovery thread.");
        }
    }

    public void show(Stage parent) {
        STAGE.setOnShowing(e -> {
            Platform.runLater(() -> {
                double ownerX = parent.getX();
                double ownerY = parent.getY();
                double ownerWidth = parent.getWidth();
                double ownerHeight = parent.getHeight();

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
        
        STAGE.initModality(Modality.APPLICATION_MODAL);
        STAGE.showAndWait();
    }
}
