package model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import esayhelper.DBHelper;
import esayhelper.JSONHelper;
import esayhelper.formHelper;
import esayhelper.formHelper.formdef;
import rpc.execRequest;
import esayhelper.jGrapeFW_Message;
import security.codec;
import session.session;

public class userModel {
	private static DBHelper users;
	private static formHelper _form;
	private String sessionvalue = null;
	// private HashMap<String, Object> defcol = new HashMap<>();
	static {
		users = new DBHelper("mongodb", "user");
		_form = users.getChecker();
	}

	public userModel() {
		_form.putRule("id", formdef.notNull);
		_form.putRule("password", formdef.notNull);
		_form.putRule("name", formdef.notNull);
		_form.putRule("registerip", formdef.notNull);
		_form.putRule("wbid", formdef.notNull);
	}

	@SuppressWarnings("unchecked")
	public int register(JSONObject _userInfo) {
		if (!_form.checkRuleEx(_userInfo)) {
			return 1; // 必填字段没有填
		}
		String userName = _userInfo.get("id").toString();
		if (!checkUserName(userName)) {
			return 2;// 用户名不合法
		}
		if (findUserNameByID(userName) != null) {
			return 3; // 用户名已存在
		}
		String email = _userInfo.get("email").toString();
		if (!checkEmail(email)) {
			return 4; // email格式不正确
		}
		if (findUserNameByEmail(email) != null) {
			return 5; // email已存在
		}
		String phoneno = _userInfo.get("mobphone").toString();
		if (!checkMobileNumber(phoneno)) {
			return 6; // 手机号格式错误
		}
		if (findUserNameByMoblie(phoneno) != null) {
			return 7; // 手机号已经被注册
		}
		// md5加密密码
		String secpassword = secPassword(_userInfo.get("password").toString());
		_userInfo.replace("password", secpassword);
		return users.data(_userInfo).insertOnce() != null ? 0 : 99;
	}

	public String checkLogin(JSONObject userinfo) {
		int loginMode = 0;
		String username = "";
		if (userinfo.containsKey("loginmode")) {
			loginMode = Integer.parseInt(userinfo.get("loginmode").toString());
		}
		if (loginMode == 0) {
			username = userinfo.get("id").toString();
			if (!checkUserName(username)) {
				return resultMessage(2, "");
			}
		}
		if (loginMode == 1) {
			username = userinfo.get("email").toString();
			if (!checkEmail(username)) {
				return resultMessage(4, "");
			}
		}
		if (loginMode == 2) {
			username = userinfo.get("mobphone").toString();
			if (!checkMobileNumber(username)) {
				return resultMessage(6, "");
			}
		}
		// JSONObject _obj = login(username,
		// userinfo.get("password").toString(),
		// loginMode);
		// if (_obj!=null) {
		// _obj.remove("password");
		// }
		return login(username, userinfo.get("password").toString(), loginMode);
	}

	private String login(String username, String password, int loginMode) {
		String _checkField = "";
		switch (loginMode) {
		case 0:
			_checkField = "id";
			break;
		case 1:
			_checkField = "email";
			break;
		case 2:
			_checkField = "mobphone";
			break;
		}
		password = codec.md5(password);
		JSONObject object = users.eq(_checkField, username).eq("password", password).find();
		if (object != null) {
			session session = new session();
			// if (sessionvalue != null) {
			// return resultMessage(8, "");
			// }
			//
			// sessionvalue = session.insertSession(username,
			// object.toString());
			// object.put("sid", sessionvalue);
			// object.remove("password");
			session.setget(username, object.toString());
		}
		return object != null ? object.toString() : null;
	}

	public void logout(String UserName) {
		session session = new session();
		// session.delete(sessionvalue);
		// System.out.println((String)session.get(UserName));
		// session.deleteSession(UserName);
		session.delete(UserName);
	}

	public long getpoint_username(String username) {
		long rl = 0;
		JSONObject rs = users.eq("id", username).field("point").find();
		if (rs != null) {
			rl = Long.parseLong(rs.get("point").toString());
		}
		return rl;
	}

	@SuppressWarnings("unchecked")
	public int changePW(String id, String oldPW, String newPW) {
		if (checkUser(id, oldPW)) {
			return 9;
		}
		JSONObject object = new JSONObject();
		object.put("password", codec.md5(newPW));
		object = users.eq("id", id).eq("password", codec.md5(oldPW)).data(object).update();
		return object != null ? 0 : 99;
	}

	public int edit(String _id, JSONObject userInfo) {
		if (userInfo.containsKey("id")) {
			if (!checkUserName(userInfo.get("id").toString())) {
				return 2;
			}
		}
		// if (!_form.checkRuleEx(userInfo)) {
		// return 1;
		// }
		if (userInfo.containsKey("email")) {
			if (!checkEmail(userInfo.get("email").toString())) {
				return 4;
			}
		}
		if (userInfo.containsKey("mobphone")) {
			if (!checkMobileNumber(userInfo.get("mobphone").toString())) {
				return 6;
			}
		}
		userInfo.remove("password");
		JSONObject object = users.eq("_id", new ObjectId(_id)).data(userInfo).update();
		return object != null ? 0 : 99;
	}

	public int Update(String id, String ownid, JSONObject object) {
		return users.eq("_id", new ObjectId(id)).eq("ownid", ownid).data(object).update() != null ? 0 : 99;
	}

	public JSONArray select() {
		return users.limit(20).select();
	}

	public JSONArray select(JSONObject userInfo) {
		for (Object object2 : userInfo.keySet()) {
			if (object2.equals("_id")) {
				users.eq("_id", new ObjectId(userInfo.get(object2).toString()));
			}
			users.eq(object2.toString(), userInfo.get(object2.toString()));
		}
		return users.limit(20).select();
	}

	/**
	 * 根据用户id查询用户信息
	 * 
	 * @param id
	 * @return
	 */
	public JSONObject select(String id) {
		return users.eq("id", id).find();
	}

	// public JSONObject find(String ownid) {
	// return users.eq("ownid", ownid).find();
	// }
	@SuppressWarnings("unchecked")
	public JSONObject page(int idx, int pageSize) {
		JSONArray array = users.page(idx, pageSize);
		@SuppressWarnings("unchecked")
		JSONObject object = new JSONObject();
		object.put("totalSize", (int) Math.ceil((double) users.count() / pageSize));
		object.put("currentPage", idx);
		object.put("pageSize", pageSize);
		object.put("data", array);
		return object;
	}

	@SuppressWarnings("unchecked")
	public JSONObject page(int idx, int pageSize, JSONObject userInfo) {
		for (Object object2 : userInfo.keySet()) {
			users.eq(object2.toString(), userInfo.get(object2.toString()));
		}
		JSONArray array = users.page(idx, pageSize);
		@SuppressWarnings("unchecked")
		JSONObject object = new JSONObject();
		object.put("totalSize", (int) Math.ceil((double) users.count() / pageSize));
		object.put("currentPage", idx);
		object.put("pageSize", pageSize);
		object.put("data", array);
		return object;
	}

	public int delect(String id) {
		JSONObject object = users.eq("_id", new ObjectId(id)).delete();
		return object != null ? 0 : 99;
	}

	public int delect(String[] arr) {
		users = (DBHelper) users.or();
		for (int i = 0; i < arr.length; i++) {
			users.eq("_id", arr[i]);
		}
		JSONObject object = users.delete();
		return object != null ? 0 : 99;
	}

	public JSONObject findUserNameByID(String userName) {
		return users.eq("id", userName).find();
	}

	public JSONObject findUserNameByEmail(String email) {
		return users.eq("email", email).find();
	}

	public JSONObject findUserNameByMoblie(String phoneno) {
		return users.eq("mobphone", phoneno).find();
	}

	public boolean checkUser(String id, String pw) {
		pw = secPassword(pw.toString());
		return users.eq("id", id).eq("password", pw).find() == null;
	}

	@SuppressWarnings("unchecked")
	public boolean checkEmail(String email) {
		_form.putRule("email", formdef.email);
		JSONObject _obj = new JSONObject() {
			private static final long serialVersionUID = 1L;
			{
				put("email", email);
			}
		};
		return _form.checkRule(_obj);
	}

	@SuppressWarnings("unchecked")
	public boolean checkMobileNumber(String mobile) {
		_form.putRule("mobphone", formdef.mobile);
		JSONObject _obj = new JSONObject() {
			private static final long serialVersionUID = 1L;
			{
				put("mobphone", mobile);
			}
		};
		return _form.checkRule(_obj);
	}

	public boolean checkUserName(String userName) {
		String regex = "([a-z]|[A-Z]|[0-9]|[\\u4e00-\\u9fa5])+";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(userName);
		return (userName.length() >= 7 && userName.length() <= 15) && m.matches();
	}

	/**
	 * 将map添加至JSONObject中
	 * 
	 * @param map
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONObject AddMap(HashMap<String, Object> map, JSONObject object) {
		if (map.entrySet() != null) {
			Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iterator.next();
				if (!object.containsKey(entry.getKey())) {
					object.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return object;
	}

	public String secPassword(String passwd) {
		return codec.md5(passwd);
	}

//	public String getUserPlv(String username) {
//		session session = new session();
//		// 从缓存中获取用户plv
//		String userinfo = session.get(username).toString();
//	}
	// 操作权限验证
	// public int CheckPlv() {
	// int code = 0;
	// session session = new session();
	// JSONObject object =
	// JSONHelper.string2json(session.get(sessionvalue).toString());
	// // JSONObject object = users.eq("id", Username).field("plv").find();
	// // String plv = object.get("plv").toString();
	// // 判断plv的值
	// int plv = Integer.parseInt(object.get("plv").toString());
	// if (plv >= 0 && plv < 1000) {
	// code = 1;
	// }
	// if (plv >= 1000 && plv < 2000) {
	// code = 2;
	// }
	// if (plv >= 2000 && plv < 3000) {
	// code = 3;
	// }
	// if (plv >= 30000) {
	// code = 4;
	// }
	// return code;
	// }

	public String resultMessage(int num, String message) {
		String msg = "";
		switch (num) {
		case 0:
			msg = message;
			break;
		case 1:
			msg = "必填字段为空";
			break;
		case 2:
			msg = "用户名格式错误";
			break;
		case 3:
			msg = "用户名已存在";
			break;
		case 4:
			msg = "email格式错误";
			break;
		case 5:
			msg = "email已存在";
			break;
		case 6:
			msg = "手机号格式错误";
			break;
		case 7:
			msg = "手机号已存在";
			break;
		case 8:
			msg = "该用户已登录";
			break;
		case 9:
			msg = "用户名或密码错误";
			break;
		case 10:
			msg = "没有操作权限";
			break;
		default:
			msg = "其他操作异常";
			break;
		}
		return jGrapeFW_Message.netMSG(num, msg);
	}
}
