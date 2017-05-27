package interfaceApplication;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import database.db;
import esayhelper.DBHelper;
import esayhelper.JSONHelper;
import esayhelper.TimeHelper;
import esayhelper.checkHelper;
import esayhelper.formHelper;
import esayhelper.jGrapeFW_Message;
import esayhelper.formHelper.formdef;
import nlogger.nlogger;
import session.session;

public class wechatUser {
	private static DBHelper opHelper;
	private static formHelper form;
	static {
		opHelper = new DBHelper(appsProxy.configValue().get("db").toString(),
				"UserOpenId");
		form = opHelper.getChecker();
	}

	private db openIdBind() {
		return opHelper.bind(appsProxy.appid() + "");
	}

	@SuppressWarnings("unchecked")
	public String insertOpenId(String info) {
		JSONObject object = JSONHelper.string2json(info);
		object.remove("type");
		if (!object.containsKey("isdelete")) {
			object.put("isdelete", "0");
		}
		if (!object.containsKey("time")) { // 操作时间
			object.put("time", TimeHelper.nowMillis() + "");
		}
		object.put("kickTime", ""); // 封号时间：默认为""，0为永久封号
		form.putRule("openid", formdef.notNull);
		form.putRule("name", formdef.notNull);
		form.putRule("phone", formdef.notNull);
		if (!form.checkRuleEx(object)) {
			return resultMessage(1);
		}
		if (!checkHelper.checkMobileNumber(object.get("phone").toString())) {
			return resultMessage(2); // 手机号格式错误
		}
		String openid = object.get("openid").toString();
		String userinfo = appsProxy
				.proxyCall("123.57.214.226:801",
						appsProxy.appid()+"/30/Wechat/getUserInfo/s:"
								+ openid,null, "")
				.toString();
		String message = JSONHelper.string2json(userinfo).get("message")
				.toString();
		String records = JSONHelper.string2json(message).get("records").toString();
		String headimgurl = JSONHelper.string2json(records).get("headimgurl")
				.toString();
		object.put("headimgurl", headimgurl);
		int code = openIdBind().data(object).insertOnce() != null ? 0 : 99;
		return resultMessage(code);
	}

	public String FindOpenId(String openid) {
		JSONObject object = openIdBind().eq("openid", openid).find();
		return resultMessage(0, object != null ? object.toString() : "");
	}

	public String FindById(String id) {
		JSONObject object = openIdBind().eq("_id", new ObjectId(id)).find();
		return resultMessage(object);
	}

	// 用户封号,定时解封
	public String KickUser(String openid, String info) {
		JSONObject object = JSONHelper.string2json(info);
		if( object != null ){
			String  message = FindOpenId(openid);
			if( message != null && !"".equals(message) ){
				String record = JSONHelper.string2json(message).get("message").toString();
				if (!("").equals(record)) {
					if (object.containsKey("content")) {
						object.remove("content");
					}
					int code = openIdBind().eq("openid", openid).data(object)
							.update() != null ? 0 : 99;
					return resultMessage(code, "操作成功");
				}
			}
		}
		return resultMessage(4);
	}

	@SuppressWarnings("unchecked")
	public String page(int ids, int pageSize) {
		JSONArray array = openIdBind().page(ids, pageSize);
		JSONObject object = new JSONObject();
		object.put("totalSize",
				(int) Math.ceil((double) openIdBind().count() / pageSize));
		object.put("pageSize", pageSize);
		object.put("currentPage", ids);
		object.put("data", array);
		return resultMessage(object);
	}

	@SuppressWarnings("unchecked")
	public String pageby(int ids, int pageSize, String info) {
		openIdBind().and();
		JSONObject objects = JSONHelper.string2json(info);
		for (Object obj : objects.keySet()) {
			if (obj.equals("_id")) {
				openIdBind().eq("_id",
						new ObjectId(objects.get("_id").toString()));
			}
			openIdBind().eq(obj.toString(), objects.get(obj.toString()));

		}
		JSONArray array = openIdBind().dirty().page(ids, pageSize);
		JSONObject object = new JSONObject();
		object.put("totalSize",
				(int) Math.ceil((double) openIdBind().count() / pageSize));
		object.put("pageSize", pageSize);
		object.put("currentPage", ids);
		object.put("data", array);
		return resultMessage(object);
	}

	public String unkick() {
		nlogger.logout(appsProxy.appid());
		int code = 0;
		int totalSize = (int) Math
				.ceil((double) openIdBind().eq("isdelete", "1").count() / 10);
		for (int i = 0; i < totalSize; i++) {
			JSONArray array = openIdBind().page(i, 10);
			code = judge(array);
		}
		return resultMessage(code, "解封成功");
	}

	@SuppressWarnings("unchecked")
	private int judge(JSONArray array) {
		int code = 0;
		for (int i = 0; i < array.size(); i++) {
			JSONObject object = (JSONObject) array.get(i);
			if ("0".equals(object.get("kickTime").toString())) {
				break;
			}
			long opTime = Long.parseLong(object.get("time").toString());
			long kickTime = Long.parseLong(object.get("kickTime").toString());
			String totalTime = TimeHelper
					.stampToDate(kickTime * 3600 * 24 * 1000 + opTime)
					.split(" ")[0];
			String currentTime = TimeHelper.stampToDate(TimeHelper.nowMillis())
					.split(" ")[0];
			if (!totalTime.equals(currentTime)) {
				break;
			}
			object.put("isdelete", "0");
			object.put("kickTime", "");
			object.put("time", TimeHelper.nowMillis() + "");
			code = openIdBind().eq("openid", object.get("openid").toString())
					.data(object).update() != null ? 0 : 99;
		}
		return code;
	}

	private String resultMessage(int num) {
		return resultMessage(num, "");
	}

	@SuppressWarnings("unchecked")
	private String resultMessage(JSONObject object) {
		JSONObject obj = new JSONObject();
		obj.put("records", object);
		return resultMessage(0, obj.toString());
	}

	private String resultMessage(int num, String message) {
		String msg = "";
		switch (num) {
		case 0:
			msg = message;
			break;
		case 1:
			msg = "必填字段为空";
			break;
		case 2:
			msg = "手机号格式错误";
			break;
		case 3:
			msg = "身份证号格式错误";
			break;
		case 4:
			msg = "用户不存在";
			break;
		default:
			msg = "其他操作异常";
			break;
		}
		return jGrapeFW_Message.netMSG(num, msg);
	}
}
