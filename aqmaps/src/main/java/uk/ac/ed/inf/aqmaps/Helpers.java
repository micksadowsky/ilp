package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.FeatureCollection;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import java.io.IOException;
import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;


public class Helpers {

	private static final HttpClient client = HttpClient.newHttpClient();
	private int port;
	
	public Helpers(int port) {
		this.port = port;
	}
			

	public FeatureCollection getNoFlyZones() {
		
		// Download the file
		var url_params = "/buildings/no-fly-zones.geojson";
		var jsonString = makeARequest(url_params);
		// Convert to a feature collection and return
		return FeatureCollection.fromJson(jsonString) ;
	}
	
	public ArrayList<Point> getSensorsLocations(String[] date) {
		
		var sensors_locations = new ArrayList<Point>();		
		
		// Download data from the server
		var url_params = "/maps/" + date[2] + "/" + date[1] + "/" + date[0] + "/air-quality-data.json";
		var jsonString = makeARequest(url_params); 
		Type listSensorLocationType = new TypeToken<ArrayList<SensorLocation>>() {}.getType();
		ArrayList<SensorLocation> sensorLocationList = new Gson().fromJson(jsonString, listSensorLocationType);
		
		for (var sensorLocation : sensorLocationList) {
			sensors_locations.add(
					w3wtoPoint(sensorLocation.location)
					);
		}

		return sensors_locations;
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
