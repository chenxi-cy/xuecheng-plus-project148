package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.naming.pojo.healthcheck.impl.Http;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * @author mechrev
 * @ClassName WxAuthServiceImpl
 * @description: TODO
 * @date 2023年02月05日
 * @version: 1.0
 */
@Slf4j
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService {


    @Autowired
    XcUserMapper xcUserMapper;


    public XcUser wxAuth(String code) {
        //获取access_token
        Map<String, String> access_token_map = getAccess_token(code);

        if (access_token_map == null) {
            return null;
        }
        System.out.println(access_token_map);
        String openid = access_token_map.get("openid");
        String access_token = access_token_map.get("access_token");
        //获取用户信息
        //拿access_token查询用户信息
        Map<String, String> userinfo = getUserinfo(access_token, openid);

        if (userinfo == null) {
            return null;
        }

        //添加用户到数据库
        XcUser xcUser = currentProxy.addWxUser(userinfo);
        return xcUser;
    }

    @Autowired
    WxAuthServiceImpl currentProxy;

    @Transactional
    public XcUser addWxUser(Map userInfo_map) {
        String unionid = (String) userInfo_map.get("unionid");
//根据unionid查询数据库
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        if (xcUser != null) {
            return xcUser;
        }
        String userId = UUID.randomUUID().toString();
        xcUser = new XcUser();
        xcUser.setId(userId);
        xcUser.setWxUnionid(unionid);
        //记录从微信得到的昵称
        xcUser.setNickname(userInfo_map.get("nickname").toString());
        xcUser.setUserpic(userInfo_map.get("headimgurl").toString());
        xcUser.setName(userInfo_map.get("nickname").toString());
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setUtype("101001");//学生类型
        xcUser.setStatus("1");//用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(userId);
        xcUserRole.setRoleId("17");//学生角色
        xcUserRoleMapper.insert(xcUserRole);
        return xcUser;
    }

    @Autowired
    XcUserRoleMapper xcUserRoleMapper;


    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //获取账号
        String username = authParamsDto.getUsername();

        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if (xcUser == null) {
            throw new RuntimeException("用户不存在");
        }


        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser, xcUserExt);

        return xcUserExt;
    }


    @Value("${weixin.appid}")
    String appid;
    @Value("${weixin.secret}")
    String secret;
    @Autowired
    RestTemplate restTemplate;


    private Map<String, String> getAccess_token(String code) {
        String wxUrl_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";

        //请求微信地址
        String wxUrl = String.format(wxUrl_template, appid, secret, code);
        log.info("调用微信接口申请access_token, url:{}", wxUrl);
        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);
        String result = exchange.getBody();
        Map map = JSON.parseObject(result, Map.class);
        return map;
    }

   // https://open.weixin.qq.com/connect/qrconnect?appid=wx17655f8047b85150&redirect_uri=http://vi7gmu.natappfree.cc/auth/wxLogin&response_type=code&scope=snsapi_login&state=snsapi_login#wechat_redirect
    private Map<String, String> getUserinfo(String access_token, String openid) {
        String wxUrl_template = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        //请求微信地址
        String wxUrl = String.format(wxUrl_template, access_token, openid);
        log.info("调用微信接口申请access_token, url:{}", wxUrl);
        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);
        String result = exchange.getBody();
        log.info("调用微信接口申请access_token: 返回值:{}", result);
        Map<String, String> resultMap = JSON.parseObject(result, Map.class);

        return resultMap;
    }


}
