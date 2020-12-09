package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import com.mapbox.geojson.Point;

/**
 * This is the entry point for the application.
 * 
 * @author Michal Sadowski
 *
 */
public class App {
	public int port = 80;
	// TODO delete after testing is done
	public  ArrayList<String> test_stuff = new ArrayList<String>();

	/**
	 * @param args should be seven arguments: day, month, year, start longitude,
	 *             start latitude, seed, port in format: [DD] [MM] [YYYY] [start
	 *             longitude] [start latitude] [seed] [port]
	 */
	public  void main(String[] args) {
		// Check for validity of command line arguments
		if (args.length != 7) {
			System.err.println("Wrong number of arguments given. Usage:");
			System.err
					.println("java -jar heatmap.jar [DD] [MM] [YYYY] [start longitude] [start latitude] [seed] [port]");
		} else {
			// Parse arguments
			var DD = args[0];
			var MM = args[1];
			var YYYY = args[2];
			String[] date = { DD, MM, YYYY };

			var start_lat = Double.parseDouble(args[3]);
			var start_lon = Double.parseDouble(args[4]);
			var start_loc = Point.fromLngLat(start_lon, start_lat);

			var seed = Long.parseLong(args[5]);
			var port = Integer.parseInt(args[6]);

			System.out.println("Successfully parsed arguments");

			// Get path parameters
			var srv = new Server(port, date);
			var sensors = srv.getSensorsLocations();
			var sensors_loc_hash = srv.getHashMap();
			var jts_no_fly_zones = srv.getJTSNoFlyZones();

			// Construct a path
			var path = new Path(sensors, start_loc, jts_no_fly_zones, seed);
			var flightpath = path.generatePath();

			// Perform a flight
			var drone = new Drone(start_loc, sensors_loc_hash, srv);
			drone.fly(flightpath);

			// Save readings taken during the flight
			var readings = drone.getReadings();
//			System.out.println("readings.size() = " + readings.size());

			// Save flight log
			var log_filename = "tests/flight-log/flightpath-" + DD + "-" + MM + "-" + YYYY + ".txt";
			drone.exportLog(log_filename);

			// Save the flight as a GeoJSON map
			var map = new AQMap(readings, drone.getPathMap(), sensors_loc_hash);
			var map_filename = "tests/maps/readings-" + DD + "-" + MM + "-" + YYYY + ".geojson";
			map.export(map_filename);

			// TODO delete after testing is done
//			System.out.println("Successfully finished execution.");
			test_stuff.add(start_lon + ", " + start_lat);
			test_stuff.add(DD + "-" + MM + "-" + YYYY);
			test_stuff.add("" + readings.size());
			test_stuff.add("" + flightpath.size());
		}
	}
}
