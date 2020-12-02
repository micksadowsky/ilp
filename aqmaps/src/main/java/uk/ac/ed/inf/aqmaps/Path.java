package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Collections; 

import java.lang.Math; 

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;


public class Path {

	ArrayList<Point> sensors;
	Point init_loc;
	FeatureCollection no_fly_zones;
	
	public Path(ArrayList<Point> sensors, Point init_loc, FeatureCollection no_fly_zones) {
		this.sensors = sensors;
		this.init_loc = init_loc;
		this.no_fly_zones = no_fly_zones;
	}
	
	
	
	
	public void generatePath() {
		
	}
	
	public Point chooseClosest(ArrayList<Point> points, Point ipt) {
		var distances = new ArrayList<Double>();
		
		for (var pt : points) {
			distances.add(distance(ipt, pt));
		}
		
		var min_dist = Collections.min(distances);
		return points.get(distances.indexOf(min_dist));
		
	}
	
	public Double distance(Point pt1, Point pt2) {
		var lon1 = pt1.longitude();
		var lat1 = pt1.latitude();
		var lon2 = pt2.longitude();
		var lat2 = pt2.latitude();
		
		var distance = Math.sqrt( Math.pow((lon1 - lon2), 2) + Math.pow((lat1 - lat2), 2) );
		
		return distance;
	}
	
}
