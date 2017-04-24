package testuser;

import interfaceApplication.user;

public class test {
	private static user user = new user();
	public static void main(String[] args) {
		String userInfo ="{\"id\":\"test111\",\"password\":\"123\",\"loginmode\":0}";
		System.out.println(user.UserLogin(userInfo));
//		String string = "{\"mobphone\":\"13515623654\"}";
//		System.out.println(user.UserEdit("58edd57d1a4769cbf529e0f7", string));
	}
}
