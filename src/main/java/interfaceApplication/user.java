package interfaceApplication;

import java.util.HashMap;

import org.json.simple.JSONObject;

import esayhelper.JSONHelper;
import model.userModel;

@SuppressWarnings("unchecked")
public class user {
	private userModel usermodel = new userModel();
	private HashMap<String, Object> defcol = new HashMap<>();
	private JSONObject _obj = new JSONObject();

	public user() {
		defcol.put("sex", 1);
		defcol.put("birthday", 0);
		defcol.put("point", 0);
		defcol.put("cash", 0.0);
		defcol.put("ownid", 0);
		defcol.put("time", null);
		defcol.put("lasttime", 0);
		defcol.put("ugid", 0);
		defcol.put("state", 0);
		defcol.put("isdelete", 0);
		defcol.put("isvisble", 0);
		defcol.put("plv", 1000);
	}

	/**
	 * 注册用户
	 * 
	 * @param userInfo（必填字段
	 *            id,password,name,registerip,wbid,email,mobphone）
	 * @return
	 */
	public String UserRegister(String userInfo) {
		JSONObject object = usermodel.AddMap(defcol, JSONHelper.string2json(userInfo));
		return usermodel.resultMessage(usermodel.register(object), "用户注册成功");
	}

	/**
	 * 用户登录
	 * 
	 * @param userInfo
	 *            登录信息 ( 登录名(id,email,mobphone),password 登录密码,loginmode
	 *            登录模式（0:用户名；1:email；2:手机号）)
	 * @return 除密码之外的数据
	 */
	public String UserLogin(String userInfo) {
		System.out.println("123");
		String mString = "";
		String usersinfo = usermodel.checkLogin(JSONHelper.string2json(userInfo));
		if (usersinfo != null) {
			_obj.put("records", JSONHelper.string2json(usersinfo));
			mString = usermodel.resultMessage(0, _obj.toString());
		} else {
			mString = usermodel.resultMessage(9, "");
		}
		return mString;
	}

	public String UserLogout(String UserName) {
		if (UserName.length() > 1 && UserName.length() < 128) {
			usermodel.logout(UserName);
		}
		return usermodel.resultMessage(0, "退出成功");
	}

	public String UserGetpoint(String userName) {
		String value = String.valueOf(usermodel.getpoint_username(userName));
		return usermodel.resultMessage(0, value);
	}

	public String UserChangePW(String UserName, String oldPW, String newPW) {
		return usermodel.resultMessage(usermodel.changePW(UserName, oldPW, newPW), "密码修改成功！");
	}

	public String UserEdit(String _id, String userInfo) {
		return usermodel.resultMessage(usermodel.edit(_id, JSONHelper.string2json(userInfo)), "用户信息修改成功");
	}

	public String UserSelect() {
		return usermodel.select();
	}

	public String UserSearch(String userinfo) {
		return usermodel.select(JSONHelper.string2json(userinfo));
	}

	public String UserFind(String id) {
		return usermodel.resultMessage(usermodel.select(id));
	}

	public String UserPage(int idx, int pageSize) {
		return usermodel.page(idx, pageSize);
	}

	public String UserPageBy(int idx, int pageSize, String userinfo) {
		JSONObject object = JSONHelper.string2json(userinfo);
		return usermodel.page(idx, pageSize, object);
	}

	public String UserDelect(String id) {
		return usermodel.resultMessage(usermodel.delect(id), "删除成功");
	}

	public String UserBatchDelect(String ids) {
		return usermodel.resultMessage(usermodel.delect(ids.split(",")), "批量操作成功");
	}

	public String AddLeader(String info) {
		JSONObject object = usermodel.AddMap(defcol, JSONHelper.string2json(info));
		return usermodel.resultMessage(usermodel.register(object), "新增用户成功");
	}
	//设置网站管理员
	public String FindWbBySid(String wbid,String userid) {
		return usermodel.FindWb(wbid, userid);
	}
	
	//根据网站id显示数据
	public void getUser(String wbid) {
		
	}
}
