package imageProcessing;

import org.openstreetmap.josm.gui.layer.geoimage.*;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_highgui.*;
import com.googlecode.javacv.cpp.opencv_imgproc.*;
import com.googlecode.javacv.cpp.opencv_legacy.*;
import com.googlecode.javacv.cpp.opencv_objdetect.*;

public class cascadeDetect{
    	
	public void cascadeDetect(ImageEntry e){
    	final String XML_FILE = "lib/cascade.xml"; 
    	IplImage src = cvLoadImage(e.getFile().getAbsolutePath());
    	IplImage gray = cvCreateImage(cvGetSize(src), 8, 1);

   		CvRect rects = new CvRect();

   		CascadeClassifier cascade = new CascadeClassifier();
   		cascade.load(XML_FILE);

   		CvMemStorage storage = CvMemStorage.create();

   		cvCvtColor(src, gray, CV_BGR2GRAY );

   		cascade.detectMultiScale(src,
   				rects,
   				1.1,  // scale
   				1,   // min neighbours
   				0,
   				cvSize(10, 10),
   				cvSize(100, 100));

   		

   		cvClearMemStorage(storage);

   		CvPoint center = new CvPoint(
   				rects.x() + (rects.width()/2),
   				rects.y() + (rects.height()/2));
   		cvEllipse(src,
   				center,
   				cvSize(rects.width()/2, rects.height()/2),
   				0,
   				0,
   				360,
   				CvScalar.GREEN,
   				4,
   				8,
   				0);

       }
}