package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.LineString;


/**
 * Responsible for generating a GeoJSON map
 * 
 * @author Michal Sadowski
 *
 */
public class AQMap {
	
	public ArrayList<Reading> readings;
	public ArrayList<Point> path_map;
	private HashMap<String, Point> sensor_locs;
	
	/**
	 * @param readings readings taken
	 * @param path_map path followed by the drone
	 * @param sensor_locs w3w->mapbox sensors locations
	 */
	public AQMap(ArrayList<Reading> readings, ArrayList<Point> path_map, HashMap<String, Point> sensor_locs) {
		this.readings = new ArrayList<Reading>(readings);
		this.path_map = path_map;
		this.sensor_locs = new HashMap<String, Point>(sensor_locs);
	}
	
	/**
	 * Exports the map to a GeoJSON file
	 * 
	 * @param outfile name of the file to save to
	 */
	public void export(String outfile) {
		// create a mapbox feature collection
		var feature_collection = new ArrayList<Feature>();
		
		// add features for sensors with collected reading
		for (var i = 0; i<readings.size(); i++) {
			var sensor_point = createPointFeature(readings.get(i));
			sensor_locs.remove(readings.get(i).location);
			feature_collection.add(sensor_point);
		}
		
		// add features for sensors without reading
		for (var key : sensor_locs.keySet()) {
			var fake_reading = new Reading(key, 100.0, "-2");
			var fake_sensor_point = createPointFeature(fake_reading);
			feature_collection.add(fake_sensor_point);
		}
		
		// add feature for the path taken
		feature_collection.add(createLineStringFeature(createLineString(path_map)));
				
		// save as feature collection
		var map = FeatureCollection.fromFeatures(feature_collection);
		
		// save to file
		try {
			FileWriter myWriter = new FileWriter(outfile);
			myWriter.write(map.toJson());
			myWriter.close();
			System.out.println("Successfully saved " + outfile);
		} catch (IOException e) {
			System.out.println("An error occurred:");
			e.printStackTrace();
		}
	}

	/**
	 * Creates a point feature with properties (location, rgb-string, marker-color [, marker-symbol])
	 * based on the reading value 
	 * 
	 * @param reading values of the data
	 * @return point feature
	 */
	private Feature createPointFeature(Reading reading) {
		// create a feature
		var point = sensor_locs.get(reading.location);
		var point_feature = Feature.fromGeometry(point);
		
		// choose properties based on data collected
		var battery_level = reading.battery;
		String mks;
		String cs;
		if (battery_level < 10) {
			// ignore the measurement data and report low battery
			mks = ColorSymbol.readingToMarkerSymbol(-1);
			cs = ColorSymbol.readingToRgbColor(-1);
		} else {
			var measurement = (int) Double.parseDouble(reading.reading);
			mks = ColorSymbol.readingToMarkerSymbol(measurement);
			cs = ColorSymbol.readingToRgbColor(measurement);
		}
		
		// add additional properties
		point_feature.addStringProperty("location", reading.location);
		point_feature.addStringProperty("rgb-string", cs);
		point_feature.addStringProperty("marker-color", cs);
		// only add a marker symbol when visited the sensor
		if (mks != null) {
			point_feature.addStringProperty("marker-symbol", mks);
		}

		return point_feature;
	}
	
	/**
	 * Generates a feature from the linestring
	 * @param line_string to convert to a feature
	 * @return a feature from the linestring
	 */
	private Feature createLineStringFeature(LineString line_string) {
		var linestring_feature = Feature.fromGeometry(line_string);
		return linestring_feature;
	}
	
	/**
	 * Generates a LineString from a list of points
	 * 
	 * @param points making up the linestring
	 * @return LineString from points
	 */
	private LineString createLineString(ArrayList<Point> points) {
		var linestring_points = new ArrayList<Point>(points);
		var linestring = LineString.fromLngLats(linestring_points);
		return linestring;
	}
	
}
