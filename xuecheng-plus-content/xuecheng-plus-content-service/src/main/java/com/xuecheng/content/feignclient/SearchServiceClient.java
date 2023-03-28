package com.xuecheng.content.feignclient;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.model.CourseIndex;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author mechrev
 * @ClassName MediaServiceClient
 * @description: TODO
 * @date 2023年02月01日
 * @version: 1.0
 */
@FeignClient(value = "search", fallbackFactory = SearchServiceClientFallbackFactory.class)
@RequestMapping("/search")
public interface SearchServiceClient {

    @PostMapping("/index/course")
    public Boolean add(@RequestBody CourseIndex courseIndex);

}
