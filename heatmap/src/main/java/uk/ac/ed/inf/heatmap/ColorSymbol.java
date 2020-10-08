package uk.ac.ed.inf.heatmap;
/**
 * This class deals with converting int predictions to colour and their codes
 * 
 * @author Michal Sadowski
 */
public class ColorSymbol {

	enum Color {
		GREEN,
		MEDIUMGREEN,
		LIGHTGREEN,
		LIMEGREEN,
		GOLD,
		ORANGE,
		REDORANGE,
		RED,
		BLACK,
		GREY;

		/**
		 * 
		 * @return marker symbol based on coursework specification
		 */
		String getMarkerSymbol() {
			switch(this) {
				case GREEN:
				case MEDIUMGREEN:
				case LIGHTGREEN:
				case LIMEGREEN:
					return "lighthouse";
				case GOLD:
				case ORANGE:
				case REDORANGE:
				case RED:
					return "danger";
				case BLACK:
					return "cross";
				case GREY:
					return "none";
	            default:
	                throw new IllegalArgumentException("Must be one of specified colours but was: " + this.toString());
			}
		}
		
		/**
		 * @return  RGB string of the colour
		 */
		String getColor() {
			switch(this) {
				case GREEN:
					return "#00ff00";
				case MEDIUMGREEN:
					return "#40ff00";
				case LIGHTGREEN:
					return "#80ff00";
				case LIMEGREEN:
					return "#c0ff00";
				case GOLD:
					return "#ffc000";
				case ORANGE:
					return "#ff8000";
				case REDORANGE:
					return "#ff4000";
				case RED:
					return "#ff0000";
				case BLACK:
					return "#000000";
				case GREY:
					return "#aaaaaa";
	            default:
	                throw new IllegalArgumentException("Must be one of specified colours but was: " + this.toString());
			}
		}
	}
	
	/**
	 * Converts a reading integer to an enum based on coursework specification
	 * 
	 * @param reading prediction or reading value
	 * @return one of available colour enums
	 */
	private static Color mapReading(int reading) {
		if (reading < 0 || reading >= 256) {
			throw new IllegalArgumentException("Reading must be between 0 and 255 but was: " + reading);
		} else {
			if (reading < 32) {
				return Color.GREEN;
			} else if (reading < 64) {
				return Color.MEDIUMGREEN;
			} else if (reading < 96) {
				return Color.LIGHTGREEN;
			} else if (reading < 128) {
				return Color.LIMEGREEN;
			} else if (reading < 160) {
				return Color.GOLD;
			} else if (reading < 192) {
				return Color.ORANGE;
			} else if (reading < 224) {
				return Color.REDORANGE;
			} else if (reading < 256) {
				return Color.RED;
			} else {
				return null;
			}
		}
		
	}
	
	/**
	 * Takes the reading and returns RGB colour string
	 * @param reading integer value 0 &lt x &lt 256
	 * @return rgb-string appropriate for a reading
	 */
	public static String readingToRgbColor(int reading) {
		return mapReading(reading).getColor();
	}

	/**
	 * Takes the reading and returns a marker symbol
	 * @param reading integer value 0 &lt x &lt 256
	 * @return marker symbol appropriate for a reading
	 */	public static String readingToMarkerSymbol(int reading) {
		return mapReading(reading).getMarkerSymbol();
	}
	
}
