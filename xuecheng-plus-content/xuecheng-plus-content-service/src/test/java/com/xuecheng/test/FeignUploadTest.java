package com.xuecheng.test;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @author mechrev
 * @ClassName FeignUploadTest
 * @description: TODO
 * @date 2023年02月01日
 * @version: 1.0
 */
@SpringBootTest
public class FeignUploadTest {

    @Autowired
    MediaServiceClient mediaServiceClient;

@Test
    public void test1(){
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(new File("D:\\develop\\test.html"));
        String course = mediaServiceClient.uploadFile(multipartFile, "course", "test.html");
        System.out.println(course);
    }


}
