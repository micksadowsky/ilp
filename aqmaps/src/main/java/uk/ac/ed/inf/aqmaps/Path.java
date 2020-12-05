package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Collections; 

import java.lang.Math; 

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;


public class Path {

	ArrayList<Point> sensors_point_locs;
	Point init_loc;
	FeatureCollection no_fly_zones;
	final static Double move_length = 0.0003;
	final static Double sensor_range = 0.0002;
	final static Double return_range = 0.0003;
	
	public Path(ArrayList<Point> sensors_point_locs, Point init_loc, FeatureCollection no_fly_zones) {
		this.sensors_point_locs = sensors_point_locs;
		this.init_loc = init_loc;
		this.no_fly_zones = no_fly_zones;
	}
	
	
	public ArrayList<Integer> generatePath(int seed) {
		System.out.println("Generating path");
		var order = chooseOrder();
		System.out.println("Order chosen");
		System.out.println("Finding path");
		var full_path = findPath(order);
		System.out.println("Path found");
		System.out.println("full_path = " + full_path);
		System.out.println("full_path.size() = " + full_path.size());
		return full_path;
	}
	
	
	public ArrayList<Integer> findPath(ArrayList<Point> ordered_points) {
		var full_path = new ArrayList<Integer>();
		var curr_loc = init_loc;		
		ArrayList<Integer> curr_path;
		
		var c = 0;
		// Find paths for sensors
		for (var loc : ordered_points) {
			System.out.println(" Finding path between curr_loc and sensor" + c);
			curr_path = twoPointsPath(curr_loc, loc, sensor_range);
			full_path.addAll(curr_path);
			curr_loc = moveAlongPath(curr_loc, curr_path);
			c++;
		}
		
		// Find path to return home
		curr_path = twoPointsPath(curr_loc, init_loc, return_range);
		full_path.addAll(curr_path);
		
		return full_path;
	}
	
	public static ArrayList<Integer> twoPointsPath(Point pt1, Point pt2, Double proximity) {
		var path = new ArrayList<Integer>();
		
		var curr_point = Point.fromLngLat(pt1.longitude(), pt1.latitude());
		var move_angle = closestAngle(pt1, pt2);
		var move_counter = 0;
//		System.out.println("Entering twoPointsPath loop");
		var c=0;
		while (distance(pt2, curr_point) >= proximity) {
//			System.out.println("move_angle = " + move_angle);
//			System.out.println("twoPointsPath while loop counter = " + c);
//			System.out.printf("distance(pt2, curr_point) = %f", distance(pt2, curr_point));
//			System.out.println();
			path.add(move_angle);
			curr_point = move(curr_point, move_angle);
			move_angle = closestAngle(curr_point, pt2);
			move_counter++;
			c++;
		}
		

//		System.out.println("move_counter = " + move_counter);
//		System.out.println("pt1 = " + pt1);
//		System.out.println("curr_point = " + curr_point);
//		System.out.println("pt2 = " + pt2);
//		System.out.printf("distance(pt2, curr_point) = %f", distance(pt2, curr_point));
//		System.out.println();
		var in_range = distance(pt2, curr_point)< proximity;
//		System.out.println("in_range = " + in_range);
		
		return path;
	}
	
	public static Integer closestAngle(Point pt1, Point pt2) {
		var dist = distance(pt1, pt2);
		var lat1 = pt1.latitude();
		var lon1 = pt1.longitude();
		var lat2 = pt2.latitude();
		var lon2 = pt2.longitude();
		
		var rad_angle = Math.atan2(lat2 - lat1, lon2 - lon1);

//		var rad_angle = Math.asin((lat2 - lat1)/dist);
		if (rad_angle < 0) {
			rad_angle = rad_angle + 2*Math.PI;
		}
		var deg_angle = Math.toDegrees(rad_angle);
		var approx_angle = (int) (10*(Math.round(deg_angle/10)) % 360);
//		System.out.println("rad_angle = " + rad_angle);
//		System.out.println("deg_angle = " + deg_angle);
//		System.out.println("approx_angle = " + approx_angle);
		return approx_angle;
	}
	
	public static Point moveAlongPath(Point start_point, ArrayList<Integer> path) {
		var curr_point = start_point;
		for (var angle : path) {
			curr_point = move(curr_point, angle);
		}		
		return curr_point;
	}
	
	public static Point move(Point start_point, Integer angle) {
		var rad_angle = Math.toRadians(angle);
		var new_lon = start_point.longitude() + move_length*Math.cos(rad_angle);
		var new_lat = start_point.latitude() + move_length*Math.sin(rad_angle);
		
		var end_point = Point.fromLngLat(new_lon, new_lat);
		
		return end_point;
	}
	
	public ArrayList<Point> chooseOrder() {
		var ipt = init_loc;
		var points_given = new ArrayList<Point>(sensors_point_locs);
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

//		System.out.println("Chosen order: ");
//		for (var i = 0; i<ordered_points.size(); i++) {
//			System.out.println(i+1 + " (" + ordered_points.get(i).longitude() + ", " + ordered_points.get(i).latitude() + ")");
//		}
		
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
		var pt2 = Point.fromLngLat(-3.186537, 55.942625);
		twoPointsPath(pt1, pt2, sensor_range);
		
	}
	
}
