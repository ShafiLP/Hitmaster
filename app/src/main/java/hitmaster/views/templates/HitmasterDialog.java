package hitmaster.views.templates;

import hitmaster.services.Navigation;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;

public class HitmasterDialog extends BorderPane {

    protected String dialogTitle;

    public HitmasterDialog(String dialogTitle) {
        this.dialogTitle = dialogTitle;
        
        setPadding(new Insets(10));
    }
    
    public void show() {
        Navigation.getInstance().show(this);
        Navigation.getInstance().setTitle(dialogTitle);
    }
}
