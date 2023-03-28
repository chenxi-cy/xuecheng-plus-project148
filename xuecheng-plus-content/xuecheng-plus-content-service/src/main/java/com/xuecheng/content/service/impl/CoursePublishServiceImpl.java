package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.utils.StringUtils;
import com.baomidou.mybatisplus.extension.api.R;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.feignclient.model.CourseIndex;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.content.service.jobhandler.CoursePublishTask;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author mechrev
 * @ClassName CoursePublishServiceImpl
 * @description: TODO
 * @date 2023年01月30日
 * @version: 1.0
 */
@Service
@Slf4j
public class CoursePublishServiceImpl implements CoursePublishService {


    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Autowired
    TeachplanService teachplanService;

    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        //课程基本信息、营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);

        //课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);

        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplanTree);
        return coursePreviewDto;
    }

    @Resource
    CourseBaseMapper courseBaseMapper;

    @Resource
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;

    @Override
    public void commitAudit(Long companyId, Long courseId) {
        //约束校验
        CourseBase courseBase = courseBaseMapper.selectById(courseId);

        //课程审核状态
        String auditStatus = courseBase.getAuditStatus();

        //当前审核状态为已提交不允许再次提交
        if ("202003".equals(auditStatus)) {
            XueChengPlusException.cast("当前为等待审核状态，审核完成可以再次提交。");
        }

        //本机构只允许提交本机构的课程
        if (!courseBase.getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("不允许提交其它机构的课程。");
        }

        //课程图片是否填写
        if (StringUtils.isEmpty(courseBase.getPic())) {
            XueChengPlusException.cast("提交失败，请上传课程图片");
        }

        //添加课程预发布记录
        CoursePublishPre coursePublishPre = new CoursePublishPre();

        //课程基本信息加部分营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        BeanUtils.copyProperties(courseBaseInfo, coursePublishPre);

        //课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);

        //转为json
        String courseMarketJson = JSON.toJSONString(courseMarket);
        //将课程营销信息json数据放入课程预发布表
        coursePublishPre.setMarket(courseMarketJson);

        //查询课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if (teachplanTree.size() <= 0) {
            XueChengPlusException.cast("提交失败，还没有添加课程计划");
        }
        //转json
        String teachplanTreeString = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachplanTreeString);

        //设置预发布记录状态,已提交
        coursePublishPre.setStatus("202003");

        //教学机构id
        coursePublishPre.setCompanyId(companyId);
        //提交时间
        coursePublishPre.setCreateDate(LocalDateTime.now());
        CoursePublishPre coursePublishPreUpdate = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPreUpdate == null) {
            //添加课程预发布记录
            coursePublishPreMapper.insert(coursePublishPre);
        } else {
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        //更新课程基本表的审核状态
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
    }

    @Transactional
    @Override
    public void publish(Long companyId, Long courseId) {

        //约束校验
        //查询课程预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            XueChengPlusException.cast("请先提交课程审核，审核通过才可以发布");
        }
        //本机构只允许提交本机构的课程
        if (!coursePublishPre.getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("不允许提交其它机构的课程。");
        }


        //课程审核状态
        String auditStatus = coursePublishPre.getStatus();
        //审核通过方可发布
        if (!"202004".equals(auditStatus)) {
            XueChengPlusException.cast("操作失败，课程审核通过方可发布。");
        }

        //保存课程发布信息
        saveCoursePublish(courseId);

        //保存消息表
        saveCoursePublishMessage(courseId);

        //删除课程预发布表对应记录
        coursePublishPreMapper.deleteById(courseId);

    }

    @Autowired
    CoursePublishService coursePublishService;

    @Override
    public File generateCourseHtml(Long courseId) {

        try {
            //配置freemarker
            Configuration configuration = new Configuration(Configuration.getVersion());

            //加载模板
            //选指定模板路径,classpath下templates下
            //得到classpath路径
            String classpath = this.getClass().getResource("/").getPath();
            configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
            //设置字符编码
            configuration.setDefaultEncoding("utf-8");

            //指定模板文件名称
            Template template = configuration.getTemplate("course_template.ftl");

            //准备数据
            CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(courseId);

            Map<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewInfo);

            //静态化
            //参数1：模板，参数2：数据模型
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            System.out.println(content);
            //将静态化内容输出到文件中
            InputStream inputStream = IOUtils.toInputStream(content);

            File course = File.createTempFile("course", ".html");
            log.debug("课程静态化，生成静态文件:{}", course.getAbsolutePath());

            //输出流
            FileOutputStream outputStream = new FileOutputStream(course);
            IOUtils.copy(inputStream, outputStream);
            return course;
        } catch (Exception e) {
            return null;
        }

    }


    @Autowired
    MediaServiceClient mediaServiceClient;

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String course = mediaServiceClient.uploadFile(multipartFile, "course", courseId + ".html");
        if (course == null) {
            XueChengPlusException.cast("远程调用媒资服务上传文件失败");
        }
    }

    @Autowired
    SearchServiceClient searchServiceClient;

    @Autowired
    CoursePublishMapper publishMapper;

    @Override
    public Boolean saveCourseIndex(Long courseId) {

        CoursePublish coursePublish = publishMapper.selectById(courseId);

        CourseIndex index = new CourseIndex();

        BeanUtils.copyProperties(coursePublish, index);

        Boolean add = searchServiceClient.add(index);

        if (!add) {
            XueChengPlusException.cast("创建索引失败");
        }
        return add;
    }

    @Resource
    CoursePublishMapper coursePublishMapper;

    /**
     * @param courseId 课程id
     * @return void
     * @description 保存课程发布信息
     * @author Mr.M
     * @date 2022/9/20 16:32
     */
    private void saveCoursePublish(Long courseId) {
        //整合课程发布信息
        //查询课程预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            XueChengPlusException.cast("课程预发布数据为空");
        }

        CoursePublish coursePublish = new CoursePublish();

        //拷贝到课程发布对象
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        coursePublish.setStatus("203002");
        CoursePublish coursePublishUpdate = coursePublishMapper.selectById(courseId);
        if (coursePublishUpdate == null) {
            coursePublishMapper.insert(coursePublish);
        } else {
            coursePublishMapper.updateById(coursePublish);
        }
        //更新课程基本表的发布状态
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setStatus("203002");
        courseBaseMapper.updateById(courseBase);

    }


    @Autowired
    MqMessageService messageService;

    /**
     * @param courseId 课程id
     * @return void
     * @description 保存消息表记录
     * @author Mr.M
     * @date 2022/9/20 16:32
     */
    private void saveCoursePublishMessage(Long courseId) {
        MqMessage mqMessage = messageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if (mqMessage == null) {
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
    }


    public CoursePublish getCoursePublish(Long courseId) {
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);

        return coursePublish;
    }


    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Override
    public CoursePublish getCoursePublishCache(Long courseId) {
        //查询缓存
        String jsonString = (String) redisTemplate.opsForValue().get("course_" + courseId);
        if (org.apache.commons.lang.StringUtils.isNotEmpty(jsonString)) {
            if(jsonString.equals("null")){
                 return null;
                 }
             CoursePublish coursePublish = JSON.parseObject(jsonString, CoursePublish.class);
             return coursePublish;
        } else {
            RLock lock = redissonClient.getLock("courseQuery:" + courseId);
            lock.lock();

            try {
//                try {
//                    Thread.sleep(350000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                jsonString = (String) redisTemplate.opsForValue().get("course_" + courseId);
                if (org.apache.commons.lang.StringUtils.isNotEmpty(jsonString)) {
                    CoursePublish coursePublish = JSON.parseObject(jsonString, CoursePublish.class);
                    return coursePublish;
                }
                System.out.println("从数据库查询...");
//从数据库查询
                CoursePublish coursePublish = getCoursePublish(courseId);
//                if (coursePublish != null) {
                redisTemplate.opsForValue().set("course:" + courseId,JSON.toJSONString(coursePublish),300, TimeUnit.SECONDS);

//                }
                return coursePublish;

            } finally {
                System.out.println("释放锁..." + Thread.currentThread().getId());
                lock.unlock();
            }


        }
    }

}


