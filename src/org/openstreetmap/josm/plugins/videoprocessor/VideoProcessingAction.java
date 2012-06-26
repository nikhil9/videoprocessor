package org.openstreetmap.josm.plugins.videoprocessor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.progress.*;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.gui.layer.geoimage.ImageDisplay;
import java.awt.geom.Dimension2D;
import static com.googlecode.javacv.cpp.opencv_core.cvCircle;
import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvLine;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvPointFrom32f;
import static com.googlecode.javacv.cpp.opencv_core.cvRect;
import static com.googlecode.javacv.cpp.opencv_core.cvResetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import static com.googlecode.javacv.cpp.opencv_highgui.CV_LOAD_IMAGE_GRAYSCALE;
import static com.googlecode.javacv.cpp.opencv_highgui.cvDestroyWindow;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvNamedWindow;
import static com.googlecode.javacv.cpp.opencv_highgui.cvShowImage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvWaitKey;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GRAY2BGR;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import imageProcessing.SampleMatch.sampleSettings;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_features2d.CvSURFPoint;
import com.googlecode.javacv.CanvasFrame;

import imageProcessing.SampleMatch;

class VideoProcessingAction extends AbstractAction implements LayerAction {
	
	final static boolean debug = false;
    final static String KEEP_BACKUP = "plugins.videoprocessor.keep_backup";    
    
    public VideoProcessingAction() {
        super("Process Images");
    }
    
    @Override
	public void actionPerformed(ActionEvent arg0) {
    	
    	GeoImageLayer layer = getLayer();
    	final List<ImageEntry> images = new ArrayList<ImageEntry>();
    	for (ImageEntry e : layer.getImages()){
    		if (e.getPos() != null && e.getGpsTime() != null){
    			images.add(e);
    		}
    	}
    	
    	final JPanel cont = new JPanel(new GridBagLayout());
    	cont.add(new JLabel("Videoprocessor"));
    	
    	DefaultListModel listModel = new DefaultListModel();
    	DecimalFormat dFormatter = new DecimalFormat ("###0.000000");
    	
    	for(ImageEntry e : images){
    		listModel.addElement(e.getFile().getAbsolutePath()+
    				" ("+dFormatter.format(e.getPos().lat())+","+
    				dFormatter.format(e.getPos().lon())+")");
    	}
    	
    	JList entryList = new JList(listModel);
    	JScrollPane scroll = new JScrollPane(entryList);
    	scroll.setPreferredSize(new Dimension(400, 400));
    	cont.add(scroll);
    	
    	final JPanel settingsPanel = new JPanel(new GridBagLayout());
    	settingsPanel.setBorder(BorderFactory.createTitledBorder("Settings"));
    	cont.add(settingsPanel);
    	
    	final JCheckBox backups = new JCheckBox("keep backup files", Main.pref.getBoolean(KEEP_BACKUP, true));
        settingsPanel.add(backups);
    	  	        
        int result = new ExtendedDialog(
                Main.parent,
                "VideoProcessor Plugin",
                new String[] {"OK", "Cancel"})
            .setButtonIcons(new String[] {"ok.png", "cancel.png"})
            .setContent(cont)
            .setCancelButton(2)
            .setDefaultButton(1)
            .showDialog()
            .getValue();

        if (result != 1)
            return;
    
        final boolean keep_backup = backups.isSelected();
       
        Main.pref.put(KEEP_BACKUP, keep_backup);
       
        
        Main.worker.execute(new VideoProcessingRunnable(images,
        		keep_backup));
    }
    
    static class VideoProcessingRunnable extends PleaseWaitRunnable {
        final private List<ImageEntry> images;
        final private boolean keep_backup;       

        private boolean canceled = false;
        private Boolean override_backup = null;

        private File fileFrom;
        private File fileTo;
        private File fileDelete;
        
        public VideoProcessingRunnable(List<ImageEntry> images, 
        		boolean keep_backup) {
            super("VideoProcessor");
            this.images = images;
            this.keep_backup = keep_backup;
            
        }
        
        @Override
        protected void realRun(){        	
        	
        	for (int i = 0; i < images.size(); ++i){
        		if(canceled) return;
        	        	
        	ImageEntry e = images.get(0);
        	ImageDisplay disp = new ImageDisplay();
        	disp.setImage(e.getFile(), 1);
        	//IplImage img = cvLoadImage(e.getFile().getAbsolutePath(), 
        		//	CV_LOAD_IMAGE_GRAYSCALE);
        	//final CanvasFrame canvas = new CanvasFrame("Demo");
        	//canvas.showImage(img);
        	//cvShowImage("image", img);
        	
        	fileFrom = null;
        	fileTo = null;
        	fileDelete = null;     
        	}
        }
        
        private void chooseFiles(File file) throws IOException {
            if (debug) {
                System.err.println("f: "+file.getAbsolutePath());
            }

            if (!keep_backup) {
                chooseFilesNoBackup(file);
                return;
            }
            
            File fileBackup = new File(file.getParentFile(),file.getName()+"_");
           if (fileBackup.exists()) {
                confirm_override();
                if (canceled)
                    return;

                if (override_backup) {
                    if (!fileBackup.delete())
                        throw new IOException("File could not be deleted!");
                } else {
                    chooseFilesNoBackup(file);
                    return;
                }
            }
            if (!file.renameTo(fileBackup))
                throw new IOException("Could not rename file!");

            fileFrom = fileBackup;
            fileTo = file;
            fileDelete = null;
        }
        
        private void chooseFilesNoBackup(File file) throws IOException {
            File fileTmp;
            
            fileTmp = new File(file.getParentFile(), "img" + UUID.randomUUID() 
            		+ ".jpg");
            if (debug) {
                System.err.println("TMP: "+fileTmp.getAbsolutePath());
            }
            if (! file.renameTo(fileTmp))
                throw new IOException("Could not rename file!");

            fileFrom = fileTmp;
            fileTo = file;
            fileDelete = fileTmp;
        }
        
        private void confirm_override() {
            if (override_backup != null)
                return;
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
					public void run() {
                        JLabel l = new JLabel("<html><h3>There are old backup files in the image directory!</h3>");
                        l.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
                        int override = new ExtendedDialog(
                                progressMonitor.getWindowParent(),
                                "Override old backup files?",
                                new String[] {"Cancel", "Keep old backups and continue", "Override"})
                            .setButtonIcons(new String[] {"cancel.png", "ok.png", "dialogs/delete.png"})
                            .setContent(l)
                            .setCancelButton(1)
                            .setDefaultButton(2)
                            .showDialog()
                            .getValue();
                        if (override == 2) {
                            override_backup = false;
                        } else if (override == 3) {
                            override_backup = true;
                        } else {
                            canceled = true;
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println(e);
                canceled = true;
            }
        }
        
        private void cleanupFiles() throws IOException {
            if (fileDelete != null) {
                if (!fileDelete.delete())
                    throw new IOException("Could not delete temporary file!");
            }
        }        
       
        @Override
        protected void finish() {
        }

        @Override
        protected void cancel() {
            canceled = true;
        }
    }
    
    private GeoImageLayer getLayer() {
        return (GeoImageLayer)LayerListDialog.getInstance()
        		.getModel()
        		.getSelectedLayers()
        		.get(0);
    }
    
    private boolean enabled(GeoImageLayer layer) {
        for (ImageEntry e : layer.getImages()) {
            if (e.getPos() != null && e.getGpsTime() != null)
                return true;
        }
        return false;
    }
    
    @Override
   	public Component createMenuComponent() {
           JMenuItem geotaggingItem = new JMenuItem(this);
           geotaggingItem.setEnabled(enabled(getLayer()));
           return geotaggingItem;
       }

       @Override
   	public boolean supportLayers(List<Layer> layers) {
           return layers.size() == 1 && layers.get(0) instanceof GeoImageLayer;
       }
   }