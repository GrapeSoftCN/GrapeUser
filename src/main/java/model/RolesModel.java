package model;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import check.formHelper;
import database.DBHelper;
import database.db;
import esayhelper.JSONHelper;
import esayhelper.jGrapeFW_Message;
import nlogger.nlogger;
import rpc.execRequest;
import session.session;

public class RolesModel {
	private static DBHelper role;
	private static formHelper _form;
	private static String wbid;
	private JSONObject _obj = new JSONObject();

	static {
		role = new DBHelper(appsProxy.configValue().get("db").toString(), "userGroup");
		_form = role.getChecker();
	}

	private db bind() {
		return role.bind(String.valueOf(appsProxy.appid()));
	}

	public RolesModel() {
		_form.putRule("name", formHelper.formdef.notNull);
	}

	@SuppressWarnings("unchecked")
	public int insert(JSONObject object) {
		int code = 99;
		if (object != null) {
			JSONObject obj = getSessPlv(execRequest.getChannelValue("sid"));
			if (object.containsKey("wbid") && ("").equals(object.get(wbid).toString()) && obj != null) {
				object.put("wbid", obj.get("currentWeb").toString());
			}
			try {
				if (!_form.checkRuleEx(object)) {
					return 1; // 必填字段没有填
				}
				if (select(object.get("name").toString()) != null) {
					return 2; // 角色已存在
				}
				Object object2 = bind().data(object).insertOnce();
				code = (object2 != null ? 0 : 99);
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	public int update(String id, JSONObject object) {
		int code = 99;
		JSONObject obj = null;
		try {
			if (object != null && getPlv(id) != 0) {
				try {
					obj = new JSONObject();
					obj = bind().eq("_id", new ObjectId(id)).data(object).update();
					code = (obj != null ? 0 : 99);
				} catch (Exception e) {
					nlogger.logout(e);
					code = 99;
				}
			} else {
				code = 4;
			}
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	/**
	 * 批量修改 设置排序值，调整层级关系
	 * 
	 * @param array
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public int update(JSONObject object) {
		int code = 99;
		JSONObject _obj = new JSONObject();
		if (object != null) {
			try {
				JSONObject obj = (JSONObject) object.get("_id");
				if (object.containsKey("fatherid") && object.containsKey("sort")) {
					_obj.put("fatherid", object.get("fatherid"));
					_obj.put("sort", Integer.parseInt(object.get("sort").toString()));
					code = bind().eq("_id", obj.get("$oid").toString()).data(_obj).update() != null ? 0 : 99;
				} else {
					if (object.containsKey("fatherid")) {
						code = setFatherId(object.get("$oid").toString(), object.get("fatherid").toString());
					} else {
						code = setsort(object.get("$oid").toString(), Integer.parseInt(object.get("sort").toString()));
					}
				}
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	public String select(JSONObject object) {
		JSONArray array = null;
		if (object != null) {
			try {
				array = new JSONArray();
				for (Object object2 : object.keySet()) {
					bind().eq(object2.toString(), object.get(object2.toString()));
				}
				array = bind().limit(20).select();
			} catch (Exception e) {
				nlogger.logout(e);
				array = null;
			}
		}
		return resultMessage(array);
	}

	public String select(String name) {
		JSONObject object = null;
		try {
			object = new JSONObject();
			object = bind().eq("name", name).find();
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return resultMessage(object);
	}

	@SuppressWarnings("unchecked")
	public String page(int idx, int pageSize) {
		JSONObject object = null;
		try {
			object = new JSONObject();
			JSONArray array = bind().page(idx, pageSize);
			object = new JSONObject();
			object.put("totalSize", (int) Math.ceil((double) bind().count() / pageSize));
			object.put("currentPage", idx);
			object.put("pageSize", pageSize);
			object.put("data", array);
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return resultMessage(object);
	}

	@SuppressWarnings("unchecked")
	public String page(int idx, int pageSize, JSONObject object) {
		JSONObject obj = null;
		if (object != null) {
			try {
				obj = new JSONObject();
				for (Object object2 : object.keySet()) {
					bind().eq(object2.toString(), object.get(object2.toString()));
				}
				JSONArray array = bind().dirty().page(idx, pageSize);
				obj = new JSONObject();
				obj.put("totalSize", (int) Math.ceil((double) bind().count() / pageSize));
				obj.put("currentPage", idx);
				obj.put("pageSize", pageSize);
				obj.put("data", array);
			} catch (Exception e) {
				nlogger.logout(e);
				object = null;
			}
		}
		return resultMessage(object);
	}

	public int delete(String id) {
		int code = 99;
		JSONObject object = null;
		try {
			if (getPlv(id) != 0) {
				object = new JSONObject();
				object = bind().eq("_id", new ObjectId(id)).delete();
				code = (object != null ? 0 : 99);
			}
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	public int delete(String[] arr) {
		int code = 99;
		try {
			int plv = 0;
			bind().or();
			for (int i = 0; i < arr.length; i++) {
				plv = getPlv(arr[i]);
				if (plv != 0) {
					bind().eq("_id", new ObjectId(arr[i]));
				}
			}
			long codes = bind().deleteAll();
			code = (Integer.parseInt(String.valueOf(codes)) == (plv == 0 ? arr.length : arr.length - 1) ? 0 : 99);
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	@SuppressWarnings("unchecked")
	public int setsort(String id, int num) {
		int code = 99;
		JSONObject object = new JSONObject();
		object.put("sort", num);
		if (object != null) {
			try {
				JSONObject obj = bind().eq("_id", new ObjectId(id)).data(object).update();
				code = (obj != null ? 0 : 99);
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	@SuppressWarnings("unchecked")
	public int setFatherId(String id, String fatherid) {
		int code = 99;
		JSONObject object = new JSONObject();
		object.put("fatherid", fatherid);
		if (object != null) {
			try {
				JSONObject obj = bind().eq("_id", new ObjectId(id)).data(object).update();
				code = (obj != null ? 0 : 99);
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	@SuppressWarnings("unchecked")
	public int setPlv(String id, String plv) {
		int code = 99;
		JSONObject object = new JSONObject();
		object.put("plv", plv);
		if (object != null) {
			try {
				JSONObject obj = bind().eq("_id", new ObjectId(id)).data(object).update();
				code = (obj != null ? 0 : 99);
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	// 获取角色plv
	public String getRole(String ugid) {
		JSONObject object = null;
		try {
			object = new JSONObject();
			object = bind().eq("_id", new ObjectId(ugid)).field("plv").find();
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return resultMessage(object);
	}

	private int getPlv(String id) {
		int code = 99;
		try {
			String roleplv = getRole(id);
			JSONObject object = JSONHelper.string2json(roleplv);
			if (object != null) {
				object = JSONHelper.string2json(object.get("message").toString());
				if (object != null) {
					object = JSONHelper.string2json(object.get("records").toString());
				}
				code = Integer.parseInt(object.get("plv").toString());
			}
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

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

	// 获取应用url[内网url或者外网url]，0表示内网，1表示外网
	private String getHost(int signal) {
		String host = null;
		try {
			if (signal == 0 || signal == 1) {
				host = getAppIp("host").split("/")[signal];
			}
		} catch (Exception e) {
			nlogger.logout(e);
			host = null;
		}
		return host;
	}

	@SuppressWarnings("unchecked")
	public JSONObject addMap(HashMap<String, Object> map, JSONObject object) {
		JSONObject obj = null;
		if (object != null) {
			try {
				obj = object;
				if (map.entrySet() != null) {
					Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
					while (iterator.hasNext()) {
						Map.Entry<String, Object> entry = iterator.next();
						if (!obj.containsKey(entry.getKey())) {
							obj.put(entry.getKey(), entry.getValue());
						}
					}
				}
			} catch (Exception e) {
				nlogger.logout(e);
				obj = null;
			}
		}
		return obj;
	}

	@SuppressWarnings("unchecked")
	private String resultMessage(JSONObject object) {
		if (object == null) {
			object = new JSONObject();
		}
		_obj.put("records", object);
		return resultMessage(0, _obj.toString());
	}

	@SuppressWarnings("unchecked")
	private String resultMessage(JSONArray array) {
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
			msg = "设置排序或层级失败";
			break;
		case 3:
			msg = "设置排序或层级失败";
			break;
		case 4:
			msg = "无法操作该条数据";
			break;
		case 5:
			msg = "没有创建数据权限，请联系管理员进行权限调整";
			break;
		case 6:
			msg = "没有修改数据权限，请联系管理员进行权限调整";
			break;
		case 7:
			msg = "没有删除数据权限，请联系管理员进行权限调整";
			break;
		default:
			msg = "其他操作异常";
			break;
		}
		return jGrapeFW_Message.netMSG(num, msg);
	}
}
