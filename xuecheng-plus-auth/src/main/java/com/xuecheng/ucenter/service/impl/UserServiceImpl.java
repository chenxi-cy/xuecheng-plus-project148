package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mechrev
 * @ClassName UserServiceImpl
 * @description: TODO
 * @date 2023年02月05日
 * @version: 1.0
 */
@Service
@Slf4j
public class UserServiceImpl implements UserDetailsService {
    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    ApplicationContext applicationContext;

//    @Autowired
//    AuthService authService;


    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {


        AuthParamsDto authParamsDto = null;
        try {
            //将认证参数转为AuthParamsDto类型
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            log.info("认证请求不符合项目要求:{}", s);
            throw new RuntimeException("认证请求数据格式不对");
        }

        //认证方法
        String authType = authParamsDto.getAuthType();

        AuthService authService = applicationContext.getBean(authType + "_authservice", AuthService.class);
        XcUserExt user = authService.execute(authParamsDto);

        return getUserPrincipal(user);


    }

    @Autowired
    XcMenuMapper xcMenuMapper;

    public UserDetails getUserPrincipal(XcUserExt user) {
        String password = user.getPassword();
        //查询用户权限
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(user.getId());
        List<String> permissions = new ArrayList<>();
        if (xcMenus.size() <= 0) {
            permissions.add("p1");
        } else {
            xcMenus.forEach(menu -> {
                permissions.add(menu.getCode());
            });
        }
        user.setPermissions(permissions);
        //为了安全在令牌中不放密码
        user.setPassword(null);
        //将user对象转json
        String userString = JSON.toJSONString(user);
        String[] authorities = permissions.toArray(new String[0]);
        //创建UserDetails对象
        UserDetails userDetails = User.withUsername(userString).password(password).authorities(authorities).build();
        return userDetails;
    }

}



