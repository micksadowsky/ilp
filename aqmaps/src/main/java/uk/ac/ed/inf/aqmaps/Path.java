package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Collections; 

import java.lang.Math; 

import com.mapbox.geojson.Point;

import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

public class Path {

	ArrayList<SensorLocation> sensors;
	Point init_loc;
	static ArrayList<Polygon> no_fly_zones;
	final static Double move_length = 0.0003;
	final static Double sensor_range = 0.0002;
	final static Double return_range = 0.0003;
	Integer move_counter = 0;

	public Path(ArrayList<SensorLocation> sensors, Point init_loc, ArrayList<Polygon> no_fly_zones) {
		this.sensors = sensors;
		this.init_loc = init_loc;
		this.no_fly_zones = no_fly_zones;
	}
	
	public ArrayList<PathStep> generatePath() {
		System.out.println("Generating path");
		var order = chooseOrder();
		System.out.println("Order chosen");
		System.out.println("Finding path");
		var full_path = sensorsPath(order);
		System.out.println("Path found");
//		System.out.println("full_path = " + full_path);
		System.out.println("full_path.size() = " + full_path.size());
		return full_path;
	}
	
	public ArrayList<PathStep> sensorsPath(ArrayList<SensorLocation> ordered_sensors) {
		var full_path = new ArrayList<PathStep>();
		var init_as_sensor = new SensorLocation("", init_loc);
		var curr_loc = init_as_sensor;
//		ArrayList<PathStep> curr_path;
		
		// Find paths for sensors
		for (var sen : ordered_sensors) {
			System.out.println("Finding path to sensor = " + sen.location);
			var curr_path = twoPointsPath(curr_loc, sen, sensor_range);
			full_path.addAll(curr_path);
			curr_loc = new SensorLocation("", moveAlongPath(curr_loc.point, curr_path));
		}
		
		// Find path to return home
		var curr_path = twoPointsPath(curr_loc, init_as_sensor, return_range);
		full_path.addAll(curr_path);
		
		return full_path;
	}
	
	public  ArrayList<PathStep> twoPointsPath(SensorLocation s1, SensorLocation s2, Double proximity) {
		var path = new ArrayList<PathStep>();
		
		var curr_point = Point.fromLngLat(s1.point.longitude(), s1.point.latitude());
		var dest_point = Point.fromLngLat(s2.point.longitude(), s2.point.latitude());;
		int move_angle;
		
		var c = 0;
		while (move_counter < 150) {
			if (path.size()>0) {
				move_angle = chooseAngle(curr_point, dest_point, path.get(path.size() - 1).angle, false);
			} else {
				move_angle = chooseAngle(curr_point, dest_point, null, false);
			}
			System.out.println("\n----MOVING move_angle = " + move_angle+ "\n"
					+ "");
			curr_point = move(curr_point, move_angle);
			move_counter++;
			if (distance(dest_point, curr_point) < proximity) {
				path.add(new PathStep(move_angle, s2.location));
				break;
			} else {
				path.add(new PathStep(move_angle));
			}
			c++;
		}
		return path;
	}
	
//	public static boolean willStall(Point curr_loc, Point dest_point, Integer chosen_angle) {
//		System.out.println("Checking if will stall for angle = " + chosen_angle);
//		var next_chosen_angle = chooseAngle(move(curr_loc, chosen_angle), dest_point, false);
//		System.out.println("next_chosen_angle = " + next_chosen_angle);
//		System.out.println("angle360(next_chosen_angle - 180) = " + angle360(next_chosen_angle - 180));
//
//		if (chosen_angle.equals(angle360(next_chosen_angle - 180))){
//			System.out.println("Would STALL for angle = " + chosen_angle);
//			return true;
//		} else {
//			System.out.println("Would NOT STALL for angle = " + chosen_angle);
//			return false;
//		}
//		var second_loc = move(curr_loc, chosen_angle);
//		var third_loc = move(second_loc, chooseAngle(second_loc, dest_point, false));
//		if (curr_loc.equals(third_loc)) {
//			System.out.println("Would STALL for angle = " + chosen_angle);
//			return true;
//		} else {
//			System.out.println("Would NOT STALL for angle = " + chosen_angle);
//			return false;
//		}
//	}
	
	public static Integer chooseAngle(Point curr_point, Point dest_point, Integer last_angle, boolean check_for_stalls) {
		var move_angle = closestAngle(curr_point, dest_point);
		System.out.println("closest_angle = " + move_angle);
		boolean stall;
		if (last_angle == null) {
			stall = false;
		} else {
			stall = last_angle.equals(angle360(move_angle-180));
		}
		if (forbidden(curr_point, move_angle) || stall) {
			System.out.println("Move found to be forbidden, move_angle = " + move_angle);
			var tried_angles = new ArrayList<Integer>();
			tried_angles.add(move_angle);
			if (last_angle != null) {
				tried_angles.add(angle360(last_angle-180));
			}
//			tried_angles.add(angle360(move_angle));
			move_angle = nextBestAngle(curr_point, dest_point, move_angle, tried_angles, check_for_stalls);
		}
		return move_angle;
	}
	
	public static Integer nextBestAngle(Point curr_point, Point dest_point, Integer closest_angle, ArrayList<Integer> angles_tried, boolean check_for_stalls) {
		// consider two angles closest to the original
		var smaller_angle = closest_angle;
		var greater_angle = closest_angle;
		var tried_angles = new ArrayList<Integer>(angles_tried);
		// continue expanding the angles until reaching the ones not yet tried
		var considered_angles = new ArrayList<Integer>();
		// find first legal smaller angle
		while (true) {
			smaller_angle = angle360(smaller_angle - 10) ;
			if (!tried_angles.contains(smaller_angle)) {
				tried_angles.add(smaller_angle);
//				System.out.println("tried_angles = " + tried_angles);
				if(!forbidden(curr_point, smaller_angle)) {
					System.out.println("Found a legal smaller angle = " + smaller_angle);
//					if (check_for_stalls) {
//						if (!willStall(curr_point, dest_point, smaller_angle)) {
//							considered_angles.add(smaller_angle);
//							break;
//							} else {
//								System.out.println("Skipping the angle to avoid stalling");
//							}
//					} else {
						considered_angles.add(smaller_angle);
						break;
//					}
				}
			} else {
				System.out.println("smaller angle aldready tried" + smaller_angle);
				if(tried_angles.size() == 36) {
					break;
				}
				}
			
		}
		// find first legal greater angle
		while (true) {
			greater_angle = angle360(greater_angle + 10);
			if (!tried_angles.contains(greater_angle)) {
				tried_angles.add(greater_angle);
//				System.out.println("tried_angles = " + tried_angles);
				if(!forbidden(curr_point, greater_angle)) {
					System.out.println("Found a legal greater angle = " + greater_angle);
//					if (check_for_stalls) {
//						if (!willStall(curr_point, dest_point, greater_angle)) {
//							considered_angles.add(greater_angle);
//							break;
//							} else {
//								System.out.println("Skipping the angle to avoid stalling");
//							}
//					} else {
						considered_angles.add(greater_angle);
						break;
//					}
				}
			} else {
				System.out.println("greater angle aldready tried" + greater_angle);
				if(tried_angles.size() == 36) {
					break;
				}
			}

		}
		
		// if both work, choose the one resulting closer location to the sensor
		if (considered_angles.size() == 2) {
			var smaller_angle_distance = distance(dest_point, move(curr_point, smaller_angle));
			var greater_angle_distance = distance(dest_point, move(curr_point, greater_angle));
			if (smaller_angle_distance < greater_angle_distance) {
				System.out.println("Chose smaller_angle = " + smaller_angle);
				return smaller_angle;
			} else {
				System.out.println("Chose greater_angle = " + greater_angle);
				return greater_angle;
			}
		// if only one angle legal, go for it
		} else if (considered_angles.size() == 1) {
			System.out.println("Chose one avalaible angle = " + considered_angles.get(0));
			return considered_angles.get(0);
		// otherwise, try again
		} else {
			return nextBestAngle(curr_point, dest_point, closest_angle, tried_angles, check_for_stalls);
		}
	}
	
	public static Point moveAlongPath(Point pt, ArrayList<PathStep> path) {
		var curr_point = pt;
		for (var step : path) {
			curr_point = move(curr_point, step.angle);
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
	
	public static boolean forbidden(Point from, int angle) {
		// set up points 
		var start_jts_coor = new Coordinate(from.longitude(), from.latitude());
		var end_pt = move(from, angle);
		var end_jts_coor = new Coordinate(end_pt.longitude(), end_pt.latitude());
		
		// initialise JTS stuff
		var gf = new GeometryFactory();
		var csf = gf.getCoordinateSequenceFactory();
		
		// convert to data type required by JTS
		Coordinate[] coordinate_sequence = {start_jts_coor, end_jts_coor};
		var jts_coordinate_sequence = csf.create(coordinate_sequence);
		
		// construct a JTS LineString
		var move_line = gf.createLineString(jts_coordinate_sequence);
		
		// construct the confinement area JTS Linestring
		var nw_pt = new Coordinate(-3.192473, 55.946233);
		var sw_pt = new Coordinate(-3.192473, 55.942617);
		var se_pt = new Coordinate(-3.184319, 55.942617);
		var ne_pt = new Coordinate(-3.184319, 55.946233);
		Coordinate[] campus_coordinate_sequence = {nw_pt, sw_pt, se_pt, ne_pt, nw_pt};
		var jts_campus_coordinate_sequence = csf.create(campus_coordinate_sequence);
		var campus_border = gf.createLineString(jts_campus_coordinate_sequence);

		
		// check for any intersection
		if (move_line.intersects(campus_border)) {
			return true;
		}
		
		for (var nfz : no_fly_zones) {
			if (move_line.intersects(nfz)) {
				return true;
			}
		}
		return false;
	}
	
	public ArrayList<SensorLocation> chooseOrder() {
		var ipt = init_loc;
		var points_given = new ArrayList<SensorLocation>(sensors);
		var ordered_points = new ArrayList<SensorLocation>();
		var no_points = points_given.size();
//		System.out.println("no_points = " + no_points);
		
		// Choose the first sensor to go to 
		var first_point = chooseClosest(points_given, ipt);
		ordered_points.add(first_point);
		points_given.remove(first_point);
		
		// Choose the sensor ending the path
//		var last_point = chooseClosest(points_given, ipt);
//		points_given.remove(last_point);
		
		// Calculate points in between
		var current_point = first_point;		
		for (var i=0; i<no_points-1; i++) {
			var closest = chooseClosest(points_given, current_point.point);
			ordered_points.add(closest);
			points_given.remove(closest);
			current_point = closest;
		}
		
//		ordered_points.add(last_point);

		System.out.println("Chosen order: ");
		for (var i = 0; i<ordered_points.size(); i++) {
			System.out.println(i+1 + " ("+ ordered_points.get(i).location + ", " + ordered_points.get(i).point.longitude() + ", " + ordered_points.get(i).point.latitude() + ")");
		}
		
		return ordered_points;
	}
	
	public SensorLocation chooseClosest(ArrayList<SensorLocation> points, Point ipt) {
		// initiate list
		var distances = new ArrayList<Double>();
		
		// measure distance between all points
		for (var pt : points) {
			distances.add(distance(ipt, pt.point));
		}

		// choose the closest
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
	
	public static Integer angle360(Integer angle) {
		return (((angle % 360) + 360) % 360);
	}
	
	public static void main(String[] args) {
		var pt1 = Point.fromLngLat(-3.18697, 55.942688);
		var pt2 = Point.fromLngLat(-3.186537, 55.942625);
//		twoPointsPath(pt1, pt2, sensor_range);
		
	}
	
}
