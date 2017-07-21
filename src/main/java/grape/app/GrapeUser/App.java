package grape.app.GrapeUser;

import cache.CacheHelper;
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
			booter.start(1008);
		} catch (Exception e) {
			// TODO: handle exception
		}
		
//		CacheHelper helper = new CacheHelper("redis");
//		System.out.println(helper.setget("test", "testetser",100));
//		System.out.println(helper.get("test"));
	}
}
