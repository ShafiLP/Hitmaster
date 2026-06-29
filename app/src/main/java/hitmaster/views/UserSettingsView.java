package hitmaster.views;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import hitmaster.design.StyleDialog;
import hitmaster.models.User;
import hitmaster.services.Database;
import hitmaster.services.Log;
import hitmaster.services.ThemeManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class UserSettingsView {

    private final MainMenu PARENT;
    private final Stage STAGE;
    private final User user;
    
    private ImageView avatarPreview;
    private String selectedImagePath;

    public UserSettingsView(MainMenu parent) {
        this.PARENT = parent;
        this.user = Database.getCurrentUser();
        this.selectedImagePath = null; 

        STAGE = new Stage();
        STAGE.setTitle("User Profile Settings");

        // =========================
        // ROOT LAYOUT
        // =========================
        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        // =========================
        // HEADER
        // =========================
        Label title = new Label("Profile Settings");
        title.getStyleClass().add("header");

        Label description = new Label("Update your username and profile picture.");
        description.getStyleClass().add("header-description");

        VBox header = new VBox(5, title, description);
        header.setAlignment(Pos.TOP_LEFT);

        // =========================
        // CONTENT (Formular)
        // =========================
        VBox content = new VBox(15);
        content.setFillWidth(true);

        // 1) Username Row
        TextField usernameField = new TextField(user.username);
        usernameField.getStyleClass().add("modern-textfield");
        usernameField.setPrefWidth(180);
        usernameField.setMaxWidth(180);

        HBox usernameRow = createSettingRow("Username", "Change your public profile display name.", usernameField);

        // 2) Profile Picture Row
        avatarPreview = new ImageView();
        avatarPreview.setFitWidth(45);
        avatarPreview.setFitHeight(45);
        avatarPreview.setPreserveRatio(true);
        
        Circle clip = new Circle(22.5, 22.5, 22.5);
        avatarPreview.setClip(clip);

        this.loadUserAvatar();

        Button uploadBtn = new Button("Upload Image");
        uploadBtn.getStyleClass().add("modern-button");
        uploadBtn.setPrefWidth(120);
        uploadBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Profile Picture");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );
            
            File selectedFile = fileChooser.showOpenDialog(STAGE);
            if (selectedFile != null) {
                try {
                    String fileUrl = selectedFile.toURI().toURL().toExternalForm();
                    avatarPreview.setImage(new Image(fileUrl));
                    selectedImagePath = selectedFile.getAbsolutePath(); 
                } catch (MalformedURLException ex) {
                    StyleDialog.warningDialog("Error", "Could not load the selected image.");
                }
            }
        });

        HBox avatarControlBox = new HBox(15, avatarPreview, uploadBtn);
        avatarControlBox.setAlignment(Pos.CENTER_RIGHT);

        HBox avatarRow = createSettingRow("Profile Picture", "Upload a new avatar for your profile.", avatarControlBox);

        content.getChildren().addAll(usernameRow, avatarRow);

        // =========================
        // FOOTER (Cancel & Save Buttons)
        // =========================
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("modern-button");
        cancelBtn.setCancelButton(true);
        cancelBtn.setPrefWidth(100);
        cancelBtn.setOnAction(e -> STAGE.close());

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setPrefWidth(100);
        saveBtn.setOnAction((ActionEvent e) -> {
            String newUsername = usernameField.getText().trim();
            
            if (newUsername.isEmpty()) {
                StyleDialog.warningDialog("Validation Error", "Username cannot be empty.");
                return;
            }

            user.username = newUsername;

            if (selectedImagePath != null) {
                try {
                    File prevFile = new File(user.picture);

                    if (prevFile.exists())
                        Files.delete(prevFile.toPath());

                    File sourceFile = new File(selectedImagePath);
                    String fileName = sourceFile.getName();
                    String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();

                    String userHome = System.getProperty("user.home");
                    File appStorageDir = new File(userHome, ".hitmaster/pfp");

                    if (!appStorageDir.exists()) {
                        appStorageDir.mkdirs();
                    }

                    File destFile = new File(appStorageDir, "user_" + user.id + "_" + System.currentTimeMillis() + extension);

                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    user.picture = destFile.getAbsolutePath();
                }
                catch (IOException ex) {
                    Log.Warning("Failed to save profile image: " + ex.getMessage());
                    StyleDialog.warningDialog("Error", "Could not save the profile image to resources.");
                    return;
                }
            }

            if (Database.updateUser(user)) {
                PARENT.initialiseProviderButton(); 
                PARENT.updateProfileButton();
            }

            STAGE.close();
        });

        HBox footer = new HBox(10, cancelBtn, saveBtn);
        footer.setAlignment(Pos.BOTTOM_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        // =========================
        // ROOT ASSEMBLY & SCENE
        // =========================
        root.getChildren().addAll(header, content, footer);

        Scene scene = new Scene(root, 520, 300);
        ThemeManager.getInstance().registerScene(scene);
        STAGE.setScene(scene);

        Platform.runLater(STAGE::requestFocus);
    }

    /**
     * Loads current user avatar from Database.
     * If no user avatar is found, creates a new default profile picture.
     */
    private void loadUserAvatar() {
        try {
            Image image = user.getImage();

            if (image != null) {
                avatarPreview.setImage(image);
                return;
            }
            
            // Fallback
            avatarPreview.setImage(createDefaultAvatar(user.username));
        }
        catch (Exception e) {
            avatarPreview.setImage(null); 
        }
    }

    /**
     * Creates a new default avatar, containing a grey background and the first letter of current user's username.
     * @param username Username of user.
     * @return New default avatar image.
     */
    private Image createDefaultAvatar(String username) {
        int size = 128;

        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.LIGHTGRAY);
        gc.fillOval(0, 0, size, size);

        String initial = "?";

        if (username != null && !username.isBlank()) {
            initial = username.substring(0, 1).toUpperCase();
        }

        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 64));

        Text text = new Text(initial);
        text.setFont(gc.getFont());

        Bounds bounds = text.getLayoutBounds();

        double x = (size - bounds.getWidth()) / 2;
        double y = (size - bounds.getHeight()) / 2 - bounds.getMinY();

        gc.fillText(initial, x, y);

        WritableImage image = new WritableImage(size, size);
        canvas.snapshot(null, image);

        return image;
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
        } else {
            STAGE.show();
        }
    }
}