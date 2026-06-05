package hitmaster.design;

import java.util.Random;

import javafx.scene.paint.Color;

public enum PastelColor {

    PINK(Color.rgb(255, 209, 220)),
    BLUE(Color.rgb(174, 198, 255)),
    GREEN(Color.rgb(180, 238, 180)),
    YELLOW(Color.rgb(255, 255, 186)),
    PURPLE(Color.rgb(220, 208, 255)),
    ORANGE(Color.rgb(255, 223, 186));

    private final Color color;

    PastelColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    private static final Random RANDOM = new Random();

    public static Color random() {
        PastelColor[] values = values();
        return values[RANDOM.nextInt(values.length)].getColor();
    }
}