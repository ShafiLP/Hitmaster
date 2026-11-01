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
    public static SVGPath streamToSvgPath(InputStream stream) {
        SVGPath parsedIcon = new SVGPath();
        parsedIcon.getStyleClass().add("icon");

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
