package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

import java.util.ArrayList;


/**
 * Entry point for the Air Quality Map application
 * 
 * @author Michal Sadowski
 *
 */
public class App 
{
	public int port = 80;
	/**
	 * 
	 * @param args takes the following arguments:
	 * DD MM YYYY longitude latitude seed port 
	 */
    public static void main( String[] args )
    {
    	if (args.length != 7) {
    		System.err.println("Wrong number of arguments given. Usage:");
    		System.err.println("java -jar heatmap.jar [DD] [MM] [YYYY] [start longitude] [start latitude] [seed] [port]");
    		
    		
    	} else {
    		// date    		
    		var DD = args[0];
    		var MM = args[1];
    		var YYYY = args[2];
    		String[] date = {DD, MM, YYYY};
    		
    		// starting position
    		var start_lat = Double.parseDouble(args[3]);
    		var start_lon = Double.parseDouble(args[4]);
    		var start_loc = Point.fromLngLat(start_lon, start_lat); 
    		
    		// additional values
    		var seed = Integer.parseInt(args[5]);
    		var port = Integer.parseInt(args[6]);
    		 		
    		
    		System.out.println("Successfully read arguments");
    		
    		var helper = new Helpers(port);
    		var sensors = helper.getSensorsLocations(date);

    		var no_fly_zones = helper.getNoFlyZones();
    		
    		var path = new Path(sensors, start_loc, no_fly_zones);
    		var ordered_path = path.generatePath(seed);
    		
//    		var drone = new Drone();
//    		drone.fly(route);
//    		var readings = drone.getReadings();
//    		
    		var map = new AQMap(ordered_path);
    		map.export("test_map.geojson");
    		
    		
    		
    		
    		
    		
    		
    		
    		
    	}
    	
    }
}
