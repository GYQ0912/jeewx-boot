package com.jeecg.p3.baseApi.service;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtil;
import org.apache.ibatis.annotations.Param;
import org.jeecgframework.p3.core.util.HttpUtils;
import org.jeecgframework.p3.core.util.WeiXinHttpUtil;
import org.jeecgframework.p3.core.utils.common.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.jeecg.p3.baseApi.vo.OpenAccountVo;
import com.jeecg.p3.redis.JedisPoolUtil;
import com.jeecg.p3.weixinInterface.entity.WeixinAccount;

/**
 * 获取二维码
 * @author huangqingquan
 *
 */
public interface BaseApiJwidService {

	/**
	 * 获取微信公众号的二维码
	 * @param jwid
	 * @return
	 */
	public String getQrcodeUrl(String jwid);
	
	/**
	 * 获取api凭证
	 * @param jwid
	 * @return
	 */
	public String queryTicketByJwid(String jwid);

	/**
	 * 查询，通过appid查询，查询第三方平台账号配置
	 * @param appid
	 * @return
	 */
	public OpenAccountVo queryOneByAppid(String appid);
	
	/**
	 * 通过公众号id获取access_token
	 * @param jwid
	 * @return
	 */
	public String queryAccessTokenByJwid(String jwid);
	
	String getRedisSignature(HttpServletRequest request, String jwid);

	String getRedisSignature(String url, String jwid);

	JSONObject getGzUserInfo(String openid, String jwid);

	String getNickName(String openid, String jwid);

	JSONObject getGzUserInfo2(String openid, String weixinId, String userAccessToken);

	String getShortUrl(String hdurl, String jwid);
}

