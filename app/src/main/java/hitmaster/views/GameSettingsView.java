package hitmaster.views;

import hitmaster.services.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class GameSettingsView {

    private final Stage STAGE;

    public GameSettingsView() {
        STAGE = new Stage();
        STAGE.setTitle("Match Setup");

        // =========================
        // ROOT LAYOUT
        // =========================
        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        // =========================
        // HEADER
        // =========================
        Label title = new Label("Game Settings");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label description = new Label("Configure the rules before launching the multiplayer match.");
        description.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");

        VBox header = new VBox(5, title, description);
        header.setAlignment(Pos.TOP_LEFT);

        // =========================
        // SETTINGS CONTENT
        // =========================
        VBox content = new VBox(15);
        content.setFillWidth(true);

        ComboBox<String> modeDropdown = new ComboBox<>();
        modeDropdown.getStyleClass().add("modern-dropdown");
        modeDropdown.getItems().addAll("Standard Match", "Time Attack", "Sudden Death");
        modeDropdown.getSelectionModel().selectFirst();
        modeDropdown.setPrefWidth(180);
        
        HBox modeRow = createSettingRow("Game Mode", "Select the ruleset for this session.", modeDropdown);

        ComboBox<Integer> playerDropdown = new ComboBox<>();
        playerDropdown.getStyleClass().add("modern-dropdown");
        playerDropdown.getItems().addAll(2, 3, 4, 8);
        playerDropdown.getSelectionModel().selectFirst();
        playerDropdown.setPrefWidth(180);
        
        HBox playerRow = createSettingRow("Max Players", "Limit the amount of players allowed to join.", playerDropdown);

        ComboBox<String> roundsDropdown = new ComboBox<>();
        roundsDropdown.getStyleClass().add("modern-dropdown");
        roundsDropdown.getItems().addAll("Best of 3", "Best of 5", "Endless");
        roundsDropdown.getSelectionModel().selectFirst();
        roundsDropdown.setPrefWidth(180);
        
        HBox roundsRow = createSettingRow("Match Duration", "Set how many rounds are required to win.", roundsDropdown);

        content.getChildren().addAll(modeRow, playerRow, roundsRow);

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

        Button startGame = new Button("Start Game");
        startGame.getStyleClass().add("primary-button");
        startGame.setPrefWidth(140);
        startGame.setOnAction(e -> {
            System.out.println("Spiel gestartet mit Modus: " + modeDropdown.getValue());
            STAGE.close();
        });

        // HBox für Buttons (nebeneinander im Footer)
        HBox footer = new HBox(10, cancel, startGame);
        footer.setAlignment(Pos.BOTTOM_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        // =========================
        // ROOT ASSEMBLY & SCENE
        // =========================
        root.getChildren().addAll(header, content, footer);

        Scene scene = new Scene(root, 520, 380);
        ThemeManager.getInstance().registerScene(scene);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                startGame.fire();
                event.consume(); 
            }
        });

        STAGE.setScene(scene);

        Platform.runLater(STAGE::requestFocus);
    }

    private HBox createSettingRow(String titleText, String descText, javafx.scene.Node control) {
        Label rowTitle = new Label(titleText);
        rowTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label rowDesc = new Label(descText);
        rowDesc.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

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
            });
        });

        STAGE.initModality(Modality.APPLICATION_MODAL);
        STAGE.showAndWait();
    }
}