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
	final static Double move_length = 0.0003;
	final static Double proximity = 0.0002;
	
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
	
	/**
	 * @param pt1
	 * @param pt2
	 */
	public static void twoPointsPath(Point pt1, Point pt2) {
		var curr_point = Point.fromLngLat(pt1.longitude(), pt1.latitude());
		var move_angle = closestAngle(pt1, pt2);
		var move_counter = 0;
		while (distance(pt2, curr_point) >= 0.0002) {
			curr_point = move(curr_point, move_angle);
			move_angle = closestAngle(curr_point, pt2);
			move_counter++;
		}
		
		System.out.println("move_counter = " + move_counter);
		System.out.println("pt1 = " + pt1);
		System.out.println("curr_point = " + curr_point);
		System.out.println("pt2 = " + pt2);
		System.out.printf("distance(pt2, curr_point) = %f", distance(pt2, curr_point));
		System.out.println();
		
		
		var in_range = distance(pt2, curr_point)< 0.0002;
		System.out.println("in_range = " + in_range);
	}
	
	public static Double closestAngle(Point pt1, Point pt2) {
		var dist = distance(pt1, pt2);
		var lat1 = pt1.latitude();
		var lat2 = pt2.latitude();

		var rad_angle = Math.asin((lat2 - lat1)/dist);
		var deg_angle = Math.toDegrees(rad_angle);
		var approx_angle = (double) (10*(Math.round(deg_angle/10)) % 360);
//		System.out.println("rad_angle = " + rad_angle);
//		System.out.println("deg_angle = " + deg_angle);
//		System.out.println("approx_angle = " + approx_angle);
		return approx_angle;
	}
	
	
	public static Point move(Point start_point, Double angle) {
		var rad_angle = Math.toRadians(angle);
		var new_lon = start_point.longitude() + move_length*Math.cos(rad_angle);
		var new_lat = start_point.latitude() + move_length*Math.sin(rad_angle);
		
		var end_point = Point.fromLngLat(new_lon, new_lat);
		
		return end_point;
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
	
	public static Double distance(Point pt1, Point pt2) {
		var lon1 = pt1.longitude();
		var lat1 = pt1.latitude();
		var lon2 = pt2.longitude();
		var lat2 = pt2.latitude();
		
		var distance = Math.sqrt( Math.pow((lon1 - lon2), 2) + Math.pow((lat1 - lat2), 2) );
		
//		System.out.println("Distance between (" + lon1 + ", " + lat1 + 
//				") and (" + lon2 + ", " + lat2 + ") is " + distance);
		
		
		return distance;
	}
	
	public static void main(String[] args) {
		var pt1 = Point.fromLngLat(-3.18697, 55.942688);
		var pt2 = Point.fromLngLat(-3.186537, 55.949925);
		twoPointsPath(pt1 ,pt2);
		
	}
	
}
