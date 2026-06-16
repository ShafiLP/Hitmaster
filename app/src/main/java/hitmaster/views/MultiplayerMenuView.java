package hitmaster.views;

import hitmaster.services.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MultiplayerMenuView {

    private final MainMenu PARENT;
    private final Stage STAGE;

    public MultiplayerMenuView(MainMenu parent) {
        this.PARENT = parent;

        STAGE = new Stage();
        STAGE.setTitle("Multiplayer");

        // =========================
        // ROOT LAYOUT
        // =========================
        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        // =========================
        // HEADER
        // =========================
        Label title = new Label("Multiplayer Lobby");
        title.getStyleClass().add("header");

        Label description = new Label("Host a network session or join a friend via LAN connection.");
        description.getStyleClass().add("header-description");

        VBox header = new VBox(5, title, description);
        header.setAlignment(Pos.TOP_LEFT);

        // =========================
        // CONTENT
        // =========================
        VBox content = new VBox(15);
        content.setFillWidth(true);

        // 1) Local multiplayer row
        Button localBtn = new Button("Local Game");
        localBtn.getStyleClass().add("modern-button");
        localBtn.setPrefWidth(180);
        localBtn.setOnAction(e -> {
            LocalMultiplayerSettingsView settings = new LocalMultiplayerSettingsView(PARENT);
            settings.show();

            STAGE.close();
        });

        HBox localRow = createSettingRow("Local Multiplayer", "Play with a friend on the same device.", localBtn);

        // 2) Host Game Row
        Button hostBtn = new Button("Open Lobby");
        hostBtn.getStyleClass().add("modern-button");
        hostBtn.setPrefWidth(180);
        hostBtn.setOnAction(e -> {
            // TODO: New Settings Menu
        });
        
        HBox hostRow = createSettingRow("Host Game", "Create a new session as the server host.", hostBtn);

        // 3) Join Game Row
        Button joinBtn = new Button("Connect");
        joinBtn.getStyleClass().add("modern-button");
        joinBtn.setPrefWidth(180);
        joinBtn.setOnAction(e -> {
            // TODO: New IP input view
        });

        HBox joinRow = createSettingRow("Join Game", "Connect to an existing host session.", joinBtn);

        // Alle zum Content hinzufügen
        content.getChildren().addAll(localRow, hostRow, joinRow);

        // =========================
        // FOOTER (Back Button)
        // =========================
        Button back = new Button("Back");
        back.getStyleClass().add("primary-button");
        back.setPrefWidth(120);
        back.setOnAction(e -> STAGE.close());

        HBox footer = new HBox(back);
        footer.setAlignment(Pos.BOTTOM_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        // =========================
        // ROOT ASSEMBLY & SCENE
        // =========================
        root.getChildren().addAll(header, content, footer);

        Scene scene = new Scene(root, 520, 380); // Etwas kompakter, da weniger Zeilen
        ThemeManager.getInstance().registerScene(scene);
        STAGE.setScene(scene);
    }

    private HBox createSettingRow(String titleText, String descText, javafx.scene.Node control) {
        Label rowTitle = new Label(titleText);
        rowTitle.getStyleClass().add("subheader");

        Label rowDesc = new Label(descText);
        rowDesc.getStyleClass().add("description");

        VBox textContainer = new VBox(2, rowTitle, rowDesc);
        textContainer.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10, textContainer, spacer, control);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 0, 5, 0));
        row.setStyle("-fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 0 0 1 0;");

        return row;
    }

    public void show() {
        STAGE.initModality(Modality.APPLICATION_MODAL);
        STAGE.showAndWait();
    }

    public void focus() {
        if (STAGE.isShowing()) {
            STAGE.toFront();
        } else {
            STAGE.show();
        }
    }
}