package uk.ac.ed.inf.heatmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

/**
 * This class is responsible for main operations on GeoJSON. 
 *
 * @author Michal Sadowski
 *
 */
public class Heatmap {
	// VARIABLES
	int[][] predictions;

	Polygon[][] grid;
	
	/**
	 * Default constructor with Edinburgh University's Main Campus coordinates
	 * 
	 * @param file name of file with predictions. It must be 10 predictions per each of 10 lines.
	 */
	public Heatmap(String file) {
		this.predictions = predictionsFromFile(file);
		
		Point north_west = Point.fromLngLat(-3.192473, 55.946233);
		Point south_west = Point.fromLngLat(-3.192473, 55.942617);
		Point south_east = Point.fromLngLat(-3.184319, 55.942617);
		Point north_east = Point.fromLngLat(-3.184319, 55.946233);
		Point[] grid_location = { north_west, south_west, south_east, north_east };
		this.grid = createGrid(grid_location, 10);
	}
	
	/**
	 * Can be used to specify other coordinates from the defaults
	 * 
	 * @param file name of file with predictions. It must be 10 predictions per each of 10 lines.
	 * @param grid_location list of points starting with northwest most, in anti-clockwise order
	 */
	public Heatmap(String file, Point[] grid_location) {
		this.predictions = predictionsFromFile(file);
		this.grid = createGrid(grid_location, 10);
	}
	
	// METHODS
	/**
	 * Combines each rectangle with a corresponding prediction by calling createFeature.
	 * Then saves the resulting FeatureCollection into a GeoJSON file.
	 *  
	 * @param outfile name of the file to save
	 */
	public void createHeatmap(String outfile) {
		// Create the feature collection
		var features = new ArrayList<Feature>();
		for (var i = 0; i < 10; i++) {
			for (var j = 0; j < 10; j++) {
				features.add(createFeature(grid[i][j], predictions[i][j]));
			}
		}
		var heatmap = FeatureCollection.fromFeatures(features);

		// Save to file
		try {
			FileWriter myWriter = new FileWriter(outfile);
			myWriter.write(heatmap.toJson());
			myWriter.close();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Create a GeoJSON Feature with the rectangle Polygon and properties: fill-opacity, rgb-string, and fill
	 * 
	 * @param rectangle a GeoJSON Polygon rectangle to consider
	 * @param prediction integer value 
	 * @return a GeoJSON feature
	 */
	private Feature createFeature(Polygon rectangle, int prediction) {
		var rectangleFeature = Feature.fromGeometry(rectangle);
		rectangleFeature.addNumberProperty("fill-opacity", 0.75);
		rectangleFeature.addStringProperty("rgb-string", ColorSymbol.readingToRgbColor(prediction));
		rectangleFeature.addStringProperty("fill", ColorSymbol.readingToRgbColor(prediction));
		return rectangleFeature;
	}


	/**
	 * This method generates a grid of rectangles based on the coordinates given.
	 * 
	 * @param grid_location must be counterclockwise starting from most northwest
	 * @param size determines the amount of rectangles the grid will be split into
	 * @return grid of size size.size inside grid_location
	 */
	private Polygon[][] createGrid(Point[] grid_location, int size) {
		var grid = new Polygon[size][size];

		// Calculate dimensions of each rectangle in the grid in degrees
		double long_step = (grid_location[3].longitude() - grid_location[0].longitude()) / size;
		double lat_step = (grid_location[1].latitude() - grid_location[0].latitude()) / size;

		// rows first
		for (var i = 0; i < size; i++) {
			// then columns
			for (var j = 0; j < size; j++) {
				// put a Polygon rectangle onto the grid
				var base_lat = grid_location[0].latitude() + i * lat_step;
				var base_long = grid_location[0].longitude() + j * long_step;

				grid[i][j] = createRectangle(base_long, base_lat, long_step, lat_step);
			}
		}
		return grid;
	}

	/**
	 * Creates a single rectangle polygon
	 * 
	 * @param base_long Lognitude of the most northwest point
	 * @param base_lat Latitude of the most northwest point
	 * @param long_step The "width" of the rectangle
	 * @param lat_step The "height" of the rectangle
	 * @return a rectangle to be placed on the map
	 */
	private Polygon createRectangle(double base_long, double base_lat, double long_step, double lat_step) {
		// Create the list of coordinates for the rectangle
		var rectangle_points = new ArrayList<Point>();
		// north west point
		rectangle_points.add(Point.fromLngLat(base_long, base_lat));
		// south west point
		rectangle_points.add(Point.fromLngLat((base_long), (base_lat + lat_step)));
		// south east point
		rectangle_points.add(Point.fromLngLat((base_long + long_step), (base_lat + lat_step)));
		// north east point
		rectangle_points.add(Point.fromLngLat((base_long + long_step), (base_lat)));
		// Completing LineString
		rectangle_points.add(Point.fromLngLat(base_long, base_lat));

		// Create the list of lists of coordinates of the rectangle
		var polygon_list = new ArrayList<List<Point>>();
		polygon_list.add(rectangle_points);

		// Create the polygon
		var rectangle = Polygon.fromLngLats(polygon_list);

		return rectangle;
	}

	/**
	 * Reads a file with comma separated predictions and returns an array of arrays. This must be 10x10
	 * 
	 * @param file name of the file to be read
	 * @return 10x10 array of arrays of integers, which are predictions
	 */
	private int[][] predictionsFromFile(String file) {
		try {
			// Open file
			File predFile = new File(file);
			Scanner myReader = new Scanner(predFile);

			// Initialise array
			var predictions = new int[10][10];

			// Read through the file
			var rowIndex = 0;
			while (myReader.hasNextLine()) {
				String row = myReader.nextLine();

				// Split the row of predictions and put into an array
				String[] strArray = row.split(",");
				int[] intArray = new int[strArray.length];
				for (int i = 0; i < strArray.length; i++) {
					intArray[i] = Integer.parseInt(strArray[i].strip());
				}

				// Save the row of predictions and move to the next one
				predictions[rowIndex] = intArray;
				rowIndex++;
			}
			myReader.close();

			return predictions;

		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
			return null;
		}
	}
}
