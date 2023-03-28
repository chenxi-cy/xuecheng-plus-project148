package com.xuecheng.learning.feignclient;

import com.xuecheng.base.model.RestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author mechrev
 * @ClassName MediaServiceClient
 * @description: TODO
 * @date 2023年02月15日
 * @version: 1.0
 */
@RequestMapping("/media")
@FeignClient(value = "media-api",fallbackFactory = MediaServiceClientFallbackFactory.class)
public interface MediaServiceClient {
    @GetMapping("/open/preview/{mediaId}")
    public RestResponse<String> getPlayUrlByMediaId(@PathVariable("mediaId") String mediaId);
}
