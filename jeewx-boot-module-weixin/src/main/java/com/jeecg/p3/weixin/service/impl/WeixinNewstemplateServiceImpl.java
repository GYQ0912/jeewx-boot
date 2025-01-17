package com.jeecg.p3.weixin.service.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.ibatis.annotations.Param;
import org.jeecgframework.p3.core.util.PropertiesUtil;
import org.jeecgframework.p3.core.util.WeiXinHttpUtil;
import org.jeecgframework.p3.core.utils.common.PageList;
import org.jeecgframework.p3.core.utils.common.PageQuery;
import org.jeecgframework.p3.core.utils.common.PageQueryWrapper;
import org.jeecgframework.p3.core.utils.common.Pagenation;
import org.jeewx.api.wxsendmsg.JwSendMessageAPI;
import org.jeewx.api.wxsendmsg.util.ReadImgUrls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.jeecg.p3.baseApi.service.BaseApiJwidService;
import com.jeecg.p3.commonweixin.def.CommonWeixinProperties;
import com.jeecg.p3.weixin.dao.WeixinNewsitemDao;
import com.jeecg.p3.weixin.dao.WeixinNewstemplateDao;
import com.jeecg.p3.weixin.entity.BaseGraphic;
import com.jeecg.p3.weixin.entity.UploadGraphic;
import com.jeecg.p3.weixin.entity.WeixinNewsitem;
import com.jeecg.p3.weixin.entity.WeixinNewstemplate;
import com.jeecg.p3.weixin.service.WeixinNewstemplateService;
import com.jeecg.p3.weixin.util.WeixinUtil;
import com.jeecg.p3.weixin.util.WxErrCodeUtil;


/**
 * 描述：</b>图文模板表<br>
 * @author：weijian.zhang
 * @since：2018年07月13日 12时46分13秒 星期五 
 * @version:1.0
 */
@Service("weixinNewstemplateService")
public class WeixinNewstemplateServiceImpl implements WeixinNewstemplateService {
	/**上传图片根路径*/
	@Value("${jeewx.path.upload}")
	private String upLoadPath;

	@Resource
	private WeixinNewstemplateDao weixinNewstemplateDao;
	@Resource
	private WeixinNewsitemDao weixinNewsitemDao;
	@Autowired
	private BaseApiJwidService baseApiJwidService; 
	//上传图文消息素材
	public final static String upload_group_news_url = "https://api.weixin.qq.com/cgi-bin/media/uploadnews?access_token=ACCESS_TOKEN";
	@Override
	public void doAdd(WeixinNewstemplate weixinNewstemplate) {
		weixinNewstemplateDao.insert(weixinNewstemplate);
	}

	@Override
	public void doEdit(WeixinNewstemplate weixinNewstemplate) {
		weixinNewstemplateDao.update(weixinNewstemplate);
	}

	@Override
	public void doDelete(String id) {
		weixinNewstemplateDao.delete(id);
	}

	@Override
	public WeixinNewstemplate queryById(String id) {
		WeixinNewstemplate weixinNewstemplate  = weixinNewstemplateDao.get(id);
		return weixinNewstemplate;
	}

	@Override
	public PageList<WeixinNewstemplate> queryPageList(PageQuery<WeixinNewstemplate> pageQuery) {
		PageList<WeixinNewstemplate> result = new PageList<WeixinNewstemplate>();
		Integer itemCount = weixinNewstemplateDao.count(pageQuery);
		PageQueryWrapper<WeixinNewstemplate> wrapper = new PageQueryWrapper<WeixinNewstemplate>(pageQuery.getPageNo(), pageQuery.getPageSize(),itemCount, pageQuery.getQuery());
		List<WeixinNewstemplate> list = weixinNewstemplateDao.queryPageList(wrapper);
		//author:sunkai--date:2018-10-08--for:上传状态转译
		for(WeixinNewstemplate newsTemplate:list){
			if(newsTemplate.getUploadType().equals("2")){
				if(newsTemplate.getUpdateTime() != null && newsTemplate.getUploadTime() != null && newsTemplate.getUpdateTime().after(newsTemplate.getUploadTime())){
					newsTemplate.setUploadStatus("1");
				}
			}
		}
		//author:sunkai--date:2018-10-08--for:上传状态转译
		Pagenation pagenation = new Pagenation(pageQuery.getPageNo(), itemCount, pageQuery.getPageSize());
		result.setPagenation(pagenation);
		result.setValues(list);
		return result;
	}

	//update-begin--Author:zhangweijian  Date: 20180720 for：获取所有图文素材
	//获取所有图文素材
	@Override
	public List<WeixinNewstemplate> getAllItems(String jwid,String uploadType) {
		return weixinNewstemplateDao.getAllItems(jwid,uploadType);
	}
	//update-end--Author:zhangweijian  Date: 20180720 for：获取所有图文素材

	//update-begin--Author:zhangweijian  Date: 20180802 for：上传图文素材到微信
	//上传图文素材
	@Transactional(rollbackFor = {Exception.class})
	@Override
	public String uploadNewstemplate(String id,String jwid) {
		String message=null;
		WeixinNewstemplate newsTemplate=weixinNewstemplateDao.get(id);
		//1.根据id获取当前模板的所有图文
		List<WeixinNewsitem> newsItems=weixinNewsitemDao.queryByNewstemplateId(id);
		if(newsItems.size()>0){
			//author:sunkai--date:2018-10-09--for:异常捕获
			try {
				//设置图文上传状态为上传中
				newsTemplate.setUploadType("1");
				weixinNewstemplateDao.update(newsTemplate);
				//2.遍历所有的图文
				List<BaseGraphic> baseGraphicList = new ArrayList<BaseGraphic>();
				for(int i=0;i<newsItems.size();i++){
					//3.1装载图文参数
					WeixinNewsitem newsItem=newsItems.get(i);
					BaseGraphic baseGraphic=new BaseGraphic();
					//update-begin--Author:zhangweijian  Date: 20180820 for：位置调整
					baseGraphic.setThumb_media_id(this.uploadPhoto(newsItem.getImagePath(),jwid));
					baseGraphic.setAuthor(newsItem.getAuthor());
					baseGraphic.setTitle(newsItem.getTitle());
					baseGraphic.setContent_source_url(newsItem.getUrl());
					baseGraphic.setContent(this.updateContent(newsItem.getContent(),jwid));
					baseGraphic.setDigest(newsItem.getDescription());
					//update-begin--Author:zhangweijian Date:20181016 for：传入封面图是否展示参数
					//是否显示封面图，1为显示，0为不显示
					baseGraphic.setShow_cover_pic(newsItem.getShowCoverPic());
					//update-end--Author:zhangweijian Date:20181016 for：传入封面图是否展示参数
					//update-end--Author:zhangweijian  Date: 20180820 for：位置调整
					baseGraphicList.add(baseGraphic);
					//3.2更新图文素材信息
					newsItem.setThumbMediaId(baseGraphic.getThumb_media_id());
					newsItem.setContent(baseGraphic.getContent());
					weixinNewsitemDao.update(newsItem);
				}
				UploadGraphic graphic = new UploadGraphic();
				graphic.setArticles(baseGraphicList);
				JSONObject resultJson=uploadGroupNewsTemplate(graphic,jwid);
				if(resultJson.containsKey("media_id")){
					newsTemplate.setMediaId(resultJson.getString("media_id"));
					newsTemplate.setUploadTime(new Date());
					//update-begin--Author:zhangweijian  Date: 20180807 for：上传成功状态
					//"2":上传成功；"3"：上传失败
					newsTemplate.setUploadType("2");
					//update-end--Author:zhangweijian  Date: 20180807 for：上传成功状态
					weixinNewstemplateDao.update(newsTemplate);
					message= "图文素材上传成功！";
				}else{
					//update-begin--Author:zhangweijian  Date: 20180807 for：上传成功状态
					newsTemplate.setUploadType("3");
					weixinNewstemplateDao.update(newsTemplate);
					//update-end--Author:zhangweijian  Date: 20180807 for：上传成功状态
					//author:sunkai--date:2018-09-26--for:粉丝同步错误返回码信息转义
					String msg = WxErrCodeUtil.testErrCode(resultJson.getString("errcode"));
					message= "图文素材上传失败，"+msg;
					//author:sunkai--date:2018-09-26--for:粉丝同步错误返回码信息转义
				}
			} catch (Exception e) {
				e.printStackTrace();
				message= "图文素材上传失败";
				newsTemplate.setUploadType("3");
				weixinNewstemplateDao.update(newsTemplate);
			}
			//author:sunkai--date:2018-10-09--for:异常捕获
		}else{
			message="该图文模板尚未添加图文消息！";
		}
		return message;
	}

	//替换微信图文
	private String updateContent(String content, String jwid) {
		if (content != null) {
			//获取token方法替换
			String accessToken = baseApiJwidService.queryAccessTokenByJwid(jwid);
			//String baseImageUrl = ResourceUtil.getWebProjectPath();
			//update-begin--Author:zhangweijian  Date: 20180831 for：接口方法替换
			HttpServletRequest request =((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
			String baseImageUrl=request.getSession().getServletContext().getRealPath("/");
			//update-end--Author:zhangweijian  Date: 20180831 for：接口方法替换
			String[] urls = ReadImgUrls.getImgs(content);
			
			if(urls!=null){
				Map<String, Object> tmpMap = new HashMap<String, Object>();
		        for (String str : urls) {
		        	tmpMap.put(str, str);
		        }
		        //返回一个包含所有对象的指定类型的数组
		        urls = tmpMap.keySet().toArray(new String[1]);
				
				for(String url:urls){
					if(url.indexOf("mmbiz.qpic.cn")!=-1){
						continue;
					}
					String relativeImgurl =url.replace(CommonWeixinProperties.domain,"");
					String src = null;
					try {
						src = download1(url);
					} catch (Exception e) {
						e.printStackTrace();
					}
					/*String tempimgurl ="";
					if(relativeImgurl.startsWith("http")){
						tempimgurl = relativeImgurl;
					}else{
						tempimgurl = baseImageUrl + relativeImgurl;
					}*/
					JSONObject retObj=JwSendMessageAPI.uploadImgReturnObj(accessToken, src);
					if(null!=retObj&&retObj.containsKey("url")){
						String newUrl=retObj.getString("url");
						content = content.replace(url, newUrl);
						System.out.println("正文图片"+relativeImgurl+"同步微信成功!\r\n");
					}else{
						System.out.println("正文图片"+relativeImgurl+"同步微信成功!\r\n");
					}
				}
			}
		}
		
		return content;
	}
	

	
	public String download1(String urlString) throws Exception {  
		File file=new File(urlString);
		String savePath=upLoadPath;
		String filename=file.getName();
        // 构造URL  
        URL url = new URL(urlString);  
        // 打开连接  
        URLConnection con = url.openConnection();  
        //设置请求超时为5s  
        con.setConnectTimeout(5*1000);  
        con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        // 输入流  
        InputStream is = con.getInputStream();  
      
        // 1K的数据缓冲  
        byte[] bs = new byte[1024];  
        // 读取到的数据长度  
        int len;  
        // 输出的文件流  
       File sf=new File(savePath);  
       if(!sf.exists()){  
           sf.mkdirs();  
       }  
       OutputStream os = new FileOutputStream(sf.getPath()+"\\"+filename);  
        // 开始读取  
        while ((len = is.read(bs)) != -1) {  
          os.write(bs, 0, len);  
        }  
        // 完毕，关闭所有链接  
        os.close();  
        is.close();  
        return savePath+ "/" + filename;
    }
	
	public String downloadImageFromPC(String urlString) throws Exception {  
		File file=new File(urlString);
		String basePath = upLoadPath;
		String filePath = "/upload/files/";
		String savePath = basePath + filePath;
		String fileName=file.getName();
		// 构造URL  
		URL url = new URL(urlString);  
		// 打开连接  
		URLConnection con = url.openConnection();  
		//设置请求超时为5s  
		con.setConnectTimeout(5*1000);  
		con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
		// 输入流  
		InputStream is = con.getInputStream();
		  
		// 1K的数据缓冲  
		byte[] bs = new byte[1024];  
		// 读取到的数据长度  
		int len;  
		// 输出的文件流  
		String fileExtension = fileName.substring(fileName.lastIndexOf("."));
		String newFileName=UUID.randomUUID().toString().replace("-", "")+fileExtension;
		
       File sf=new File(savePath);  
       if(!sf.exists()){  
           sf.mkdirs();  
       }  
       OutputStream os = new FileOutputStream(sf.getPath()+"\\"+newFileName);  
        // 开始读取  
        while ((len = is.read(bs)) != -1) {  
          os.write(bs, 0, len);  
        }  
        // 完毕，关闭所有链接  
        os.close();  
        is.close();  
         
        return filePath + newFileName;
    }
	
	//图片上传微信服务器
	private String uploadPhoto(String imagePath, String jwid) {
		String media_id="";
		HttpServletRequest request =((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
		//获取token方法替换
		String accessToken = baseApiJwidService.queryAccessTokenByJwid(jwid);
		//String url=request.getSession().getServletContext().getRealPath("/")+imagePath;
		String url = upLoadPath + imagePath;
		JSONObject jsonObj=WeixinUtil.sendMedia("image", url, accessToken);
		if(jsonObj!=null){
			if(jsonObj.containsKey("errcode")){
				//author:sunkai--date:2018-09-26--for:粉丝同步错误返回码信息转义
				//String errcode = jsonObj.getString("errcode");
				//String errmsg = jsonObj.getString("errmsg");
				String msg = WxErrCodeUtil.testErrCode(jsonObj.getString("errcode"));
				System.out.println("图片同步微信服务器失败! "+msg);
				//author:sunkai--date:2018-09-26--for:粉丝同步错误返回码信息转义
			}else{
				System.out.println("图片素材同步微信服务器成功");
				media_id = jsonObj.getString("media_id");
			}
		}
		return media_id;
	}

	//上传图文素材
	private JSONObject uploadGroupNewsTemplate(UploadGraphic graphic, String jwid) {
		//获取token方法替换
		String accessToken = baseApiJwidService.queryAccessTokenByJwid(jwid);
		if(accessToken!=null){
			String requestUrl = upload_group_news_url.replace("ACCESS_TOKEN", accessToken);
			JSONObject obj = JSONObject.fromObject(graphic);
			JSONObject result = WeixinUtil.httpRequest(requestUrl, "POST", obj.toString());
			return result;
		}
		return null;
	}
	//update-begin--Author:zhangweijian  Date: 20180802 for：上传图文素材到微信

	@Override
	public void receiveArticle(WeixinNewstemplate weixinNewstemplate, WeixinNewsitem weixinNewsitem) {
		String content = weixinNewsitem.getContent();
		
		String[] urls = ReadImgUrls.getImgs(content);
		
		if(urls != null){
			Map<String, Object> tmpMap = new HashMap<String, Object>();
	        for (String str : urls) {
	        	tmpMap.put(str, str);
	        }
	        //返回一个包含所有对象的指定类型的数组
	        urls = tmpMap.keySet().toArray(new String[1]);
			
			for(String url:urls){
				if(url.indexOf("mmbiz.qpic.cn")!=-1){
					continue;
				}
				String relativeImgurl =url.replace(CommonWeixinProperties.domain,"");
				String newUrl = null;
				try {
					newUrl = downloadImageFromPC(url);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				content = content.replace(url, CommonWeixinProperties.domain + "tzbwx" + newUrl);
			}
		}
		
		weixinNewsitem.setContent(content);
		
		//检查今天是否有图文素材
		List<WeixinNewstemplate> newsTemplsteList = weixinNewstemplateDao.queryFromCurrentDay();
		if (newsTemplsteList != null && newsTemplsteList.size() > 0) {
			weixinNewstemplate = newsTemplsteList.get(0);
		}
		
		saveWeixinNewstemplate(weixinNewstemplate, weixinNewsitem);
	}
	
	@Transactional(rollbackFor = {Exception.class})
	private void saveWeixinNewstemplate(WeixinNewstemplate weixinNewstemplate, WeixinNewsitem weixinNewsitem) {
		if (weixinNewstemplate.getId() == null) {
			weixinNewstemplateDao.insert(weixinNewstemplate);
		}
		
		weixinNewsitem.setNewstemplateId(weixinNewstemplate.getId());
		
		List<WeixinNewsitem> newsitemList = weixinNewsitemDao.
				queryByNewstemplateIdAndArticleId(weixinNewstemplate.getId(), weixinNewsitem.getArticleId());
		if (newsitemList != null && newsitemList.size() > 0) {
			weixinNewsitemDao.update(weixinNewsitem);
		} else {
			weixinNewsitemDao.insert(weixinNewsitem);
		}
	}
	
}

