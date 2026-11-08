package hitmaster.design;

import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class UiBuilder {

    public static Separator getHorizontalSeparator() {
        Separator seperator = new Separator();
        seperator.setStyle("-fx-padding: 4; -fx-background-color: transparent;");
        seperator.setOrientation(Orientation.HORIZONTAL);
        return seperator;
    }

    public static Separator getVerticalSeparator() {
        Separator seperator = new Separator();
        seperator.setStyle("-fx-padding: 4;");
        seperator.setOrientation(Orientation.VERTICAL);
        return seperator;
    }

    public static Region getHorizontalSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    public static Region getVerticalSpacer() {
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
}
