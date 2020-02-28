package com.atguigu.gmall.passport.controller;

import com.atguigu.gmall.util.HttpclientUtil;

import java.util.HashMap;
import java.util.Map;

public class TextSina {
    public static void main(String[] args) {

        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("client_id","2204542556");
        paramMap.put("client_secret","be0eddbac17f83adcd66bd129504d052");
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri","http://passport.gmall.com:8086/vlogin");
        paramMap.put("code","f3e6c5e89d3cf80148102afbd74144c7");

        String result = HttpclientUtil.doPost("https://api.weibo.com/oauth2/access_token", paramMap);

        System.out.println(result);

    }
}
