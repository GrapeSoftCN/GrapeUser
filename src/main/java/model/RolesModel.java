package model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import esayhelper.DBHelper;
import esayhelper.JSONHelper;
import esayhelper.formHelper;
import esayhelper.jGrapeFW_Message;
import rpc.execRequest;

public class RolesModel {
	private static DBHelper role;
	private static formHelper _form;
	// private static String wbid;
	private JSONObject _obj = new JSONObject();

	static {
		// session session = new session();
		// String info = session.get("username").toString();
		// wbid = JSONHelper.string2json(info).get("currentWeb").toString();
		role = new DBHelper("mongodb", "userGroup");
		_form = role.getChecker();
	}

	public RolesModel() {
		_form.putRule("name", formHelper.formdef.notNull);
	}

	@SuppressWarnings("unchecked")
	public int insert(JSONObject object) {
		// object.put("wbid", wbid);
		if (!_form.checkRuleEx(object)) {
			return 1; // 必填字段没有填
		}
		if (select(object.get("name").toString()) != null) {
			return 2; // 角色已存在
		}
		JSONObject obj = new JSONObject();
		obj.put("name", object.get("name").toString());
		String tip = execRequest
				._run("GrapeAuth/Auth/AddAuth/s:" + obj.toString(), null)
				.toString();
		long code = (long) JSONHelper.string2json(tip).get("errorcode");
		if (code == 0) {
			Object object2 = role.data(object).insertOnce();
			if (object2 != null) {
				return 0;
			}
			tip = execRequest._run("GrapeAuth/Auth/DeleteAuth/s:"
					+ object.get("name").toString(), null).toString();
			code = (long) JSONHelper.string2json(tip).get("errorcode");
		}
		return code == 0 ? 0 : 99;
	}

	public int update(String id, JSONObject object) {
		return role.eq("_id", new ObjectId(id)).data(object).update() != null
				? 0 : 99;
	}

	// 是否分表
	// public DBHelper bindfield(JSONObject object) {
	// if (object.containsKey("wbid")) {
	// if (!"0".equals(object.get("ownid").toString())) {
	// role = (DBHelper) role.bind(object.get("ownid").toString());
	// }
	// }
	// return role;
	// }
	/**
	 * 批量修改 设置排序值，调整层级关系
	 * 
	 * @param array
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public int update(JSONObject object) {
		int code = 0;
		JSONObject _obj = new JSONObject();
		if (object.containsKey("fatherid") && object.containsKey("sort")) {
			_obj.put("fatherid", object.get("fatherid"));
			_obj.put("sort", Integer.parseInt(object.get("sort").toString()));
			code = role.eq("_id", object.get("_id").toString()).data(_obj)
					.update() != null ? 0 : 99;
		} else {
			if (object.containsKey("fatherid")) {
				code = setFatherId(object.get("_id").toString(),
						object.get("fatherid").toString());
			} else {
				code = setsort(object.get("_id").toString(),
						Integer.parseInt(object.get("sort").toString()));
			}
		}
		return code;
	}

	public JSONArray select(JSONObject object) {
		for (Object object2 : object.keySet()) {
			role.eq(object2.toString(), object.get(object2.toString()));
		}
		return role.limit(20).select();
	}

	public JSONObject select(String name) {
		return role.eq("name", name).find();
	}

	@SuppressWarnings("unchecked")
	public JSONObject page(int idx, int pageSize) {
		JSONArray array = role.page(idx, pageSize);
		JSONObject object = new JSONObject();
		object.put("totalSize",
				(int) Math.ceil((double) role.count() / pageSize));
		object.put("currentPage", idx);
		object.put("pageSize", pageSize);
		object.put("data", array);
		return object;
	}

	@SuppressWarnings("unchecked")
	public JSONObject page(int idx, int pageSize, JSONObject object) {
		for (Object object2 : object.keySet()) {
			role.eq(object2.toString(), object.get(object2.toString()));
		}
		JSONArray array = role.page(idx, pageSize);
		JSONObject objects = new JSONObject();
		objects.put("totalSize",
				(int) Math.ceil((double) role.count() / pageSize));
		objects.put("currentPage", idx);
		objects.put("pageSize", pageSize);
		objects.put("data", array);
		return objects;
	}

	public int delete(String id) {
		return role.eq("_id", new ObjectId(id)).delete() != null ? 0 : 99;
	}

	public int delete(String[] arr) {
		role.or();
		for (String string : arr) {
			role.eq("_id", string);
		}
		return role.deleteAll() != arr.length ? 0 : 99;
	}

	@SuppressWarnings("unchecked")
	public int setsort(String id, int num) {
		JSONObject _obj = new JSONObject();
		_obj.put("sort", num);
		return role.eq("_id", new ObjectId(id)).data(_obj).update() != null ? 0
				: 99;
	}

	@SuppressWarnings("unchecked")
	public int setFatherId(String id, String fatherid) {
		JSONObject _obj = new JSONObject();
		_obj.put("fatherid", fatherid);
		return role.eq("_id", new ObjectId(id)).data(_obj).update() != null ? 0
				: 99;
	}

	@SuppressWarnings("unchecked")
	public int setPlv(String id, String plv) {
		JSONObject _obj = new JSONObject();
		_obj.put("plv", plv);
		return role.eq("_id", new ObjectId(id)).data(_obj).update() != null ? 0
				: 99;
	}

	// 获取角色plv
	public JSONObject getRole(String ugid) {
		return role.eq("_id", new ObjectId(ugid)).find();
	}

	@SuppressWarnings("unchecked")
	public JSONObject addMap(HashMap<String, Object> map, JSONObject object) {
		if (map.entrySet() != null) {
			Iterator<Entry<String, Object>> iterator = map.entrySet()
					.iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Object> entry = iterator.next();
				if (!object.containsKey(entry.getKey())) {
					object.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return object;
	}

	@SuppressWarnings("unchecked")
	public String resultMessage(JSONObject object) {
		_obj.put("records", object);
		return resultMessage(0, _obj.toString());
	}

	@SuppressWarnings("unchecked")
	public String resultMessage(JSONArray array) {
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
			msg = "没有创建数据权限，请联系管理员进行权限调整";
			break;
		case 5:
			msg = "没有修改数据权限，请联系管理员进行权限调整";
			break;
		case 6:
			msg = "没有删除数据权限，请联系管理员进行权限调整";
			break;
		default:
			msg = "其他操作异常";
			break;
		}
		return jGrapeFW_Message.netMSG(num, msg);
	}
}
