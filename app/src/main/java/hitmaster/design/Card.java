package hitmaster.design;

import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public abstract class Card extends StackPane {

    protected static int SIZE = 150;

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
        setPrefSize(SIZE, SIZE);

        Rectangle clip = new Rectangle();

        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());

        clip.setArcWidth(24);
        clip.setArcHeight(24);

        setClip(clip);

        setClip(clip);

        getStyleClass().add("card");


        // ----- Drag & Drop -----
        setOnMousePressed((MouseEvent e) -> {
            if (!isDraggable)
                return;

            mouseX = e.getX();
            mouseY = e.getY();

            toFront();

            if (dragStarted != null)
                dragStarted.run();
        });

        setOnMouseDragged((MouseEvent e) -> {
            if (!isDraggable)
                return;

            if (getParent() != null) {
                Point2D localParam = getParent().sceneToLocal(e.getSceneX(), e.getSceneY());

                double newX = localParam.getX() - mouseX;
                double newY = localParam.getY() - mouseY;
                
                double parentWidth = getParent().getBoundsInLocal().getWidth();
                double parentHeight = getParent().getBoundsInLocal().getHeight();
                
                double maxX = parentWidth - getWidth();
                double maxY = parentHeight - getHeight();
                
                if (newX < 0) newX = 0;
                if (newX > maxX) newX = maxX;
                if (newY < 0) newY = 0;
                if (newY > maxY) newY = maxY;
                
                setLayoutX(newX);
                setLayoutY(newY);
            }

            if (dragListener != null) {
                dragListener.onDrag(e.getSceneX(), e.getSceneY());
            }
        });

        setOnMouseReleased(e -> {
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
        setStyle("-fx-border-radius: 12; -fx-border-color:" + color + ";");
    }

    /**
     * Resets border colour of Card back to black.
     */
    public void resetBorderColor() {
        setStyle("-fx-border-radius: 12; -fx-border-color:black;");
    }

    public static int getSize() {
        return SIZE;
    }
}
