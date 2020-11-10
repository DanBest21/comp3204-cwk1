package uk.ac.soton.ecs.db5n17.ch7;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.processing.convolution.FSobelMagnitude;
import org.openimaj.image.processing.convolution.FourierConvolve;
import org.openimaj.image.processing.convolution.LaplacianOfGaussian2D;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.image.processing.face.detection.CLMFaceDetector;
import org.openimaj.image.processing.threshold.AdaptiveLocalThresholdMean;
import org.openimaj.image.processing.threshold.OtsuThreshold;
import org.openimaj.image.processor.PixelProcessor;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.video.Video;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.capture.VideoCaptureException;
import org.openimaj.video.xuggle.XuggleVideo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * OpenIMAJ Tutorial - Chapter 7: Processing video
 *
 */
public class App
{
    public static void main( String[] args ) throws MalformedURLException, VideoCaptureException
    {
        // Retrieve the Keyboard Cat video.
        Video<MBFImage> video = new XuggleVideo(new URL("http://static.openimaj.org/media/tutorial/keyboardcat.flv"));

        VideoDisplay<MBFImage> display;

        // Apply edge detection to the Keyboard Cat video by displaying it frame-by-frame.
        for (MBFImage mbfImage : video)
        {
            DisplayUtilities.displayName(mbfImage.process(new CannyEdgeDetector()), "videoFrames");
        }

        video = new VideoCapture(320, 240);

        display = VideoDisplay.createVideoDisplay(video);

        // Apply Adaptive Local Thresholding using the mean via a video listener that is attached to it
        display.addVideoListener(
                new VideoDisplayListener<MBFImage>()
                {
                    public void beforeUpdate(MBFImage frame)
                    {
                        frame.processInplace(new AdaptiveLocalThresholdMean(100));
                    }

                    public void afterUpdate(VideoDisplay<MBFImage> display) { }
                });
    }
}
