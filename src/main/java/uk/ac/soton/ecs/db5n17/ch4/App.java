package uk.ac.soton.ecs.db5n17.ch4;

import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.pixel.statistics.HistogramModel;
import org.openimaj.math.statistics.distribution.MultidimensionalHistogram;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * OpenIMAJ Tutorial - Chapter 4: Global image features
 *
 */
public class App
{
    public static void main( String[] args ) throws IOException
    {
        // MultidimensionalHistogram histogram = new MultidimensionalHistogram( 4, 4, 4 );

        // HistogramModel model = new HistogramModel( 4, 4, 4 );
        // model.estimateModel( image );
        // MultidimensionalHistogram histogram = model.histogram;

        // Load an array of images.
        URL[] imageURLs = new URL[]
                {
                    new URL( "http://openimaj.org/tutorial/figs/hist1.jpg" ),
                    new URL( "http://openimaj.org/tutorial/figs/hist2.jpg" ),
                    new URL( "http://openimaj.org/tutorial/figs/hist3.jpg" )
                };

        // Create a list of MultidimensionalHistogram objects and a 4x4x4 (64) bin HistogramModel object.
        List<MultidimensionalHistogram> histograms = new ArrayList<>();
        HistogramModel model = new HistogramModel(4, 4, 4);

        // For each image, create a colour histogram and add it to the list.
        for( URL u : imageURLs )
        {
            model.estimateModel(ImageUtilities.readMBF(u));
            histograms.add( model.histogram.clone() );
        }

        // double distanceScore = histogram1.compare( histogram2, DoubleFVComparison.EUCLIDEAN );

        // Exercise 1: Finding and displaying similar images
        double minDistance = Double.MAX_VALUE;
        int minImageA = 0, minImageB = 0;

        // For each different histogram, compare the distance between the two.
        for( int i = 0; i < histograms.size(); i++ )
        {
            for( int j = i; j < histograms.size(); j++ )
            {
                // If this is the same histogram, we can skip it.
                if (i == j)
                    continue;

                // Otherwise, compare the two using Euclidean distance.
                // double distance = histograms.get(i).compare( histograms.get(j), DoubleFVComparison.EUCLIDEAN );
                // Exercise 2: Exploring comparison measures
                // When using the INTERSECTION comparison measure, the two most similar images changes from the two sunsets
                // to the sunset coming over the horizon, and the image of the Earth taken from the moon.
                // This shows that the comparison measure is crucial in order to achieve the desired results for such an
                // algorithm.
                double distance = histograms.get(i).compare( histograms.get(j), DoubleFVComparison.INTERSECTION );
                System.out.println("Comparison between histogram " + i + " and histogram " + j + ": " + distance);

                // If this distance is below the so far minimum found distance, update the minDistance variable, and the minImageA and minImageB variables
                // that keep track of the (identifiers of the) two images that formed this minimum distance.
                if (distance < minDistance)
                {
                    minDistance = distance;
                    minImageA = i;
                    minImageB = j;
                }
            }
        }

        // Display the two images that had the most similar colour histograms.
        // In the case of the provided images, this is indeed the two images I would have expected (two separate images of sunsets)
        // to have closer colour histograms.
        DisplayUtilities.display(ImageUtilities.readMBF(imageURLs[minImageA]), "Image A");
        DisplayUtilities.display(ImageUtilities.readMBF(imageURLs[minImageB]), "Image B");
    }
}
