//package com.xuecheng.content.config;
//
//import org.redisson.Redisson;
//import org.redisson.api.RedissonClient;
//import org.redisson.codec.JsonJacksonCodec;
//import org.redisson.config.SingleServerConfig;
//import org.springframework.context.annotation.Bean;
//
///**
// * @author mechrev
// * @ClassName RedissonConfig
// * @description: TODO
// * @date 2023年02月16日
// * @version: 1.0
// */
//public class RedissonConfig {
//
//    @Bean
//    public RedissonClient redissonClient(){
//        Config conf = new Config();
//        //单节点模式
//        SingleServerConfig singleServerConfig = conf.useSingleServer();
//        String property = environment.getProperty("redisson.host.config");
//        //设置连接地址：redis://127.0.0.1:6379
//        singleServerConfig.setAddress(property);
//        //设置连接密码
//        singleServerConfig.setPassword(environment.getProperty("redisson.host.password"));
//        //使用json序列化方式
//        Codec codec = new JsonJacksonCodec();
//        conf.setCodec(codec);
//        RedissonClient redissonClient = Redisson.create(conf);
//        return redissonClient;
//    }
//}
