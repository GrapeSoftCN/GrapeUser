package model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import esayhelper.DBHelper;
import esayhelper.formHelper;
import esayhelper.jGrapeFW_Message;

public class RolesModel {
	private static DBHelper role;
	private static formHelper _form;
	static {
		role = new DBHelper("mongodb", "role");
		_form = role.getChecker();
	}

	public RolesModel() {
		_form.putRule("name", formHelper.formdef.notNull);
	}

	public int insert(JSONObject object) {
		if (!_form.checkRuleEx(object)) {
			return 1; // 必填字段没有填
		}
		return role.data(object).insertOnce() != null ? 0 : 99;
	}

	public int update(String id, JSONObject object) {
		return role.eq("_id", new ObjectId(id)).data(object).update() != null ? 0 : 99;
	}

	public int update(String id, String ownid, JSONObject object) {
		return role.eq("_id", new ObjectId(id)).eq("ownid", ownid).data(object).update() != null ? 0 : 99;
	}

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
			code = role.eq("_id", object.get("_id").toString()).data(_obj).update() != null ? 0 : 99;
		} else {
			if (object.containsKey("fatherid")) {
				code = setFatherId(object.get("_id").toString(), object.get("fatherid").toString());
			} else {
				code = setsort(object.get("_id").toString(), Integer.parseInt(object.get("sort").toString()));
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

	public JSONObject page(int idx, int pageSize) {
		JSONArray array = role.page(idx, pageSize);
		return page2Json(role, idx, pageSize, array);
	}

	public JSONObject page(int idx, int pageSize, String ownid) {
		JSONArray array = role.eq("ownid", ownid).page(idx, pageSize);
		return page2Json(role, idx, pageSize, array);
	}

	public JSONObject page(int idx, int pageSize, JSONObject object) {
		for (Object object2 : object.keySet()) {
			role.eq(object2.toString(), object.get(object2.toString()));
		}
		JSONArray array = role.page(idx, pageSize);
		return page2Json(role, idx, pageSize, array);
	}

	public JSONObject page(int idx, int pageSize, JSONObject object, String ownid) {
		for (Object object2 : object.keySet()) {
			role.eq(object2.toString(), object.get(object2.toString()));
		}
		JSONArray array = role.eq("ownid", ownid).page(idx, pageSize);
		return page2Json(role, idx, pageSize, array);
	}

	@SuppressWarnings("unchecked")
	public JSONObject page2Json(DBHelper role, int idx, int pageSize, JSONArray array) {
		JSONObject object = new JSONObject();
		object.put("totalSize", (int) Math.ceil((double) role.count() / pageSize));
		object.put("currentPage", idx);
		object.put("pageSize", pageSize);
		object.put("data", array);
		return object;
	}

	public int delete(String id) {
		return role.eq("_id", new ObjectId(id)).delete() != null ? 0 : 99;
	}

	public int delete(String[] arr) {
		role = (DBHelper) role.or();
		for (String string : arr) {
			role.eq("_id", string);
		}
		return role.delete() != null ? 0 : 99;
	}

	@SuppressWarnings("unchecked")
	public int setsort(String id, int num) {
		JSONObject _obj = new JSONObject();
		_obj.put("sort", num);
		return role.eq("_id", new ObjectId(id)).data(_obj).update() != null ? 0 : 99;
	}

	@SuppressWarnings("unchecked")
	public int setFatherId(String id, String fatherid) {
		JSONObject _obj = new JSONObject();
		_obj.put("fatherid", fatherid);
		return role.eq("_id", new ObjectId(id)).data(_obj).update() != null ? 0 : 99;
	}
	public int setPlv(String id, String plv) {
		JSONObject _obj = new JSONObject();
		_obj.put("plv", plv);
		return role.eq("_id", new ObjectId(id)).data(_obj).update() != null ? 0 : 99;
	}

	//获取角色plv
	public JSONObject getRole(String ugid) {
		return role.eq("_id", new ObjectId(ugid)).find();
	}
	@SuppressWarnings("unchecked")
	public JSONObject addMap(HashMap<String, Object> map, JSONObject object) {
		if (map.entrySet() != null) {
			Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Object> entry = iterator.next();
				if (!object.containsKey(entry.getKey())) {
					object.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return object;
	}

	public String getID() {
		String str = UUID.randomUUID().toString().trim();
		return str.replace("-", "");
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
		default:
			msg = "其他操作异常";
			break;
		}
		return jGrapeFW_Message.netMSG(num, msg);
	}
}
