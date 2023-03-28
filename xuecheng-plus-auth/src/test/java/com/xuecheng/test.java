package com.xuecheng;

import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.po.XcUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author mechrev
 * @ClassName test
 * @description: TODO
 * @date 2023年02月05日
 * @version: 1.0
 */
@SpringBootTest
public class test {

    @Autowired
    XcUserMapper xcUserMapper;

    @Test
    public void test(){
        XcUser xcUser = new XcUser();
        xcUser.setUsername("zcx");
        xcUser.setPassword("413214");
        xcUser.setName("z");
        xcUser.setUtype("101001");
        xcUser.setStatus("1");
        java.util.Date day=new Date();
        SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(sdf);

        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);
    }

}
