package uk.ac.soton.ecs.db5n17.ch2;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.shape.Ellipse;

import java.io.IOException;
import java.net.URL;

/**
 * OpenIMAJ Tutorial - Chapter 2: Processing your first image
 *
 */
public class App
{
    public static void main( String[] args ) throws IOException
    {
        MBFImage image = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/sinaface.jpg"));
        DisplayUtilities.createNamedWindow("window", "Chapter 2: Processing your first image");

        displayImageAndWait(image);
        displayImageAndWait(image.getBand(0));

        MBFImage clone = image.clone();

        // This code...
//        for (int y=0; y<image.getHeight(); y++)
//        {
//            for(int x=0; x<image.getWidth(); x++)
//            {
//                clone.getBand(1).pixels[y][x] = 0;
//                clone.getBand(2).pixels[y][x] = 0;
//            }
//        }
        // Can be simplified to:
        clone.getBand(1).fill(0f);
        clone.getBand(2).fill(0f);

        displayImageAndWait(clone);

        image.processInplace(new CannyEdgeDetector());

        displayImageAndWait(image);

        // First draw the speech bubbles as filled white ellipses.
        image.drawShapeFilled(new Ellipse(700f, 450f, 20f, 10f, 0f), RGBColour.WHITE);
        image.drawShapeFilled(new Ellipse(650f, 425f, 25f, 12f, 0f), RGBColour.WHITE);
        image.drawShapeFilled(new Ellipse(600f, 380f, 30f, 15f, 0f), RGBColour.WHITE);
        image.drawShapeFilled(new Ellipse(500f, 300f, 100f, 70f, 0f), RGBColour.WHITE);

        // Then draw over them with a black ellipse outline to give a border effect.
        image.drawShape(new Ellipse(700f, 450f, 20f, 10f, 0f), 3, RGBColour.BLACK);
        image.drawShape(new Ellipse(650f, 425f, 25f, 12f, 0f), 3, RGBColour.BLACK);
        image.drawShape(new Ellipse(600f, 380f, 30f, 15f, 0f), 3, RGBColour.BLACK);
        image.drawShape(new Ellipse(500f, 300f, 100f, 70f, 0f), 3, RGBColour.BLACK);
        image.drawText("OpenIMAJ is", 425, 300, HersheyFont.ASTROLOGY, 20, RGBColour.BLACK);
        image.drawText("Awesome", 425, 330, HersheyFont.ASTROLOGY, 20, RGBColour.BLACK);
        DisplayUtilities.displayName(image, "window");
    }

    // Helper function to ensure that the user has to hit enter to cycle to the next image in the program
    private static void displayImageAndWait(MBFImage image) throws IOException
    {
        System.out.println("Press to continue...");
        DisplayUtilities.displayName(image, "window");
        System.in.read();
    }

    // Equivalent helper function for FImages rather than MBFImages
    private static void displayImageAndWait(FImage image) throws IOException
    {
        System.out.println("Press to continue...");
        DisplayUtilities.displayName(image, "window");
        System.in.read();
    }
}
