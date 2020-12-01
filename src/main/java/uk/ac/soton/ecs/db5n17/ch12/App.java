package uk.ac.soton.ecs.db5n17.ch12;

import de.bwaldvogel.liblinear.SolverType;
import org.openimaj.data.DataSource;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.experiment.dataset.sampling.GroupSampler;
import org.openimaj.experiment.dataset.sampling.GroupedUniformRandomisedSampler;
import org.openimaj.experiment.dataset.split.GroupedRandomSplitter;
import org.openimaj.experiment.evaluation.classification.ClassificationEvaluator;
import org.openimaj.experiment.evaluation.classification.ClassificationResult;
import org.openimaj.experiment.evaluation.classification.analysers.confusionmatrix.CMAnalyser;
import org.openimaj.experiment.evaluation.classification.analysers.confusionmatrix.CMResult;
import org.openimaj.feature.DiskCachingFeatureExtractor;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.SparseIntFV;
import org.openimaj.feature.local.data.LocalFeatureListDataSource;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101;
import org.openimaj.image.feature.dense.gradient.dsift.ByteDSIFTKeypoint;
import org.openimaj.image.feature.dense.gradient.dsift.DenseSIFT;
import org.openimaj.image.feature.dense.gradient.dsift.PyramidDenseSIFT;
import org.openimaj.image.feature.local.aggregate.BagOfVisualWords;
import org.openimaj.image.feature.local.aggregate.BlockSpatialAggregator;
import org.openimaj.image.feature.local.aggregate.PyramidSpatialAggregator;
import org.openimaj.io.IOUtils;
import org.openimaj.ml.annotation.linear.LiblinearAnnotator;
import org.openimaj.ml.clustering.ByteCentroidsResult;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.kmeans.ByteKMeans;
import org.openimaj.ml.kernel.HomogeneousKernelMap;
import org.openimaj.util.pair.IntFloatPair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenIMAJ Tutorial - Chapter 12: Classification with Caltech 101
 *
 */
public class App
{
    // A Pyramid Histogram of Words (PHOW) feature extractor.
    static class PHOWExtractor implements FeatureExtractor<DoubleFV, Caltech101.Record<FImage>>
    {
        PyramidDenseSIFT<FImage> pdsift;
        HardAssigner<byte[], float[], IntFloatPair> assigner;

        public PHOWExtractor(PyramidDenseSIFT<FImage> pdsift, HardAssigner<byte[], float[], IntFloatPair> assigner)
        {
            this.pdsift = pdsift;
            this.assigner = assigner;
        }

        public DoubleFV extractFeature(Caltech101.Record<FImage> object)
        {
            // Analyse the dense SIFT features of the image.
            FImage image = object.getImage();
            pdsift.analyseImage(image);

            // Construct a Bag of Visual Words (BoVW) for each Dense SIFT feature in the assigner.
            BagOfVisualWords<byte[]> bovw = new BagOfVisualWords<>(assigner);

            // Compute 4 histograms across the image (2x2 grid).
//            BlockSpatialAggregator<byte[], SparseIntFV> spatial = new BlockSpatialAggregator<byte[], SparseIntFV>(
//                    bovw, 2, 2);

            // Use a PyramidSpatialAggregator with [2, 4] blocks.
            PyramidSpatialAggregator<byte[], SparseIntFV> spatial = new PyramidSpatialAggregator<>(
                    bovw, 2, 4);

            // Return a normalized aggregate histogram.
            return spatial.aggregate(pdsift.getByteKeypoints(0.015f), image.getBounds()).normaliseFV();
        }
    }

    public static void main( String[] args ) throws IOException
    {
        // Load all the data from the Caltech 101 dataset.
        GroupedDataset<String, VFSListDataset<Caltech101.Record<FImage>>, Caltech101.Record<FImage>> allData =
                Caltech101.getData(ImageUtilities.FIMAGE_READER);

        // Load the first five classes of the Caltech 101 dataset (by sampling the allData GroupedDataset variable).
        GroupedDataset<String, ListDataset<Caltech101.Record<FImage>>, Caltech101.Record<FImage>> data =
                GroupSampler.sample(allData, 5, false);

        // Split the data into 15 training images, and 15 testing images.
        GroupedRandomSplitter<String, Caltech101.Record<FImage>> splits =
                new GroupedRandomSplitter<>(allData, 15, 0, 15);

        // Construct a Dense SIFT extractor for usage in a PHOW (Pyramid Histogram of Words).
        DenseSIFT dsift = new DenseSIFT(3, 7);
        PyramidDenseSIFT<FImage> pdsift = new PyramidDenseSIFT<>(dsift, 6f, 4, 6, 8, 10);

        // Train a vector quantiser which returns an assigner for dense SIFT features.
        // Note: This trainer uses a sample of 30 images from each group.
        HardAssigner<byte[], float[], IntFloatPair> assigner =
                trainQuantiser(GroupedUniformRandomisedSampler.sample(splits.getTrainingDataset(), 30), pdsift);

        // Cache the assigner to a file using the IOUtils.writeToFile() method.
        File cachedAssignerFile = new File("C://Users//Dan//Desktop//comp3204//cache//assigner");
        cachedAssignerFile.createNewFile();
        IOUtils.writeToFile(assigner, cachedAssignerFile);

        // Create a new PHOW (Pyramid Histogram of Words) feature extractor.
        FeatureExtractor<DoubleFV, Caltech101.Record<FImage>> extractor = new PHOWExtractor(pdsift, (HardAssigner)IOUtils.readFromFile(cachedAssignerFile));

        // Exercise 1: Apply a Homogeneous Kernel Map
        // Wrap a HomogenousKernelMap of type Chi2 around the extractor.
        // This has a noticeable impact of the performance of the classifier, increasing an average accuracy of about 70% to one of about 80%.
        HomogeneousKernelMap kernelMap = new HomogeneousKernelMap(HomogeneousKernelMap.KernelType.Chi2, HomogeneousKernelMap.WindowType.Rectangular);
        FeatureExtractor<DoubleFV, Caltech101.Record<FImage>> wrappedExtractor = kernelMap.createWrappedExtractor(extractor);

        // Exercise 2: Feature caching
        // Cache the extractor to a file using the DiskCachingFeatureExtractor class, which means features that have already been generated do not need to be regenerated.
        File cachedExtractorFile = new File("C://Users//Dan//Desktop//comp3204//cache//extractor");
        cachedExtractorFile.createNewFile();
        DiskCachingFeatureExtractor<DoubleFV, Caltech101.Record<FImage>> cachedExtractor = new DiskCachingFeatureExtractor<>(cachedExtractorFile, wrappedExtractor);

        // Construct and train a linear classifier using the LiblinearAnnotator class.
        LiblinearAnnotator<Caltech101.Record<FImage>, String> ann = new LiblinearAnnotator<>(
                cachedExtractor, LiblinearAnnotator.Mode.MULTICLASS, SolverType.L2R_L2LOSS_SVC, 1.0, 0.00001);
        ann.train(splits.getTrainingDataset());

        // Evaluate how well our classifier is working and output the accuracy.
        ClassificationEvaluator<CMResult<String>, String, Caltech101.Record<FImage>> eval =
                new ClassificationEvaluator<>(ann, splits.getTestDataset(), new CMAnalyser<Caltech101.Record<FImage>, String>(CMAnalyser.Strategy.SINGLE));

        Map<Caltech101.Record<FImage>, ClassificationResult<String>> guesses = eval.evaluate();
        CMResult<String> result = eval.analyse(guesses);

        // Exercise 3: The whole dataset
        // The level of classifier performance across the entire dataset alongside all the suggested changes is 75%.
        System.out.println(result.getDetailReport());
    }

    // trainQuantiser() builds a HardAssigner that can be used as a vector quantiser.
    static HardAssigner<byte[], float[], IntFloatPair> trainQuantiser(Dataset<Caltech101.Record<FImage>> sample, PyramidDenseSIFT<FImage> pdsift)
    {
        List<LocalFeatureList<ByteDSIFTKeypoint>> allkeys = new ArrayList<>();

        // For each record in the sample, analyse the image and add its dense SIFT features as a key.
        for (Caltech101.Record<FImage> rec : sample)
        {
            FImage img = rec.getImage();

            pdsift.analyseImage(img);
            allkeys.add(pdsift.getByteKeypoints(0.005f));
        }

        // Reduce the sample size to 10000 if more dense SIFT features were found.
        if (allkeys.size() > 10000)
            allkeys = allkeys.subList(0, 10000);

        // Perform K-Means clustering (where K is 600) on these dense SIFT features, and return a HardAssigner object from this.
        ByteKMeans km = ByteKMeans.createKDTreeEnsemble(600);
        DataSource<byte[]> datasource = new LocalFeatureListDataSource<>(allkeys);
        ByteCentroidsResult result = km.cluster(datasource);

        return result.defaultHardAssigner();
    }
}
