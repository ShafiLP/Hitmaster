package hitmaster.design;

import hitmaster.services.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
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

        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);
        root.setAlignment(Pos.CENTER);

        Label label = new Label(message);
        label.setWrapText(true);

        Button closeButton = new Button("OK");
        closeButton.getStyleClass().add("primary-button");
        closeButton.setOnAction(e -> stage.close());

        root.getChildren().addAll(label, closeButton);

        Scene scene = new Scene(root, 400, 200);
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

        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);
        root.setAlignment(Pos.CENTER);

        Label label = new Label(message);
        label.setWrapText(true);

        Button closeButton = new Button("OK");
        closeButton.getStyleClass().add("warning-button");
        closeButton.setOnAction(e -> stage.close());

        root.getChildren().addAll(label, closeButton);

        Scene scene = new Scene(root, 400, 200);
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

        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);
        root.setAlignment(Pos.CENTER);

        Label label = new Label(message);
        label.setWrapText(true);

        Button closeButton = new Button("OK");
        closeButton.getStyleClass().add("error-button");
        closeButton.setOnAction(e -> stage.close());

        root.getChildren().addAll(label, closeButton);

        Scene scene = new Scene(root, 400, 200);
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

        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);
        root.setAlignment(Pos.CENTER);

        Label label = new Label(message);
        label.setWrapText(true);

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

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

        buttonBox.getChildren().addAll(yesButton, noButton);
        root.getChildren().addAll(label, buttonBox);

        Scene scene = new Scene(root, 400, 200);
        ThemeManager.getInstance().registerScene(scene);

        stage.setScene(scene);
        stage.showAndWait();

        return result[0];
    }
}
