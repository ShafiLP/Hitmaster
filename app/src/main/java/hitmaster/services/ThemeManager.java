package hitmaster.services;

import javafx.scene.Scene;

public class ThemeManager {

    public static void apply(Scene scene, String theme) {

        scene.getStylesheets().clear();
        scene.getStylesheets().add(
            ThemeManager.class.getResource("/themes/" + theme + ".css")
                .toExternalForm()
        );
    }
}