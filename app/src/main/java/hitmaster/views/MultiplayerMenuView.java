package hitmaster.views;

import hitmaster.design.UiBuilder;
import hitmaster.services.Navigation;
import hitmaster.services.SvgParser;
import hitmaster.views.templates.HitmasterDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MultiplayerMenuView extends HitmasterDialog {

    public MultiplayerMenuView() {
        super("Multiplayer");

        // ===== HEADER & DESCRIPTION =====
        VBox headerBox = new VBox(3);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label headerLabel = new Label("Multiplayer");
        headerLabel.getStyleClass().add("header");

        Label descriptionLabel = new Label("Connect to a friend's lobby or create your own one!");
        descriptionLabel.getStyleClass().add("description");

        headerBox.getChildren().addAll(headerLabel, descriptionLabel, UiBuilder.getHorizontalSeparator());
        setTop(headerBox);


        // ===== JOIN & CREATE LOBBY =====
        CreateJoinLobbyView lobbyView = new CreateJoinLobbyView();
        lobbyView.setPadding(new Insets(10, 0, 0, 0));
        setCenter(lobbyView);


        // ===== BOTTOM CONTROLS =====
        VBox bottomBox = new VBox(5);
        HBox controllsBox = new HBox(10);

        Button backButton = new Button();
        backButton.setTooltip(new Tooltip("Go Back"));
        backButton.setGraphic(SvgParser.streamToColoredSvgIcon(
            getClass().getResourceAsStream("/icons/arrow-left.svg"),
            "-primary-brush"
        ));

        backButton.setOnAction(e -> {
            lobbyView.stopDiscovery();
            Navigation.getInstance().toMainMenu();
        });

        controllsBox.getChildren().addAll(backButton, UiBuilder.getHorizontalSpacer());
        bottomBox.getChildren().add(controllsBox);
        setBottom(bottomBox);
    }
}
