package hitmaster.design;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class ToggleSwitch extends ToggleButton {

    private static final double WIDTH = 50;
    private static final double HEIGHT = 28;
    private static final double PADDING = 4;
    
    private final Circle thumb = new Circle(10, Color.WHITE);
    private final Region background = new Region();

    public ToggleSwitch() {
        setText("");

        setPrefSize(WIDTH, HEIGHT);
        setMinSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);

        background.setPrefSize(WIDTH, HEIGHT);
        background.setStyle("""
            -fx-background-color: -light-gray;
            -fx-background-radius: 28;
        """);

        StackPane container = new StackPane(background, thumb);

        StackPane.setAlignment(thumb, Pos.CENTER_LEFT);
        StackPane.setMargin(thumb, new Insets(0, 0, 0, 4));

        setGraphic(container);
        updateThumb();

        selectedProperty().addListener((obs, oldValue, newValue) -> {
            background.setStyle(
                "-fx-background-color: " + (newValue ? "-accent-brush" : "-light-gray") + ";" +
                "-fx-background-radius: 38;"
            );

            updateThumb();
        });

        setStyle("""
            -fx-background-color: transparent;
            -fx-padding: 0;
        """);
    }

    private void updateThumb() {
        boolean selected = isSelected();

        StackPane.setAlignment(
            thumb,
            selected ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT
        );

        StackPane.setMargin(
            thumb,
            new Insets(0, PADDING, 0, PADDING)
        );
    }
}
