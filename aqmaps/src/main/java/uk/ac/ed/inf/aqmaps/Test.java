package uk.ac.ed.inf.aqmaps;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Test {
//	public static void main(String[] args) {
//		var results = new ArrayList<ArrayList<String>>();
//		String [] coordinates = {"55.9444 -3.1878",  "55.9432 -3.1872", "55.9427 -3.1845", "55.9447 -3.1868", "55.9427 -3.1898"}; 
//		for (var coors : coordinates) {
//			var split_coors = coors.split(" ");
//			for (var year = 2020; year <= 2021; year++) {
//				for (var month = 1; month<=12; month++) {
//					for (var day = 1; day <=28; day++) {
//
//						String formatted_day;
//						if (day<10) {
//							formatted_day = "0" + day;
//						} else {
//							formatted_day = ""+day;
//						}
//						
//						String formatted_month;
//						if (month<10) {
//							formatted_month = "0" + month;
//						} else {
//							formatted_month = ""+month;
//						}
//						String formatted_year = ""+year;
//						String[] app_args = {formatted_day, formatted_month, formatted_year, split_coors[0], split_coors[1], "22", "80"};
//						System.out.println("Testing for "+coors+" "+formatted_day+" "+ formatted_month+" "+formatted_year);
//
//						var tmp_app = new App();
//						tmp_app.main(app_args);
//						var result = tmp_app.test_stuff;
//						results.add(result);
//						if (Integer.parseInt(result.get(2))<33) {System.out.println("FALIED!");}
//					}
//				}	
//			}
//		}	
//		try {
//			BufferedWriter writer = new BufferedWriter(new FileWriter("log-angle-final.csv"));
//			writer.write("START COORS, DATE, readings.size(), flightpath.size()");
//			writer.newLine();
//			for (var result : results) {
//				var str = result.get(0) + ", " + result.get(1) + ", " + result.get(2)+ ", " + result.get(3);
//				System.out.println(str);
//				writer.write(str);
//				writer.newLine();
//			}
//			writer.close();
//			System.out.println("Successfully saved test log");
//		} catch (IOException e) {
//			System.out.println("An error occurred:");
//			e.printStackTrace();
//		}}
	}

