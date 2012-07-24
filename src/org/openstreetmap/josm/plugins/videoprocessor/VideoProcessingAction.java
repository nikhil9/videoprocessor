package org.openstreetmap.josm.plugins.videoprocessor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
import javax.swing.*;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSlider;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.progress.*;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.gui.layer.geoimage.ImageViewerDialog;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import java.awt.geom.Dimension2D;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_legacy.*;
import com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_highgui.*;
import com.googlecode.javacv.cpp.opencv_legacy.*;
import com.googlecode.javacv.cpp.opencv_objdetect.CascadeClassifier;
import com.googlecode.javacv.CanvasFrame;
import imageProcessing.*;


class VideoProcessingAction extends AbstractAction implements LayerAction {
	
	final static boolean debug = false;
	
	public VideoProcessingAction() {
        super("Process Images");
    }
	
    @Override
	public void actionPerformed(ActionEvent arg0) {
    	
    	GeoImageLayer layer = getLayer();
    	final List<ImageEntry> images = new ArrayList<ImageEntry>();
    	for (ImageEntry e : layer.getImages()){   		
    			images.add(e);   		
    	}
    	    	    	
    	/*for(ImageEntry e : images){
    		listModel.addElement(e.getFile().getAbsolutePath());    		
    	}*/
    	
    	final Processor frame = new Processor();
    	frame.pack();
    	//Mark for display in the center of the screen
    	frame.setLocationRelativeTo(null);
    	//Exit application when frame is closed.
    	//frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    	frame.setVisible(true);
				
        Main.worker.execute(new VideoProcessingRunnable(images));
       
    }
    
    static class VideoProcessingRunnable extends PleaseWaitRunnable {
        final private List<ImageEntry> images;       
        private boolean canceled = false;     
        
        public VideoProcessingRunnable(List<ImageEntry> images) {
            super("Videoprocessor");
            this.images = images;
        }    
        
        @Override
        protected void realRun(){
        	for (int i = 0; i < images.size(); ++i){
        		if(canceled) return;
        		ImageEntry e = images.get(i);
        		progressMonitor.worked(1);
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