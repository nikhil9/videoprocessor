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
import javax.swing.UIManager;

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
	private JSlider timeline;
	private JButton play,back,forward;
	
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
    	
    	final JPanel cont = new JPanel(new GridBagLayout());
    	
    	GridBagConstraints c = new GridBagConstraints();
    	DefaultListModel listModel = new DefaultListModel();
    	DecimalFormat dFormatter = new DecimalFormat ("###0.000000");
    	
    	for(ImageEntry e : images){
    		listModel.addElement(e.getFile().getAbsolutePath());
    		cascadeDetect(e);
    	}
    	
    	ImageViewerDialog.showImage(layer, images.get(0) );;
    	        
    	
    	Canvas can = new Canvas(); 
    	JList entryList = new JList(listModel);
    	JScrollPane scroll = new JScrollPane(entryList);
    	scroll.setPreferredSize(new Dimension(500, 250));
    	c.fill = GridBagConstraints.CENTER ;
        c.gridx = 1;
        c.gridy = 0;
    	cont.add(scroll, c);  
    	
    	 //cvNamedWindow("hello");
        //cvWaitKey(0);
    	
    	/*timeline = new JSlider(0,500,0);
        timeline.setMajorTickSpacing(5);
        timeline.setMinorTickSpacing(2);
        timeline.setPaintTicks(true);
        play= new JButton("play");*/
        back= new JButton("<");
        forward= new JButton(">");
        c.fill = GridBagConstraints.EAST;
        c.gridx = 0;
        c.gridy = 0;        
        cont.add(back, c);      
        c.fill = GridBagConstraints.WEST;
        c.gridx = 2;
        c.gridy = 0;
        cont.add(forward, c);
        /*cont.add(timeline, BorderLayout.SOUTH);
        cont.add(play, BorderLayout.EAST);
        cont.add(back, BorderLayout.EAST);
        cont.add(forward, BorderLayout.EAST);*/
        /*final JPanel controlsPanel=new JPanel();
        controlsPanel.setLayout(new FlowLayout());
        
        controlsPanel.add(play);
        controlsPanel.add(back);
        controlsPanel.add(forward);*/
    	
    	int result = new ExtendedDialog(
                Main.parent,
                "VideoProcessor Plugin",
                new String[] {"Process", "Cancel"})
            .setButtonIcons(new String[] {"ok.png", "cancel.png"})
            .setContent(cont)
            .setCancelButton(2)
            .setDefaultButton(1)
            .showDialog()
            .getValue();

        if (result != 1)
            return;
        
       
        
        /*final CanvasFrame canvas = new CanvasFrame("Demo");
        canvas.setSize(400, 400);
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);*/
        Main.worker.execute(new VideoProcessingRunnable(images));
       // Main.worker.execute(cvNamedWindow("hello"));
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