package grape.app.GrapeUser;

import httpServer.booter;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		booter booter = new booter();
		try {
			System.out.println("GrapeUser!");
			System.setProperty("AppName", "GrapeUser");
			booter.start(1002);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
