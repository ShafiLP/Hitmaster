package hitmaster.design;

import hitmaster.services.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Style Dialog class.
 * Contains styled dialogs to display information.
 * Used instead of default dialogs to match the application design.
 */
public class StyleDialog {

    /**
     * Shows an info dialog with an "OK" button to confirm.
     * @param title Title of dialog.
     * @param message Message content of dialog.
     */
    public static void infoDialog(String title, String message) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);

        Button closeButton = new Button("OK");
        closeButton.getStyleClass().add("primary-button");
        closeButton.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(closeButton);
        VBox root = createDialogLayout(message, buttonBox, "info-icon");

        Scene scene = new Scene(root, 450, 220);
        ThemeManager.getInstance().registerScene(scene);
        
        stage.setScene(scene);
        stage.showAndWait();
    }

    /**
     * Shows a warning dialog with an "OK" button to confirm.
     * @param title Title of dialog.
     * @param message Message content of dialog.
     */
    public static void warningDialog(String title, String message) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);

        Button closeButton = new Button("OK");
        closeButton.getStyleClass().add("warning-button");
        closeButton.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(closeButton);
        VBox root = createDialogLayout(message, buttonBox, "warning-icon");

        Scene scene = new Scene(root, 450, 220);
        ThemeManager.getInstance().registerScene(scene);
        
        stage.setScene(scene);
        stage.showAndWait();
    }

    /**
     * Shows an error dialog with an "OK" button to confirm.
     * @param title Title of dialog.
     * @param message Message content of dialog.
     */
    public static void errorDialog(String title, String message) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);

        Button closeButton = new Button("OK");
        closeButton.getStyleClass().add("error-button");
        closeButton.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(closeButton);
        VBox root = createDialogLayout(message, buttonBox, "error-icon");

        Scene scene = new Scene(root, 450, 220);
        ThemeManager.getInstance().registerScene(scene);
        
        stage.setScene(scene);
        stage.showAndWait();
    }

    /**
     * Shows an info dialog with an "OK" button to confirm.
     * @param title Title of dialog.
     * @param message Message content of dialog.
     * @param buttonText Content of confirm button.
     * @return Confirmation result.
     */
    public static boolean questionDialog(String title, String message, String buttonText) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);

        HBox buttonBox = new HBox(15);

        Button yesButton = new Button(buttonText);
        Button noButton = new Button("Back");

        yesButton.getStyleClass().add("primary-button");
        noButton.getStyleClass().add("error-button");

        final boolean[] result = {false};

        yesButton.setOnAction(e -> {
            result[0] = true;
            stage.close();
        });

        noButton.setOnAction(e -> {
            result[0] = false;
            stage.close();
        });

        buttonBox.getChildren().addAll(noButton, yesButton);
        VBox root = createDialogLayout(message, buttonBox, "question-icon");

        Scene scene = new Scene(root, 450, 220);
        ThemeManager.getInstance().registerScene(scene);

        stage.setScene(scene);
        stage.showAndWait();

        return result[0];
    }

    private static VBox createDialogLayout(String message, HBox buttonBox, String iconClass) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        HBox contentBox = new HBox(20);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        Label iconLabel = new Label();
        iconLabel.getStyleClass().addAll("dialog-icon", iconClass);

        switch (iconClass) {
            case "info-icon" -> iconLabel.setText("🛈");
            case "warning-icon" -> iconLabel.setText("⚠");
            case "error-icon" -> iconLabel.setText("❌");
            case "question-icon" -> iconLabel.setText("❓");
        }

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("subheader");
        messageLabel.setWrapText(true);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);

        contentBox.getChildren().addAll(iconLabel, messageLabel);

        buttonBox.setAlignment(Pos.BOTTOM_RIGHT);

        root.getChildren().addAll(contentBox, buttonBox);
        return root;
    }
}
