package imageProcessing;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.logging.Logger;
import com.googlecode.javacv.BaseChildSettings;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_features2d.CvSURFParams;
import com.googlecode.javacv.cpp.opencv_features2d.CvSURFPoint;
import com.googlecode.javacv.cpp.opencv_flann.Index;
import com.googlecode.javacv.cpp.opencv_flann.IndexParams;
import com.googlecode.javacv.cpp.opencv_flann.KDTreeIndexParams;
import com.googlecode.javacv.cpp.opencv_flann.SearchParams;

import static com.googlecode.javacv.cpp.opencv_calib3d.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_flann.*;
import static com.googlecode.javacv.cpp.opencv_features2d.*;

public class SampleMatch{
	
	public SampleMatch(IplImage sampleImage) {
        settings = new sampleSettings();
        settings.sampleImage = sampleImage;
        setSettings(settings);
    }
    public SampleMatch(sampleSettings settings) {
        setSettings(settings);
    }

    public static class sampleSettings extends BaseChildSettings {
        
        CvSURFParams param = cvSURFParams(500, 1);
        double distanceThresh = 0.8;
        int matchMin = 4;
        double ransacReprojThresh = 1.0;
        boolean useFLANN = false;
        IplImage sampleImage = null;

        public IplImage getObjectImage() {
            return sampleImage;
        }
        public void setObjectImage(IplImage sampleImage) {
            this.sampleImage = sampleImage;
        }

        public boolean isExtended() {
            return param.extended() != 0;
        }
        public void setExtended(boolean extended) {
            param.extended(extended ? 1 : 0);
        }

        public boolean isUpright() {
            return param.upright() != 0;
        }
        public void setUpright(boolean upright) {
            param.upright(upright ? 1 : 0);
        }

        public double getHessianThresh() {
            return param.hessianThreshold();
        }
        public void setHessianThresh(double hessianThresh) {
            param.hessianThreshold(hessianThresh);
        }

        public int getnOctaves() {
            return param.nOctaves();
        }
        public void setnOctaves(int nOctaves) {
            param.nOctaves(nOctaves);
        }

        public int getnOctaveLayers() {
            return param.nOctaveLayers();
        }
        public void setnOctaveLayers(int nOctaveLayers) {
            param.nOctaveLayers(nOctaveLayers);
        }

        public double getDistanceThreshold() {
            return distanceThresh;
        }
        public void setDistanceThresh(double distanceThresh) {
            this.distanceThresh = distanceThresh;
        }

        public int getMatchesMin() {
            return matchMin;
        }
        public void setMatchMin(int matchMin) {
            this.matchMin = matchMin;
        }

        public double getRansacReprojThresh() {
            return ransacReprojThresh;
        }
        public void setRansacReprojThresh(double ransacReprojThresh) {
            this.ransacReprojThresh = ransacReprojThresh;
        }

        public boolean isUseFLANN() {
            return useFLANN;
        }
        public void setUseFLANN(boolean useFLANN) {
            this.useFLANN = useFLANN;
        }
    }

    private sampleSettings settings;
    public sampleSettings getSettings() {
        return settings;
    }
    public void setSettings(sampleSettings settings) {
        this.settings = settings;

        CvSeq keypoints = new CvSeq(null), descriptors = new CvSeq(null);
        cvClearMemStorage(storage);
        cvExtractSURF(settings.sampleImage, null, keypoints, descriptors, storage, settings.param, 0);

        int total = descriptors.total();
        int size = descriptors.elem_size();
        objectKeypoints = new CvSURFPoint[total];
        objectDescriptors = new FloatBuffer[total];
        for (int i = 0; i < total; i++ ) {
            objectKeypoints[i] = new CvSURFPoint(cvGetSeqElem(keypoints, i));
            objectDescriptors[i] = cvGetSeqElem(descriptors, i).capacity(size).asByteBuffer().asFloatBuffer();
        }
        if (settings.useFLANN) {
            int length = objectDescriptors[0].capacity();
            objectMat  = CvMat.create(total, length, CV_32F, 1);
            imageMat   = CvMat.create(total, length, CV_32F, 1);
            indicesMat = CvMat.create(total,      2, CV_32S, 1);
            distsMat   = CvMat.create(total,      2, CV_32F, 1);

            flannIndex = new Index();
            indexParams = new KDTreeIndexParams(4); // using 4 randomized kdtrees
            searchParams = new SearchParams(64, 0, true); // maximum number of leafs checked
        }
        pt1  = CvMat.create(1, total, CV_32F, 2);
        pt2  = CvMat.create(1, total, CV_32F, 2);
        mask = CvMat.create(1, total, CV_8U,  1);
        H    = CvMat.create(3, 3);
        ptpairs = new ArrayList<Integer>(2*objectDescriptors.length);
        logger.info(total + " object descriptors");
    }

    private static final Logger logger = Logger.getLogger(SampleMatch.class.getName());

    private CvMemStorage storage     = CvMemStorage.create();
    private CvMemStorage tempStorage = CvMemStorage.create();
    CvSURFPoint[] objectKeypoints   = null;

	CvSURFPoint[] imageKeypoints = null;
    private FloatBuffer[] objectDescriptors = null, imageDescriptors = null;
    private CvMat objectMat, imageMat, indicesMat, distsMat;
    private Index flannIndex = null;
    private IndexParams indexParams = null;
    private SearchParams searchParams = null;
    private CvMat pt1 = null, pt2 = null, mask = null, H = null;
    ArrayList<Integer> ptpairs = null;

    public double[] find(IplImage image) {
        CvSeq keypoints = new CvSeq(null), descriptors = new CvSeq(null);
        cvClearMemStorage(tempStorage);
        cvExtractSURF(image, null, keypoints, descriptors, tempStorage, settings.param, 0);

        int total = descriptors.total();
        int size = descriptors.elem_size();
        imageKeypoints = new CvSURFPoint[total];
        imageDescriptors = new FloatBuffer[total];
        for (int i = 0; i < total; i++ ) {
            imageKeypoints[i] = new CvSURFPoint(cvGetSeqElem(keypoints, i));
            imageDescriptors[i] = cvGetSeqElem(descriptors, i).capacity(size).asByteBuffer().asFloatBuffer();
        }
        logger.info(total + " image descriptors");

        int w = settings.sampleImage.width();
        int h = settings.sampleImage.height();
        double[] srcCorners = {0, 0,  w, 0,  w, h,  0, h};
        double[] dstCorners = locatePlanarObject(objectKeypoints, objectDescriptors,
                imageKeypoints, imageDescriptors, srcCorners);
        return dstCorners;
    }

    private double compareSURFDescriptors(FloatBuffer d1, FloatBuffer d2, double best) {
        double totalCost = 0;
        assert (d1.capacity() == d2.capacity() && d1.capacity() % 4 == 0);
        for (int i = 0; i < d1.capacity(); i += 4 ) {
            double t0 = d1.get(i  ) - d2.get(i  );
            double t1 = d1.get(i+1) - d2.get(i+1);
            double t2 = d1.get(i+2) - d2.get(i+2);
            double t3 = d1.get(i+3) - d2.get(i+3);
            totalCost += t0*t0 + t1*t1 + t2*t2 + t3*t3;
            if (totalCost > best)
                break;
        }
        return totalCost;
    }

    private int naiveNearestNeighbor(FloatBuffer vec, int laplacian,
            CvSURFPoint[] modelKeypoints, FloatBuffer[] modelDescriptors) {
        int neighbor = -1;
        double d, dist1 = 1e6, dist2 = 1e6;

        for (int i = 0; i < modelDescriptors.length; i++) {
            CvSURFPoint kp = modelKeypoints[i];
            FloatBuffer mvec = modelDescriptors[i];
            if (laplacian != kp.laplacian())
                continue;
            d = compareSURFDescriptors(vec, mvec, dist2);
            if (d < dist1) {
                dist2 = dist1;
                dist1 = d;
                neighbor = i;
            } else if (d < dist2) {
                dist2 = d;
            }
        }
        if (dist1 < settings.distanceThresh*dist2)
            return neighbor;
        return -1;
    }

    private void findPairs(CvSURFPoint[] objectKeypoints, FloatBuffer[] objectDescriptors,
               CvSURFPoint[] imageKeypoints, FloatBuffer[] imageDescriptors) {
        for (int i = 0; i < objectDescriptors.length; i++ ) {
            CvSURFPoint kp = objectKeypoints[i];
            FloatBuffer descriptor = objectDescriptors[i];
            int nearestNeighbor = naiveNearestNeighbor(descriptor, kp.laplacian(), imageKeypoints, imageDescriptors);
            if (nearestNeighbor >= 0) {
                ptpairs.add(i);
                ptpairs.add(nearestNeighbor);
            }
        }
    }

    private void flannFindPairs(FloatBuffer[] objectDescriptors,  FloatBuffer[] imageDescriptors) {
        int length = objectDescriptors[0].capacity();

        if (imageMat.rows() < imageDescriptors.length) {
            imageMat = CvMat.create(imageDescriptors.length, length, CV_32F, 1);
        }
        int imageRows = imageMat.rows();
        imageMat.rows(imageDescriptors.length);

        // copy descriptors
        FloatBuffer objectBuf = objectMat.getFloatBuffer();
        for (int i = 0; i < objectDescriptors.length; i++) {
            objectBuf.put(objectDescriptors[i]);
        }

        FloatBuffer imageBuf = imageMat.getFloatBuffer();
        for (int i = 0; i < imageDescriptors.length; i++) {
            imageBuf.put(imageDescriptors[i]);
        }

        // find nearest neighbors using FLANN
        flannIndex.build(imageMat, indexParams, FLANN_DIST_L2);
        flannIndex.knnSearch(objectMat, indicesMat, distsMat, 2, searchParams);

        IntBuffer indicesBuf = indicesMat.getIntBuffer();
        FloatBuffer distsBuf = distsMat.getFloatBuffer();
        for (int i = 0; i < objectDescriptors.length; i++) {
            if (distsBuf.get(2*i) < settings.distanceThresh*distsBuf.get(2*i+1)) {
                ptpairs.add(i);
                ptpairs.add(indicesBuf.get(2*i));
            }
        }
        imageMat.rows(imageRows);
    }

    /* a rough implementation for object location */
    private double[] locatePlanarObject(CvSURFPoint[] objectKeypoints, FloatBuffer[] objectDescriptors,
            CvSURFPoint[] imageKeypoints, FloatBuffer[] imageDescriptors, double[] srcCorners) {
        ptpairs.clear();
        if (settings.useFLANN) {
            flannFindPairs(objectDescriptors, imageDescriptors);
        } else {
            findPairs(objectKeypoints, objectDescriptors, imageKeypoints, imageDescriptors);
        }
        int n = ptpairs.size()/2;
        logger.info(n + " matching pairs found");
        if (n < settings.matchMin) {
            return null;
        }

        pt1 .cols(n);
        pt2 .cols(n);
        mask.cols(n);
        for (int i = 0; i < n; i++) {
            CvPoint2D32f p1 = objectKeypoints[ptpairs.get(2*i)].pt();
            pt1.put(2*i, p1.x()); pt1.put(2*i+1, p1.y());
            CvPoint2D32f p2 = imageKeypoints[ptpairs.get(2*i+1)].pt();
            pt2.put(2*i, p2.x()); pt2.put(2*i+1, p2.y());
        }

        if (cvFindHomography(pt1, pt2, H, CV_RANSAC, settings.ransacReprojThresh, mask) == 0) {
            return null;
        }
        if (cvCountNonZero(mask) < settings.matchMin) {
            return null;
        }

        double[] h = H.get();
        double[] dstCorners = new double[srcCorners.length];
        for(int i = 0; i < srcCorners.length/2; i++) {
            double x = srcCorners[2*i], y = srcCorners[2*i + 1];
            double Z = 1/(h[6]*x + h[7]*y + h[8]);
            double X = (h[0]*x + h[1]*y + h[2])*Z;
            double Y = (h[3]*x + h[4]*y + h[5])*Z;
            dstCorners[2*i    ] = X;
            dstCorners[2*i + 1] = Y;
        }

        pt1 .cols(objectDescriptors.length);
        pt2 .cols(objectDescriptors.length);
        mask.cols(objectDescriptors.length);
        return dstCorners;
    }

}