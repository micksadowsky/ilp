package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.locationtech.jts.geom.Coordinate;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.LineString;


public class AQMap {
	
	public ArrayList<Reading> readings;
//	public Point init_point;
	public ArrayList<Point> path_map;
	private HashMap<String, Point> sensor_locs;
	
	public AQMap(ArrayList<Reading> readings, ArrayList<Point> path_map, HashMap<String, Point> sensor_locs) {
		this.readings = new ArrayList<Reading>(readings);
//		this.init_point = init_point;
		this.path_map = path_map;
		this.sensor_locs = new HashMap<String, Point>(sensor_locs);
	}
	
	public void export(String outfile) {
		// Create the feature collection
		var feature_collection = new ArrayList<Feature>();
		
		// sensors with collected reading
		for (var i = 0; i<readings.size(); i++) {
			var sensor_point = createPointFeature(readings.get(i), Integer.toString(i));
			sensor_locs.remove(readings.get(i).location);
			feature_collection.add(sensor_point);
		}
		
		// sensors without reading
		for (var key : sensor_locs.keySet()) {
			var fake_reading = new Reading(key, 100.0, "-2");
			var fake_sensor_point = createPointFeature(fake_reading, "unvisited");
//			sensor_locs.remove(key);
			feature_collection.add(fake_sensor_point);
		}
		
		// path shadow
		feature_collection.add(createLineStringFeature(createLineString(path_map)));
		
		// Campus border
		var nw_pt = Point.fromLngLat(-3.192473, 55.946233);
		var sw_pt = Point.fromLngLat(-3.192473, 55.942617);
		var se_pt = Point.fromLngLat(-3.184319, 55.942617);
		var ne_pt = Point.fromLngLat(-3.184319, 55.946233);
		Point[] campus_coordinate_sequence = {nw_pt, sw_pt, se_pt, ne_pt, nw_pt};
		feature_collection.add(Feature.fromGeometry(LineString.fromLngLats(Arrays.asList(campus_coordinate_sequence))));
		
		//save as feature collection
		var map = FeatureCollection.fromFeatures(feature_collection);
		// Save to file
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

	public Feature createPointFeature(Reading reading, String index) {
		var location = sensor_locs.get(reading.location);
		var point_feature = Feature.fromGeometry(location);
		point_feature.addStringProperty("index", index);
		var battery_level = reading.battery;
		String mks;
		String cs;
		if (battery_level < 10) {
			mks = ColorSymbol.readingToMarkerSymbol(-1);
			cs = ColorSymbol.readingToRgbColor(-1);
		} else {
			var measurement = (int) Double.parseDouble(reading.reading);
			mks = ColorSymbol.readingToMarkerSymbol(measurement);
			cs = ColorSymbol.readingToRgbColor(measurement);
		}
		
		point_feature.addStringProperty("location", reading.location);
		point_feature.addStringProperty("rgb-string", cs);
		point_feature.addStringProperty("marker-color", cs);
		if (mks != null) {
			point_feature.addStringProperty("marker-symbol", mks);
		}

		return point_feature;
	}
	
	public Feature createLineStringFeature(LineString line_string) {
		var linestring_feature = Feature.fromGeometry(line_string);
		return linestring_feature;
	}
	
	public LineString createLineString(ArrayList<Point> points) {
		var linestring_points = new ArrayList<Point>(points);
//		linestring_points.add(0, init_point);
		var linestring = LineString.fromLngLats(linestring_points);
		return linestring;
	}
	
	

}
