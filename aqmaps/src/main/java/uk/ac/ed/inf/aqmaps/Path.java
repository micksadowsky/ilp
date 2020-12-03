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
	
	
	
	public ArrayList<Point> generatePath(int seed) {
		System.out.println("Generating path");
		var order = chooseOrder(sensors, init_loc);
		return order;
	}
	
	
	public ArrayList<Point> chooseOrder(ArrayList<Point> points, Point ipt) {
		var points_given = new ArrayList<Point>(points);
		var no_points = points_given.size();
		var ordered_points = new ArrayList<Point>();
		
		// Choose the first sensor to go to 
		var first_point = chooseClosest(points_given, ipt);
		ordered_points.add(first_point);
		points_given.remove(first_point);
		
		// Choose the sensor ending the path
		var last_point = chooseClosest(points_given, ipt);
		points_given.remove(last_point);
		
		// calculate points in between
		var current_point = first_point;		
		for (var i=0; i<no_points-2; i++) {
			var closest = chooseClosest(points_given, current_point);
			ordered_points.add(closest);
			points_given.remove(closest);
			current_point = closest;
		}
		
		ordered_points.add(last_point);

		
		System.out.println("Chosen order: ");
		for (var i = 0; i<ordered_points.size(); i++) {
			System.out.println(i+1 + " (" + ordered_points.get(i).longitude() + ", " + ordered_points.get(i).latitude() + ")");
		}
		
		return ordered_points;
	}
	
	
	public Point chooseClosest(ArrayList<Point> points, Point ipt) {
		var distances = new ArrayList<Double>();
		
		for (var pt : points) {
			distances.add(distance(ipt, pt));
		}
		
		var min_dist = Collections.min(distances);
		var closest = points.get(distances.indexOf(min_dist));
		
//		System.out.println("Closest point chosen: (" + closest.longitude() + ", " + closest.latitude() + ")");
		
		return closest;
		
	}
	
	public Double distance(Point pt1, Point pt2) {
		var lon1 = pt1.longitude();
		var lat1 = pt1.latitude();
		var lon2 = pt2.longitude();
		var lat2 = pt2.latitude();
		
		var distance = Math.sqrt( Math.pow((lon1 - lon2), 2) + Math.pow((lat1 - lat2), 2) );
		
//		System.out.println("Distance between (" + lon1 + ", " + lat1 + 
//				") and (" + lon2 + ", " + lat2 + ") is " + distance);
		
		
		return distance;
	}
	
	public void main() {
		
	}
	
}
