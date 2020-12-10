package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

public class LogEntry {
	private Integer no;
	private Point loc_before;
	private Point loc_after;
	private Integer angle;
	private String sensor;
	
	
	public LogEntry(Integer no, Point loc_before, Point loc_after, Integer angle, String sensor) {
		this.no = no;
		this.loc_before = loc_before;
		this.loc_after = loc_after;
		this.angle = angle;
		this.sensor = sensor;
	}

	public String getSensor() {
		return sensor;
	}

	public Integer getNo() {
		return no;
	}

	public Point getLoc_before() {
		return loc_before;
	}

	public Point getLoc_after() {
		return loc_after;
	}

	public Integer getAngle() {
		return angle;
	}
}
