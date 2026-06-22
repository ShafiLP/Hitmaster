package hitmaster.views;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import hitmaster.GameLogic;
import hitmaster.models.GameOptions;
import hitmaster.models.Player;
import hitmaster.services.Log;
import hitmaster.services.NetworkManager;
import hitmaster.services.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class WaitingForPlayerView {

    private final MainMenu PARENT;
    private final Stage STAGE;
    private final Stage PREV_STAGE;

    private NetworkManager netManager;
    private GameOptions options;

    Button startGame;

    public WaitingForPlayerView(MainMenu parent, Stage prevStage, GameOptions options) {
        this.PARENT = parent;
        this.PREV_STAGE = prevStage;
        this.options = options;

        STAGE = new Stage();
        STAGE.setTitle("Waiting for Players");

        // =========================
        // ROOT LAYOUT
        // =========================
        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        // =========================
        // HEADER
        // =========================
        Label title = new Label("Waiting For Players");
        title.getStyleClass().add("header");

        Label description = new Label("Waiting for other players to joing your hosted game.");
        description.getStyleClass().add("header-description");

        VBox header = new VBox(5, title, description);

        // =========================
        // SETTINGS CONTENT
        // =========================
        VBox content = new VBox(15);
        content.setFillWidth(true);
        content.setAlignment(Pos.CENTER);

        String hostIp = "error";
        try {
            hostIp = Inet4Address.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) {
            Log.Error("IPv4 from current machine couldn't be found.");
        }
        String[] finalIp = new String[1];
        finalIp[0] = hostIp;

        String censoredIp = hostIp.replaceAll("[^.]", "*");

        Label ipLabel = new Label(censoredIp);
        ipLabel.getStyleClass().add("subheader");

        Button toggle = new Button("Show IP");
        toggle.getStyleClass().add("modern-button");
        toggle.setOnAction(e -> {
            if (ipLabel.getText().equals(censoredIp)) {
                ipLabel.setText(finalIp[0]);
                toggle.setText("Hide IP");
            } else {
                ipLabel.setText(censoredIp);
                toggle.setText("Show IP");
            }
        });

        content.getChildren().addAll(ipLabel, toggle);

        // =========================
        // FOOTER (Cancel & Start)
        // =========================
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("modern-button");
        cancel.setPrefWidth(120);
        cancel.setOnAction(e -> {
            if (netManager != null) {
                netManager.closeConnection();
            }
            STAGE.close();
        });

        startGame = new Button("Connect");
        startGame.getStyleClass().add("primary-button");
        startGame.setPrefWidth(140);
        startGame.setDisable(true);

        startGame.setOnAction(e -> {
            STAGE.close();

            GameLogic gameLogic = new GameLogic(options, false, netManager);
            PARENT.setStage(gameLogic.getView(), true);
        });

        HBox footer = new HBox(10, cancel, startGame);
        footer.setAlignment(Pos.BOTTOM_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        // =========================
        // ROOT ASSEMBLY & SCENE
        // =========================
        root.getChildren().addAll(header, content, footer);

        Scene scene = new Scene(root, 520, 240);
        ThemeManager.getInstance().registerScene(scene);
        STAGE.setScene(scene);
    }

    public void show() {
        this.netManager = new NetworkManager();

        int port = 5050; // Same as client!
        netManager.startAsHost(port, receivedObj -> {
            if (receivedObj instanceof Player clientPlayer) {
                
                this.options.players[1] = clientPlayer;
                options.players[1].role = Player.Role.CLIENT;
                options.players[1].decodeImage();
                netManager.sendObject(this.options);

                Platform.runLater(() -> {
                    STAGE.close();

                    GameLogic gameLogic = new GameLogic(options, true, netManager);
                    PARENT.setStage(gameLogic.getView(), true);
                });
            }
        });

        if (PREV_STAGE != null) 
            PREV_STAGE.close();

        STAGE.setOnCloseRequest(e -> {
            if (netManager != null) netManager.closeConnection();
        });
        STAGE.show();
    }
}
