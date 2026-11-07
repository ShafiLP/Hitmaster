package hitmaster.design;

import java.util.LinkedList;

import hitmaster.services.SvgParser;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.shape.SVGPath;

public class ShowHideTextField extends TextField {

    // ----- Global Values -----
    private String originalText;

    private char hideChar = '•';
    private final LinkedList<Character> exceptionChars = new LinkedList<>();

    private boolean isHidden = true;
    private boolean updatingDisplay = false;

    private final Button toggleShowHideButton = new Button();

    // ----- Icons -----
    private final SVGPath textVisibleIcon = SvgParser.streamToSvgIcon(
        getClass().getResourceAsStream("/icons/eyeOpened.svg")
    );
    private final SVGPath textHiddenIcon = SvgParser.streamToSvgIcon(
        getClass().getResourceAsStream("/icons/eyeClosed.svg")
    );

    /**
     * Initializes a new ShowHideTextField.
     * Works similar to a PasswordField, but show/hide can be toggled,
     * hideChar can be adjusted and an array of exception characters can be added.
     * @param text String text inside TextField.
     * @param hiddenAtInit boolean If true, hides all characters at init.
     * @see <a href="https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/PasswordField.html">PasswordField</a>
     */
    public ShowHideTextField(String text, boolean hiddenAtInit) {
        super(text);

        originalText = text == null ? "" : text;
        isHidden = hiddenAtInit;

        setTextFormatter(new TextFormatter<String>(change -> {
            if (updatingDisplay || !change.isContentChange()) {
                return change;
            }

            int start = change.getRangeStart();
            int end = change.getRangeEnd();

            String input = change.getText();

            // Update original text
            originalText =
                originalText.substring(0, start)
                + input
                + originalText.substring(end);

            // Mask input
            if (isHidden) {
                change.setText(mask(input));
            }

            return change;
        }));

        toggleShowHide();

        toggleShowHideButton.getStyleClass().add("clickable-icon");
        toggleShowHideButton.setOnAction(e -> {
            isHidden = !isHidden;
            toggleShowHide();
        });

        toggleShowHideButton.setManaged(false);
        toggleShowHideButton.setViewOrder(-1);

        toggleShowHideButton.setMinSize(20, 20);
        toggleShowHideButton.setPrefSize(20, 20);
        toggleShowHideButton.setMaxSize(20, 20);
        toggleShowHideButton.resize(20, 20);

        toggleShowHideButton.layoutXProperty().bind(widthProperty().subtract(32));
        toggleShowHideButton.layoutYProperty().bind(heightProperty().subtract(20).divide(2));

        getChildren().add(toggleShowHideButton);
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

    private void toggleShowHide() {
        int anchor = getAnchor();
        int caret = getCaretPosition();

        updatingDisplay = true;

        try {
            setText(isHidden ? mask(originalText) : originalText);

            int length = getLength();

            selectRange(
                Math.min(anchor, length),
                Math.min(caret, length)
            );

        } finally {
            updatingDisplay = false;
        }

        toggleShowHideButton.setGraphic(
            isHidden ? textHiddenIcon : textVisibleIcon
        );
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

    @Override
    public void replaceText(int start, int end, String text) {
        if (text == null) return;

        originalText = originalText.substring(0, start)
            + text
            + originalText.substring(end);

        toggleShowHide();
        positionCaret(start + text.length());
    }

    @Override
    public void replaceSelection(String text) {
        replaceText(
            getSelection().getStart(),
            getSelection().getEnd(),
            text
        );
    }
}