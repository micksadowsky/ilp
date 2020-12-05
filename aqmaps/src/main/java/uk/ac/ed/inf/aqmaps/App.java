package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

import java.util.ArrayList;


public class App 
{
	public int port = 80;
	
    /**
     * @param args
     */
    /**
     * @param args
     */
    public static void main( String[] args )
    {
    	if (args.length != 7) {
    		System.err.println("Wrong number of arguments given. Usage:");
    		System.err.println("java -jar heatmap.jar [DD] [MM] [YYYY] [start longitude] [start latitude] [seed] [port]");
    	} else {
    		// Parse arguments
    		var DD = args[0];
    		var MM = args[1];
    		var YYYY = args[2];
    		String[] date = {DD, MM, YYYY};
    		
    		var start_lat = Double.parseDouble(args[3]);
    		var start_lon = Double.parseDouble(args[4]);
    		var start_loc = Point.fromLngLat(start_lon, start_lat); 
    		
    		var seed = Integer.parseInt(args[5]);
    		var port = Integer.parseInt(args[6]);
    		 		
    		System.out.println("Successfully parsed arguments");
    		
    		// Get path parameters
    		var helper = new Helpers(port, date);
    		var sensors_hash = helper.getSensorsLocations();
    		var sensors_point_locs = new ArrayList<Point>(sensors_hash.values());
    		var no_fly_zones = helper.getNoFlyZones();
    		
    		// Construct a path
    		var path = new Path(sensors_point_locs, start_loc, no_fly_zones);
    		var ordered_directions = path.generatePath(seed);
    		
    		// Perform a flight
    		var drone = new Drone(start_loc, sensors_hash, helper);
    		drone.fly(ordered_directions);
    		var readings = drone.getReadings();
    		System.out.println(readings);
    		
    		// Save flight log
    		var log_filename = "flightpath-"+ DD + "-" + MM + "-" + YYYY + ".txt"; 
    		drone.exportLog(log_filename);	
    		
    		// Save map
    		var map = new AQMap(readings, drone.getPathMap(), helper);
    		var map_filename = "readings-"+ DD + "-" + MM + "-" + YYYY + ".geojson";
    		map.export(map_filename);
    		
    		System.out.println("Successfully finished execution.");
    		
    		
    	}
    	
    }
}
