package uk.ac.ed.inf.aqmaps;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.mapbox.geojson.Point;

/**
 * Imitates a drone
 * 
 * @author Michal Sadowski
 *
 */
public class Drone {

	private Point init_loc;
	private ArrayList<LogEntry> log = new ArrayList<LogEntry>();
	private HashMap<String, Point> sensors_hash;
	private Server srv;
	private ArrayList<Reading> readings = new ArrayList<Reading>(); 

	/**
	 * @param init_loc starting location
	 * @param sensors_hash a map of w3w names of the sensors to their point locations
	 * @param srv server to get readings from
	 */
	public Drone(Point init_loc, HashMap<String, Point> sensors_hash, Server srv) {
		this.init_loc = init_loc;
		this.sensors_hash = sensors_hash;
		this.srv = srv;
	}

	/**
	 * Saves the log of the flight to a file
	 * 
	 * @param outfile name of the file to save to
	 */
	public void export(String outfile) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outfile));
			for (var l : log) {
				var str = l.getNo() + "," + l.getLoc_before().longitude() + "," + l.getLoc_before().latitude() + ","
						+ l.getAngle() + "," + l.getLoc_after().longitude() + "," + l.getLoc_after().latitude() + ","+l.getSensor();
				writer.write(str);
				writer.newLine();
			}
			writer.close();
			System.out.println("Successfully saved " + outfile);
		} catch (IOException e) {
			System.out.println("An error occurred:");
			e.printStackTrace();
		}
	}

	/**
	 * Performs a flight by following the specified path and saves readings from the sensors.
	 * 
	 * @param path a list of instructions for the flight specifying where to move and when to take readings 
	 */
	public void fly(ArrayList<PathStep> path) {
		var curr_loc = init_loc;
		// follow the provided path
		for (var i = 0; i < path.size(); i++) {
			// move
			var path_step = path.get(i);
			var moved_loc = Path.move(curr_loc, path_step.angle);
			
			// take a reading (or not)
			var sensor_to_read = path_step.sensor_to_read;
			// check if this path step recommends connecting to the sensor
			if (sensor_to_read != "null" && sensor_to_read != null && sensor_to_read != "") {
				// if in range, save the reading
				if (inRangeOf(moved_loc, sensors_hash.get(sensor_to_read))){
					readings.add(read(sensor_to_read));
				}
			}
			// report to the log
			var curr_log = new LogEntry(i+1, curr_loc, moved_loc, path_step.angle, sensor_to_read);
			log.add(curr_log);

			// update the location for next move
			curr_loc = moved_loc;
		}
	}

	/**
	 * Creates a list of points where the drone has been
	 * 
	 * @return list of visited points
	 */
	public ArrayList<Point> getPathMap() {
		var path_map = new ArrayList<Point>();
		// add initial location
		path_map.add(log.get(0).getLoc_before());

		// add all other points
		for (var e : log) {
			path_map.add(e.getLoc_after());
		}

		return path_map;
	}
	
	/**
	 * Getter for readings
	 * 
	 * @return Returns readings taken during the flight
	 */
	public ArrayList<Reading> getReadings(){
		return readings;
	}
	
	/**
	 * Returns reading data from the specified sensor
	 * 
	 * @param sensor_w3w the location/name of the sensor to read from 
	 * @return returns data read
	 */
	private Reading read(String sensor_w3w) {
		var reading = srv.getReading(sensor_w3w);
		return reading;
	}
	
	/**
	 * Checks whether a point is in range of the specified sensor
	 * 
	 * @param loc the point to check
	 * @param sensor sensor to consider
	 * @return Returns true if in range, false otherwise
	 */
	private boolean inRangeOf(Point loc, Point sensor) {	
            if (Path.distance(loc, sensor) < Path.sensor_range) {
            	return true;
            } else {
            	return false;
        }
	}
	
}
