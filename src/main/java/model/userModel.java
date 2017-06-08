package model;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import apps.appsProxy;
import database.db;
import esayhelper.DBHelper;
import esayhelper.JSONHelper;
import esayhelper.formHelper;
import esayhelper.formHelper.formdef;
import nlogger.nlogger;
import rpc.execRequest;
import esayhelper.jGrapeFW_Message;
import security.codec;
import session.session;

public class userModel {
	private static DBHelper users;
	private static formHelper _form;
	private JSONObject _obj = new JSONObject();
	// private static String wbid;
	private static session session = new session();
	static {
		users = (DBHelper) new DBHelper(appsProxy.configValue().get("db").toString(), "userList");
		_form = users.getChecker();
	}

	private db bind() {
		return users.bind(String.valueOf(appsProxy.appid()));
	}

	public userModel() {
		_form.putRule("id", formdef.notNull);
		// _form.putRule("password", formdef.notNull);
		_form.putRule("name", formdef.notNull);
		// _form.putRule("registerip", formdef.notNull);
		// _form.putRule("wbid", formdef.notNull);
		// _form.putRule("IDCard", formdef.notNull);
	}

	@SuppressWarnings("unchecked")
	public int register(JSONObject _userInfo) {
		int code = 99;
		if (_userInfo != null) {
			try {
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
				if (_userInfo.containsKey("email")) {
					String email = _userInfo.get("email").toString();
					if (!checkEmail(email)) {
						return 4; // email格式不正确
					}
					if (findUserNameByEmail(email) != null) {
						return 5; // email已存在
					}
				}
				// 发送邮箱验证码,对邮箱进行验证
				// String message = execRequest
				// ._run("GrapeEmail/Email/ActiveEmail/s:1" + "/s:" + email,
				// null)
				// .toString();
				// long tip = (long)
				// JSONHelper.string2json(message).get("errorcode");
				// if (Integer.parseInt(String.valueOf(tip)) == 0) {
				// //邮箱发送成功，等待用户输入验证码,中断当前操作
				// interrupt._break("", "", "", null);
				// }
				if (_userInfo.containsKey("mobphone")) {
					String phoneno = _userInfo.get("mobphone").toString();
					if (!checkMobileNumber(phoneno)) {
						return 6; // 手机号格式错误
					}
					if (findUserNameByMoblie(phoneno) != null) {
						return 7; // 手机号已经被注册
					}
				}
				// md5加密密码
				String secpassword = secPassword(_userInfo.get("password").toString());
				_userInfo.replace("password", secpassword);
				Object object = bind().data(_userInfo).insertOnce();
				code = (object != null ? 0 : 99);
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	public String checkLogin(JSONObject userinfo) {
		int loginMode = 0;
		String username = "";
		String password = "";
		if (userinfo.containsKey("loginmode")) {
			loginMode = Integer.parseInt(userinfo.get("loginmode").toString());
		}
		switch (loginMode) {
		case 0:
			username = userinfo.get("id").toString();
			if (!checkUserName(username)) {
				return resultMessage(2, "");
			}
			password = userinfo.get("password").toString();
			break;
		case 1:
			username = userinfo.get("email").toString();
			if (!checkEmail(username)) {
				return resultMessage(4, "");
			}
			password = userinfo.get("password").toString();
			break;
		case 2:
			username = userinfo.get("mobphone").toString();
			if (!checkMobileNumber(username)) {
				return resultMessage(6, "");
			}
			password = userinfo.get("password").toString();
			break;
		case 3:
			username = userinfo.get("name").toString();
			password = userinfo.get("IDCard").toString();
			break;
		}
		// if (loginMode == 0) {
		// username = userinfo.get("id").toString();
		// if (!checkUserName(username)) {
		// return resultMessage(2, "");
		// }
		// }
		// if (loginMode == 1) {
		// username = userinfo.get("email").toString();
		// if (!checkEmail(username)) {
		// return resultMessage(4, "");
		// }
		// }
		// if (loginMode == 2) {
		// username = userinfo.get("mobphone").toString();
		// if (!checkMobileNumber(username)) {
		// return resultMessage(6, "");
		// }
		// }
		JSONObject object = login(username, password, loginMode);
		return object != null ? object.toString() : null;
	}

	// 用户登录，默认登录用户能够管理的所有站点的第一个站点，
	// 同时获取所管理网站的id及网站名称
	@SuppressWarnings("unchecked")
	private JSONObject login(String username, String password, int loginMode) {
		String sid = "";
		String _checkField = "";
		String field = "password";
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
		case 3:
			_checkField = "name";
			field = "IDCard";
			break;
		}
		if (field.equals("password")) {
			password = codec.md5(password);
		}
		JSONObject object = bind().eq(_checkField, username).eq(field, password).find();
		if (object != null) {
			String wbid = object.get("wbid").toString();
			JSONArray array = getWbID(wbid);
			if (array == null) {
				array = new JSONArray();
			}
			object.remove("wbid");
			object.remove("password");
			wbid = wbid.split(",")[0];
			object.put("currentWeb", wbid);
			object.put("webinfo", array);
			sid = session.createSession(username, object);
			object.put("sid", sid);
		}
		return object;
	}

	@SuppressWarnings("unchecked")
	private JSONArray getWbID(String wbid) {
		JSONArray webs = null;
		JSONObject object2;
		// String webinfo = execRequest
		// ._run("GrapeWebInfo/WebInfo/WebFindById/s:" + wbid, null)
		// .toString();
		if (wbid.equals("0")) {
			return webs;
		}
		try {
			webs = new JSONArray();
			String webinfo = appsProxy.proxyCall(getAppIp("host").split("/")[0],
					appsProxy.appid() + "/17/WebInfo/WebFindById/s:" + wbid, null, "").toString();
			// wbid对应的信息
			JSONObject rs = JSONHelper.string2json(webinfo);
			if (rs != null) {
				rs = JSONHelper.string2json(rs.get("message").toString());
				if (rs != null) {
					String records = rs.get("records").toString();
					JSONArray array = (JSONArray) JSONValue.parse(records);
					if (array.size() > 0) {
						JSONObject objects = null;
						JSONObject objid = null;
						for (int i = 0, len = array.size(); i < len; i++) {
							object2 = (JSONObject) array.get(i);
							objid = (JSONObject) object2.get("_id");
							objects = new JSONObject();
							objects.put("wbid", objid.get("$oid").toString());
							objects.put("wbname", object2.get("title").toString());
							webs.add(objects);
						}
					}
				}
			}
		} catch (Exception e) {
			webs = null;
		}
		return webs;
	}

	public void logout(String sid) {
		session.deleteSession(sid);
	}

	public long getpoint_username(String username) {
		long rl = 0;
		JSONObject rs = bind().eq("id", username).field("point").find();
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
		object = bind().eq("id", id).eq("password", codec.md5(oldPW)).data(object).update();
		return object != null ? 0 : 99;
	}

	public int edit(String _id, JSONObject userInfo) {
		int code = 99;
		if (userInfo != null) {
			try {
				if (userInfo.containsKey("id")) {
					if (!checkUserName(userInfo.get("id").toString())) {
						return 2;
					}
				}
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
				if (userInfo.containsKey("password")) {
					userInfo.remove("password");
				}
				JSONObject object = bind().eq("_id", new ObjectId(_id)).data(userInfo).update();
				code = (object != null ? 0 : 99);
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	public int Update(String id, String ownid, JSONObject object) {
		return bind().eq("_id", new ObjectId(id)).eq("ownid", ownid).data(object).update() != null ? 0 : 99;
	}

	public String select() {
		JSONArray array = null;
		try {
			array = new JSONArray();
			array = bind().limit(20).select();
		} catch (Exception e) {
			nlogger.logout(e);
			array = null;
		}
		return resultMessage(array);
	}

	public String select(JSONObject userInfo) {
		JSONArray array = null;
		if (userInfo != null) {
			try {
				array = new JSONArray();
				for (Object object2 : userInfo.keySet()) {
					if (object2.equals("_id")) {
						bind().eq("_id", new ObjectId(userInfo.get(object2).toString()));
					}
					bind().eq(object2.toString(), userInfo.get(object2.toString()));
				}
				array = bind().limit(20).select();
			} catch (Exception e) {
				nlogger.logout(e);
				array = null;
			}
		}
		return resultMessage(array);
	}

	/**
	 * 根据用户id查询用户信息
	 * 
	 * @param id
	 * @return
	 */
	public JSONObject select(String id) {
		JSONObject object = bind().eq("id", id).find();
		return object != null ? object : null;
	}

	@SuppressWarnings("unchecked")
	public String page(int idx, int pageSize) {
		JSONObject object = null;
		JSONArray array = new JSONArray();
		Object objects = execRequest.getChannelValue("sid");
		System.out.println(objects);
		JSONObject obj = getSessPlv(objects);
		if (obj != null) {
			try {
				// 获取角色权限
				int roleplv = Integer.parseInt(obj.get("rolePlv").toString());
				if (roleplv > 10000) {
					array = bind().page(idx, pageSize);
				}
				if (roleplv > 5000 && roleplv <= 10000) {
					array = bind().eq("wbid", (String) obj.get("currentWeb")).page(idx, pageSize);
				}
				if (roleplv > 3000 && roleplv <= 5000) {
					JSONObject oid = (JSONObject) obj.get("_id");
					array = bind().like("ownid", oid.get("$oid").toString()).eq("wbid", (String) obj.get("currentWeb"))
							.page(idx, pageSize);
				}
				object = new JSONObject();
				object.put("totalSize", (int) Math.ceil((double) array.size() / pageSize));
				object.put("currentPage", idx);
				object.put("pageSize", pageSize);
				object.put("data", array);
			} catch (Exception e) {
				nlogger.logout(e);
				object = null;
			}
		}
		return resultMessage(object);
	}

	@SuppressWarnings("unchecked")
	public String page(int idx, int pageSize, JSONObject userInfo) {
		JSONObject object = null;
		if (userInfo != null) {
			try {
				object = new JSONObject();
				for (Object object2 : userInfo.keySet()) {
					if ("_id".equals(object2.toString())) {
						bind().eq("_id", new ObjectId(userInfo.get("_id").toString()));
					}
					bind().eq(object2.toString(), userInfo.get(object2.toString()));
				}
				JSONArray array = bind().dirty().page(idx, pageSize);
				object.put("totalSize", (int) Math.ceil((double) bind().count() / pageSize));
				object.put("currentPage", idx);
				object.put("pageSize", pageSize);
				object.put("data", array);
			} catch (Exception e) {
				nlogger.logout(e);
				object = null;
			}
		}
		return resultMessage(object);
	}

	public int delect(String id) {
		int code = 99;
		JSONObject object = null;
		try {
			object = new JSONObject();
			object = bind().eq("_id", new ObjectId(id)).delete();
			code = (object != null ? 0 : 99);
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	public int delect(String[] arr) {
		int code = 99;
		try {
			bind().or();
			for (int i = 0; i < arr.length; i++) {
				bind().eq("_id", new ObjectId(arr[i]));
			}
			long codes = bind().deleteAll();
			code = (Integer.parseInt(String.valueOf(codes)) == arr.length ? 0 : 99);
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	public JSONObject findUserNameByID(String userName) {
		return bind().eq("id", userName).find();
	}

	public JSONObject findUserNameByEmail(String email) {
		return bind().eq("email", email).find();
	}

	public JSONObject findUserNameByMoblie(String phoneno) {
		return bind().eq("mobphone", phoneno).find();
	}

	public JSONObject findUserByCard(String name, String IDCard) {
		return bind().eq("name", name).eq("IDCard", IDCard).find();
	}

	public boolean checkUser(String id, String pw) {
		pw = secPassword(pw.toString());
		return bind().eq("id", id).eq("password", pw).find() == null;
	}

	@SuppressWarnings("unchecked")
	public boolean checkEmail(String email) {
		_form.putRule("email", formdef.email);
		JSONObject _obj = new JSONObject();
		_obj.put("email", email);
		return _form.checkRule(_obj);
	}

	@SuppressWarnings("unchecked")
	public boolean checkMobileNumber(String mobile) {
		_form.putRule("mobphone", formdef.mobile);
		JSONObject _obj = new JSONObject();
		_obj.put("mobphone", mobile);
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

	//excel导入数据到数据库
	public String Import(JSONArray array) {
		int code = 0;
		for (int i = 0; i < array.size(); i++) {
			if (code == 0) {
				code = bind().data((JSONObject) array.get(i)).insertOnce() != null ? 0 : 99;
			}else{
				code = 99;
			}
		}
		return resultMessage(code, "导入excel成功");
	}

	// 中断当前操作，等待用户输入验证码
	// public void breakCurrent(String ckcode, String uniqueName) {
	//
	// }
	private JSONObject FindByPrimary(String _id) {
		JSONObject object = null;
		try {
			object = new JSONObject();
			object = bind().eq("_id", new ObjectId(_id)).field("wbid").find();
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return object;
	}

	// 设置网站管理员，userid为用户表 _id 字段
	@SuppressWarnings("unchecked")
	public String FindWb(String wbid, String userid) {
		int code = 99;
		// 获取该用户已拥有的网站
		JSONObject object = FindByPrimary(userid);
		if (object != null) {
			try {
				String tempwbid = (String) object.get("wbid");
				if (!("").equals(tempwbid)) {
					wbid = String.join(",", wbid, tempwbid);
				}
				object.put("wbid", wbid);
				code = bind().eq("_id", new ObjectId(userid)).data(object).update() != null ? 0 : 99;
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return resultMessage(code, "设置网站管理员成功");
	}

	// public JSONArray FindByWbid(String wbid){
	// JSONArray array = null;
	// try {
	// array = new JSONArray();
	// array = bind().eq("wbid", wbid).limit(50).select();
	// } catch (Exception e) {
	// nlogger.logout(e);
	// array = null;
	// }
	// return array;
	// }
	// 获取会话信息
	@SuppressWarnings("unchecked")
	private JSONObject getSessPlv(Object object) {
		JSONObject object2 = null;
		session session = new session();
		try {
			JSONObject objects = new JSONObject();
			int roleplv = 0;
			object2 = new JSONObject();
			if (object != null) {
				object2 = session.getSession(object.toString());
				if (object2 != null) {
					String info = appsProxy
							.proxyCall(getAppIp("host").split("/")[0],
									appsProxy.appid() + "/16/roles/getRole/" + object2.get("ugid").toString(), null, "")
							.toString();
					objects = JSONHelper.string2json(info);
					if (objects != null) {
						objects = JSONHelper.string2json(objects.get("message").toString());
					}
					if (objects != null) {
						objects = JSONHelper.string2json(objects.get("records").toString());
					}
					if (objects != null) {
						roleplv = Integer.parseInt(objects.get("plv").toString());
					}
				}
				object2.put("rolePlv", roleplv);
			} else {
				object2.put("rolePlv", 0);
			}
		} catch (Exception e) {
			nlogger.logout(e);
			object2 = null;
		}
		return object2;
	}

	//
	private String getAppIp(String key) {
		String value = "";
		try {
			Properties pro = new Properties();
			pro.load(new FileInputStream("URLConfig.properties"));
			value = pro.getProperty(key);
		} catch (Exception e) {
			value = "";
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	public String resultMessage(JSONObject object) {
		if (object == null) {
			object = new JSONObject();
		}
		_obj.put("records", object);
		return resultMessage(0, _obj.toString());
	}

	@SuppressWarnings("unchecked")
	public String resultMessage(JSONArray array) {
		if (array == null) {
			array = new JSONArray();
		}
		_obj.put("records", array);
		return resultMessage(0, _obj.toString());
	}

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
			msg = "登录信息填写错误";
			break;
		case 10:
			msg = "没有操作权限";
			break;
		case 11:
			msg = "获取excel文件内容失败";
			break;
		default:
			msg = "其他操作异常";
			break;
		}
		return jGrapeFW_Message.netMSG(num, msg);
	}
}
