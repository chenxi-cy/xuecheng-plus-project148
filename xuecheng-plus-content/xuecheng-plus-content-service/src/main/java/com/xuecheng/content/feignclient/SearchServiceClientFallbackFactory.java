package com.xuecheng.content.feignclient;

import com.xuecheng.content.feignclient.model.CourseIndex;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author mechrev
 * @ClassName SearchServiceClientFallbackFactory
 * @description: TODO
 * @date 2023年02月01日
 * @version: 1.0
 */
@Slf4j
@Component
public class SearchServiceClientFallbackFactory implements FallbackFactory<SearchServiceClient> {
    @Override
    public SearchServiceClient create(Throwable cause) {
        return new SearchServiceClient() {
            @Override
            public Boolean add(CourseIndex courseIndex) {
                cause.printStackTrace();
                log.debug("调用搜索发生熔断降级", cause.getMessage());
                return false;
            }
        };
    }
}
