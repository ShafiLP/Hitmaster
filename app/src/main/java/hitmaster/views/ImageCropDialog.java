package hitmaster.views;

import hitmaster.services.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ImageCropDialog {

    private final Image sourceImage;
    private final Stage dialogStage;
    
    private double cropX = 50;
    private double cropY = 50;
    private double cropSize = 150;
    
    private double mouseStartX;
    private double mouseStartY;
    private boolean isDragging = false;
    private boolean isResizing = false;

    private WritableImage croppedResult = null;

    public ImageCropDialog(Stage owner, Image sourceImage) {
        this.sourceImage = sourceImage;
        this.dialogStage = new Stage();
        this.dialogStage.initModality(Modality.APPLICATION_MODAL);
        this.dialogStage.initOwner(owner);
        this.dialogStage.setTitle("Crop Profile Picture");
    }

    public WritableImage showAndGetResult() {
        double displayWidth = sourceImage.getWidth();
        double displayHeight = sourceImage.getHeight();
        double maxDimension = 400; // Max size

        if (displayWidth > maxDimension || displayHeight > maxDimension) {
            double ratio = displayWidth / displayHeight;
            if (ratio > 1) {
                displayWidth = maxDimension;
                displayHeight = maxDimension / ratio;
            }
            else {
                displayHeight = maxDimension;
                displayWidth = maxDimension * ratio;
            }
        }

        final double finalWidth = displayWidth;
        final double finalHeight = displayHeight;

        cropSize = Math.min(finalWidth, finalHeight) * 0.6;
        cropX = (finalWidth - cropSize) / 2;
        cropY = (finalHeight - cropSize) / 2;

        Canvas canvas = new Canvas(finalWidth, finalHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Runnable draw = () -> {
            gc.clearRect(0, 0, finalWidth, finalHeight);
            
            gc.drawImage(sourceImage, 0, 0, finalWidth, finalHeight);

            gc.setFill(new Color(0, 0, 0, 0.5));
            gc.fillRect(0, 0, finalWidth, finalHeight);

            gc.save();
            gc.beginPath();
            gc.rect(cropX, cropY, cropSize, cropSize);
            gc.clip();
            gc.drawImage(sourceImage, 0, 0, finalWidth, finalHeight);

            gc.setStroke(new Color(1, 1, 1, 0.35));
            gc.setLineWidth(1);
            gc.setLineDashes(6, 4); 

            double third = cropSize / 3;
            gc.strokeLine(cropX + third, cropY, cropX + third, cropY + cropSize);
            gc.strokeLine(cropX + third * 2, cropY, cropX + third * 2, cropY + cropSize);


            gc.strokeLine(cropX, cropY + third, cropX + cropSize, cropY + third);
            gc.strokeLine(cropX, cropY + third * 2, cropX + cropSize, cropY + third * 2);
            
            gc.setLineDashes(null);
            gc.restore();


            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeRect(cropX, cropY, cropSize, cropSize);


            gc.setFill(Color.WHITE);
            gc.fillRect(cropX + cropSize - 6, cropY + cropSize - 6, 8, 8);
        };

        draw.run();

        canvas.setOnMouseMoved(e -> {
            double handleX = cropX + cropSize;
            double handleY = cropY + cropSize;
            
            if (Math.abs(e.getX() - handleX) < 15 && Math.abs(e.getY() - handleY) < 15) {
                canvas.setCursor(Cursor.NW_RESIZE);
            }
            else if (e.getX() >= cropX && e.getX() <= cropX + cropSize &&
                       e.getY() >= cropY && e.getY() <= cropY + cropSize) {
                canvas.setCursor(Cursor.MOVE);
            }
            else {
                canvas.setCursor(Cursor.DEFAULT);
            }
        });

        canvas.setOnMousePressed(e -> {
            mouseStartX = e.getX();
            mouseStartY = e.getY();

            double handleX = cropX + cropSize;
            double handleY = cropY + cropSize;
            
            if (Math.abs(e.getX() - handleX) < 15 && Math.abs(e.getY() - handleY) < 15) {
                isResizing = true;
            }
            else if (e.getX() >= cropX && e.getX() <= cropX + cropSize &&
                     e.getY() >= cropY && e.getY() <= cropY + cropSize) {
                isDragging = true;
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (isResizing) {
                double targetSizeX = e.getX() - cropX;
                double targetSizeY = e.getY() - cropY;
                
                double newSize = Math.max(50, (targetSizeX + targetSizeY) / 2);
                
                if (cropX + newSize > finalWidth) {
                    newSize = finalWidth - cropX;
                }

                if (cropY + newSize > finalHeight) {
                    newSize = finalHeight - cropY;
                }
                
                cropSize = newSize;

            }
            else if (isDragging) {
                double deltaX = e.getX() - mouseStartX;
                double deltaY = e.getY() - mouseStartY;

                double newX = cropX + deltaX;
                double newY = cropY + deltaY;

                if (newX >= 0 && newX + cropSize <= finalWidth) {
                    cropX = newX;
                }

                if (newY >= 0 && newY + cropSize <= finalHeight) {
                    cropY = newY;
                }
            }

            mouseStartX = e.getX();
            mouseStartY = e.getY();
            draw.run();
        });

        canvas.setOnMouseReleased(e -> {
            isDragging = false;
            isResizing = false;
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("error-button");
        cancelBtn.setOnAction(e -> dialogStage.close());

        Button cropBtn = new Button("Apply");
        cropBtn.getStyleClass().add("primary-button");
        cropBtn.setOnAction(e -> {
            double scaleX = sourceImage.getWidth() / finalWidth;
            double scaleY = sourceImage.getHeight() / finalHeight;

            int sourceX = (int) (cropX * scaleX);
            int sourceY = (int) (cropY * scaleY);
            int sourceSize = (int) (cropSize * scaleX);

            sourceX = Math.max(0, Math.min(sourceX, (int) sourceImage.getWidth() - 1));
            sourceY = Math.max(0, Math.min(sourceY, (int) sourceImage.getHeight() - 1));
            sourceSize = Math.min(sourceSize, (int) sourceImage.getWidth() - sourceX);
            sourceSize = Math.min(sourceSize, (int) sourceImage.getHeight() - sourceY);

            if (sourceSize > 0) {
                croppedResult = new WritableImage(sourceImage.getPixelReader(), sourceX, sourceY, sourceSize, sourceSize);
            }
            dialogStage.close();
        });

        HBox buttons = new HBox(10, cancelBtn, cropBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        Label infoLabel = new Label("Drag to move, pull the bottom-right corner to resize.");
        infoLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");

        VBox layout = new VBox(10, infoLabel, canvas, buttons);
        layout.setPadding(new Insets(15));
        layout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(layout);
        ThemeManager.getInstance().registerScene(scene);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();

        return croppedResult;
    }
}