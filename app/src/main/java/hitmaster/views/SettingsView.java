package hitmaster.views;

import hitmaster.services.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SettingsView {

    private final MainMenu PARENT;
    private final Stage STAGE;

    public SettingsView(MainMenu parent) {
        this.PARENT = parent;

        STAGE = new Stage();
        STAGE.setTitle("Settings");

        // =========================
        // ROOT LAYOUT
        // =========================
        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        // =========================
        // HEADER
        // =========================
        Label title = new Label("Settings");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label description = new Label("Customize your application experience.");
        description.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");

        VBox header = new VBox(5, title, description);
        header.setAlignment(Pos.TOP_LEFT);

        // =========================
        // SETTINGS CONTENT (Formular)
        // =========================
        VBox content = new VBox(15);
        content.setFillWidth(true);

        // 1) Language Selection Row
        ComboBox<String> languageDropdown = new ComboBox<>();
        languageDropdown.getStyleClass().add("modern-dropdown");
        languageDropdown.getItems().addAll("English", "German");
        languageDropdown.getSelectionModel().selectFirst();
        languageDropdown.setPrefWidth(180);
        
        languageDropdown.setOnAction(e -> {
            String selectedLanguage = languageDropdown.getValue();
            // TODO: Logic for language change
        });
        
        HBox languageRow = createSettingRow("Application Theme", "Set local language for Hitmaster.", languageDropdown);

        // 2) Theme Selection Row
        ComboBox<ThemeManager.Theme> themeDropdown = new ComboBox<>();
        themeDropdown.getStyleClass().add("modern-dropdown");
        themeDropdown.getItems().addAll(ThemeManager.Theme.values());
        themeDropdown.getSelectionModel().select(ThemeManager.getInstance().getCurrentTheme());
        themeDropdown.setPrefWidth(180);
        
        themeDropdown.setOnAction(e -> {
            ThemeManager.Theme selectedTheme = themeDropdown.getValue();
            if (selectedTheme != null)
                ThemeManager.getInstance().setTheme(selectedTheme);
        });
        
        HBox themeRow = createSettingRow("Application Theme", "Change the visual appearance of Hitmaster.", themeDropdown);

        // 3) Music Provider Row
        Button manageMusicBtn = new Button("Configure");
        manageMusicBtn.getStyleClass().add("modern-button");
        manageMusicBtn.setPrefWidth(180);
        manageMusicBtn.setOnAction(e -> {
            ProviderSettingsView providerSettings = new ProviderSettingsView(PARENT);
            providerSettings.show();
        });
        
        HBox musicRow = createSettingRow("Music Providers", "Connect and manage your streaming accounts.", manageMusicBtn);

        // 4) Manage Sets Row
        Button manageSetsBtn = new Button("Manage Sets");
        manageSetsBtn.getStyleClass().add("modern-button");
        manageSetsBtn.setPrefWidth(180);
        manageSetsBtn.setOnAction(e -> {
            // TODO: New Sets frame
        });
        
        HBox setsRow = createSettingRow("Song Sets", "Import, export or edit your custom song packages.", manageSetsBtn);

        // 5) Updates Row
        Button updateBtn = new Button("Check for Updates");
        updateBtn.getStyleClass().add("modern-button");
        updateBtn.setPrefWidth(180);
        updateBtn.setOnAction(e -> {
            // TODO: Update logic
        });
        
        HBox updateRow = createSettingRow("Software Update", "Look for new features and bugfixes.", updateBtn);

        // 6) Add all to content pane
        content.getChildren().addAll(languageRow, themeRow, musicRow, setsRow, updateRow);

        // =========================
        // FOOTER (Close Button)
        // =========================
        Button close = new Button("Close");
        close.getStyleClass().add("primary-button");
        close.setPrefWidth(120);
        close.setOnAction(e -> STAGE.close());

        HBox footer = new HBox(close);
        footer.setAlignment(Pos.BOTTOM_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        // =========================
        // ROOT ASSEMBLY & SCENE
        // =========================
        root.getChildren().addAll(header, content, footer);

        Scene scene = new Scene(root, 520, 480);
        ThemeManager.getInstance().registerScene(scene);
        STAGE.setScene(scene);
    }

    /**
     * Hilfsmethode, um eine konsistente Einstellungszeile mit Label, Beschreibung 
     * und dem Steuerelement auf der rechten Seite zu bauen.
     */
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