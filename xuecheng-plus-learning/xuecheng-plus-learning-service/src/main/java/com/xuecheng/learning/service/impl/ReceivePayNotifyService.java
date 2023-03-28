package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author mechrev
 * @ClassName ReceivePayNotifyService
 * @description: TODO
 * @date 2023年02月13日
 * @version: 1.0
 */
@Slf4j
@Service
public class ReceivePayNotifyService {

    @Autowired
    XcChooseCourseMapper chooseCourseMapper;

    @Autowired
    MyCourseTablesService myCourseTablesService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = {PayNotifyConfig.PAYNOTIFY_QUEUE})
    public void receive(String message) {
        MqMessage mqMessage = JSON.parseObject(message, MqMessage.class);

        //判断是否是自己的消息
        String messageType = mqMessage.getMessageType();
        //记录订单类型
        String businessKey2 = mqMessage.getBusinessKey2();

        if (PayNotifyConfig.MESSAGE_TYPE.equals(messageType) && "60201".equals(businessKey2)) {
            String businessKey1 = mqMessage.getBusinessKey1();
            XcChooseCourse xcChooseCourse = chooseCourseMapper.selectById(businessKey1);
            if (xcChooseCourse == null) {
                log.info("收到结果通知，查询不到记录,businessKey1:{}", businessKey1);
                return;
            }
            XcChooseCourse xcChooseCourse_update = new XcChooseCourse();
            xcChooseCourse.setStatus("701001");//选课成功
            chooseCourseMapper.update(xcChooseCourse_update, new LambdaQueryWrapper<XcChooseCourse>().eq(XcChooseCourse::getId, businessKey1));


            xcChooseCourse= chooseCourseMapper.selectById(businessKey1);
            //向我的课程表添加课程
            myCourseTablesService.addCourseTabls(xcChooseCourse);

            send(mqMessage);

        }
    }


    /**
     * @description 回复消息
     * @param message  回复消息
     * @return void
     * @author Mr.M
     * @date 2022/9/20 9:43
     */
    public void send(MqMessage message){
        //转json
        String msg = JSON.toJSONString(message);
        // 发送消息
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_REPLY_QUEUE, msg);
        log.debug("学习中心服务向订单服务回复消息:{}",message);
    }

}
