package uk.ac.ed.inf.aqmaps;

import uk.ac.ed.inf.aqmaps.Reading;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
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


public class Server {

	private static final HttpClient client = HttpClient.newHttpClient();
	private int port;
	private String[] date;
	
	public Server(int port, String[] date) {
		this.port = port;
		this.date = date;
	}
	
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

	
	public Point w3wtoPoint(String w3w) {
		var words = w3w.split("\\.");
		var url_params = "/words/" + words[0] + "/" + words[1] + "/" + words[2] + "/details.json";
		var jsonString = makeARequest(url_params); 

		var point_long_lat = new Gson().fromJson(jsonString, Coordinates.class);
		
		return Point.fromLngLat(point_long_lat.coordinates.lng, point_long_lat.coordinates.lat);
	}
	
	public String makeARequest(String url_params) {
		
		// Compose the URL
		var url_string = "http://localhost:" + port + url_params;		
		// Create a request
		
		HttpRequest request = null;
		try {
			request = HttpRequest.newBuilder()
					.uri(URI.create(url_string))
					.build();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
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
			System.out.println(jts_polygon);
		}
		return jts_polygons_list;
	}
	
	public FeatureCollection getNoFlyZones() {
		// Download the file
		var url_params = "/buildings/no-fly-zones.geojson";
		var jsonString = makeARequest(url_params);
		// Convert to a feature collection and return
		return FeatureCollection.fromJson(jsonString);
	}	
}



