package uk.ac.ed.inf.aqmaps;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.mapbox.geojson.Point;

public class Drone {

	private Point init_loc;
	private ArrayList<LogEntry> log = new ArrayList<LogEntry>();
	private HashMap<String, Point> sensors_hash;
	private Helpers helper;
	private ArrayList<Reading> readings = new ArrayList<Reading>(); 


	public Drone(Point init_loc, HashMap<String, Point> sensors_hash, Helpers helper) {
		this.init_loc = init_loc;
		this.sensors_hash = sensors_hash;
		this.helper = helper;
	}

	public ArrayList<Reading> getReadings(){
		return readings;
	}
	
	public void fly(ArrayList<Integer> path) {
		var curr_loc = init_loc;
		for (var i = 0; i < path.size(); i++) {
			// move
			var angle = path.get(i);
			var moved_loc = Path.move(curr_loc, angle);
			// take a reading (or not)
			var nearby_sensor = sensorInRange(moved_loc);
			if (nearby_sensor != null) {
				System.out.println("nearby_sensor not null: " + nearby_sensor);
				readings.add(read(nearby_sensor));
			}
			var curr_log = new LogEntry(i+1, curr_loc, moved_loc, angle, nearby_sensor);
//			System.out.println("curr_log = " + curr_log);
			log.add(curr_log);
			curr_loc = moved_loc;
		}
	}

	public Reading read(String sensor_w3w) {
		var reading = helper.getReading(sensor_w3w);
		return reading;
	}
	
	public String sensorInRange(Point loc) {
        for (HashMap.Entry<String,Point> sen : sensors_hash.entrySet()) {	
            if (Path.distance(loc, sen.getValue()) < Path.sensor_range) {
            	return sen.getKey();
            } 
        }
		return null;
	}
	
	public void exportLog(String outfile) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outfile));
			for (var l : log) {
				var str = l.getNo() + "," + l.getLoc_before().longitude() + "," + l.getLoc_before().latitude() + ","
						+ l.getAngle() + "," + l.getLoc_after().longitude() + "," + l.getLoc_after().latitude() + ","+l.getSensor();
				System.out.println(str);
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

	public ArrayList<Point> getPathMap() {
		var path_map = new ArrayList<Point>();
		path_map.add(log.get(0).getLoc_before());

		for (var e : log) {
			path_map.add(e.getLoc_after());
		}

		return path_map;
	}
}
