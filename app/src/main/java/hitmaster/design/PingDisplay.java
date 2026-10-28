package hitmaster.design;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class PingDisplay extends HBox {

    // Ping values
    private final int WARN_VALUE;
    private final int CRIT_VALUE;

    // Conection status values
    private ConnectionStatus connStatus;
    private ConnectionStatus prevStatus;

    // UI elements
    private final Label pingLabel;
    private final ImageView[] connectionIcon;
    private Image[] originalIcons;
    
    /**
     * Constructor for PingDisplay class.
     * Initializes dummy status with "??" as ping text and grey icon.
     * @param warn For this value and higher the icon turns yellow.
     * @param crit For this value and higher the icon turns red.
     */
    public PingDisplay(int warn, int crit) {
        super(5);
        
        // Set critical ping values
        if (crit < warn) crit = warn++;
        WARN_VALUE = warn;
        CRIT_VALUE = crit;

        // Initialize connection icon
        getStyleClass().add("ping-display");
        connStatus = ConnectionStatus.UNKNOWN;
        prevStatus = ConnectionStatus.NO_VALUE;
        connectionIcon = buildConnectionIcon();

        Pane iconPane = new Pane();
        iconPane.getChildren().addAll(connectionIcon);
        getChildren().add(iconPane);

        // Initialize ping text
        pingLabel = new Label();
        pingLabel.getStyleClass().add("description");
        setPingText("??");
        getChildren().add(pingLabel);

        // Wait for scene and update Icon
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) updateConnectionIcon();
        });
    }

    /**
     * Updates connection icon and ping text depending on ping value.
     * Uses WARN_VALUE and CRIT_VALUE to indicate connection status.
     * @param ping Ping to apply icon and text to.
     */
    public void setPing(int ping) {
        Platform.runLater(() -> {
            // 1) Update connection status based on ping
            if (ping >= WARN_VALUE) {
                if (ping >= CRIT_VALUE) {
                    connStatus = ConnectionStatus.CRIT;
                }
                else {
                    connStatus = ConnectionStatus.WARN;
                }
            }
            else {
                connStatus = ConnectionStatus.GOOD;
            }   

            // 2) Update ping text
            setPingText(String.valueOf(ping));

            // 3) Update icon
            updateConnectionIcon();
        });
    }

    /**
     * Sets display text for the current ping.
     * Adds "ms" at the end of given ping parameter.
     * @param ping Ping to display on text field with "ms".
     */
    private void setPingText(String ping) {
        if (ping == null) return;
        pingLabel.setText(ping + "ms");
    }

    /**
     * Sets color for each column of connection icon.
     * Uses flag connStatus to choose colors.
     * GOOD: all columns green.
     * WARN: first two columns yellow, last grey.
     * CRIT: first column red, others grey.
     * UNKNOWN: all columns grey.
     */
    private void updateConnectionIcon() {
        if (connectionIcon == null) return;
        if (connStatus == null) return;
        if (connStatus == prevStatus) return;

        Color grey = Color.GREY;
        Color green = Color.LIMEGREEN;
        Color yellow = Color.YELLOW;
        Color red = Color.RED;

        switch (connStatus) {

            case GOOD -> {
                for (int i = 0; i < connectionIcon.length; i++) {
                    connectionIcon[i].setImage(tintImage(getOriginalIcon(i), green));
                }
            }

            case WARN -> {
                System.out.println("Warn");
                connectionIcon[0].setImage(tintImage(getOriginalIcon(0), yellow));
                connectionIcon[1].setImage(tintImage(getOriginalIcon(1), yellow));
                connectionIcon[2].setImage(tintImage(getOriginalIcon(2), grey));
            }

            case CRIT -> {
                System.out.println("Crit");
                connectionIcon[0].setImage(tintImage(getOriginalIcon(0), red));
                connectionIcon[1].setImage(tintImage(getOriginalIcon(1), grey));
                connectionIcon[2].setImage(tintImage(getOriginalIcon(2), grey));
            }

            case UNKNOWN, NO_VALUE -> {
                for (int i = 0; i < connectionIcon.length; i++) {
                    connectionIcon[i].setImage(tintImage(getOriginalIcon(i), grey));
                }
            }
        }

        prevStatus = connStatus;
    }

    /**
     * Builds the connection icon from three seperate columns.
     * Place elements of returned array on top of each other to properly build the icon.
     * @return Connection icon as ImageView array.
     */
    private ImageView[] buildConnectionIcon() {
        originalIcons = new Image[] {
            new Image(getClass().getResourceAsStream(
                "/icons/connectionLow.png")),
            new Image(getClass().getResourceAsStream(
                "/icons/connectionMid.png")),
            new Image(getClass().getResourceAsStream(
                "/icons/connectionHigh.png"))
        };

        ImageView[] icons = new ImageView[originalIcons.length];

        for (int i = 0; i < icons.length; i++) {
            icons[i] = new ImageView(originalIcons[i]);

            icons[i].setFitWidth(16);
            icons[i].setFitHeight(16);
            icons[i].setPreserveRatio(true);
            icons[i].setSmooth(true);
        }

        return icons;
    }

    private Image getOriginalIcon(int index) {
        return originalIcons[index];
    }


    /**
     * Changes color of an image icon and returns it.
     * @param source Image object to change color of.
     * @param color New color of image source.
     * @return
     */
    private Image tintImage(Image source, Color color) {
        PixelReader reader = source.getPixelReader();

        int width = (int) source.getWidth();
        int height = (int) source.getHeight();

        WritableImage result = new WritableImage(width, height);

        PixelWriter writer = result.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                Color pixel = reader.getColor(x, y);

                writer.setColor(
                    x,
                    y,
                    new Color(
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        pixel.getOpacity()
                    )
                );
            }
        }

        return result;
    }
}

enum ConnectionStatus {
    GOOD,
    WARN,
    CRIT,
    UNKNOWN,
    NO_VALUE
}
