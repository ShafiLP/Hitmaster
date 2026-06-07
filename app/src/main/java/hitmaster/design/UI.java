package hitmaster.design;

import javafx.scene.control.Button;

public class UI {

    // ==============================
    // BUTTONS
    // ==============================

    public static Button primaryButton(String displayText) {
        return new Button(); // TODO
    }

    public static Button secondaryButton(String displayText) {
        return new Button(); // TODO
    }

    public static Button iconButton(String displayText) {
        return new Button(); // TODO
    }

    public static Button navButton(String displayText) {
        Button button = new Button(displayText);
        button.setStyle("""
            -fx-background-color: derive(-fx-base, -10%);
            -fx-font-size: 14px;
            -fx-text-fill: white;
            -fx-background-radius: 10;
            -fx-padding: 8 14;
            -fx-cursor: hand;
        """);
        button.setOnMouseEntered(e ->
            button.setStyle("""
                -fx-background-color: derive(-fx-base, -20%);
                -fx-text-fill: white;
                -fx-background-radius: 10;
                -fx-padding: 8 14;
                -fx-cursor: hand;
            """)
        );
        button.setOnMouseExited(e ->
            button.setStyle("""
                -fx-background-color: derive(-fx-base, -10%);
                -fx-text-fill: white;
                -fx-background-radius: 10;
                -fx-padding: 8 14;
                -fx-cursor: hand;
            """)
        );
        return button;
    }

    public static Button settingsButton() {
        Button button = new Button("⚙");
        button.getStyleClass().add("settings-button");
        return button;
    }

    public static Button quitButton() {
        Button button = new Button("Q");
        button.setStyle("""
            -fx-background-color: #1f2937;
            -fx-text-fill: white;
            -fx-background-radius: 10;
            -fx-padding: 8 14;
            -fx-cursor: hand;
        """);
        button.setOnAction(e -> {
            System.exit(0);
            // TODO: Dialog
        });
        return button;
    }
}
