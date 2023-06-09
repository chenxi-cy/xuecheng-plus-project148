package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @author mechrev
 * @ClassName CoursePublishTask
 * @description: TODO
 * @date 2023年01月31日
 * @version: 1.0
 */

@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex="+shardIndex+",shardTotal="+shardTotal);
        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex,shardTotal,"course_publish",5,60);
    }


    @Override
    public boolean execute(MqMessage mqMessage) {
//        //获取消息相关的业务信息
        log.debug("开始执行ke'成任务：{}",mqMessage.getBusinessKey1());

        String businessKey1 = mqMessage.getBusinessKey1();
        long courseId = Integer.parseInt(businessKey1);
//        //课程静态化
        generateCourseHtml(mqMessage,courseId);
//        //课程缓存
//        saveCourseCache(mqMessage,courseId);
//        //课程索引
        saveCourseIndex(mqMessage,courseId);

        return true;
    }

    @Autowired
    CoursePublishService coursePublishService;

    //生成课程静态化页面并上传至文件系统
    public void saveCourseIndex(MqMessage mqMessage,long courseId){

        log.debug("开始进行课程静态化,课程id:{}",courseId);
        //消息id
        Long id = mqMessage.getId();
        //消息处理的service
        MqMessageService mqMessageService = this.getMqMessageService();
        //消息幂等性处理
        int stageTwo = mqMessageService.getStageTwo(id);
        if(stageTwo>0){
            log.debug("当前阶段是创建课程索引,任务已经完成不再处理,任务信息:{}",mqMessage);
            return ;
        }


        coursePublishService.saveCourseIndex(courseId);




        //保存第一阶段状态
        mqMessageService.completedStageTwo(id);  //完成第二个阶段

    }
 //生成课程静态化页面并上传至文件系统
    public void generateCourseHtml(MqMessage mqMessage,long courseId){

        log.debug("开始进行课程静态化,课程id:{}",courseId);
        //消息id
        Long id = mqMessage.getId();
        //消息处理的service
        MqMessageService mqMessageService = this.getMqMessageService();
        //消息幂等性处理
        int stageOne = mqMessageService.getStageOne(id);
        if(stageOne>0){
            log.debug("当前阶段是静态化课程信息任务已经完成不再处理,任务信息:{}",mqMessage);
            return ;
        }

        //生成静态化页面
        File file = coursePublishService.generateCourseHtml(courseId);
        if(file == null){
            XueChengPlusException.cast("课程静态化异常");
        }
        //上传静态化页面
        coursePublishService.uploadCourseHtml(courseId,file);
        //保存第一阶段状态
        mqMessageService.completedStageOne(id);

    }



}
