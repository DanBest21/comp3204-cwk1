package uk.ac.soton.ecs.db5n17.ch1;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;

/**
 * OpenIMAJ Tutorial - Chapter 1: Getting started with OpenIMAJ using Maven
 *
 */
public class App
{
    public static void main( String[] args )
    {
    	// Create an image
        MBFImage image = new MBFImage(800,70, ColourSpace.RGB);

        // Fill the image with red
        image.fill(RGBColour.RED);

        // Exercise 1: Playing with the sample application
        // Render some test into the image
        image.drawText("Make America Great Again", 10, 60, HersheyFont.TIMES_BOLD, 50, RGBColour.WHITE);

        // Apply a Gaussian blur
        image.processInplace(new FGaussianConvolve(2f));
        
        // Display the image
        DisplayUtilities.display(image);
    }
}
