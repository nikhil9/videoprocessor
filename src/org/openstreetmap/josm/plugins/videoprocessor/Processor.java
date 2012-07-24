package org.openstreetmap.josm.plugins.videoprocessor;

import static com.googlecode.javacv.cpp.opencv_core.cvFlip;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2RGB;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
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

import com.googlecode.javacv.cpp.opencv_core.*;

import org.openstreetmap.josm.gui.layer.geoimage.*;


public final class Processor extends JFrame {

	private final JFileChooser fileChooser = new JFileChooser();
	
	private final JLabel imageView = new JLabel();
	private IplImage image = null;


	public Processor() throws HeadlessException {
		super("Videoprocessor");
				
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
		processAction.setEnabled(false);
		final Action openImageAction = new AbstractAction("Open Image") {

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

		final JPanel buttonsPanel = new JPanel(new GridLayout(0, 1, 0, 4));
		buttonsPanel.add(new JButton(openImageAction));
		buttonsPanel.add(new JButton(processAction));
	
		final JPanel leftPane = new JPanel();
		leftPane.add(buttonsPanel);
		add(leftPane, BorderLayout.WEST);
		
		final JScrollPane imageScrollPane = new JScrollPane(imageView);
		imageScrollPane.setPreferredSize(new Dimension(640, 480));
		add(imageScrollPane, BorderLayout.CENTER);
	}
	
	private IplImage openImage() {
		
		if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
			return null;
		}

		final String path = fileChooser.getSelectedFile().getAbsolutePath();
		final IplImage newImage = cvLoadImage(path);
		if (newImage != null) {
			return newImage;
		} else {
			showMessageDialog(this, "Cannot open image file: " + path, getTitle(), ERROR_MESSAGE);
			return null;
		}
	}


	private void processImage(final IplImage src) {		
		cvFlip(src, src, 0);	
	}


	
		
	
}