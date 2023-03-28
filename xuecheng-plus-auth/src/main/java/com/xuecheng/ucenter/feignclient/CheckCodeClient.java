package com.xuecheng.ucenter.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author mechrev
 * @ClassName CheckCodeClient
 * @description: TODO
 * @date 2023年02月05日
 * @version: 1.0
 */
@FeignClient(value = "checkcode")
public interface CheckCodeClient {
//    @PostMapping(value = "/verify")
//    public Boolean verify(@RequestParam("key") String key, @RequestParam("code")String code);


    @PostMapping(value = "/checkcode/verify")
    public Boolean verify(@RequestParam("key") String key, @RequestParam("code")String code);

}
