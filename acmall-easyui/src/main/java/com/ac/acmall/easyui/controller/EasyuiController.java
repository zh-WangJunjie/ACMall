package com.atguigu.gmall.easyui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class EasyuiController {

    @RequestMapping("index")
    public String index() {

        return "index";
    }
}
