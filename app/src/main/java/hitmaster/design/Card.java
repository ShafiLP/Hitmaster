package hitmaster.design;

import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public abstract class Card extends StackPane {

    protected boolean isDraggable = true;
    
    protected double mouseX;
    protected double mouseY;

    protected Runnable dragStarted;
    protected DragListener dragListener;
    protected Runnable dragFinished;

    public interface DragListener {
        void onDrag(double sceneX, double sceneY);
    }

    /**
     * Created a draggable card object for User Interfaces.
     * Initializes drag & drop logic, sets style and size.
     */
    public Card() {
        // ----- Set Layout and Style -----
        this.setPrefSize(150, 150);

        Rectangle clip = new Rectangle();

        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());

        clip.setArcWidth(24);
        clip.setArcHeight(24);

        setClip(clip);

        this.setClip(clip);

        this.getStyleClass().add("card");


        // ----- Drag & Drop -----
        this.setOnMousePressed((MouseEvent e) -> {
            if (!isDraggable)
                return;

            mouseX = e.getX();
            mouseY = e.getY();

            this.toFront();

            if (dragStarted != null)
                dragStarted.run();
        });

        this.setOnMouseDragged((MouseEvent e) -> {
            if (!isDraggable)
                return;

            if (getParent() != null) {
                Point2D localParam = getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
                setLayoutX(localParam.getX() - mouseX);
                setLayoutY(localParam.getY() - mouseY);
            }

            if (dragListener != null) {
                dragListener.onDrag(e.getSceneX(), e.getSceneY());
            }
        });

        this.setOnMouseReleased(e -> {
            if (!isDraggable)
                return;

            if (dragFinished != null)
                dragFinished.run();
        });
    }

    /**
     * Set the action to run when starting to drag the Card.
     * @param dragStarted Runnable action to run when starting dragging.
     */
    public void setOnDragStarted(Runnable dragStarted) {
        this.dragStarted = dragStarted;
    }

    /**
     * Set the action to run while dragging the Card.
     * @param dragListener Runnable action to run during drag action.
     */
    public void setOnDragged(DragListener dragListener) {
        this.dragListener = dragListener;
    }

    /**
     * Set the action to run while dragging the Card.
     * @param dragListener Runnable action to run during drag action.
     */
    public void setOnDragFinished(Runnable dragFinished) {
        this.dragFinished = dragFinished;
    }

    /**
     * Set state if Card should be draggable.
     * @param draggable Boolean if card should be set draggable or not.
     */
    public void setDraggable(boolean draggable) {
        this.isDraggable = draggable;
    }

    /**
     * Get drag state of Card.
     * @return True if Card is draggable, false if Card is not draggable.
     */
    public boolean isDraggable() {
        return isDraggable;
    }

    /**
     * Sets border colour of the Card.
     * @param color CSS attribute for Border Color as String.
     */
    public void setBorderColor(String color) {
        this.setStyle("-fx-border-radius: 12; -fx-border-color:" + color + ";");
    }

    /**
     * Resets border colour of Card back to black.
     */
    public void resetBorderColor() {
        this.setStyle("-fx-border-radius: 12; -fx-border-color:black;");
    }
}
