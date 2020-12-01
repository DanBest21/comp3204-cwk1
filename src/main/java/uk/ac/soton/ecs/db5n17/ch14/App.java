package uk.ac.soton.ecs.db5n17.ch14;

import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.experiment.dataset.sampling.GroupSampler;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.resize.ResizeProcessor;
import org.openimaj.time.Timer;
import org.openimaj.util.function.Operation;
import org.openimaj.util.parallel.Parallel;
import org.openimaj.util.parallel.partition.RangePartitioner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * OpenIMAJ Tutorial - Chapter 14: Parallel Processing
 *
 */
public class App
{
    public static void main( String[] args ) throws IOException
    {
        // OpenIMAJ Parallel implementation of a for loop from 0 to 9 that prints each number.
        // Notice the order has changed since this is concurrent code.
        Parallel.forIndex(0, 10, 1, new Operation<Integer>()
        {
            public void perform(Integer i)
            {
                System.out.println(i);
            }
        });

        // Load the Caltech 101 image dataset.
        VFSGroupDataset<MBFImage> allImages = Caltech101.getImages(ImageUtilities.MBFIMAGE_READER);

        // Create a subset of images from the first 8 groups.
        GroupedDataset<String, ListDataset<MBFImage>, MBFImage> images = GroupSampler.sample(allImages, 8, false);

        // Build the average image for each group.
        final List<MBFImage> output = new ArrayList<>();
        final ResizeProcessor resize = new ResizeProcessor(200);

        // Time how long the averaging process takes.
        Timer t1 = Timer.timer();

        // For each image group:
        for (ListDataset<MBFImage> clzImages : images.values())
        {
            // Create an image for accumulation.
            MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);

            // For each image in the group:
            for (MBFImage i : clzImages)
            {
                // Create a temporary white image.
                MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);
                tmp.fill(RGBColour.WHITE);

                // Normalise the image and draw it onto the white image.
                MBFImage small = i.process(resize).normalise();
                int x = (200 - small.getWidth()) / 2;
                int y = (200 - small.getHeight()) / 2;
                tmp.drawImage(small, x, y);

                // Accumulate the image.
                current.addInplace(tmp);
            }

            // Divide the accumulated image by the number of samples used to create it.
            current.divideInplace((float) clzImages.size());

            // Add the newly created average image to the output list.
            output.add(current);
        }

        System.out.println("Time: " + t1.duration() + "ms");

        // Display the results of the image averaging.
        DisplayUtilities.display("Images (non-parallelied)", output);

        output.clear();

        // Time how long the averaging process takes.
        Timer t2 = Timer.timer();

        // For each image group:
        for (ListDataset<MBFImage> clzImages : images.values())
        {
            // Create an image for accumulation.
            final MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);

            // Now run the same code using the OpenIMAJ Parallel class.
            Parallel.forEach(clzImages, new Operation<MBFImage>()
            {
                public void perform(MBFImage i)
                {
                    final MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);
                    tmp.fill(RGBColour.WHITE);

                    final MBFImage small = i.process(resize).normalise();
                    final int x = (200 - small.getWidth()) / 2;
                    final int y = (200 - small.getHeight()) / 2;
                    tmp.drawImage(small, x, y);

                    // Note that this implementation uses synchronized on current,
                    // to ensure only one object accesses it at any given time.
                    synchronized (current)
                    {
                        current.addInplace(tmp);
                    }
                }
            });

            // Divide the accumulated image by the number of samples used to create it.
            current.divideInplace((float) clzImages.size());

            // Add the newly created average image to the output list.
            output.add(current);
        }

        System.out.println("Time (parallel): " + t2.duration() + "ms");

        DisplayUtilities.display("Images (parallelised)", output);

        output.clear();

        // Time how long the averaging process takes.
        Timer t3 = Timer.timer();

        // For each image group:
        for (ListDataset<MBFImage> clzImages : images.values())
        {
            // Create an image for accumulation.
            final MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);

            // Run the same code yet again, this time using a partitioned variant of the for each loop.
            // This will will give a collection of images a thread rather than each individual image its own thread.
            Parallel.forEachPartitioned(new RangePartitioner<>(clzImages), new Operation<Iterator<MBFImage>>()
            {
                public void perform(Iterator<MBFImage> it)
                {
                    // Created an extra temporary image to hold intermediary results.
                    MBFImage tmpAccum = new MBFImage(200, 200, 3);
                    MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);

                    while (it.hasNext())
                    {
                        final MBFImage i = it.next();
                        tmp.fill(RGBColour.WHITE);

                        final MBFImage small = i.process(resize).normalise();
                        final int x = (200 - small.getWidth()) / 2;
                        final int y = (200 - small.getHeight()) / 2;
                        tmp.drawImage(small, x, y);
                        tmpAccum.addInplace(tmp);
                    }

                    synchronized (current)
                    {
                        current.addInplace(tmpAccum);
                    }
                }
            });

            // Divide the accumulated image by the number of samples used to create it.
            current.divideInplace((float) clzImages.size());

            // Add the newly created average image to the output list.
            output.add(current);
        }

        System.out.println("Time (partition parallelised): " + t3.duration() + "ms");

        DisplayUtilities.display("Images (parition parallelised)", output);

        output.clear();

        // Exercise 1: Parallelise the outer loop
        // This method is faster than not parallelising it at all (~7000ms rather than ~14000ms), but is much slower than the inner loop parallelisation and
        // partition parallelisation (~2500ms). It also means that every variable declared outside the loop needed to be made final, which may not necessarily
        // be a con as this is generally good Java practice, but it does limit variables to be made final.
        Timer t4 = Timer.timer();

        // Run the same code again, but instead applying the parallelisation to the outside loop.
        Parallel.forEach(images.values(), new Operation<ListDataset<MBFImage>>()
        {
            @Override
            public void perform(ListDataset<MBFImage> clzImages)
            {
                MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);

                for (MBFImage i : clzImages)
                {
                    MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);
                    tmp.fill(RGBColour.WHITE);

                    MBFImage small = i.process(resize).normalise();
                    int x = (200 - small.getWidth()) / 2;
                    int y = (200 - small.getHeight()) / 2;
                    tmp.drawImage(small, x, y);

                    current.addInplace(tmp);
                }

                current.divideInplace((float) clzImages.size());

                // We needed to make output final in this method since it is now in an anonymous class.
                output.add(current);
            }
        });

        System.out.println("Time (dataset parallelised): " + t4.duration() + "ms");

        DisplayUtilities.display("Images (dataset parallelised)", output);
    }
}
