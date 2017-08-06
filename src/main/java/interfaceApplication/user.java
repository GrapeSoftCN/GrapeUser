package interfaceApplication;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import checkCode.checkCodeHelper;
import json.JSONHelper;
import jxl.Sheet;
import jxl.Workbook;
import model.userModel;
import rpc.execRequest;
import security.codec;
import session.session;
import sms.ruoyaMASDB;
import time.TimeHelper;

@SuppressWarnings("unchecked")
public class user {
	private userModel usermodel = new userModel();
	private HashMap<String, Object> defcol = new HashMap<>();
	private JSONObject _obj = new JSONObject();
	private String sid = null;
	private session session;
	private JSONObject userInfo = new JSONObject();

	public user() {
		session = new session();
		sid = (String) execRequest.getChannelValue("sid");
		if (sid != null) {
			userInfo = session.getSession(sid);
		}
		defcol.put("sex", 1); // 性别，默认为男
		defcol.put("birthday", 0); // 出生日期
		defcol.put("point", 0); // 积分
		defcol.put("cash", 0.0); // 重要积分
		defcol.put("ownid", 0);
		defcol.put("time", null); // 注册时间
		defcol.put("lasttime", 0); // 上次登录时间
		defcol.put("ugid", 0); // 角色id
		defcol.put("state", 0); // 状态
		defcol.put("isdelete", 0); // 是否删除状态[0:未删除状态；1:删除状态]
		defcol.put("isvisble", 0); // 是否可见
		defcol.put("plv", 1000); // 权限值
		defcol.put("IDcard", ""); // 身份证号
		defcol.put("wbid", (userInfo != null && userInfo.size() != 0) ? (String) userInfo.get("currentweb") : ""); // 所属网站id
		defcol.put("loginCount", 0);
		// defcol.put("userNo", SetUserNumber()); // 用户编号
	}

	/**
	 * 注册用户
	 * 
	 * @param userInfo（必填字段
	 *            id,password,name,registerip,wbid,email,mobphone）
	 * @return
	 */
	public String UserRegister(String userInfo) {
		return usermodel.resultMessage(usermodel.register(JSONHelper.string2json(userInfo)), "用户注册成功");
	}

	// 获取验证码,发送至用户手机号
	public String getVerifyCode(String phone) {
		String code = checkCodeHelper.getCheckCode(phone, 6);
		code = ruoyaMASDB.sendSMS(phone, "验证码为：" + code + "有效时间为。。。，请在有效时间内进行验证");
		return usermodel.resultMessage((code != null ? 0 : 99), "验证码发送成功");
	}

	// 验证用户输入的验证码
	public String checkVerifyCode(String phone, String code) {
		boolean flag = checkCodeHelper.checkCode(phone, code);
		return usermodel.resultMessage(flag == true ? 0 : 99, "验证成功");
	}

	/**
	 * 用户登录
	 * 
	 * @param userInfo
	 *            登录信息 ( 登录名(id,email,mobphone),password 登录密码,loginmode
	 *            登录模式（0:用户名+密码；1:email+密码；2:手机号+密码；3:真实姓名+身份证号）)
	 * @return 除密码之外的数据
	 */
	public String UserLogin(String userInfo) {
		String mString = "";
		JSONObject object;
		String usersinfo = usermodel.checkLogin(JSONHelper.string2json(userInfo));
		if (usersinfo != null) {
			object = JSONObject.toJSON(usersinfo);
			_obj.put("records", object);
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

	public String UserChangePWFront(String UserName, String oldPW, String newPW, int loginmode) {
		return usermodel.resultMessage(usermodel.changePWs(UserName, oldPW, newPW, loginmode), "密码修改成功！");
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

	/** ------前台用户查询--------- **/
	public String UserPageFront(String wbid, int idx, int pageSize) {
		return usermodel.page(wbid, idx, pageSize, null);
	}

	public String UserPageByFront(String wbid, int idx, int pageSize, String userinfo) {
		return usermodel.page(wbid, idx, pageSize, userinfo);
	}

	/** ------后台用户查询--------- **/
	public String UserPage(int idx, int pageSize) {
		// return usermodel.page(idx, pageSize);
		return usermodel.page(null, idx, pageSize, null);
	}

	public String UserPageBy(int idx, int pageSize, String userinfo) {
		// JSONObject object = JSONHelper.string2json(userinfo);
		// return usermodel.page(idx, pageSize, object);
		return usermodel.page(null, idx, pageSize, userinfo);
	}

	public String UserDelete(String id) {
		return usermodel.resultMessage(usermodel.delect(id), "删除成功");
	}

	public String UserBatchDelect(String ids) {
		return usermodel.resultMessage(usermodel.delect(ids.split(",")), "批量操作成功");
	}

	public String AddLeader(String info) {
		JSONObject object = usermodel.AddMap(defcol, JSONHelper.string2json(info));
		return usermodel.resultMessage(usermodel.register(object), "新增用户成功");
	}

	// 设置网站管理员
	public String FindWbBySid(String wbid, String userid) {
		return usermodel.FindWb(wbid, userid);
	}

	// 根据用户名和身份证号查询数据
	public String findByCard(String name, String IDCard) {
		return usermodel.findUserByCard(name, IDCard).toString();
	}

	// 从excel表中导入数据到数据库表中
	public String ExcelImport(String filepath) {
		filepath = codec.DecodeHtmlTag(filepath);
		// String path = "C://JavaCode/tomcat/webapps/"+getImageUri(filepath);
		// String path = filepath;
		JSONArray array = new JSONArray();
		List<JSONObject> list = getAllByExcel(filepath);
		if (list == null) {
			return usermodel.resultMessage(11, "");
		}
		for (JSONObject jsonObject : list) {
			array.add(jsonObject);
		}
		return usermodel.Import(array);
	}

	/**
	 * 获取当前用户姓名，及当前时间，添加至水印图片中
	 * 
	 * @project GrapeUser
	 * @package interfaceApplication
	 * @file user.java
	 * 
	 * @return
	 *
	 */
	public String getUserImage() {
		String name = "";
		if (userInfo != null && userInfo.size() != 0) {
			name = userInfo.getString("name");
		}
		return CreateImage(name);
	}

	/**
	 * 生成图片
	 * 
	 * @project File
	 * @package interfaceApplication
	 * @file HandleImage.java
	 * 
	 * @param path
	 * @param name
	 * @return
	 *
	 */
	private String CreateImage(String name) {
		int width = 200;
		int height = 200;
		Font font = new Font("宋体", Font.BOLD, 12);
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		String currentTime = TimeHelper.stampToDate(TimeHelper.nowMillis());
		try {
			BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D g2 = bi.createGraphics();
			g2.rotate(Math.toRadians(-45), width / 2, height / 2);
			g2.setFont(font);
			g2.setColor(new Color(230, 230, 230));

			g2.drawString(name, 30, 50);
			g2.drawString(currentTime, 30, 80);
			g2.dispose();
			// 输出字节流格式
			ImageIO.write(bi, "PNG", buffer);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "data:image/jpeg;base64," + Base64.encodeBase64String(buffer.toByteArray());
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

	private String getFileIp(String key, int sign) {
		String value = "";
		try {
			if (sign == 0 || sign == 1) {
				value = getAppIp(key).split("/")[sign];
			}
		} catch (Exception e) {
			value = "";
		}
		return value;
	}

	/**
	 * 设置用户编号
	 * 
	 * @project GrapeUser
	 * @package interfaceApplication
	 * @file user.java
	 * 
	 *
	 */
	private String SetUserNumber() {
		int code = codec.randomString(5).hashCode(); // 获取5位随机字符串的hashcode
		if (code < 0) {
			code = -code;
		}
		// 获取11位字符串，不足11位的开头数字补1，其余补0
		String value = String.format("%d%010d", 1, code);
		return String.valueOf(value);
	}

	private List<JSONObject> getAllByExcel(String file) {
		List<JSONObject> list = null;
		JSONObject object = new JSONObject();
		try {
			list = new ArrayList<>();
			Workbook rwb = Workbook.getWorkbook(new File(file));
			Sheet[] value = rwb.getSheets();
			for (Sheet rs : value) {
				int clos = rs.getColumns();// 得到所有的列
				int rows = rs.getRows();// 得到所有的行
				if (clos == 0 && rows == 0) {
					break;
				}
				for (int i = 1; i < rows; i++) {
					for (int j = 1; j < clos; j++) {
						object.put("name", rs.getCell(j++, i).getContents());
						object.put("phone", rs.getCell(j++, i).getContents());
						object.put("IDcard", rs.getCell(j++, i).getContents());
						list.add(object);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			list = null;
		}
		return list;
	}

	private String getImageUri(String imageURL) {
		String subString;
		String rString = null;
		int i = imageURL.toLowerCase().indexOf("http://");
		if (i >= 0) {
			subString = imageURL.substring(i + 7);
			rString = subString.split("/")[0];
		}
		return rString;
	}
}
