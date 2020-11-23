package uk.ac.soton.ecs.db5n17.ch6;

import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.dataset.FlickrImageDataset;
import org.openimaj.util.api.auth.DefaultTokenFactory;
import org.openimaj.util.api.auth.common.FlickrAPIToken;

import java.util.Map;

/**
 * OpenIMAJ Tutorial - Chapter 6: Image Datasets
 *
 */
public class App
{
    public static void main( String[] args ) throws Exception
    {
        // Read in images on the drive as grey-scale FImages.
        VFSListDataset<FImage> images =
                new VFSListDataset<>("C:/Users/Dan/Pictures/WoW/Shadowlands", ImageUtilities.FIMAGE_READER);

        // Print out the number of items in the dataset.
        System.out.println(images.size());

        // Display a random image from the dataset.
        DisplayUtilities.display(images.getRandomInstance(), "A random image from the dataset");

        // Display all images.
        DisplayUtilities.display("My images", images);

        // Create a dataset from a zip file hosted on a web-server containing faces.
        VFSListDataset<FImage> faces =
                new VFSListDataset<>("zip:http://datasets.openimaj.org/att_faces.zip", ImageUtilities.FIMAGE_READER);
        DisplayUtilities.display("ATT faces", faces);

        // Create a grouped dataset from the same zip file that maintains the directory groupings.
        VFSGroupDataset<FImage> groupedFaces =
                new VFSGroupDataset<>( "zip:http://datasets.openimaj.org/att_faces.zip", ImageUtilities.FIMAGE_READER);

        // Output each group in its own window, which correspond to an individual in the dataset.
        for (final Map.Entry<String, VFSListDataset<FImage>> entry : groupedFaces.entrySet())
        {
            DisplayUtilities.display(entry.getKey(), entry.getValue());
        }

        // Dynamically construct a dataset of images from a Flickr search for cats.
        FlickrAPIToken flickrToken = DefaultTokenFactory.get(FlickrAPIToken.class);
        FlickrImageDataset<FImage> cats =
                FlickrImageDataset.create(ImageUtilities.FIMAGE_READER, flickrToken, "cats", 10);
        DisplayUtilities.display("Cats", cats);

        // Exercise 1: Exploring Grouped Datasets
        // Loop through each individual in the dataset and display a random instance of an image of them.
        for (String name : groupedFaces.keySet())
        {
            DisplayUtilities.display(groupedFaces.get(name).getRandomInstance(), "Random image of " + name);
        }

        // Exercise 2: Find out more about VFS datasets
        // Other sources of data that are supported for building datasets include:
        // - URLs
        // - TAR files
        // - JAR files
        // - HTTP files (4 and 5)
        // - FTP files

        // Exercise 3: Try the BingImageDataset dataset
        // (Skipped due to the API currently currently not working for BingImageDataset)

        // Exercise 4: Using MapBackedDataset
        // (Skipped due to the API currently currently not working for BingImageDataset)
    }
}
