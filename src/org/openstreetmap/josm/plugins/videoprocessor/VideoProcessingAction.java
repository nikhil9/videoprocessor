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
import java.awt.geom.Dimension2D;

class VideoProcessingAction extends AbstractAction implements LayerAction {
	
	final static boolean debug = false;
    final static String KEEP_BACKUP = "plugins.videoprocessor.keep_backup";
    final static String CHANGE_MTIME = "plugins.videoprocessor.change-mtime";
    final static String MTIME_MODE = "plugins.videoprocessor.mtime-mode";
    final static int MTIME_MODE_GPS = 1;
    final static int MTIME_MODE_PREVIOUS_VALUE = 2;
    
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
    	
    	final JCheckBox setMTime = new JCheckBox("change file modification time:", Main.pref.getBoolean(CHANGE_MTIME, false));
        settingsPanel.add(setMTime);
        
        final String[] mTimeModeArray = {"----", "to gps time", "to previous value (unchanged mtime)"};
        final JComboBox mTimeMode = new JComboBox(mTimeModeArray);
        {
            String mTimeModePref = Main.pref.get(MTIME_MODE, null);
            int mTimeIdx = 0;
            if ("gps".equals(mTimeModePref)) {
                mTimeIdx = 1;
            } else if ("previous".equals(mTimeModePref)) {
                mTimeIdx = 2;
            }
            mTimeMode.setSelectedIndex(setMTime.isSelected() ? mTimeIdx : 0);
        }
        settingsPanel.add(mTimeMode);
        
        setMTime.addActionListener(new ActionListener(){
            @Override
			public void actionPerformed(ActionEvent e) {
                if (setMTime.isSelected()) {
                    mTimeMode.setEnabled(true);
                } else {
                    mTimeMode.setSelectedIndex(0);
                    mTimeMode.setEnabled(false);
                }
            }
        });
        
        setMTime.setSelected(!setMTime.isSelected());
        setMTime.doClick();
        
        int result = new ExtendedDialog(
                Main.parent,
                "Photo Geotagging Plugin",
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
        final boolean change_mtime = setMTime.isSelected();
        Main.pref.put(KEEP_BACKUP, keep_backup);
        Main.pref.put(CHANGE_MTIME, change_mtime);
        if (change_mtime) {
            String mTimeModePref;
            switch (mTimeMode.getSelectedIndex()) {
            case 1:
                mTimeModePref = "gps";
                break;
            case 2:
                mTimeModePref = "previous";
                break;
            default:
                mTimeModePref = null;
            }
            Main.pref.put(MTIME_MODE, mTimeModePref);
        }
        
        Main.worker.execute(new VideoProcessingRunnable(images, keep_backup, mTimeMode.getSelectedIndex()));
    }
    
    static class VideoProcessingRunnable extends PleaseWaitRunnable {
        final private List<ImageEntry> images;
        final private boolean keep_backup;
        final private int mTimeMode;

        private boolean canceled = false;
        private Boolean override_backup = null;

        private File fileFrom;
        private File fileTo;
        private File fileDelete;
        
        public VideoProcessingRunnable(List<ImageEntry> images, 
        		boolean keep_backup, int mTimeMode) {
            super("Photo Geotagging Plugin");
            this.images = images;
            this.keep_backup = keep_backup;
            this.mTimeMode = mTimeMode;
        }
        
        @Override
        protected void realRun(){        	
        	
        	for (int i = 0; i < images.size(); ++i){
        		if(canceled) return;
        	        	
        	ImageEntry e = images.get(i);
        	
        	fileFrom = null;
        	fileTo = null;
        	fileDelete = null;
        	
        	try{
        		if(mTimeMode != 0){
        			testMTimeReadAndWrite(e.getFile());
        		}
        		
        		Long mTime = null;
        		if (mTimeMode == MTIME_MODE_PREVIOUS_VALUE) {
                    mTime = e.getFile().lastModified();
                    if (mTime.equals(0L))
                        throw new IOException("Could not read mtime.");
                }
        		chooseFiles(e.getFile());
                if (canceled) return;             
                
                if (mTimeMode == MTIME_MODE_GPS) {
                    mTime = e.getGpsTime().getTime();
                }

                if (mTime != null) {
                    if (!fileTo.setLastModified(mTime))
                        throw new IOException("Could not write mtime.");
                }                
                cleanupFiles();    
                
        	}catch (final IOException ioe) {
                ioe.printStackTrace();
                
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
					public void run() {
                        JOptionPane.showMessageDialog(Main.parent, ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
                return;        	
        }
       progressMonitor.worked(1);       
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
        
        boolean testMTimeReadAndWriteDone = false;
        
        private void testMTimeReadAndWrite(File file) throws IOException {
            if (testMTimeReadAndWriteDone)  // do this only once
                return;
            File fileTest = File.createTempFile("geo", ".txt", file.getParentFile());
            long mTimeTest = fileTest.lastModified();
            if (mTimeTest == 0L)
                throw new IOException("Test failed: Could not read mtime.");
            if (!fileTest.setLastModified(mTimeTest))
                throw new IOException("Test failed: Could not write mtime.");
            if (!fileTest.delete())
                throw new IOException("Could not delete temporary file!");

            testMTimeReadAndWriteDone = true;
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