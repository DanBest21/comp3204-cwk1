package uk.ac.soton.ecs.db5n17.ch5;

import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.matcher.*;
import org.openimaj.feature.local.matcher.consistent.ConsistentLocalFeatureMatcher2d;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.math.geometry.transforms.HomographyRefinement;
import org.openimaj.math.geometry.transforms.estimation.RobustAffineTransformEstimator;
import org.openimaj.math.geometry.transforms.estimation.RobustHomographyEstimator;
import org.openimaj.math.model.fit.RANSAC;

import java.io.IOException;
import java.net.URL;

/**
 * OpenIMAJ Tutorial - Chapter 5: SIFT and feature matching
 *
 */
public class App
{
    public static void main( String[] args ) throws IOException
    {
        // Read two images, the first will be used as query to find a matching local feature in the second target image.
        MBFImage query = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/query.jpg"));
        MBFImage target = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/target.jpg"));

        // Create a Difference of Gaussian feature detector, which is described by a SIFT detector.
        // These features will be invariant to size changes, rotation and position.
        DoGSIFTEngine engine = new DoGSIFTEngine();
        LocalFeatureList<Keypoint> queryKeypoints = engine.findFeatures(query.flatten());
        LocalFeatureList<Keypoint> targetKeypoints = engine.findFeatures(target.flatten());

        // Construct a matcher, that will match points in the query image to points in the target image.
        LocalFeatureMatcher<Keypoint> matcher = new BasicMatcher<>(80);
        matcher.setModelFeatures(queryKeypoints);
        matcher.findMatches(targetKeypoints);

        // Draw any matches found between the two images.
        MBFImage basicMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.RED);
        DisplayUtilities.display(basicMatches, "Basic Matches");

        // Exercise 1: Different matchers
        // Construct a two-way matcher, that will match points where both images assume the roles of the model and the object.
        // Matches must be found in both images to be accepted, and any one to many matches are also rejected.
        matcher = new BasicTwoWayMatcher<>();
        matcher.setModelFeatures(queryKeypoints);
        matcher.findMatches(targetKeypoints);

        // Draw any matches found between the two images.
        MBFImage basicTwoWayMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.RED);
        DisplayUtilities.display(basicTwoWayMatches, "Basic Two-way Matches");

        // Construct a basic keypoint matcher, which will match to the two closest keypoints to the target and check that the
        // distance to them is sufficiently large.
        matcher = new FastBasicKeypointMatcher<>();
        matcher.setModelFeatures(queryKeypoints);
        matcher.findMatches(targetKeypoints);

        // Draw any matches found between the two images.
        MBFImage fastBasicMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.RED);
        DisplayUtilities.display(fastBasicMatches, "Fast Basic Keypoint Matches");

        // Setup a RANSAC model fitter that is configured to find Affine Transforms, and a new ConsistentLocalFeatureMatcher that uses this model.
        RobustAffineTransformEstimator modelFitter = new RobustAffineTransformEstimator(50.0, 1500,
                new RANSAC.PercentageInliersStoppingCondition(0.5));
        matcher = new ConsistentLocalFeatureMatcher2d<>(
                new FastBasicKeypointMatcher<>(8), modelFitter);

        // Perform matches based on this model.
        matcher.setModelFeatures(queryKeypoints);
        matcher.findMatches(targetKeypoints);

        // Draw and display the image.
        MBFImage consistentMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(),
                RGBColour.RED);
        DisplayUtilities.display(consistentMatches, "Consistent Matches (Affine Transform/RANSAC)");

        // Exercise 2: Different models
        // Setup a RANSAC model fitter that is configured using homography (and a Bucketing sampling strategy), and a new ConsistentLocalFeatureMatcher
        // that uses this model.
        RobustHomographyEstimator modelHomographyFitter = new RobustHomographyEstimator(50.0, 1500,
                new RANSAC.PercentageInliersStoppingCondition(0.5), HomographyRefinement.SINGLE_IMAGE_TRANSFER);
        matcher = new ConsistentLocalFeatureMatcher2d<>(
                new FastBasicKeypointMatcher<>(8), modelHomographyFitter);

        // Perform matches based on this model.
        matcher.setModelFeatures(queryKeypoints);
        matcher.findMatches(targetKeypoints);

        // Draw and display the image.
        MBFImage consistentHomographyMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(),
                RGBColour.RED);
        DisplayUtilities.display(consistentHomographyMatches, "Consistent Matches (Homography/RANSAC)");

        // Setup a LMedS model fitter that is configured using homography (and a Bucketing sampling strategy), and a new ConsistentLocalFeatureMatcher
        // that uses this model.
        RobustHomographyEstimator modelLMedSFitter = new RobustHomographyEstimator(10.0, HomographyRefinement.SINGLE_IMAGE_TRANSFER);
        matcher = new ConsistentLocalFeatureMatcher2d<>(
                new FastBasicKeypointMatcher<>(8), modelLMedSFitter);

        // Perform matches based on this model.
        matcher.setModelFeatures(queryKeypoints);
        matcher.findMatches(targetKeypoints);

        // Draw and display the image.
        MBFImage consistentLMedSMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(),
                RGBColour.RED);
        DisplayUtilities.display(consistentLMedSMatches, "Consistent Matches (Homography/LMedS)");

        // Draw a box of the estimated position of the query image in the target image.
        target.drawShape(
                query.getBounds().transform(modelFitter.getModel().getTransform().inverse()), 3,RGBColour.BLUE);
        DisplayUtilities.display(target, "Estimated position of the query image in the target image");
    }
}
