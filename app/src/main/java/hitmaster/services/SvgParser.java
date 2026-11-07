package hitmaster.services;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javafx.scene.shape.SVGPath;

public class SvgParser {
    
    public static SVGPath streamToSvgIcon(InputStream stream) {
        SVGPath parsedIcon = streamToSvgPath(stream);
        parsedIcon.getStyleClass().add("icon");

        return parsedIcon;
    }

    public static SVGPath streamToColoredSvgIcon(InputStream stream, String cssColor) {
        SVGPath parsedIcon = streamToSvgPath(stream);
        parsedIcon.setStyle("-fx-fill: " + cssColor + ";");

        return parsedIcon;
    }

    private static SVGPath streamToSvgPath(InputStream stream) {
        SVGPath parsedIcon = new SVGPath();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(stream);

            String path = document
                .getElementsByTagName("path")
                .item(0)
                .getAttributes()
                .getNamedItem("d")
                .getNodeValue();

            parsedIcon.setContent(path);
        }
        catch (IOException | ParserConfigurationException | DOMException | SAXException e) {
            e.printStackTrace();
        }

        return parsedIcon;
    }
}
