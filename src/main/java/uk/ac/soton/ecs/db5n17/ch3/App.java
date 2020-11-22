package uk.ac.soton.ecs.db5n17.ch3;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.connectedcomponent.GreyscaleConnectedComponentLabeler;
import org.openimaj.image.pixel.ConnectedComponent;
import org.openimaj.image.processor.PixelProcessor;
import org.openimaj.image.segmentation.FelzenszwalbHuttenlocherSegmenter;
import org.openimaj.image.segmentation.SegmentationUtilities;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.ml.clustering.FloatCentroidsResult;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.kmeans.FloatKMeans;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * OpenIMAJ Tutorial - Chapter 3: Introduction to clustering, segmentation and connected components
 *
 */
public class App
{
    public static void main( String[] args ) throws IOException
    {
        // Load an image and clone it for use in Exercise 2 later.
        MBFImage input = ImageUtilities.readMBF(new URL("https://upload.wikimedia.org/wikipedia/commons/thumb/a/af/All_Gizah_Pyramids.jpg/1280px-All_Gizah_Pyramids.jpg"));
        MBFImage fhInput = input.clone();

        // Apply a colour-space transform to the image.
        input = ColourSpace.convert(input, ColourSpace.CIE_Lab);

        // Construct the K-Means algorithm.
        FloatKMeans cluster = FloatKMeans.createExact(2);

        // Flatten the pixels of an image into the required form for K-Means Clustering.
        float[][] imageData = input.getPixelVectorNative(new float[input.getWidth() * input.getHeight()][3]);

        // Run the K-Means Clustering algorithm and produce a result.
        // Note: We need to make result and centroids final to use them in the PixelProcessor anonymous class.
        final FloatCentroidsResult result = cluster.cluster(imageData);

        // Print out the co-ordinates of the centroids produced for each class.
        final float[][] centroids = result.centroids;
        for (float[] fs : centroids)
        {
            System.out.println(Arrays.toString(fs));
        }

        // Classify each pixel to its respective class and then replace this pixel with the centroid of its respective class.
        // This code...
//        for (int y=0; y<input.getHeight(); y++)
//        {
//            for (int x=0; x<input.getWidth(); x++)
//            {
//                float[] pixel = input.getPixelNative(x, y);
//                int centroid = assigner.assign(pixel);
//                input.setPixelNative(x, y, centroids[centroid]);
//            }
//        }

        // Exercise 1. The Pixel Processor
        // Can be converted into:
        input.processInplace(new PixelProcessor<Float[]>()
        {
            // Create the HardAssigner in the anonymous class.
            HardAssigner<float[],?,?> assigner = result.defaultHardAssigner();

            // Since processPixel needs to use the object version of the Float class, we need to convert to the primitive version of float for the HardAssigner to use.
            public Float[] processPixel(Float[] pixel)
            {
                // First convert the Float[] pixel variable to a primitive.
                float[] pixelPrim = new float[pixel.length];

                for (int i = 0; i < pixel.length; i++)
                {
                    pixelPrim[i] = pixel[i];
                }

                // Get the centroid value using the primitive float[].
                int centroid = assigner.assign(pixelPrim);

                // Now convert centroids[centroid] (which is float[]) back into an Object array of type Float[].
                Float[] processedPixel = new Float[centroids[centroid].length];

                for (int i = 0; i < centroids[centroid].length; i++)
                {
                    processedPixel[i] = centroids[centroid][i];
                }

                return processedPixel;
            }
        });

        /*
         * The advantage of using a PixelProcessor over the simplistic method of two for loops is it is significantly more efficient than looping over each pixel in the image,
         * as it instead just loops through each pixel in the image instead in the form of a Float[] variable.
         * However, the key disadvantage is that it requires these pixels to be in the object type of Float[], whereas the HardAssigner requires the primitive type of float[],
         * meaning we have to do some fairly inefficient (though not as costly as the nested for loops) conversions to process each pixel.
         */

        // Display the resultant image.
        input = ColourSpace.convert(input, ColourSpace.RGB);
        DisplayUtilities.display(input, "K-Means Clustering");

        // Find connected components using the GreyscaleConnectedComponentLabeller.
        GreyscaleConnectedComponentLabeler labeler = new GreyscaleConnectedComponentLabeler();
        List<ConnectedComponent> components = labeler.findComponents(input.flatten());

        // Label connected components as points if they are at least 50 pixels.
        int i = 0;
        for (ConnectedComponent comp : components)
        {
            if (comp.calculateArea() < 50)
                continue;
            input.drawText("Point:" + (i++), comp.calculateCentroidPixel(), HersheyFont.TIMES_MEDIUM, 20);
        }

        // Display the segmented image.
        DisplayUtilities.display(input, "Primitive Segmentation");

        // Exercise 2: A real segmentation algorithm
        // Create the FelzenszwalbHuttenlocherSegmenter, use it to create a list of ConnectComponent objects, and then render the result.
        FelzenszwalbHuttenlocherSegmenter<MBFImage> fhSegmenter = new FelzenszwalbHuttenlocherSegmenter<>();
        List<ConnectedComponent> fhComponents = fhSegmenter.segment(fhInput);
        MBFImage segmentedImage = SegmentationUtilities.renderSegments(fhInput, fhComponents);

        // This technique is quite clearly far more complex and accurate than our naïve approach.
        DisplayUtilities.display(segmentedImage, "Felzenszwalb Huttenlocher Segmentation");
    }
}
