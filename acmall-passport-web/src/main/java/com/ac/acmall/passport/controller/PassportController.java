package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {
    @Reference
    UserService userService;


    //第三方用户登录
    //携带的参数为code
    @RequestMapping("vlogin")
    public String vlogin(String code,HttpServletRequest request) {
        //用code换取access_token
        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("client_id","2204542556");
        paramMap.put("client_secret","be0eddbac17f83adcd66bd129504d052");
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri","http://passport.gmall.com:8086/vlogin");
        paramMap.put("code",code);

        String result = HttpclientUtil.doPost("https://api.weibo.com/oauth2/access_token", paramMap);
        Map<String,Object> resultMap = new HashMap<>();

        Map<String,Object> map = JSON.parseObject(result, resultMap.getClass());

        if (map != null && map.size()>0){

            //用access_token和uid换取用户信息
            String access_token = (String) map.get("access_token");
            String uid = (String) map.get("uid");
            //在保存第三方用户信息之前，先判断一下该用户是否已经在数据库里面存在了
            UmsMember umsMember = userService.isOuserExists(uid);
            if (umsMember == null) {
                String url = "https://api.weibo.com/2/users/show.json?access_token=" + access_token + "&uid=" + uid;
                String userMessage = HttpclientUtil.doGet(url);
                //保存用户信息
                Map<String,Object> userMap = JSON.parseObject(userMessage, resultMap.getClass());
                String nickname = (String) userMap.get("name");
                String gender = (String) userMap.get("gender");
                System.out.println(userMap);
                umsMember = new UmsMember();
                umsMember.setSourceType("2");//sina用户登录
                umsMember.setSourceUid(uid);
                umsMember.setAccessToken(access_token);
                umsMember.setAccessCode(code);
                umsMember.setNickname(nickname);
                //umsMember.setGender(gender);

                umsMember = userService.saveOuser(umsMember);
            }
            //生成token
            String token = "";
            if (umsMember != null && (!StringUtils.isEmpty(umsMember.getId()))) {
                String umsMemberId = umsMember.getId();
                //校验成功，返回用户凭证token
                String atguigukey = "atguigugmall0722";
                String remoteAddr = request.getRemoteAddr();
                Map<String,String> tokenMap = new HashMap<>();
                tokenMap.put("userId",umsMemberId);
                tokenMap.put("nickname","Jerry");
                token = JwtUtil.encode(atguigukey, tokenMap, remoteAddr);
                //将用户的token放入redis缓存
                userService.putTokenCache(umsMemberId,token);
            }

            //重定向到
            return "redirect:http://search.gmall.com:8083/index?newToken="+token;
        } else {
            //提示错误信息
            return "fail";
        }
    }

    //中心化认证，校验用户凭证
    @RequestMapping("verify")
    public Map<String,String> verify(HttpServletRequest request, String token, String currentIp) {

        //校验用户凭证
        String atguiguKey = "atguigugmall0722";
        //解密
        Map<String ,String> resultMap = JwtUtil.decode(atguiguKey, token, currentIp);
        String userId = resultMap.get("userId");
        Map<String,String> map = new HashMap<>();
        if (resultMap != null) {
            //校验成功返回success已经用户信息
            map.put("success","success");
            map.put("userId",userId);
            return map;
        } else {
            //校验失败
            map.put("success","fail");
            return map;
        }
    }
    
    //用户没有登录时，跳转到本方法，并且携带原来的页面请求url
    //当用户成功登录时，返回到原页面
    @RequestMapping("index")
    public String index(HttpServletRequest request, ModelMap map) {
        String returnUrl = request.getParameter("ReturnUrl");
        map.put("ReturnUrl",returnUrl);
        return "index";
    }

    //登录验证
    @RequestMapping("login")
    @ResponseBody
    public String login(HttpServletRequest request,UmsMember umsMember) {
        //调取userService服务，核对用户信息
        //String userId = "1";
        UmsMember user = userService.login(umsMember);
        String userId = user.getId();

        if (!StringUtils.isEmpty(userId)) {
            //验证成功，返回用户凭证token
            String atguiguKey = "atguigugmall0722";
            String remoteAddr = request.getRemoteAddr();
            String ip = remoteAddr;

            Map<String,String> map = new HashMap<>();
            map.put("userId",userId);

            String token = JwtUtil.encode(atguiguKey, map, ip);

            //将用户的token存入redis缓存
            userService.putTokenCache(userId,token);

            //提取cookie中的userKey
            String userKey = CookieUtil.getCookieValue(request, "user-key", true);

            //发送成功登录的信息
            userService.sendLoginSuccessQueue(userId,userKey);
            return token;

        } else {
            //验证失败
            String fail = "用户名或者密码错误";
            return fail;
        }
    }

}
