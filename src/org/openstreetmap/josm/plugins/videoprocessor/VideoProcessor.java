package org.openstreetmap.josm.plugins.videoprocessor;

import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;


public class VideoProcessor extends Plugin{

	public VideoProcessor(PluginInformation info) {
		super(info);
		GeoImageLayer.registerMenuAddition(new VideoProcessingAction());
		}
}