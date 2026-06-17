package hitmaster.design;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class StatusBar extends BorderPane {

    private final Label infoLabel;
    private final Label timerLabel;

    public StatusBar() {

        infoLabel = new Label("Waiting for players...");
        timerLabel = new Label("00:00 ⏱");

        infoLabel.getStyleClass().add("subheader");
        timerLabel.getStyleClass().add("subheader");

        this.setLeft(infoLabel);
        this.setRight(timerLabel);

        this.setPadding(new Insets(8));
        this.setPrefHeight(40);

        this.getStyleClass().add("status-bar");
    }

    public void setInfoText(String text) {
        infoLabel.setText(text);
    }

    public void setTimerText(String text) {
        timerLabel.setText(text);
    }

    public void setRemainingTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;

        timerLabel.setText(String.format("%02d:%02d", minutes, secs) + " ⏱");
    }

    public String getInfoText() {
        return infoLabel.getText();
    }

    public String getTimerText() {
        return timerLabel.getText();
    }
}