package uk.ac.soton.ecs.db5n17.ch13;

import org.apache.commons.vfs2.FileSystemException;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.experiment.dataset.split.GroupedRandomSplitter;
import org.openimaj.experiment.dataset.util.DatasetAdaptors;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.model.EigenImages;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;

import java.nio.file.FileSystem;
import java.util.*;

/**
 * OpenIMAJ Tutorial - Chapter 13: Face recognition 101 - Eigenfaces
 *
 */
public class App
{
    public static void main( String[] args ) throws FileSystemException
    {
        // Load the dataset.
        VFSGroupDataset<FImage> dataset =
                new VFSGroupDataset<FImage>("zip:http://datasets.openimaj.org/att_faces.zip", ImageUtilities.FIMAGE_READER);

        // Split the dataset into training and testing data.
        // Exercise 2: Explore the effect of training set size
        // Reducing the training set size has a significant effect on the performance of the classifier - the improvement of more training data has diminishing returns beyond
        // a certain point, so by reducing the number of training images, we can see a drastically steeper fall in accuracy. Furthermore, the accuracy of the classifier becomes
        // less consistent than the ~93% at 5 images. We can also see that visually, the first twelve basis images produced look more and more similar to each other the more we
        // lower this value (the square glasses become quite a defining feature).
        int nTraining = 5;
        int nTesting = 5;
        GroupedRandomSplitter<String, FImage> splits =
                new GroupedRandomSplitter<String, FImage>(dataset, nTraining, 0, nTesting);
        GroupedDataset<String, ListDataset<FImage>, FImage> training = splits.getTrainingDataset();
        GroupedDataset<String, ListDataset<FImage>, FImage> testing = splits.getTestDataset();

        // Use the training data to learn the PCA basis.
        List<FImage> basisImages = DatasetAdaptors.asList(training);
        int nEigenvectors = 100;
        EigenImages eigen = new EigenImages(nEigenvectors);
        eigen.train(basisImages);

        // Draw the first twelve basis vectors (Eigenfaces).
        List<FImage> eigenFaces = new ArrayList<FImage>();
        for (int i = 0; i < 12; i++) {
            eigenFaces.add(eigen.visualisePC(i));
        }
        DisplayUtilities.display("EigenFaces", eigenFaces);

        // Build a database of features from the training images.
        Map<String, DoubleFV[]> features = new HashMap<String, DoubleFV[]>();
        for (final String person : training.getGroups())
        {
            final DoubleFV[] fvs = new DoubleFV[nTraining];

            for (int i = 0; i < nTraining; i++)
            {
                final FImage face = training.get(person).get(i);
                fvs[i] = eigen.extractFeature(face);
            }
            features.put(person, fvs);
        }

        // Exercise 1: Reconstructing faces
        // First, generate a random number between 0 to n - 1, which corresponds to the person in the training group we will select.
        int randomPerson = (int)(Math.random() * training.size());
        DoubleFV[] randomFeatures = features.get(Arrays.asList(training.getGroups().toArray()).get(randomPerson));
        // Then, get a random feature by generating a random number between 0 and k - 1, where k is the number of feature vectors that correspond to that person in the set.
        int randomFeature = (int)(Math.random() * randomFeatures.length);
        // Finally, reconstruct the face and then display the normalised result.
        FImage reconstructedFace = eigen.reconstruct(randomFeatures[randomFeature]);
        DisplayUtilities.display(reconstructedFace.normalise());

        // Loop through each identifier (person) in the testing dataset.
        double correct = 0, incorrect = 0;
        for (String truePerson : testing.getGroups())
        {
            // For each face of that person in the dataset:
            for (FImage face : testing.get(truePerson))
            {
                // Extract the feature of the image.
                DoubleFV testFeature = eigen.extractFeature(face);

                // Compare the extracted feature with every feature in the training data, for every identifier in the training data.
                // Return the minimum distance and corresponding identifier for the closest feature using Euclidean distance, which
                // should correspond to the person with the most similar face.
                String bestPerson = null;
                // Exercise 3: Apply a threshold
                // Apply a threshold, which is twice the current minimum distance. If this threshold is exceeded, we waive the prediction and simply
                // return "unknown" to indicate the model is unsure. This value will then not affect the accuracy of the model.
                // 7 times the current minimum distance seems to work well as a threshold for this situation, as it slightly improves the accuracy of the model
                // to ~95%, although makes it a bit more inconsistent based on the order in which the faces are compared.
                double minDistance = Double.MAX_VALUE, thresholdModifier = 7.0, threshold = minDistance * thresholdModifier;
                for (final String person : features.keySet())
                {
                    for (final DoubleFV fv : features.get(person))
                    {
                        double distance = fv.compare(testFeature, DoubleFVComparison.EUCLIDEAN);

                        if (distance > threshold)
                        {
                            bestPerson = "unknown";
                        }

                        if (distance < minDistance)
                        {
                            minDistance = distance;
                            threshold = minDistance * thresholdModifier;
                            bestPerson = person;
                        }
                    }
                }

                // Determine if the model was correct or not, and update the correct and incorrect variables appropriately.
                System.out.println("Actual: " + truePerson + "\tguess: " + bestPerson);

                if (!bestPerson.equals("unknown"))
                {
                    if (truePerson.equals(bestPerson))
                        correct++;
                    else
                        incorrect++;
                }
            }
        }

        System.out.println("Accuracy: " + (correct / (correct + incorrect)));
    }
}
