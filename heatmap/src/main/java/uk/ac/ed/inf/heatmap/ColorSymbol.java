package uk.ac.ed.inf.heatmap;

public class ColorSymbol {

// Colours available
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

//		Returns Marker Symbol
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
		
//		Returns RGB string of the colour
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
	
	//	Takes the reading  and returns an appropriate enum 
	static Color mapReading(int reading) {
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
	
//	Takes the reading and returns RGB colour string
	static String readingToRgbColor(int reading) {
		return mapReading(reading).getColor();
	}

	//	Takes the reading and returns an appropriate marker symbol string
	static String readingToMarkerSymbol(int reading) {
		return mapReading(reading).getMarkerSymbol();
	}
	
	public static void main(String[] args) {
		System.out.println("2: " + readingToRgbColor(2));
		System.out.println("2: " + readingToMarkerSymbol(2));
		System.out.println("43: " + readingToRgbColor(43));
		System.out.println("43: " + readingToMarkerSymbol(43));
		System.out.println("129: " + readingToRgbColor(129));
		System.out.println("129: " + readingToMarkerSymbol(129));
		System.out.println("255: " + readingToRgbColor(255));
		System.out.println("255: " + readingToMarkerSymbol(255));
	}
	
}
