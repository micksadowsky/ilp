package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.LineString;



public class AQMap {
	
	public ArrayList<Point> sensors;
	public Point init_point;
	
	public AQMap(ArrayList<Point> sensors, Point init_point) {
		this.sensors = new ArrayList<Point>(sensors);
		this.init_point = init_point;
	}
	
	public void export(String outfile) {
		// Create the feature collection
		var feature_collection = new ArrayList<Feature>();
		
		
		for (var i = 0; i<sensors.size(); i++) {
			feature_collection.add(createPointFeature(sensors.get(i), Integer.toString(i)));
//			System.out.println("AQMap.export(sensors): sensor = " + sensor);

		}
		
		feature_collection.add(createLineStringFeature(createLineString(sensors)));
		
//		System.out.println("AQMap.export(sensors): feature_collection = " + feature_collection);

		var map = FeatureCollection.fromFeatures(feature_collection);

		// Save to file
		try {
			FileWriter myWriter = new FileWriter(outfile);
			myWriter.write(map.toJson());
			myWriter.close();
		} catch (IOException e) {
			System.out.println("An error occurred:");
			e.printStackTrace();
		}
	}
	
	public Feature createLineStringFeature(LineString line_string) {
		var linestring_feature = Feature.fromGeometry(line_string);
		return linestring_feature;
	}
	
	public LineString createLineString(ArrayList<Point> points) {
		var linestring_points = new ArrayList<Point>(points);
		linestring_points.add(0, init_point);
		var linestring = LineString.fromLngLats(linestring_points);
		return linestring;
	}
	
	public Feature createPointFeature(Point point, String index) {
		var point_feature = Feature.fromGeometry(point);
		point_feature.addStringProperty("name", index);
		point_feature.addStringProperty("rgb-string", "#aaaaaa");
//		System.out.println(point_feature);
		return point_feature;
	}
	

}
