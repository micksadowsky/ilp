package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Collections;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import com.mapbox.geojson.Point;

/**
 * It is responsible for determining the path, i.e. instructions on how to fly and when to take readings. 
 * 
 * @author Michal Sadowski
 *
 */
public class Path {

	private ArrayList<SensorLocation> sensors;
	private Point init_loc;
	private ArrayList<Polygon> no_fly_zones;
	
	private final static Double move_length = 0.0003;
	final static Double sensor_range = 0.0002;
	private final static Double return_range = 0.0003;
	private int move_counter = 0;

	/**
	 * @param sensors an unordered list of sensors to consider
	 * @param init_loc start and end location
	 * @param no_fly_zones areas to avoid in JTS type
	 */
	public Path(ArrayList<SensorLocation> sensors, Point init_loc, ArrayList<Polygon> no_fly_zones) {
		this.sensors = sensors;
		this.init_loc = init_loc;
		this.no_fly_zones = no_fly_zones;
	}

	/**
	 * Generates a path starting at the init_loc of the object, visiting all sensors, and returns close to init_loc
	 *  
	 * @return returns the path
	 */
	public ArrayList<PathStep> generatePath() {
		// choose order in which to visit sensors
		var order = chooseOrder();
		// find how to visit the sensors in that order and construct a path
		var full_path = sensorsPath(order);
		
		return full_path;
	}

	/**
	 * Calculates a path between sensors in a given order and determines after which move
	 * the drone should be in range of specific sensors
	 * 
	 * @param ordered_sensors an ordered ArrayList of sensors to visit
	 * @return the path between the specified ordered sensors
	 */
	private ArrayList<PathStep> sensorsPath(ArrayList<SensorLocation> ordered_sensors) {
		// an ArrayList of steps
		var full_path = new ArrayList<PathStep>();
		// convert the initial location to the type required by twoPointsPath
		var init_as_sensor = new SensorLocation("null", init_loc);
		// set the starting point to initial location
		var curr_loc = init_as_sensor;

		// Find paths between the starting point and the first sensor, then other sensors
		for (var sen : ordered_sensors) {
//			System.out.println("Finding path to sensor = " + sen.location);
			var curr_path = twoPointsPath(curr_loc, sen, sensor_range);
			full_path.addAll(curr_path);
			curr_loc = new SensorLocation("", moveAlongPath(curr_loc.point, curr_path));
		}

		// Find path to return home
		var curr_path = twoPointsPath(curr_loc, init_as_sensor, return_range);
		full_path.addAll(curr_path);

		return full_path;
	}

	/**
	 * Calculates the steps (angles at which to move) to get from sensor s1 
	 * to a location at maximum distance of proximity to s2. Each step determines
	 * whether to take a reading after a move.
	 * 
	 * @param s1 sensor to start the path at
	 * @param s2 sensor to finish the path at
	 * @param proximity the maximum distance from the destination point
	 * @return returns a list of steps
	 */
	private ArrayList<PathStep> twoPointsPath(SensorLocation s1, SensorLocation s2, Double proximity) {
		// initiate a list of steps
		var path = new ArrayList<PathStep>();

		// make a copy of the sensor locations
		var curr_point = Point.fromLngLat(s1.point.longitude(), s1.point.latitude());
		var dest_point = Point.fromLngLat(s2.point.longitude(), s2.point.latitude());
		int move_angle;

		// limit the calculations to 150 moves
		while (move_counter < 150) {
			// find the direction of the move
			if (path.size() > 0) {
				move_angle = chooseAngle(curr_point, dest_point, path.get(path.size() - 1).angle);
			} else {
				move_angle = chooseAngle(curr_point, dest_point, null);
			}
			curr_point = move(curr_point, move_angle);
			move_counter++;
			
			// determine whether to instruct to read a sensor
			if (distance(dest_point, curr_point) < proximity) {
				path.add(new PathStep(move_angle, s2.location));
				break;
			} else {
				path.add(new PathStep(move_angle));
			}
		}
		return path;
	}

	/**
	 * Choose angle to indicate best direction to move in to reach destination
	 * 
	 * @param curr_point the current location of the drone
	 * @param dest_point the destination point
	 * @param last_angle the angle, which got us to curr_point
	 * @return returns an angle, in which the drone should move
	 */
	private Integer chooseAngle(Point curr_point, Point dest_point, Integer last_angle) {
		// Choose an allowed angle to the destination
		var move_angle = closestAngle(curr_point, dest_point);
//		System.out.println("closest_angle = " + move_angle);
		
		// prevent from returning to the same point
		boolean stall;
		if (last_angle == null) {
			stall = false;
		} else {
			stall = last_angle.equals(angle360(move_angle - 180));
		}
		
		// if next move forbidden or about to get stuck  
		if (forbidden(curr_point, move_angle) || stall) {
//			System.out.println("Move found to be forbidden, move_angle = " + move_angle);
			
			// keep track of angles already ruled out 
			var tried_angles = new ArrayList<Integer>();
			tried_angles.add(move_angle);
			if (last_angle != null) {
				tried_angles.add(angle360(last_angle - 180));
			}
			
			// find another allowed angle switching to more advanced decision making
			move_angle = nextBestAngle(curr_point, dest_point, move_angle, tried_angles);
		}
		
		return move_angle;
	}

	/**
	 * Determine next available point. Keeps increasing/decreasing the angle until not forbidden.
	 * Then returns better one.
	 * 
	 * @param curr_point current location
	 * @param dest_point destination location
	 * @param closest_angle the angle closest to a straight line but resulting in a forbidden move
	 * @param angles_tried list of already discarded angles
	 * @return next best angle
	 */
	private Integer nextBestAngle(Point curr_point, Point dest_point, Integer closest_angle,
			ArrayList<Integer> angles_tried) {
		
		// keep track of discarded angles
		var tried_angles = new ArrayList<Integer>(angles_tried);
		var considered_angles = new ArrayList<Integer>();
		
		// find first legal angle by decreasing it
		var decreased_angle = adjustAngle(closest_angle, -10, curr_point, tried_angles);
		if (decreased_angle != null) {
			considered_angles.add(decreased_angle);
		}
		
		// find first legal angle by increasing it
		var increased_angle = adjustAngle(closest_angle, 10, curr_point, tried_angles);
		if (increased_angle != null) {
			considered_angles.add(increased_angle);
		}

		if (considered_angles.size() == 2) {
			// if two angles possible, decide based on multiple factors
			return angleBasedOnPoints(decreased_angle, increased_angle, curr_point, dest_point);
		} else {
			// if only one angle found, return it
			return considered_angles.get(0);
		}
	}

	/**
	 * Chooses one of two angles based on four factors:
	 * 1. Ratio of forbidden moves out of the possible 36 angles 
	 * at the point resulting in moving in the angle direction
	 * 2. Whether a move in default direction at such point would be legal
	 * 3. Distance to the destination at such points
	 * 4. How close the considered angles are to the closest angle
	 *  
	 *  These factors are given weights: 2, 2, 2, 1 respectively. 
	 *  If the ratio is similar for both angles,the distance weight
	 *  is increased by one
	 * 
	 * @param decreased_angle one angle to consider
	 * @param increased_angle another angle to consider
	 * @param curr_point current location
	 * @param dest_point destination point
	 * @return the better scoring angle
	 */
	private Integer angleBasedOnPoints(int decreased_angle, int increased_angle, Point curr_point, Point dest_point) {
		// points after moving in direction of considered angle			
		var decreased_angle_point = move(curr_point, decreased_angle);
		var increased_angle_point = move(curr_point, increased_angle);

		// default angle to destination from the point after move in direction of angle
		var closest_angle_for_decreased = closestAngle(decreased_angle_point, dest_point); 
		var closest_angle_for_increased = closestAngle(increased_angle_point, dest_point); 

		// next default move forbidden
		var next_move_for_decreased_forbidden = forbidden(decreased_angle_point, closest_angle_for_decreased);
		var next_move_for_greater_forbidden = forbidden(increased_angle_point, closest_angle_for_increased);
		
		// Ratios forbidden/available
		var angle_ratio_for_decreased = ratioOfForbidden(decreased_angle_point);
		var angle_ratio_for_increased = ratioOfForbidden(increased_angle_point);

		// Calculate difference between considered angle and closest one
		var exact_angle_closest = angle(curr_point, dest_point);
		var exact_angle_decreased = angle(curr_point, decreased_angle_point);
		var exact_angle_increased = angle(curr_point, increased_angle_point);
		var decreased_angle_comparison = Math.abs(exact_angle_closest-exact_angle_decreased);
		var increased_angle_comparison = Math.abs(exact_angle_increased-exact_angle_closest);

		// Calculate distance to dest point from point after move in considered angle 
		var decreased_angle_distance = distance(dest_point, move(curr_point, decreased_angle));
		var increased_angle_distance = distance(dest_point, move(curr_point, increased_angle));

		// Score the considered angles
		var decreased_points_score = 0;
		var increased_points_score = 0;

		// give weights to each factor
		var angle_weight = 1;
		var distance_weight = 2;
		var ratio_weight = 2;
		var notforbidden_weight = 2;
		

		// Assign points for forbidden/possible ratio
		if (angle_ratio_for_decreased < angle_ratio_for_increased) { decreased_points_score = decreased_points_score+ratio_weight;}
		if (angle_ratio_for_decreased > angle_ratio_for_increased) { increased_points_score = increased_points_score+ratio_weight;}
		// If the ratio is similar, increase the weight of the distance factor 
		if (Math.abs(angle_ratio_for_decreased - angle_ratio_for_increased)<0.1) {distance_weight++;}
			
		// Assign points for the next default move being allowed
		if (!next_move_for_decreased_forbidden) {decreased_points_score = decreased_points_score+notforbidden_weight;}
		if (!next_move_for_greater_forbidden) {increased_points_score = increased_points_score+notforbidden_weight;}
		
		// Assign points for being similar to the closest angle
		if (decreased_angle_comparison < increased_angle_comparison) { decreased_points_score = decreased_points_score+angle_weight;;}
		if (decreased_angle_comparison > increased_angle_comparison) { increased_points_score = increased_points_score+angle_weight;}

		// Assign points for resulting in being closer to the goal
		if (decreased_angle_distance < increased_angle_distance) { decreased_points_score = decreased_points_score+distance_weight;}
		if (decreased_angle_distance > increased_angle_distance) { increased_points_score = increased_points_score+distance_weight;}
		
		// Choose the angle having more points
		if (decreased_points_score > increased_points_score) {
			return decreased_angle;
		} else {
			return increased_angle;
		}
	}
	
	/**
	 * Find first not forbidden angle by adjusting it by the step size
	 * 
	 * @param angle the angle to adjust
	 * @param step either +10 or -10
	 * @param curr_point current location
	 * @param tried_angles list of angles already discarded
	 * @return first allowed angle 
	 */
	private Integer adjustAngle(int angle, int step, Point curr_point, ArrayList<Integer> tried_angles) {
		var adjusted_angle = angle;
		while(tried_angles.size() < 36) {
			adjusted_angle = angle360(adjusted_angle + step);
			if (!tried_angles.contains(adjusted_angle)) {
				tried_angles.add(adjusted_angle);
				if (!forbidden(curr_point, adjusted_angle)) {
					return adjusted_angle;
				}
			}
		}
		return null;
	}
	
	/**
	 * Choose a multiple of 10 angle closest to represent the straight line from pt1 to pt2
	 * 
	 * @param pt1 starting point
	 * @param pt2 end point
	 * @return Returns the closest angle being multiple of 10
	 */
	private Integer closestAngle(Point pt1, Point pt2) {
		// get the exact angle
		var angle = angle(pt1, pt2);
		// round up to an angle that's a multiple of 10
		var approx_angle = (int) (10 * (Math.round(angle / 10)));
		// convert positive between 0 and 360
		approx_angle = angle360(approx_angle);

		return approx_angle;
	}

	/**
	 * Converts angles to a value in [0, 360) degrees
	 * 
	 * @param angle is an angle in degrees
	 * @return returns an angle that is in [0, 360) degrees
	 */
	private Integer angle360(Integer angle) {
		return (((angle % 360) + 360) % 360);
	}
	
	/**
	 * Compute the exact angle between two points on a map with 0 = west, 90 = north
	 * 
	 * @param pt1 first point
	 * @param pt2 second point
	 * @return angle in degrees
	 */
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
	 * Calculates the ratio of forbidden moves out of the possible 36 angles: [forbidden moves]/[36 possible directions] 
	 * 
	 * @param pt the point at which to calculate the ratio
	 * @return returns a ratio of forbidden directions to the number of all possible directions
	 */
	private Double ratioOfForbidden(Point pt) {
		var forbidden_count = 0.;
		for (var angle = 0; angle<360; angle=angle+10) {
			if (forbidden(pt, angle360(angle))) {
				forbidden_count++;
			}
		}
		var ratio = forbidden_count/36.;
		return ratio;
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
		// initialise JTS stuff
		var gf = new GeometryFactory();
		var csf = gf.getCoordinateSequenceFactory();
		
		// Represent the MOVE
		var start_jts_coor = new Coordinate(from.longitude(), from.latitude());
		var end_pt = move(from, angle);
		var end_jts_coor = new Coordinate(end_pt.longitude(), end_pt.latitude());
		Coordinate[] move_coordinate_sequence = { start_jts_coor, end_jts_coor };
		var jts_move_coordinate_sequence = csf.create(move_coordinate_sequence);
		// construct a JTS LineString to represent the move
		var move_line = gf.createLineString(jts_move_coordinate_sequence);

		// Represent the CONFINEMENT AREA
		var nw_pt = new Coordinate(-3.192473, 55.946233);
		var sw_pt = new Coordinate(-3.192473, 55.942617);
		var se_pt = new Coordinate(-3.184319, 55.942617);
		var ne_pt = new Coordinate(-3.184319, 55.946233);
		Coordinate[] campus_coordinate_sequence = { nw_pt, sw_pt, se_pt, ne_pt, nw_pt };
		var jts_campus_coordinate_sequence = csf.create(campus_coordinate_sequence);
		// construct the confinement area JTS LinearRing
		var confinement_area = gf.createLinearRing(jts_campus_coordinate_sequence);
		
		// CHECKS
		// check for intersection with the confinement area
		if (move_line.intersects(confinement_area)) {
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
		// set up ArrayLists 
		var points_given = new ArrayList<SensorLocation>(sensors);
		var ordered_points = new ArrayList<SensorLocation>();
		// fix the number of sensors to order
		var no_points = points_given.size();

		// choose the first sensor to go to
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
	
}
