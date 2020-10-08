package uk.ac.ed.inf.heatmap;
/**
 * Entry point for the application
 * 
 * @author Michal Sadowski
 *
 */
public class App 
{
	/**
	 * 
	 * @param args takes filename where predictions are stored
	 */
    public static void main( String[] args )
    {
    	if (args.length != 1) {
    		System.err.println("No arguments given. Usage:");
    		System.err.println("java -jar heatmap.jar [filename]");
    	} else {
    		var filename = args[0];
    		var heatmap = new Heatmap(filename);
    		heatmap.createHeatmap("heatmap.geojson");
    		System.out.println("Successfully generated a heatmap!");
    	}
    	
    }
}
