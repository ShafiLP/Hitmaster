package hitmaster.views;

import hitmaster.design.StyleDialog;
import hitmaster.models.User;
import hitmaster.services.Database;
import hitmaster.services.ThemeManager;
import hitmaster.views.Mutliplayer.ConnectToLobbyView;
import hitmaster.views.Mutliplayer.LobbyHostView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MultiplayerMenuView {

    private final MainMenu PARENT;
    private final Stage STAGE;

    private final User USER;

    public MultiplayerMenuView(MainMenu parent) {
        this.PARENT = parent;
        this.USER = Database.getInstance().getCurrentUser();

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
            settings.show(STAGE);

            STAGE.close();
        });

        HBox localRow = createSettingRow("Local Multiplayer", "Play with a friend on the same device.", localBtn);

        // 2) Host Game Row
        Button hostBtn = new Button("Open Lobby");
        hostBtn.getStyleClass().add("modern-button");
        hostBtn.setPrefWidth(180);
        hostBtn.setOnAction(e -> {
            this.showCreateLobbyDialog();
            STAGE.close();
        });
        
        HBox hostRow = createSettingRow("Host Game", "Create a new session as the server host.", hostBtn);

        // 3) Join Game Row
        Button joinBtn = new Button("Connect");
        joinBtn.getStyleClass().add("modern-button");
        joinBtn.setPrefWidth(180);
        joinBtn.setOnAction(e -> {
            ConnectToLobbyView connect = new ConnectToLobbyView(PARENT, STAGE);
            connect.show();

            STAGE.close();
        });

        HBox joinRow = createSettingRow("Join Game", "Connect to an existing host session.", joinBtn);

        content.getChildren().addAll(localRow, hostRow, joinRow);

        // =========================
        // FOOTER (Back Button)
        // =========================
        Button back = new Button("Back");
        back.getStyleClass().add("primary-button");
        back.setCancelButton(true);
        back.setPrefWidth(120);
        back.setOnAction(e -> STAGE.close());

        HBox footer = new HBox(back);
        footer.setAlignment(Pos.BOTTOM_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        // =========================
        // ROOT ASSEMBLY & SCENE
        // =========================
        root.getChildren().addAll(header, content, footer);

        Scene scene = new Scene(root, 520, 380);
        ThemeManager.getInstance().registerScene(scene);
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

    public void focus() {
        if (STAGE.isShowing()) {
            STAGE.toFront();
        }
        else {
            STAGE.show();
        }
    }

    // ==============================
    // Creating Lobby Dialog
    // ==============================

    /**
     * Opens dialog for creating a new LAN multiplayer lobby.
     * User can make input for lobby name and password.
     */
    private void showCreateLobbyDialog() {
        Stage stage = new Stage();
        stage.setTitle("Create Lobby");
        stage.initModality(Modality.APPLICATION_MODAL);

        // ===== Header & Description =====
        Label headerLabel = new Label("Create New Lobby");
        headerLabel.getStyleClass().add("header");

        Label descriptionLabel = new Label("Set up your lobby details below.\nYou can optionally set a password.");
        descriptionLabel.getStyleClass().add("description");
        descriptionLabel.setWrapText(true);

        // ===== Inputs =====
        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(15);
        inputGrid.setVgap(12);
        inputGrid.setPadding(new Insets(10, 0, 10, 0));

        // Lobby Name
        Label lobbyNamePrompt = new Label("Lobby Name: ");
        lobbyNamePrompt.getStyleClass().add("subheader");

        TextField lobbyNameInput = new TextField(USER.username + "'s Lobby");
        lobbyNameInput.getStyleClass().add("modern-textbox");
        lobbyNameInput.setPromptText("Lobby Name");
        lobbyNameInput.setPrefWidth(200);

        // Password
        Label passwordPrompt = new Label("Password: ");
        passwordPrompt.getStyleClass().add("subheader");

        TextField passwordInput = new TextField();
        passwordInput.getStyleClass().add("modern-textbox");
        passwordInput.setPromptText("Password");
        passwordInput.setPrefWidth(200);

        // Add Elements to Grid
        inputGrid.add(lobbyNamePrompt, 0, 0);
        inputGrid.add(lobbyNameInput, 1, 0);
        inputGrid.add(passwordPrompt, 0, 1);
        inputGrid.add(passwordInput, 1, 1);

        // ===== Buttons =====
        Button hostLobbyButton = new Button("Host");
        hostLobbyButton.getStyleClass().add("primary-button");
        hostLobbyButton.setOnAction(e -> {
            if (!lobbyNameInput.getText().isEmpty() || !lobbyNameInput.getText().isBlank()) {
                stage.close();
                this.createNewLobby(lobbyNameInput.getText().trim(), passwordInput.getText().trim());
            }
            else {
                StyleDialog.errorDialog("Enter Lobby Name", "Please enter a valid name for your lobby before hosting!");
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("error-button");
        cancelButton.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(15);
        buttonBox.getChildren().addAll(cancelButton, hostLobbyButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        // ===== Build Layout =====
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(headerLabel, descriptionLabel, inputGrid, buttonBox);

        Scene scene = new Scene(root);
        ThemeManager.getInstance().registerScene(scene);

        // Place centered to parent
        stage.setOnShowing(e -> {
            Platform.runLater(() -> {
                double ownerX = STAGE.getX();
                double ownerY = STAGE.getY();
                double ownerWidth = STAGE.getWidth();
                double ownerHeight = STAGE.getHeight();

                double newWidth = STAGE.getWidth();
                double newHeight = STAGE.getHeight();

                double centerX = ownerX + (ownerWidth / 2.0) - (newWidth / 2.0);
                double centerY = ownerY + (ownerHeight / 2.0) - (newHeight / 2.0);

                stage.setX(centerX);
                stage.setY(centerY);
            });
        });

        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.showAndWait();
    }

    /**
     * Creates a new LobbyHostView. Lobby name and password will be used as parameters.
     * @param lobbyName Name of lobby to create.
     * @param password Password of lobby to create. Blank if none.
     */
    private void createNewLobby(String lobbyName, String password) {
        LobbyHostView hostLobbyView = new LobbyHostView(PARENT, lobbyName, password, 5050);
        hostLobbyView.show(STAGE);
    }
}