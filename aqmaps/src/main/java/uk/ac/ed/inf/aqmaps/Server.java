package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.geojson.FeatureCollection;
import com.google.gson.Gson;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Coordinate;

import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

/**
 * It's responsible for getting data from the server
 * 
 * @author Michal Sadowski
 *
 */
public class Server {
	private static final HttpClient client = HttpClient.newHttpClient();
	private int port;
	private String[] date;
	
	/**
	 * @param port port at which the server is working
	 * @param date date for which to give readings
	 */
	public Server(int port, String[] date) {
		this.port = port;
		this.date = date;
	}
	
	/**
	 * Download reading data from the server for a specified sensor and data
	 * 
	 * @param from_sensor the w3w location of the server for which to return data
	 * @return returns a reading
	 */
	public Reading getReading(String from_sensor) {
		// Download data from the server
		var url_params = "/maps/" + date[2] + "/" + date[1] + "/" + date[0] + "/air-quality-data.json";
		var jsonString = makeARequest(url_params); 
		
		// put JSON data into list of custom datatype
		Type listReadingType = new TypeToken<ArrayList<Reading>>() {}.getType();
		ArrayList<Reading> sensorReadingList = new Gson().fromJson(jsonString, listReadingType);
		
		for (var data : sensorReadingList) {
			if (from_sensor.equals(data.location)) {
				return data;
			}
		}
		return null;
	}
	
	/**
	 * Downloads sensor locations
	 * @return list of SensorLocation type
	 */
	public ArrayList<SensorLocation> getSensorsLocations() {
		// Download data from the server
		var url_params = "/maps/" + date[2] + "/" + date[1] + "/" + date[0] + "/air-quality-data.json";
		var jsonString = makeARequest(url_params); 
		
		// put JSON data into list of custom data types
		Type listSensorLocationType = new TypeToken<ArrayList<SensorLocation>>() {}.getType();
		ArrayList<SensorLocation> sensor_locations = new Gson().fromJson(jsonString, listSensorLocationType);
		
		// fill in the Point field
		for (var sl : sensor_locations) {
			sl.point = w3wtoPoint(sl.location);
		}
		return sensor_locations;
	}	
	
	/**
	 * Creates a hash map in the form: [String: What3Words location] --> [MapBox Point: location on a map]
	 * @return a hashmap
	 */
	public HashMap<String, Point> getHashMap() {
		var hash_map = new HashMap<String, Point>();
		// Download data from the server
		var url_params = "/maps/" + date[2] + "/" + date[1] + "/" + date[0] + "/air-quality-data.json";
		var jsonString = makeARequest(url_params); 
		
		// copy JSON data into Java data type
		Type listSensorLocationType = new TypeToken<ArrayList<SensorLocation>>() {}.getType();
		ArrayList<SensorLocation> sensorW3WLocationList = new Gson().fromJson(jsonString, listSensorLocationType);
		
		// Populate the hash map
		for (var sensorW3WLocation : sensorW3WLocationList) {
			var w3wloc = sensorW3WLocation.location;
			var degloc = w3wtoPoint(w3wloc);
			hash_map.put(w3wloc, degloc);
		}
		return hash_map;
	}

	/**
	 * Downloads no fly zones from the server and converts the result to a type
	 * required by JTS
	 * 
	 * @return a list of no fly zone JTS polygons
	 */
	public ArrayList<org.locationtech.jts.geom.Polygon> getJTSNoFlyZones(){
		// get Mapbox style no-fly-zones
		var nfzs =  getNoFlyZones();
		
		//initialise JTS stuff
		var gf = new GeometryFactory();
		var csf = gf.getCoordinateSequenceFactory();
		
		// create a list of JTS Polygons
		var jts_polygons_list = new ArrayList<org.locationtech.jts.geom.Polygon>();
		
		// iterate through no fly zones
		for (var nfz : nfzs.features()) {
			// get each Mapbox polygon
			var pol = (Polygon) nfz.geometry();
			var pol_coors = pol.coordinates();
			
			// convert Mapbox coordinates to JTS coordinates
			var jts_coors_list = new ArrayList<Coordinate>();
			// double for loop due to Mapbox convention
			for (var pt_list : pol_coors) {
				for (var pt : pt_list) {
					var jts_coor = new Coordinate(pt.longitude(), pt.latitude());
					jts_coors_list.add(jts_coor);
				}
			}
			var jts_coordinate_sequence = csf.create(jts_coors_list.toArray(new Coordinate[0]));
			
			// create a JTS polygon and add to list
			var jts_polygon = gf.createPolygon(jts_coordinate_sequence);
			jts_polygons_list.add(jts_polygon);
		}
		return jts_polygons_list;
	}
	
	/**
	 * Downloads no fly zones from the server.
	 * 
	 * @return a feature collection from the no fly zones
	 */
	private FeatureCollection getNoFlyZones() {
		// Download the file
		var url_params = "/buildings/no-fly-zones.geojson";
		var jsonString = makeARequest(url_params);
		// Convert to a feature collection and return
		return FeatureCollection.fromJson(jsonString);
	}
	
	/**
	 * Converts a What3Words location to a MapBox point by requesting it from the server
	 * 
	 * @param w3w the What3Words location string
	 * @return Returns a point at centre of the W3W location
	 */
	private Point w3wtoPoint(String w3w) {
		var words = w3w.split("\\.");
		var url_params = "/words/" + words[0] + "/" + words[1] + "/" + words[2] + "/details.json";
		var jsonString = makeARequest(url_params); 

		// save the Coordinates field to a variable 
		var point_long_lat = new Gson().fromJson(jsonString, Coordinates.class);
		
		// create a point
		var point = Point.fromLngLat(point_long_lat.coordinates.lng, point_long_lat.coordinates.lat);
		
		return point;
	}
	
	/**
	 * Makes a request to a server at localhost and returns body of the response
	 * 
	 * @param url_params the parameters of the request
	 * @return body of the request
	 */
	private String makeARequest(String url_params) {
		// Compose the URL
		var url_string = "http://localhost:" + port + url_params;		
		
		// Create a request
		HttpRequest request = null;
		try {
			request = HttpRequest.newBuilder()
					.uri(URI.create(url_string))
					.build();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		// Send the request
		HttpResponse<String> response = null;
		try {
			response = client.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Deal with the response
		if (response.statusCode() != 200) {
			System.err.println("Received " + response.statusCode() + " from " + url_string);
			return null;
		} else {
			return response.body();			
			}
	}

}



