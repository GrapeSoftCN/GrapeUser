package interfaceApplication;

import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import apps.appsProxy;
import json.JSONHelper;
import model.RolesModel;
import nlogger.nlogger;

public class roles {
	private RolesModel rolesModel = new RolesModel();
	private HashMap<String, Object> defcol = new HashMap<>();

	public roles() {
		defcol.put("ownid", appsProxy.appid());
		defcol.put("sort", 0);
		defcol.put("fatherid", 0);
		defcol.put("wbid", "0");
		defcol.put("plv", 1000); // 权限值
	}

	public String RoleInsert(String roleInfo) {
		JSONObject object = rolesModel.addMap(defcol, JSONHelper.string2json(roleInfo));
		return rolesModel.resultMessage(rolesModel.insert(object), "新增角色成功");
	}

	public String RoleUpdate(String id, String roleInfo) {
		return rolesModel.resultMessage(rolesModel.update(id, JSONHelper.string2json(roleInfo)), "修改角色成功");
	}

	/**
	 * 批量修改 修改排序值和层级关系
	 * 
	 * @param arraystring
	 * @return
	 */
	public String RoleUpdateBatch(String arraystring) {
		int code = 99;
		JSONArray array = (JSONArray) JSONValue.parse(arraystring);
		if (array.size() != 0) {
			for (int i = 0; i < array.size(); i++) {
				if (code != 0) {
					return rolesModel.resultMessage(2, "");
				}
				code = rolesModel.update((JSONObject) array.get(i));
			}
		}
		return rolesModel.resultMessage(code, "设置顺序或层级成功");
	}

	public String RoleSearch(String roleInfo) {
		return rolesModel.select(JSONHelper.string2json(roleInfo));
	}

	public String RoleDelete(String id) {
		return rolesModel.resultMessage(rolesModel.delete(id), "角色删除成功");
	}

	public String RoleBatchDelete(String ids) {
		return rolesModel.resultMessage(rolesModel.delete(ids.split(",")), "批量删除成功");
	}

	public String RolePage(int idx, int pageSize) {
		return rolesModel.page(idx, pageSize);
	}

	public String RolePageBy(int idx, int pageSize, String roleInfo) {
		return rolesModel.page(idx, pageSize, JSONHelper.string2json(roleInfo));
	}

	public String RoleSetSort(String id, int sort) {
		return rolesModel.resultMessage(rolesModel.setsort(id, sort), "设置排序值成功");
	}

	public String RoleSetFatherId(String id, String fatherid) {
		return rolesModel.resultMessage(rolesModel.setFatherId(id, fatherid), "上级用户组设置成功");
	}

	public String RoleSetPlv(String id, String plv) {
		return rolesModel.resultMessage(rolesModel.setPlv(id, plv), "权限设置成功");
	}

	// 根据角色id获取角色信息
	public String getRole(String ugid) {
		return rolesModel.getRole(ugid);
	}
}
