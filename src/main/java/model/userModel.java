package model;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils.Null;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.jGrapeFW_Message;
import apps.appsProxy;
import authority.privilige;
import check.checkHelper;
import check.formHelper;
import check.formHelper.formdef;
import database.DBHelper;
import database.db;
import json.JSONHelper;
import nlogger.nlogger;
import rpc.execRequest;
import security.codec;
import session.session;
import time.TimeHelper;

public class userModel {
	private DBHelper users;
	private formHelper _form;
	private JSONObject _obj = new JSONObject();
	private JSONObject UserInfo = null;
	private session session = new session();
	private HashMap<String, Object> defcol = new HashMap<>();
	private String sid = null;
	private Map<String, String> ssessionmap = new Hashtable<>();

	private db bind() {
		return users.bind(String.valueOf(appsProxy.appid()));
	}

	public userModel() {
		users = new DBHelper(appsProxy.configValue().get("db").toString(), "userList");
		_form = users.getChecker();
		sid = (String) execRequest.getChannelValue("sid");
		if (sid != null) {
			UserInfo = new JSONObject();
			UserInfo = session.getSession(sid);
		}
		_form.putRule("id", formdef.notNull);
		_form.putRule("password", formdef.notNull);
		_form.putRule("name", formdef.notNull);

		defcol.put("sex", 1);
		defcol.put("birthday", 0);
		defcol.put("point", 0);
		defcol.put("cash", 0.0);
		defcol.put("ownid", 0);
		defcol.put("time", TimeHelper.nowMillis());
		defcol.put("lasttime", 0);
		defcol.put("ugid", 0);
		defcol.put("state", 0);
		defcol.put("isdelete", 0);
		defcol.put("isvisble", 0);
		defcol.put("plv", 1000);
		defcol.put("IDcard", "");
		defcol.put("wbid", "");
		defcol.put("registerip", "");
		defcol.put("mobphone", "");
		defcol.put("email", "");
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
				_userInfo = AddMap(defcol, _userInfo);
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
		JSONObject object = null;
		int loginMode = 0;
		String username = "";
		String password = "";
		try {
			if (userinfo.containsKey("loginmode")) {
				loginMode = Integer.parseInt(userinfo.get("loginmode").toString());
			}
			switch (loginMode) {
			case 0:// id,pw模式
				username = userinfo.get("id").toString();
				if (!checkUserName(username)) {
					return resultMessage(2, "");
				}
				if (userinfo.containsKey("password")) {
					password = userinfo.get("password").toString();
				}
				break;
			case 1:// email,password模式
				username = userinfo.get("email").toString();
				if (!checkEmail(username)) {
					return resultMessage(4, "");
				}
				if (userinfo.containsKey("password")) {
					password = userinfo.get("password").toString();
				}
				break;
			case 2://
				username = userinfo.get("mobphone").toString();
				if (!checkMobileNumber(username)) {
					return resultMessage(6, "");
				}
				if (userinfo.containsKey("password")) {
					password = userinfo.get("password").toString();
				}
				break;
			case 3:
				// username = userinfo.get("name").toString();
				// password = userinfo.get("IDcard").toString();
				username = userinfo.get("IDcard").toString();
				password = userinfo.get("name").toString();
				break;
			case 4:
				// username = userinfo.get("name").toString();
				// password = userinfo.get("IDcard").toString();

				String name = userinfo.get("name").toString();
				if (userinfo.containsKey("password")) {
					password = userinfo.get("password").toString();
				}
				JSONObject rs = getIDCard(name, password);
				username = rs == null ? null : rs.get("IDcard").toString();
				password = name;
				break;
			}
			object = null;
			if (username != null) {
				object = new JSONObject();
				object = login(username, password, loginMode);
			}
		} catch (Exception e) {
			e.printStackTrace();
			object = null;
		}
		return object != null ? object.toString() : null;
	}

	// 用户登录，默认登录用户能够管理的所有站点的第一个站点，
	// 同时获取所管理网站的id及网站名称
	@SuppressWarnings("unchecked")
	private JSONObject login(String username, String password, int loginMode) {
		JSONObject loginData;
		int code;
		if (ssessionmap.containsKey(username)) {
			session.delete(ssessionmap.get(username));
			// session.deleteSession(ssessionmap.get(username));
		}
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
		case 4:
			_checkField = "IDcard";
			field = "name";
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
			sid = session.createSession(username, object, 86400);
			ssessionmap.put(username, sid);
			object.put("sid", sid);
			// 新增登录次数,获取用户id
			loginData = AddCount(_checkField, username);
			String _id = ( (JSONObject)object.get("_id") ).getString("$oid");
			code = edit(_id, loginData);
		}
		return object;
	}

	/**
	 * 增加登录次数
	 * 
	 * @project GrapeUser
	 * @package interfaceApplication
	 * @file user.java
	 * 
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	private JSONObject AddCount(String checkField, String value) {
		JSONObject obj = new JSONObject();
		long times = 0;
		String values;
		try {
			// 获取登录次数
			JSONObject logincount = bind().eq(checkField, value).field("logincount").limit(1).find();
			if (logincount != null && logincount.size() != 0) {
				values = String.valueOf(logincount.get("logincount"));
				if (values.contains("$numberLong")) {
					logincount = JSONObject.toJSON(values);
					values = (logincount != null && logincount.size() != 0) ? logincount.getString("$numberLong") : "0";
				}
				times = Long.parseLong(values) + 1;
			}
		} catch (Exception e) {
			nlogger.logout(e);
		}
		obj.put("logincount", times);
		return obj;
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
					JSONArray array = JSONArray.toJSONArray(records);
					// JSONArray array = (JSONArray) JSONValue.parse(records);
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
		String GrapeSID = (String) execRequest.getChannelValue("sid");
		if (GrapeSID == null) {
			if (!ssessionmap.containsKey(sid)) {
				GrapeSID = ssessionmap.get(sid);
			}
		}
		session.deleteSession(GrapeSID);
	}

	public long getpoint_username(String username) {
		long rl = 0;
		JSONObject rs = bind().eq("id", username).field("point").find();
		if (rs != null) {
			rl = Long.parseLong(rs.get("point").toString());
		}
		return rl;
	}

	// 通过用户名id，修改密码 --后台改密操作
	@SuppressWarnings("unchecked")
	public int changePW(String id, String oldPW, String newPW) {
		if (checkUser(id, oldPW)) {
			return 9;
		}
		JSONObject object = new JSONObject();
		object.put("password", secPassword(newPW));
		object = bind().eq("id", id).eq("password", codec.md5(oldPW)).data(object).update();
		return object != null ? 0 : 99;
	}

	// 修改密码:loginmode:0:用户名id；1：email；2：mobphone；3：name or IDcard
	@SuppressWarnings("unchecked")
	public int changePWs(String id, String oldPW, String newPW, int loginmode) {
		JSONObject object = new JSONObject();
		String _checkField = "";
		switch (loginmode) {
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
			if (!checkHelper.checkPersonCardID(id)) {
				object = getIDCard(id, oldPW);
				id = object == null ? null : object.get("IDcard").toString();
			}
			_checkField = "IDcard";
			break;
		}
		if (checkUsers(_checkField, id, oldPW)) {
			return 9;
		}
		JSONObject obj = new JSONObject();
		obj.put("password", secPassword(newPW));
		object = bind().eq(_checkField, id).eq("password", secPassword(oldPW)).data(obj).update();
		return object != null ? 0 : 99;
	}

	// 验证用户信息，email+pwd，mobphone+pwd；IDcard+pwd
	private boolean checkUsers(String field, String value, String pw) {
		return bind().eq(field, value).eq("password", secPassword(pw)).find() == null;
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
		JSONObject object = new JSONObject();
		JSONArray array = new JSONArray();
		int roleSign = getRoleSign();
		if (UserInfo != null) {
			db db = bind();
			try {
				// 获取角色权限
				if (roleSign == 5 || roleSign == 4) {
					array = db.page(idx, pageSize);
				}
				if (roleSign == 3 || roleSign == 2) {
					db.eq("wbid", (String) UserInfo.get("currentWeb"));
					array = db.dirty().page(idx, pageSize);
				}
				object.put("totalSize", (int) Math.ceil((double) db.count() / pageSize));
			} catch (Exception e) {
				nlogger.logout(e);
				object.put("totalSize", 0);
			} finally {
				db.clear();
			}
			object.put("currentPage", idx);
			object.put("pageSize", pageSize);
			object.put("data", array);
		}
		return resultMessage(object);
	}

	@SuppressWarnings("unchecked")
	public String page(int idx, int pageSize, JSONObject userInfo) {
		db db = bind();
		JSONObject object = new JSONObject();
		JSONArray array = new JSONArray();

		try {
			if (userInfo != null) {
				for (Object object2 : userInfo.keySet()) {
					if ("_id".equals(object2.toString())) {
						db.eq("_id", new ObjectId(userInfo.get("_id").toString()));
					}
					db.eq(object2.toString(), userInfo.get(object2.toString()));
				}
				array = db.dirty().page(idx, pageSize);
				object.put("totalSize", (int) Math.ceil((double) db.count() / pageSize));
			} else {
				object.put("totalSize", 0);
			}
		} catch (Exception e) {
			nlogger.logout(e);
			object.put("totalSize", 0);
		} finally {
			db.clear();
		}
		object.put("currentPage", idx);
		object.put("pageSize", pageSize);
		object.put("data", array);
		return resultMessage(object);
	}

	public int delect(String id) {
		int code = 99;
		try {
			JSONObject object = bind().eq("_id", new ObjectId(id)).delete();
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
		pw = secPassword(pw);
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

	// excel导入数据到数据库
	public String Import(JSONArray array) {
		int code = 0;
		for (int i = 0; i < array.size(); i++) {
			if (code == 0) {
				code = bind().data((JSONObject) array.get(i)).insertOnce() != null ? 0 : 99;
			} else {
				code = 99;
			}
		}
		return resultMessage(code, "导入excel成功");
	}

	// 通过姓名和密码获取身份证号
	private JSONObject getIDCard(String name, String password) {
		JSONObject object = bind().eq("name", name)
				.eq("password", !password.equals("") ? codec.md5(password) : password).field("IDcard").find();
		return object != null ? object : null;
	}

	/**
	 * 根据角色plv，获取角色级别
	 * 
	 * @project GrapeSuggest
	 * @package interfaceApplication
	 * @file Suggest.java
	 * 
	 * @return
	 *
	 */
	private int getRoleSign() {
		int roleSign = 0; // 游客
		if (sid != null) {
			try {
				privilige privil = new privilige(sid);
				int roleplv = privil.getRolePV(appsProxy.appidString());
				if (roleplv >= 1000 && roleplv < 3000) {
					roleSign = 1; // 普通用户即企业员工
				}
				if (roleplv >= 3000 && roleplv < 5000) {
					roleSign = 2; // 栏目管理员
				}
				if (roleplv >= 5000 && roleplv < 8000) {
					roleSign = 3; // 企业管理员
				}
				if (roleplv >= 8000 && roleplv < 10000) {
					roleSign = 4; // 监督管理员
				}
				if (roleplv >= 10000) {
					roleSign = 5; // 总管理员
				}
			} catch (Exception e) {
				nlogger.logout(e);
				roleSign = 0;
			}
		}
		return roleSign;
	}

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
