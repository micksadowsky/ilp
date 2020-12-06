package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

public class SensorLocation {
	String location;
	Point point;
	
	public SensorLocation(String w3w, Point point) {
		this.location = w3w;
		this.point  = point;
	}
}
