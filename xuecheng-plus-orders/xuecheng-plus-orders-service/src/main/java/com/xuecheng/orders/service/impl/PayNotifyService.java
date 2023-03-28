package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.PayNotifyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author mechrev
 * @ClassName PayNotifyService
 * @description: TODO
 * @date 2023年02月13日
 * @version: 1.0
 */
@Slf4j
@Service
public class PayNotifyService {


    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    MqMessageService mqMessageService;

    /**
     * @param message 消息内容
     * @return void
     * @description 发送消息
     * @author Mr.M
     * @date 2022/9/20 9:43
     */
    public void send(MqMessage message) {
        //转json
        String msg = JSON.toJSONString(message);
        // 发送消息
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT, "", msg);
        log.debug("像消息队列发送支付结果消息完成:{}", message);
    }

}
