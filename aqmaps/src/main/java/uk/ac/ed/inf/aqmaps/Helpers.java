package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.FeatureCollection;
import com.google.gson.Gson;

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


public class Helpers {

	private static final HttpClient client = HttpClient.newHttpClient();
	private int port;
	private HashMap<String, Point> sensors_w3w = new HashMap<String, Point>();
	private String[] date;
	
	public Helpers(int port, String[] date) {
		this.port = port;
		this.date = date;
	}
	
	public Reading getReading(String from_sensor) {
		// Download data from the server
		var url_params = "/maps/" + date[2] + "/" + date[1] + "/" + date[0] + "/air-quality-data.json";
		var jsonString = makeARequest(url_params); 
		Type listReadingType = new TypeToken<ArrayList<Reading>>() {}.getType();
		ArrayList<Reading> sensorReadingList = new Gson().fromJson(jsonString, listReadingType);
		System.out.println("from_sensor = " + from_sensor);
		for (var data : sensorReadingList) {
			System.out.println("data.location = " + data.location);
			if (from_sensor.equals(data.location)) {
				return data;
			}
		}
		return null;
	}
	
	public FeatureCollection getNoFlyZones() {
		
		// Download the file
		var url_params = "/buildings/no-fly-zones.geojson";
		var jsonString = makeARequest(url_params);
		// Convert to a feature collection and return
		return FeatureCollection.fromJson(jsonString);
	}
	
	public Point pointFromW3W (String w3w) {
		return sensors_w3w.get(w3w);
	}
	
	
	public HashMap<String, Point> getSensorsLocations() {
		// Download data from the server
		var url_params = "/maps/" + date[2] + "/" + date[1] + "/" + date[0] + "/air-quality-data.json";
		var jsonString = makeARequest(url_params); 
		Type listSensorLocationType = new TypeToken<ArrayList<SensorLocation>>() {}.getType();
		ArrayList<SensorLocation> sensorW3WLocationList = new Gson().fromJson(jsonString, listSensorLocationType);
		
		// Populate the hash map
		for (var sensorW3WLocation : sensorW3WLocationList) {
			
			var w3wloc = sensorW3WLocation.location;
			var degloc = w3wtoPoint(w3wloc);
			sensors_w3w.put(w3wloc, degloc);
		}

		return sensors_w3w;
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
	}
