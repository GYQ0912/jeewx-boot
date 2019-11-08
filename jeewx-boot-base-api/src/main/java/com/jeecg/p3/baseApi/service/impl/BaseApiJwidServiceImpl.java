package com.jeecg.p3.baseApi.service.impl;


import java.util.HashMap;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.jeecg.p3.baseApi.vo.OpenAccountVo;
import com.jeecg.p3.redis.JedisPoolUtil;
import com.jeecg.p3.weixinInterface.entity.WeixinAccount;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtil;
import org.jeecgframework.p3.core.util.HttpUtils;
import org.jeecgframework.p3.core.util.WeiXinHttpUtil;
import org.jeecgframework.p3.core.utils.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.jeecg.p3.baseApi.dao.BaseApiJwidDao;
import com.jeecg.p3.baseApi.service.BaseApiJwidService;

@Service("baseApiJwidService")
public class BaseApiJwidServiceImpl implements BaseApiJwidService {
	public final static Logger logger = LoggerFactory.getLogger(BaseApiJwidServiceImpl.class);
	
	@Resource
	private BaseApiJwidDao baseApiJwidDao;
	
	@Override
	public String getQrcodeUrl(String jwid) {
		String jwWebJwid = baseApiJwidDao.queryOneByJwid(jwid);
		return jwWebJwid==null?null:"/upload/img/commonweixin/"+jwWebJwid;
	}
	
	@Override
	public String queryTicketByJwid(String jwid) {
		return baseApiJwidDao.queryTicketByJwid(jwid);
	}

	/**
	 * 查询，通过appid查询，查询第三方平台账号配置
	 * @param appid
	 * @return
	 */
	public OpenAccountVo queryOneByAppid(String appid){
		return baseApiJwidDao.queryOneByAppid(appid);
	}

	@Override
	public String queryAccessTokenByJwid(String jwid) {
		return baseApiJwidDao.queryAccessTokenByJwid(jwid);
	}
	
	public String getRedisSignature(HttpServletRequest request, String jwid) {
		long startTime = System.currentTimeMillis();
		String signature = null;
		String url = request.getRequestURL() + "?" + request.getQueryString();
		if (url.indexOf("#") != -1) {
			url = url.substring(0, url.indexOf("#"));
		}

		WeixinAccount po = JedisPoolUtil.getWxAccount(jwid);
		String jsapi_ticket = po.getJsapiticket();
		String need_make_string = "jsapi_ticket=" + jsapi_ticket + "&noncestr=" + "oDxlNmsjqvV9D29r" + "&timestamp="
				+ "1420942347" + "&url=" + url;
		logger.debug("----------通过Redis缓存获取公众号，本地生成签名----------------------getRedisSignature-------");
		logger.debug("----------------------need_make_string--------------：" + need_make_string);

		try {
			signature = DigestUtils.shaHex(need_make_string);
		} catch (Exception arg10) {
			arg10.printStackTrace();
		}

		long endTime = System.currentTimeMillis();
		logger.debug("--------生成签名------方法： getRedisSignature-----耗时：" + (endTime - startTime) + "ms");
		return signature;
	}

	public String getRedisSignature(String url, String jwid) {
		long startTime = System.currentTimeMillis();
		String signature = null;
		WeixinAccount po = JedisPoolUtil.getWxAccount(jwid);
		String jsapi_ticket = po.getJsapiticket();
		String need_make_string = "jsapi_ticket=" + jsapi_ticket + "&noncestr=" + "oDxlNmsjqvV9D29r" + "&timestamp="
				+ "1420942347" + "&url=" + url;
		logger.debug("----------通过Redis缓存获取公众号，本地生成签名----------------------getRedisSignature-------");
		logger.debug("----------------------need_make_string--------------：" + need_make_string);

		try {
			signature = DigestUtils.shaHex(need_make_string);
		} catch (Exception arg9) {
			arg9.printStackTrace();
		}

		long endTime = System.currentTimeMillis();
		logger.debug("--------生成签名------方法： getRedisSignature-----耗时：" + (endTime - startTime) + "ms");
		return signature;
	}

	public JSONObject getGzUserInfo(String openid, String jwid) {
		long startTime = System.currentTimeMillis();
		String redistoken = queryAccessTokenByJwid(jwid);
		if (redistoken == null) {
			logger.error("---------------getRedisWeixinToken ---获取缓存token失败----------------");
			return null;
		} else {
			String requestUrl = "https://api.weixin.qq.com/cgi-bin/user/info?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN"
					.replace("ACCESS_TOKEN", redistoken);
			requestUrl = requestUrl.replace("OPENID", openid);
			JSONObject jsonObj = WeiXinHttpUtil.sendGet(requestUrl);
			long endTime = System.currentTimeMillis();
			logger.info("----通过微信服务端获取粉丝信息----方法：getGzUserInfo------耗时：" + (endTime - startTime) + "ms");
			if (jsonObj != null && !jsonObj.containsKey("errcode")) {
				return jsonObj;
			} else {
				logger.info("-----------------getGzUserInfo--获取粉丝信息失败--------------------" + jsonObj.toString());
				return null;
			}
		}
	}

	public String getNickName(String openid, String jwid) {
		JSONObject jsonObj = getGzUserInfo(openid, jwid);
		return jsonObj != null && jsonObj.containsKey("nickname") ? jsonObj.getString("nickname") : null;
	}

	public JSONObject getGzUserInfo2(String openid, String weixinId, String userAccessToken) {
		long startTime = System.currentTimeMillis();
		String requestUrl = null;
		if (StringUtils.isNotBlank(userAccessToken)) {
			requestUrl = "https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN"
					.replace("ACCESS_TOKEN", userAccessToken);
		} else {
			String jsonObj = queryAccessTokenByJwid(weixinId);
			requestUrl = "https://api.weixin.qq.com/cgi-bin/user/info?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN"
					.replace("ACCESS_TOKEN", jsonObj);
		}

		requestUrl = requestUrl.replace("OPENID", openid);
		JSONObject jsonObj1 = WeiXinHttpUtil.sendGet(requestUrl);
		long endTime = System.currentTimeMillis();
		logger.info("----通过微信服务端获取粉丝信息----方法：getGzUserInfo2------耗时：" + (endTime - startTime) + "ms");
		if (jsonObj1 != null && !jsonObj1.containsKey("errcode")) {
			return jsonObj1;
		} else {
			logger.info("-----------------getGzUserInfo2--获取粉丝信息失败--------------------" + jsonObj1.toString());
			return null;
		}
	}

	public String getShortUrl(String hdurl, String jwid) {
		logger.info("getShortUrl request jwid={}, hdurl={}.", new Object[] { jwid, hdurl });
		if (!StringUtil.isEmpty(hdurl) && !StringUtil.isEmpty(jwid)) {
			String accessToken = queryAccessTokenByJwid(jwid);
			if (StringUtil.isEmpty(accessToken)) {
				return null;
			} else {
				HashMap paramMap = new HashMap();
				paramMap.put("action", "long2short");
				paramMap.put("long_url", hdurl);
				JSONObject obj = new JSONObject(paramMap);
				logger.info("getShortUrl request jsonStr={}.", new Object[] { obj.toString() });
				String str = HttpUtils.doPostJson(
						"https://api.weixin.qq.com/cgi-bin/shorturl?access_token=" + accessToken, obj.toString());
				logger.info("getShortUrl response jsonStr={}.", new Object[] { str });
				if (str != null) {
					JSONObject jsonStr = JSONObject.parseObject(str);
					if (jsonStr.containsKey("errcode") && "0".equals(jsonStr.getString("errcode"))
							&& jsonStr.containsKey("short_url")) {
						String shortUrl = jsonStr.getString("short_url").replaceAll("\\/", "/");
						logger.info("getShortUrl response short_url={}.", new Object[] { shortUrl });
						return shortUrl;
					}
				}

				return null;
			}
		} else {
			return null;
		}
	}

}
