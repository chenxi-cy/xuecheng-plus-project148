package com.xuecheng.content.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author mechrev
 * @ClassName FreemarkerController
 * @description: TODO
 * @date 2023年01月30日
 * @version: 1.0
 */
@Controller
public class FreemarkerController {


    @GetMapping("/testfreemarker")
    public ModelAndView test() {
        ModelAndView view = new ModelAndView();
        view.addObject("name", "赵晨希");
        view.setViewName("test");

        return view;
    }
}
