package com.atguigu.gmall.search.controller;

import com.atguigu.gmall.annotation.LoginRequired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {
    @LoginRequired(isNeededSuccess = false)
    @RequestMapping("index")
    public String index(){
        return "index";
    }

}
