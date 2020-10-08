package uk.ac.ed.inf.heatmap;

import java.util.ArrayList;
import java.util.List;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class Heatmap {

	int[][] predictions;
	Polygon[][] grid;

	//	Constructor
	public Heatmap(Point[] grid_location, String file) {
		this.predictions = predictionsFromFile(file);
		this.grid = createGrid(grid_location, 10);
	}

	
	// Reads a file with comma separated predictions and returns an array of arrays
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

	// Creates a grid of size sizeXsize inside grid_location 
	// Must be counterclockwise starting most northwest
	private Polygon[][] createGrid(Point[] grid_location, int size) {
		var grid = new Polygon[size][size];
		
		// Calculate dimensions of each rectangle in the grid in degrees
		double long_step = (grid_location[3].longitude() - grid_location[0].longitude())/size;
		double lat_step = (grid_location[1].latitude() - grid_location[0].latitude())/size;
		
		// rows first		
		for (var i=0; i<size; i++) {
			// then columns			
			for (var j=0; j<size; j++) {
				// put a Polygon rectangle onto the grid
				var base_lat = grid_location[0].latitude() + i*lat_step;
				var base_long = grid_location[0].longitude() + j*long_step;
				
				System.out.println("createGrid: i="+i+", j="+j);
				System.out.println("createGrid: base_long = "+base_long);
				System.out.println("createGrid: base_lat = "+base_lat);
				
				grid[i][j] = createRectangle(base_long, base_lat, long_step, lat_step);
			}
		}
		return grid;
	}
	
	// Create single rectangle polygon
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
	
	
	public void createHeatmap(String outfile) {
		var features = new ArrayList<Feature>();
		for (var i=0; i<10; i++) {
			for (var j=0; j<10; j++) {
//				System.out.println("createHeatmap: i="+i+", j="+j);
//				System.out.println("createHeatmap: grid[i][j]:");
//				System.out.println(grid[i][j]);
//				System.out.println("createHeatmap: predictions[i][j]:");
//				System.out.println(predictions[i][j]);

				features.add(createFeature(grid[i][j], predictions[i][j]));
			}
		}
		var heatmap = FeatureCollection.fromFeatures(features);
		
		try {
		      FileWriter myWriter = new FileWriter(outfile);
		      var json = heatmap.toJson();
		      System.out.println(json);
		      myWriter.write(json);
		      myWriter.close();
		      System.out.println("Successfully wrote to the file.");
		    } catch (IOException e) {
		      System.out.println("An error occurred.");
		      e.printStackTrace();
		    }
	}
	
	// Create Feature with polygon and properties
	public Feature createFeature(Polygon rectangle, int prediction) {
		var rectangleFeature = Feature.fromGeometry(rectangle);
		rectangleFeature.addNumberProperty("fill-opacity", 0.75);
		rectangleFeature.addStringProperty("rgb-string", ColorSymbol.readingToRgbColor(prediction));
		rectangleFeature.addStringProperty("fill", ColorSymbol.readingToRgbColor(prediction));
		return rectangleFeature;
	}
	

	public static void main(String[] args) {
		Point north_west = Point.fromLngLat(-3.192473, 55.946233);
		Point south_west = Point.fromLngLat(-3.192473, 55.942617);
		Point south_east = Point.fromLngLat(-3.184319, 55.942617);	
		Point north_east = Point.fromLngLat(-3.184319, 55.946233);
		Point[] grid_location = {north_west, south_west, south_east, north_east};
		
		var test_heatmap_1 = new Heatmap(grid_location, "predictions.txt");
		test_heatmap_1.createHeatmap("output.geojson");
				
	}
}
