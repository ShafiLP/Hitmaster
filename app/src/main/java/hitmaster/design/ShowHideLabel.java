package hitmaster.design;

import java.util.LinkedList;

import hitmaster.services.SvgParser;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

public class ShowHideLabel extends HBox {
    
    // ----- Global Values -----
    private String originalText = "";;

    private final Label label = new Label();
    private final Button toggle = new Button();
    private boolean isHidden = true;

    private char hideChar = '•';
    private final LinkedList<Character> exceptionChars = new LinkedList<>();

    // ----- Icons -----
    private final SVGPath textVisibleIcon = SvgParser.streamToSvgPath(
        getClass().getResourceAsStream("/icons/eyeOpened.svg")
    );
    private final SVGPath textHiddenIcon = SvgParser.streamToSvgPath(
        getClass().getResourceAsStream("/icons/eyeClosed.svg")
    );

    public ShowHideLabel() {
        super(10);

        toggle.getStyleClass().add("clickable-icon");
        toggle.setOnAction(e -> {
            isHidden = !isHidden;
            toggleShowHide();
        });

        toggle.setMinSize(20, 20);
        toggle.setPrefSize(20, 20);
        toggle.setMaxSize(20, 20);
        toggle.resize(20, 20);

        getChildren().addAll(label, toggle);
    }

    public ShowHideLabel(String text) {
        this();

        originalText = text;
        label.setText(originalText);

        toggleShowHide();
    }

    public ShowHideLabel(boolean hiddenAtInit) {
        this();

        isHidden = hiddenAtInit;

        toggleShowHide();
    }

    public ShowHideLabel(String text, boolean hiddenAtInit) {
        this();

        isHidden = hiddenAtInit;
        originalText = text;
        label.setText(originalText);
        
        toggleShowHide();
    }

    private void toggleShowHide() {
        label.setText(isHidden ? mask(originalText) : originalText);
        toggle.setGraphic(
            isHidden ? textHiddenIcon : textVisibleIcon
        );
    }

    private String mask(String text) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (exceptionChars.contains(c)) {
                result.append(c);
            } else {
                result.append(hideChar);
            }
        }

        return result.toString();
    }

    public void setOriginalText(String text) {
        originalText = text == null ? "" : text;
        toggleShowHide();
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setHideChar(char hideChar) {
        this.hideChar = hideChar;
        toggleShowHide();
    }

    public char getHideChar() {
        return hideChar;
    }

    public void addToExceptionChars(char exception) {
        if (exceptionChars.contains(exception)) return;
        exceptionChars.add(exception);
        toggleShowHide();
    }

    public void removeFromExceptionChars(char exception) {
        if (exceptionChars.contains(exception)) {
            exceptionChars.remove(exception);
            toggleShowHide();
        }
    }

    public LinkedList<Character> getExceptionChars() {
        return exceptionChars;
    }

    public Label getLabel() {
        return label;
    }
}
