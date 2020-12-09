package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import com.mapbox.geojson.Point;

public class Path {

	ArrayList<SensorLocation> sensors;
	Point init_loc;
	static ArrayList<Polygon> no_fly_zones;
	final static Double move_length = 0.0003;
	final static Double sensor_range = 0.0002;
	final static Double return_range = 0.0003;
	Integer move_counter = 0;
	long seed;

	public Path(ArrayList<SensorLocation> sensors, Point init_loc, ArrayList<Polygon> no_fly_zones, long seed) {
		this.sensors = sensors;
		this.init_loc = init_loc;
		this.seed = seed;
		this.no_fly_zones = no_fly_zones;
	}

	public ArrayList<PathStep> generatePath() {
		System.out.println("Generating path");
		var order = chooseOrder();
		System.out.println("Order chosen");
		System.out.println("Finding path");
		var full_path = sensorsPath(order);
		System.out.println("Path found");
		System.out.println("full_path.size() = " + full_path.size());
		return full_path;
	}

	private ArrayList<PathStep> sensorsPath(ArrayList<SensorLocation> ordered_sensors) {
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

	private ArrayList<PathStep> twoPointsPath(SensorLocation s1, SensorLocation s2, Double proximity) {
		var path = new ArrayList<PathStep>();

		var curr_point = Point.fromLngLat(s1.point.longitude(), s1.point.latitude());
		var dest_point = Point.fromLngLat(s2.point.longitude(), s2.point.latitude());
		;
		int move_angle;

		var c = 0;
		while (move_counter < 150) {
			if (path.size() > 0) {
				move_angle = chooseAngle(curr_point, dest_point, path.get(path.size() - 1).angle, false);
			} else {
				move_angle = chooseAngle(curr_point, dest_point, null, false);
			}
			System.out.println("\n----MOVING move_angle = " + move_angle + "\n" + "");
			curr_point = move(curr_point, move_angle);
			move_counter++;
			if (distance(dest_point, curr_point) < proximity) {
				path.add(new PathStep(move_angle, s2.location));
				break;
			} else {
				path.add(new PathStep(move_angle));
			}
			System.out.println(move_counter);
			c++;
		}
		return path;
	}


	private Double ratioOfForbidden(Point pt, Integer angle) {
		var min_angle = angle-180;
		var max_angle = angle+180;
		var forbidden_count = 0.;
		var total_count = 0.;
		for (var i = min_angle; i<=max_angle; i = i+10) {
			total_count++;
			if (forbidden(pt, angle360(i))) {
				forbidden_count++;
			}
		}
		var ratio = forbidden_count/total_count;
		return ratio;
	}
	private Integer chooseAngle(Point curr_point, Point dest_point, Integer last_angle,
			boolean check_for_stalls) {
		var move_angle = closestAngle(curr_point, dest_point);
		System.out.println("closest_angle = " + move_angle);
		boolean stall;
		if (last_angle == null) {
			stall = false;
		} else {
			stall = last_angle.equals(angle360(move_angle - 180));
		}
		if (forbidden(curr_point, move_angle) || stall) {
			System.out.println("Move found to be forbidden, move_angle = " + move_angle);
			var tried_angles = new ArrayList<Integer>();
			if (last_angle != null) {
				tried_angles.add(angle360(last_angle - 180));
			}
			tried_angles.add(angle360(move_angle));
			move_angle = nextBestAngle(curr_point, dest_point, move_angle, tried_angles, check_for_stalls);
		}
		return move_angle;
	}

	/**
	 * Choose angle between two points 
	 * 
	 * @param curr_point
	 * @param dest_point
	 * @param closest_angle
	 * @param angles_tried
	 * @param check_for_stalls
	 * @return
	 */
	private Integer nextBestAngle(Point curr_point, Point dest_point, Integer closest_angle,
			ArrayList<Integer> angles_tried, boolean check_for_stalls) {
		// consider two angles closest to the original
		var smaller_angle = closest_angle;
		var greater_angle = closest_angle;
		var tried_angles = new ArrayList<Integer>(angles_tried);
		// continue expanding the angles until reaching the ones not yet tried
		var considered_angles = new ArrayList<Integer>();
		// find first legal smaller angle
		while (true) {
			smaller_angle = angle360(smaller_angle - 10);
			if (!tried_angles.contains(smaller_angle)) {
				tried_angles.add(smaller_angle);
				if (!forbidden(curr_point, smaller_angle)) {
					System.out.println("Found a legal smaller angle = " + smaller_angle);
					considered_angles.add(smaller_angle);
					break;
				}
			} else {
				System.out.println("smaller angle aldready tried" + smaller_angle);
				if (tried_angles.size() == 36) {
					break;
				}
			}
		}
		// find first legal greater angle
		while (true) {
			greater_angle = angle360(greater_angle + 10);
			if (!tried_angles.contains(greater_angle)) {
				tried_angles.add(greater_angle);
				if (!forbidden(curr_point, greater_angle)) {
					System.out.println("Found a legal greater angle = " + greater_angle);
					considered_angles.add(greater_angle);
					break;
				}
			} else {
				System.out.println("greater angle aldready tried" + greater_angle);
				if (tried_angles.size() == 36) {
					break;
				}
			}
		}

		// if both work, choose the one resulting closer location to the sensor
		if (considered_angles.size() == 2) {

			// points after moving at each angle			
			var smaller_angle_point = move(curr_point, smaller_angle);
			var greater_angle_point = move(curr_point, greater_angle);

			var closest_angle_for_smaller = closestAngle(smaller_angle_point, dest_point); 
			var closest_angle_for_greater = closestAngle(greater_angle_point, dest_point); 

			// next default move forbidden
			var next_smaller_forbidden = forbidden(smaller_angle_point, closest_angle_for_smaller);
			var next_greater_forbidden = forbidden(greater_angle_point, closest_angle_for_greater);
			System.out.println("next_smaller_forbidden = "+ next_smaller_forbidden);
			System.out.println("next_greater_forbidden = "+ next_greater_forbidden);

			
			// Ratios forbidden/available
			var smaller_angle_ratio = ratioOfForbidden(smaller_angle_point, closest_angle_for_smaller);
			var greater_angle_ratio = ratioOfForbidden(greater_angle_point, closest_angle_for_greater);
			System.out.println("smaller_angle_ratio = "+ smaller_angle_ratio);
			System.out.println("greater_angle_ratio = "+ greater_angle_ratio);
			
			// Comparing angles to the closest one
			var angle_dest = angle(curr_point, dest_point);
			var angle_smaller = angle(curr_point, smaller_angle_point);
			var angle_greater = angle(curr_point, greater_angle_point);
//			System.out.println("angle_dest = "+ angle_dest);
//			System.out.println("angle_smaller = "+ angle_smaller);
//			System.out.println("angle_greater = "+ angle_greater);
			
			var smaller_angle_comparison = Math.abs(angle_dest-angle_smaller);
			System.out.println("smaller_angle_comparison = "+ smaller_angle_comparison);
			var greater_angle_comparison = Math.abs(angle_greater-angle_dest);
			System.out.println("greater_angle_comparison = "+ greater_angle_comparison);
			
			// distance comparison
			var smaller_angle_distance = distance(dest_point, move(curr_point, smaller_angle));
			System.out.println("smaller_angle_distance = "+ smaller_angle_distance);
			var greater_angle_distance = distance(dest_point, move(curr_point, greater_angle));
			System.out.println("greater_angle_distance = "+ greater_angle_distance);

			var randomizer = new Random(seed);
			var random_angle = considered_angles.get(randomizer.nextInt(considered_angles.size()));
			
			var smaller_points = 0;
			var greater_points = 0;
			
			if (smaller_angle_comparison < greater_angle_comparison) { smaller_points++;}
			if (smaller_angle_comparison > greater_angle_comparison) { greater_points++;}

			if (smaller_angle_distance < greater_angle_distance) { smaller_points++;}
			if (smaller_angle_distance > greater_angle_distance) { greater_points++;}
			
			if (smaller_angle_ratio < greater_angle_ratio) { smaller_points++;}
			if (smaller_angle_ratio > greater_angle_ratio) { greater_points++;}

			if (!next_smaller_forbidden) {smaller_points = smaller_points+2;}
			if (!next_greater_forbidden) {greater_points = greater_points+2;}

			System.out.println("smaller_points = " + smaller_points);
			System.out.println("greater_points = " + greater_points);

			
			if (smaller_points > greater_points) {
				System.out.println("Chose smaller_angle with points = " + smaller_points);
				return smaller_angle;
			} else {
				System.out.println("Chose greater_angle with points = " + greater_points);
				return greater_angle;
			}
			
			
//			if (smaller_angle_comparison < greater_angle_comparison) {
//				System.out.println("smaller_angle_comparison is smaller");
//				System.out.println("Chose smaller_angle = " + smaller_angle);
//				return smaller_angle;
//			}else if (smaller_angle_comparison > greater_angle_comparison){
//				System.out.println("greater_angle_comparison is smaller");
//				System.out.println("Chose greater_angle = " + greater_angle);
//				return greater_angle;
//			}else {
//				System.out.println("greater_angle_ratio are equal");
//				if (smaller_angle_distance < greater_angle_distance) {
//					System.out.println("Chose smaller_angle = " + smaller_angle);
//					return smaller_angle;
//				} else {
//					System.out.println("Chose greater_angle = " + greater_angle);
//					return greater_angle;
//				}
//				// lallalalal
//
//			}			
			
			// if only one angle legal, go for it
		} else if (considered_angles.size() == 1) {
			System.out.println("Chose one avalaible angle = " + considered_angles.get(0));
			return considered_angles.get(0);
			// otherwise, try again
		} else {
			return nextBestAngle(curr_point, dest_point, closest_angle, tried_angles, check_for_stalls);
		}
	}

	/**
	 * Choose a multiple of 10 angle closest to represent the straight line from pt1 to pt2
	 * 
	 * @param pt1 starting point
	 * @param pt2 end point
	 * @return Returns the closest angle being multiple of 10
	 */
	private Integer closestAngle(Point pt1, Point pt2) {
		// parse parameters
		var lat1 = pt1.latitude();
		var lon1 = pt1.longitude();
		var lat2 = pt2.latitude();
		var lon2 = pt2.longitude();

		// calculate the angle from a straight line between points
		var rad_angle = Math.atan2(lat2 - lat1, lon2 - lon1);
		var deg_angle = Math.toDegrees(rad_angle);
		
		// round up to an angle that's a multiple of 10
		var approx_angle = (int) (10 * (Math.round(deg_angle / 10)));
		approx_angle = angle360(approx_angle);

		return approx_angle;
	}

	private Double angle(Point pt1, Point pt2) {
		var lat1 = pt1.latitude();
		var lon1 = pt1.longitude();
		var lat2 = pt2.latitude();
		var lon2 = pt2.longitude();
		// calculate the angle from a straight line between points
		var rad_angle = Math.atan2(lat2 - lat1, lon2 - lon1);
		var deg_angle = Math.toDegrees(rad_angle);
		
		return deg_angle;
	}
	
	/**
	 * Checks whether a move would result inside a no fly zone or outside the confinement area, 
	 * using the JTS library.
	 * 
	 * @param from starting point
	 * @param angle direction of the move
	 * @return Returns true if move is forbidden, false, otherwise
	 */
	private boolean forbidden(Point from, int angle) {
		// set up points
		var start_jts_coor = new Coordinate(from.longitude(), from.latitude());
		var end_pt = move(from, angle);
		var end_jts_coor = new Coordinate(end_pt.longitude(), end_pt.latitude());

		// initialise JTS stuff
		var gf = new GeometryFactory();
		var csf = gf.getCoordinateSequenceFactory();

		// convert to data type required by JTS
		Coordinate[] coordinate_sequence = { start_jts_coor, end_jts_coor };
		var jts_coordinate_sequence = csf.create(coordinate_sequence);

		// construct a JTS LineString to represent the move
		var move_line = gf.createLineString(jts_coordinate_sequence);

		// construct the confinement area JTS LineString
		var nw_pt = new Coordinate(-3.192473, 55.946233);
		var sw_pt = new Coordinate(-3.192473, 55.942617);
		var se_pt = new Coordinate(-3.184319, 55.942617);
		var ne_pt = new Coordinate(-3.184319, 55.946233);
		Coordinate[] campus_coordinate_sequence = { nw_pt, sw_pt, se_pt, ne_pt, nw_pt };
		var jts_campus_coordinate_sequence = csf.create(campus_coordinate_sequence);
		var campus_border = gf.createLineString(jts_campus_coordinate_sequence);

		// check for intersection with the confinement area
		if (move_line.intersects(campus_border)) {
			return true;
		}

		// check for intersection with any of the no fly zones
		for (var nfz : no_fly_zones) {
			if (move_line.intersects(nfz)) {
				return true;
			}
		}
		return false;
	}
	

	/**
	 * Calculates the location resulting from moving along the specified path
	 * @param pt starting point
	 * @param path the path to follow
	 * @return Returns resulting location Point
	 */
	private Point moveAlongPath(Point pt, ArrayList<PathStep> path) {
		var curr_point = pt;
		for (var step : path) {
			curr_point = move(curr_point, step.angle);
		}
		return curr_point;
	}

	/**
	 * Calculates the location resulting from a single move in direction of the angle
	 * 
	 * @param start_point the point from which to move 
	 * @param angle the direction of the move
	 * @return the resulting new location Point
	 */
	public static Point move(Point start_point, Integer angle) {
		var rad_angle = Math.toRadians(angle);
		var new_lon = start_point.longitude() + move_length * Math.cos(rad_angle);
		var new_lat = start_point.latitude() + move_length * Math.sin(rad_angle);

		var end_point = Point.fromLngLat(new_lon, new_lat);

		return end_point;
	}


	/**
	 * Determines the order in which to visit sensors. It selects the closest sensor, then sensor closest to that sensor
	 * and so on. 
	 * 
	 * @return Returns an ordered ArrayList of SensorLocations
	 */
	private ArrayList<SensorLocation> chooseOrder() {
		// Set up ArrayLists 
		var points_given = new ArrayList<SensorLocation>(sensors);
		var ordered_points = new ArrayList<SensorLocation>();
		// fix the number of sensors to order
		var no_points = points_given.size();

		// Choose the first sensor to go to
		var first_sensor = chooseClosest(points_given, init_loc);
		ordered_points.add(first_sensor);
		points_given.remove(first_sensor);

		// Choose order for the remaining sensors
		var current_point = first_sensor; //first_sensor;
		for (var i = 0; i < no_points - 1; i++) {
			var closest = chooseClosest(points_given, current_point.point);
			ordered_points.add(closest);
			points_given.remove(closest);
			current_point = closest;
		}

		// Print out the result
		System.out.println("Chosen order: ");
		for (var i = 0; i < ordered_points.size(); i++) {
			System.out.println(i + 1 + " (" + ordered_points.get(i).location + ", "
					+ ordered_points.get(i).point.longitude() + ", " + ordered_points.get(i).point.latitude() + ")");
		}

		return ordered_points;
	}

	/**
	 * Chooses a point from an ArrayList of Sensor Locations that closes to the specified point
	 * @param points is an ArrayList of SensorLocations
	 * @param ipt is a Point to which the distance is measured
	 * @return returns a SensorLocation that is closest to point ipt
	 */
	private SensorLocation chooseClosest(ArrayList<SensorLocation> points, Point ipt) {
		// initiate list
		var distances = new ArrayList<Double>();

		// measure distance between all points
		for (var pt : points) {
			distances.add(distance(ipt, pt.point));
		}

		// choose the closest
		var min_dist = Collections.min(distances);
		var closest = points.get(distances.indexOf(min_dist));

		return closest;
	}

	
	/**
	 * Calculates distance between two points using the Pythagoras's theorem
	 * 
	 * @param pt1 Point 1
	 * @param pt2 Point 2
	 * @return distance in degrees
	 */
	public static Double distance(Point pt1, Point pt2) {
		var lon1 = pt1.longitude();
		var lat1 = pt1.latitude();
		var lon2 = pt2.longitude();
		var lat2 = pt2.latitude();

		var distance = Math.sqrt(Math.pow((lon2 - lon1), 2) + Math.pow((lat2 - lat1), 2));

		return distance;
	}

	/**
	 * Converts angles to a value in [0, 360) degrees
	 * 
	 * @param angle is an angle in degrees
	 * @return returns an angle that is in [0, 360) degrees
	 */
	public static Integer angle360(Integer angle) {
		return (((angle % 360) + 360) % 360);
	}
	
	public static void main(String[] args) {

	}
	
//	public static boolean willStall(Point curr_loc, Point dest_point, Integer chosen_angle) {
//	System.out.println("Checking if will stall for angle = " + chosen_angle);
//	var next_chosen_angle = chooseAngle(move(curr_loc, chosen_angle), dest_point, false);
//	System.out.println("next_chosen_angle = " + next_chosen_angle);
//	System.out.println("angle360(next_chosen_angle - 180) = " + angle360(next_chosen_angle - 180));
//
//	if (chosen_angle.equals(angle360(next_chosen_angle - 180))){
//		System.out.println("Would STALL for angle = " + chosen_angle);
//		return true;
//	} else {
//		System.out.println("Would NOT STALL for angle = " + chosen_angle);
//		return false;
//	}
//	var second_loc = move(curr_loc, chosen_angle);
//	var third_loc = move(second_loc, chooseAngle(second_loc, dest_point, false));
//	if (curr_loc.equals(third_loc)) {
//		System.out.println("Would STALL for angle = " + chosen_angle);
//		return true;
//	} else {
//		System.out.println("Would NOT STALL for angle = " + chosen_angle);
//		return false;
//	}
//}



}
