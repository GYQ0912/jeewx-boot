package com.jeecg.p3.baseApi.service;

import org.apache.ibatis.annotations.Param;

import com.jeecg.p3.baseApi.vo.OpenAccountVo;

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
}

