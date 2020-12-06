package uk.ac.ed.inf.aqmaps;

public class PathStep {
	public Integer angle;
	public String sensor_to_read;
	
	public PathStep(Integer angle, String sensor_to_read) {
		this.angle = angle;
		this.sensor_to_read = sensor_to_read;
	}
	
	public PathStep(Integer angle) {
		this.angle = angle;
	}
}
