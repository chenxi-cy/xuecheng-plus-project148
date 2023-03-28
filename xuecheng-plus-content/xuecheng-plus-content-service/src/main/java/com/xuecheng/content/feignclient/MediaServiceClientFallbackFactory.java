package com.xuecheng.content.feignclient;


import com.xuecheng.content.feignclient.MediaServiceClient;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author mechrev
 * @ClassName MediaServiceClientFallbackFactory
 * @description: TODO
 * @date 2023年02月01日
 * @version: 1.0
 */

@Slf4j
@Component
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {
    @Override
    public MediaServiceClient create(Throwable cause) {
        return new MediaServiceClient() {
            @Override
            public String uploadFile(MultipartFile upload, String folder, String objectName) {
                log.error("远程调用媒资管理服务熔断异常：{}", cause.getMessage());
                return null;
            }
        };

    }
}
