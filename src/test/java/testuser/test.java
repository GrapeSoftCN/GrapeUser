package testuser;

import httpServer.booter;
import interfaceApplication.user;

public class test {
	public static void main(String[] args) {
//		user user = new user();
//		String userInfo ="{\"id\":\"test222\",\"password\":\"123\",\"loginmode\":0}";
//		System.out.println(user.UserLogin(userInfo));
//		String string = "{\"mobphone\":\"13515623654\"}";
//		System.out.println(user.UserEdit("58edd57d1a4769cbf529e0f7", string));
		booter booter = new booter();
		try {
			System.out.println("GrapeUser!");
			System.setProperty("AppName", "GrapeUser");
			booter.start(1003);
		} catch (Exception e) {
			// TODO: handle exception
		}
//		System.out.println(new user().UserPage(1, 2));
	}
}
