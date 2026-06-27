package hitmaster.views;

import hitmaster.models.User;
import hitmaster.services.Database;
import hitmaster.services.Spotify;
import hitmaster.services.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
        // GRID (2x2)
        // =========================
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        // TODO: Replace placeholders
        Button[] connections = new Button[4];
        for (int i = 0; i < connections.length; i++) {
            connections[i] = new Button("Add connection");
            connections[i].getStyleClass().add("modern-button");
        }
        
        connections[0].setOnAction(e -> {
            showOverlay();
            
            authThread = new Thread(() -> {
                boolean success = Spotify.createSpotifyConnection();
                Platform.runLater(() -> {
                    hideOverlay();
                    if (success) {
                        PARENT.initialiseProviderButton();
                        connections[0].setText("Connected");
                        connections[0].setStyle("-fx-background-color: rgba(0, 255, 0, 0.2);");
                    }
                });
            });
            authThread.start();
        });
        
        User user = Database.getCurrentUser();
        if (user.provider != null && user.provider.equals("spotify") && Spotify.requestSpotifyConnection() != null) {
            connections[0].setText("Connected");
            connections[0].setStyle("-fx-background-color: rgba(0, 255, 0, 0.2);");
        }

        grid.add(createCell("spotify.png", "Spotify", connections[0]), 0, 0);
        grid.add(createCell("apple_music.png", "Apple Music", connections[1]), 1, 0);
        grid.add(createCell("amazon_music.png", "Amazon Music", connections[2]), 0, 1);
        grid.add(createCell("deezer.png", "Deezer", connections[3]), 1, 1);

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
        root.getChildren().addAll(header, grid, footer);
        
        // Inhalt in den Container packen
        ROOT.getChildren().add(root);

        Scene scene = new Scene(ROOT, 650, 270);
        ThemeManager.getInstance().registerScene(scene);
        STAGE.setScene(scene);

        Platform.runLater(STAGE::requestFocus);
    }

    private HBox createCell(String imagePath, String text, Button button) {
        ImageView image = new ImageView(
            new Image(getClass().getResourceAsStream("/icons/" + imagePath))
        );
        image.setFitWidth(40);
        image.setFitHeight(40);
        image.setPreserveRatio(true);

        Label label = new Label(text);
        label.getStyleClass().add("subheader");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox cell = new HBox(10, image, label, spacer, button);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setPadding(new Insets(10));

        return cell;
    }

    // =========================
    // OVERLAY MANAGEMENT
    // =========================
    private void showOverlay() {
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
            
            this.hideOverlay();
        });

        dialogBox.getChildren().addAll(messageLabel, cancelButton);
        overlay.getChildren().add(dialogBox);

        ROOT.getChildren().add(overlay);
    }

    private void hideOverlay() {
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