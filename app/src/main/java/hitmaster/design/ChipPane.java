package hitmaster.design;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class ChipPane extends HBox {

    private final StackPane[] slots = new StackPane[3];
    private int activeChipsCount = 0;

    public ChipPane() {
        super(15); // 15px = Padding between chips
        this.setAlignment(Pos.CENTER);

        // 3 Placeholder slots
        for (int i = 0; i < 3; i++) {
            slots[i] = createPlaceholderSlot();
            this.getChildren().add(slots[i]);
        }
    }

    /**
     * Creates a circle with dotted border as placehol
     */
    private StackPane createPlaceholderSlot() {
        StackPane slot = new StackPane();
        slot.setPrefSize(65, 65);
        slot.setMinSize(65, 65);
        slot.setMaxSize(65, 65);
        
        slot.getStyleClass().add("chip-placeholder");
        return slot;
    }

    /**
     * Creates a HITMASTER chip label.
     */
    private Label createHitmasterChip() {
        Label chip = new Label("HIT\nMASTER");
        chip.setAlignment(Pos.CENTER);
        chip.setPrefSize(61, 61);
        
        chip.getStyleClass().add("chip");
        return chip;
    }

    /**
     * Adds a chip to the next free slot.
     */
    public void addChip() {
        if (activeChipsCount < 3) {
            Label newChip = createHitmasterChip();
            slots[activeChipsCount].getChildren().add(newChip);
            activeChipsCount++;
        }
    }

    /**
     * Removes the last added chip.
     */
    public void removeChip() {
        if (activeChipsCount > 0) {
            activeChipsCount--;
            slots[activeChipsCount].getChildren().clear();
        }
    }

    /**
     * Returns the current amount of chips.
     */
    public int getActiveChipsCount() {
        return activeChipsCount;
    }
}
