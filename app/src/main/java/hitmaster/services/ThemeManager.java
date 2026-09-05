package hitmaster.services;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Scene;

public class ThemeManager {

    public enum Theme {
        GLOBAL("/styles/style.css"),
        LIGHT("/styles/light.css"),
        DARK("/styles/dark.css");

        private final String path;
        Theme(String path) { this.path = path; }
        public String getPath() { return path; }
    }

    private static ThemeManager instance;
    private final Theme globalTheme = Theme.GLOBAL;
    private Theme currentTheme = Theme.LIGHT;
    private final List<Scene> registeredScenes = new ArrayList<>();

    private ThemeManager() {}

    public static synchronized ThemeManager getInstance() {
        if (instance == null)
            instance = new ThemeManager();

        return instance;
    }

    public void registerScene(Scene scene) {
        if (!registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
            applyThemeToScene(scene, globalTheme);
            applyThemeToScene(scene, currentTheme);
        }
    }

    public void setTheme(Theme theme) {
        this.currentTheme = theme;

        for (Scene scene : registeredScenes) {
            applyThemeToScene(scene, theme);
        }
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    private void applyThemeToScene(Scene scene, Theme theme) {
        scene.getStylesheets().removeIf(path ->
            path.contains("dark.css") || path.contains("light.css")
        );

        String themeUrl = getClass().getResource(theme.getPath()).toExternalForm();
        scene.getStylesheets().add(themeUrl);
    }
}