package org.openstreetmap.josm.plugins.videoprocessor;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.javacv.cpp.opencv_core.*;

import com.googlecode.javacv.cpp.opencv_legacy.*;
import com.googlecode.javacv.cpp.opencv_objdetect.*;

import org.openstreetmap.josm.gui.layer.geoimage.*;


public final class Processor extends JFrame {

	private final JFileChooser fileChooser = new JFileChooser();
	public String XML_FILE = "lib/cascade.xml";
	private final JLabel imageView = new JLabel();
	private IplImage image = null;
	
	private int tempIndex;
	private int totalIndex;
	private int next;
	private int prev;
	
	List<ImageEntry> imageList;
	ImageEntry imageEntry;


	public Processor() throws HeadlessException {
		super("Videoprocessor");
				
		final Action nextAction = new AbstractAction(">") {
			@Override
			public void actionPerformed(final ActionEvent e) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try {
					if (next == tempIndex){						
					}
					else{				
						nextImageEntry(imageList);
						final IplImage img = openImage();
						if (next == 0 || next == 1){
							prev = 1;
						}
						else{
							prev = next - 1;
						}
						next = next + 1;						

					if (img != null) {
						image = img;
						imageView.setIcon(new 
								ImageIcon(image.getBufferedImage()));
						}
					}
				} finally {
					setCursor(Cursor.getDefaultCursor());
				}
			}
		};
		
		final Action prevAction = new AbstractAction("<") {
			@Override
			public void actionPerformed(final ActionEvent e) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try {
					prevImageEntry(imageList);
					final IplImage img = openImage();
					next = prev + 1;
					if(prev == 0){						
					}
					else{
						prev = prev - 1;
					}
					if (img != null) {
						image = img;
						imageView.setIcon(new 
								ImageIcon(image.getBufferedImage()));
						}					
				} finally {
					setCursor(Cursor.getDefaultCursor());
				}
			}
		};
		
		final Action processAction = new AbstractAction("Process") {
			@Override
			public void actionPerformed(final ActionEvent e) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try {
					if (image != null) {
						processImage(image);
						imageView.setIcon(new ImageIcon(image.getBufferedImage()));
					} else {
						
					}
				} finally {
					setCursor(Cursor.getDefaultCursor());
				}
			}
		};
		
		
		/*final Action openImageAction = new AbstractAction("Open Image") {

			@Override
			public void actionPerformed(final ActionEvent e) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try {
					final IplImage img = openImage();

					if (img != null) {
						image = img;
						imageView.setIcon(new ImageIcon(image.getBufferedImage()));
						processAction.setEnabled(true);

					}
				} finally {
					setCursor(Cursor.getDefaultCursor());
				}
			}
		};
		
		*/
		
		final JPanel buttonsPanel = new JPanel(new GridLayout(0, 3, 0, 8));
		buttonsPanel.add(new JButton(prevAction));
		buttonsPanel.add(new JButton(nextAction));
		buttonsPanel.add(new JButton(processAction));
		
		final JPanel botPanel = new JPanel();
		botPanel.add(buttonsPanel);
		add(botPanel, BorderLayout.SOUTH);
		
		
		final JPanel botPane = new JPanel();
		botPane.add(botPanel);
		add(botPane, BorderLayout.SOUTH);
		
		final JScrollPane imageScrollPane = new JScrollPane(imageView);
		imageScrollPane.setPreferredSize(new Dimension(640, 480));
		add(imageScrollPane, BorderLayout.CENTER);
	}
	
	private IplImage openImage() {
		final String path = imageEntry.getFile().getAbsolutePath();
		if (imageEntry == null){
			showMessageDialog(this, "Cannot open image file: " + path, getTitle(), ERROR_MESSAGE);
			return null;
		}
		else{
			
			final IplImage newImage = cvLoadImage(path);
			
			if (newImage != null) {
				return newImage;
			} else {
				showMessageDialog(this, "Cannot open image file: " + path, getTitle(), ERROR_MESSAGE);
				return null;
			}
		}		
	}


	private void processImage(final IplImage src) {		
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
	
	
	public void getImageEntry(List<ImageEntry> imageList){
		this.imageList =  imageList;	
		imageEntry = imageList.get(0);
		tempIndex = imageList.size();		 
		next = 1;
		prev = 0;
		IplImage image = openImage();
		imageView.setIcon(new ImageIcon(image.getBufferedImage()));
		}
		
		
	
	private void nextImageEntry(List<ImageEntry> imageList){
		imageEntry = imageList.get(next);
		
	}
	
	private void prevImageEntry(List<ImageEntry> imageList){
		imageEntry = imageList.get(prev);
		
	}	

}