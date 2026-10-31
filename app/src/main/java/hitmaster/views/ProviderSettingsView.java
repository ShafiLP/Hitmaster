package hitmaster.views;

import java.util.Properties;

import hitmaster.services.Spotify;
import hitmaster.services.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ProviderSettingsView {

    private final Stage STAGE;
    private final MainMenu PARENT;
    private final StackPane ROOT;
    private StackPane overlay;
    private Thread authThread;

    public ProviderSettingsView(MainMenu parent) {
        this.PARENT = parent;

        STAGE = new Stage();
        STAGE.setTitle("Provider Settings");

        // =========================
        // ROOT CONTAINER (StackPane)
        // =========================
        ROOT = new StackPane();

        // =========================
        // CONTENT LAYOUT (VBox)
        // =========================
        VBox root = new VBox(15);
        root.setPadding(new Insets(15));
        root.setFillWidth(true);

        // =========================
        // HEADER
        // =========================
        Label title = new Label("Provider Settings");
        title.getStyleClass().add("header");

        Label description = new Label("Manage your music provider connections.");
        description.getStyleClass().add("header-description");

        VBox header = new VBox(5, title, description);
        header.setAlignment(Pos.CENTER);
        header.setMaxWidth(Double.MAX_VALUE);

        // =========================
        // SPOTIFY
        // =========================
        Properties spotifyProperties = Spotify.readProperties();

        VBox spotifyBox = new VBox(20);
        spotifyBox.setAlignment(Pos.TOP_CENTER);
        spotifyBox.setPadding(new Insets(10));
        spotifyBox.setStyle(
            "-fx-background-color: -spotify-background;" +
            "-fx-background-radius: 10;" +
            "-fx-border-radius: 10;" +
            "-fx-border-color: -primary-brush;" +
            "-fx-border-width: 1px;"
        );

        ImageView spotifyImage = new ImageView(
            new Image(getClass().getResourceAsStream("/icons/spotify.png"))
        );
        spotifyImage.setFitWidth(40);
        spotifyImage.setFitHeight(40);
        spotifyImage.setPreserveRatio(true);

        Label spotifyLabel = new Label("Spotify");
        spotifyLabel.getStyleClass().add("subheader");

        HBox spotifyRow = new HBox(10, spotifyImage, spotifyLabel);
        spotifyRow.setAlignment(Pos.CENTER);

        Label spotifyClientIdLabel = new Label("Client ID:");
        TextField spotifyClientIdTextField = new TextField() {{
            setText(spotifyProperties.getProperty("clientId"));
            setPromptText("Client ID...");
            setMaxWidth(Double.MAX_VALUE);
        }};

        Label spotifyClientSecretLabel = new Label("Client Secret:");
        TextField spotifyClientSecretTextField = new TextField() {{
            setText(spotifyProperties.getProperty("clientSecret"));
            setPromptText("Client Secret...");
            setMaxWidth(Double.MAX_VALUE);
        }};

        Button spotifyConnectButton = new Button("Connect");
        spotifyConnectButton.setOnAction(e -> {
            // Check for empty fields
            boolean validInputs = true;

            if (spotifyClientIdTextField.getText().isEmpty() || spotifyClientIdTextField.getText().isBlank()) {
                if (!spotifyClientIdTextField.getStyleClass().contains("invalid-text-input")) {
                    spotifyClientIdTextField.getStyleClass().add("invalid-text-input");
                }
                validInputs = false;
            } else {
                spotifyClientIdTextField.getStyleClass().remove("invalid-text-input");
            }

            if (spotifyClientSecretTextField.getText().isEmpty() || spotifyClientSecretTextField.getText().isBlank()) {
                if (!spotifyClientSecretTextField.getStyleClass().contains("invalid-text-input")) {
                    spotifyClientSecretTextField.getStyleClass().add("invalid-text-input");
                }
                validInputs = false;
            } else {
                spotifyClientSecretTextField.getStyleClass().remove("invalid-text-input");
            }

            if (!validInputs) return;

            // Set client ID and secret in properties
            Spotify.setClientIdAndClientSecret(spotifyClientIdTextField.getText(), spotifyClientSecretTextField.getText());

            // Open authentification
            showPendingAuthorization();
            authThread = new Thread(() -> {
                boolean success = Spotify.createSpotifyConnection();
                Platform.runLater(() -> {
                    hidePendingAuthorization();
                    if (success) {
                        PARENT.initialiseProviderButton();
                        spotifyConnectButton.setText("Connected");
                    }
                });
            });
            authThread.start();
        });

        GridPane spotifyGrid = new GridPane() {{
            setHgap(15);
            setVgap(10);
            setAlignment(Pos.CENTER);
        }};

        ColumnConstraints labelColumn = new ColumnConstraints() {{
            setMinWidth(100);
            setPrefWidth(100);
        }};

        ColumnConstraints inputColumn = new ColumnConstraints() {{
            setHgrow(Priority.ALWAYS);
            setFillWidth(true);
        }};

        spotifyGrid.getColumnConstraints().addAll(labelColumn, inputColumn);

        spotifyGrid.add(spotifyClientIdLabel, 0, 0);
        spotifyGrid.add(spotifyClientIdTextField, 1, 0);

        spotifyGrid.add(spotifyClientSecretLabel, 0, 1);
        spotifyGrid.add(spotifyClientSecretTextField, 1, 1);

        spotifyGrid.add(spotifyConnectButton, 0, 2);

        spotifyBox.getChildren().addAll(spotifyRow, spotifyGrid);

        // =========================
        // FOOTER (Close button)
        // =========================
        Button close = new Button("Close");
        close.getStyleClass().add("primary-button");
        close.setCancelButton(true);
        close.setOnAction(e -> STAGE.close());

        HBox footer = new HBox(close);
        footer.setAlignment(Pos.BOTTOM_CENTER);
        footer.setMaxWidth(Double.MAX_VALUE);

        // =========================
        // ROOT ASSEMBLY
        // =========================
        root.getChildren().addAll(header, spotifyBox, footer);
        
        // Inhalt in den Container packen
        ROOT.getChildren().add(root);

        Scene scene = new Scene(ROOT, 650, 320);
        ThemeManager.getInstance().registerScene(scene);
        STAGE.setScene(scene);

        Platform.runLater(ROOT::requestFocus);
    }

    // =========================
    // OVERLAY MANAGEMENT
    // =========================
    private void showPendingAuthorization() {
        if (overlay != null) return;

        overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
        overlay.setFocusTraversable(true);

        VBox dialogBox = new VBox(15);
        dialogBox.setAlignment(Pos.CENTER);
        dialogBox.setPadding(new Insets(20, 30, 20, 30));
        dialogBox.setMaxSize(280, 140); // Feste Größe für die innere Box

        dialogBox.setStyle("""
            -fx-background-color: -fx-background; 
            -fx-border-color: -fx-box-border;
            -fx-border-width: 1;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
        """);

        Label messageLabel = new Label("Continue in Browser");
        messageLabel.getStyleClass().add("subheader");

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("modern-button");
        
        cancelButton.setOnAction(e -> {
            // Stop background thread
            if (authThread != null && authThread.isAlive()) {
                authThread.interrupt(); 
            }
            
            // Stop authentication
            Spotify.cancelAuthentication();
            
            hidePendingAuthorization();
        });

        dialogBox.getChildren().addAll(messageLabel, cancelButton);
        overlay.getChildren().add(dialogBox);

        ROOT.getChildren().add(overlay);
    }

    private void hidePendingAuthorization() {
        if (overlay != null) {
            ROOT.getChildren().remove(overlay);
            overlay = null;
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
}