package com.xuecheng.orders.jobhandler;

import com.alibaba.fastjson.JSON;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.service.impl.PayNotifyService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mechrev
 * @ClassName PayNotifyTask
 * @description: TODO
 * @date 2023年02月13日
 * @version: 1.0
 */
@Slf4j
@Component
public class PayNotifyTask extends MessageProcessAbstract {

    //支付结果通知消息类型
    public static final String MESSAGE_TYPE = "payresult_notify";


    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    PayNotifyService payNotifyService;




    //任务调度入口
    @XxlJob("NotifyPayResultJobHandler")
    public void notifyPayResultJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex="+shardIndex+",shardTotal="+shardTotal);
        process(shardIndex,shardTotal,MESSAGE_TYPE,100,60);
    }


    @Override
    public boolean execute(MqMessage mqMessage) {
        log.debug("开始进行支付结果通知:{}",mqMessage.toString());
        //发送到消息队列
        payNotifyService.send(mqMessage);

        //由于消息表的记录需要等到订单服务收到回复后才能删除，这里返回false不让消息sdk自动删除
        return false;
    }



    //接收回复
    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_REPLY_QUEUE)
    public void receive(String message) {
        //获取消息
        MqMessage mqMessage = JSON.parseObject(message, MqMessage.class);
        log.debug("接收支付结果回复:{}", mqMessage);

        //完成支付通知
        mqMessageService.completed(mqMessage.getId());
    }



}
