package org.openstreetmap.josm.plugins.videoprocessor;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.*;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import javax.swing.*;
import static com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_core.cvFlip;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2RGB;

public class VideoProcessor extends Plugin{

	public VideoProcessor(PluginInformation info) {
		super(info);
		}
}