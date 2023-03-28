package com.xuecheng.learning.feignclient;

import com.xuecheng.content.model.po.CoursePublish;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author mechrev
 * @ClassName ContentServiceClientFallBackFactorty
 * @description: TODO
 * @date 2023年02月10日
 * @version: 1.0
 */
@Slf4j
@Component
public class ContentServiceClientFallBackFactorty implements FallbackFactory<ContentServiceClient> {

    @Override
    public ContentServiceClient create(Throwable cause) {
        return new ContentServiceClient() {
            @Override
            public CoursePublish getCoursepublish(Long courseId) {
                log.debug("调用内容管理服务，发生熔断降级", cause.getMessage());
                return null;
            }
        };
    }
}
