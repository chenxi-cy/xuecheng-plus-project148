package com.xuecheng.learning.feignclient;

import com.xuecheng.content.model.po.CoursePublish;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author mechrev
 * @ClassName ContentServiceClient
 * @description: TODO
 * @date 2023年02月10日
 * @version: 1.0
 */

@RequestMapping("/content")
@FeignClient(value = "content-api",fallbackFactory =ContentServiceClientFallBackFactorty.class )
public interface ContentServiceClient {
    @GetMapping("/r/coursepublish/{courseId}")
    public CoursePublish getCoursepublish(@PathVariable("courseId") Long courseId);

}
