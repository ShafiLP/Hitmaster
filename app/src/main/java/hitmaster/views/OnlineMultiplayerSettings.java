package hitmaster.views;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import hitmaster.models.GameOptions;
import hitmaster.services.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class OnlineMultiplayerSettings {

    private final Stage STAGE;

    public OnlineMultiplayerSettings(GameOptions options) {
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
        title.getStyleClass().add("header");

        Label description = new Label("Configure the rules before launching the multiplayer match.");
        description.getStyleClass().add("header-description");

        VBox header = new VBox(5, title, description);
        header.setAlignment(Pos.TOP_LEFT);

        // =========================
        // SETTINGS CONTENT
        // =========================
        VBox content = new VBox(15);
        content.setFillWidth(true);

        // 1) Configure Sets
        Button manageSetsBtn = new Button("Manage Sets");
        manageSetsBtn.getStyleClass().add("modern-button");
        manageSetsBtn.setPrefWidth(180);
        manageSetsBtn.setOnAction(e -> {
            SetManagerView setManagerView = new SetManagerView();
            setManagerView.show(STAGE);
        });
        
        HBox setsRow = createSettingRow("Song Sets", "Import, export or edit your custom song packages.", manageSetsBtn);

        // 2) Turn time
        TextField turnTimeInput = new TextField();
        turnTimeInput.setTextFormatter(createNumberFormatter());
        turnTimeInput.setPromptText("Turn Time...");
        turnTimeInput.setText(String.valueOf(options.moveTime));
        turnTimeInput.getStyleClass().add("modern-textbox");
        turnTimeInput.setPrefWidth(180);
        HBox turnTimeControl = createSecondsInput(turnTimeInput, 300);
        
        HBox turnTimeRow = createSettingRow("Turn Time", "Set time each player gets to log in their guess.", turnTimeControl);

        // 3) Steal time input
        TextField stealTimeInput = new TextField();
        stealTimeInput.setTextFormatter(createNumberFormatter());
        stealTimeInput.setPromptText("Steal Time...");
        stealTimeInput.setText(String.valueOf(options.stealTime));
        stealTimeInput.getStyleClass().add("modern-textbox");
        stealTimeInput.setPrefWidth(180);
        HBox stealTimeControl = createSecondsInput(stealTimeInput, 30);
        
        HBox stealTimeRow = createSettingRow("Steal Time", "Time players get after guesses to steal a card.", stealTimeControl);

        content.getChildren().addAll(setsRow, turnTimeRow, stealTimeRow);

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

        Button confirmButton = new Button("Confirm");
        confirmButton.getStyleClass().add("primary-button");
        confirmButton.setPrefWidth(140);
        confirmButton.setOnAction(e -> {
            options.moveTime = Integer.parseInt(turnTimeInput.getText());
            options.stealTime = Integer.parseInt(stealTimeInput.getText());

            STAGE.close();
        });

        HBox footer = new HBox(10, cancel, confirmButton);
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
                confirmButton.fire();
                event.consume(); 
            }
        });

        STAGE.setScene(scene);

        Platform.runLater(STAGE::requestFocus);
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

    private HBox createSecondsInput(TextField field, int defaultValue) {
        field.getStyleClass().add("modern-textbox");
        field.setPrefWidth(80);
        field.setText(String.valueOf(defaultValue));

        Label unit = new Label("s");
        unit.getStyleClass().add("description");

        HBox box = new HBox(5, field, unit);
        box.setAlignment(Pos.CENTER_LEFT);

        return box;
    }

    private TextFormatter<String> createNumberFormatter() {

        Pattern pattern = Pattern.compile("\\d{0,3}"); // max 3 digits

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();

            if (pattern.matcher(newText).matches()) {
                return change;
            }
            return null;
        };

        return new TextFormatter<>(filter);
    }

    public void showAndWait(Stage parent) {
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