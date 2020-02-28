package com.atguigu.gmall.interceptor;

import com.atguigu.gmall.annotation.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 拦截器
 */
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {


        //该拦截器作用是，对标注有特定注解的方法进行拦截
        //获取要处理的方法
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        //获取标注该方法上的注解
        LoginRequired loginRequired = handlerMethod.getMethodAnnotation(LoginRequired.class);

        //判断，如果loginRequired为null，说明该方法上面没有标注LoginRequired注解，直接放行即可
        if (loginRequired == null) {
            return true;
        }

        //走到这一步说明要处理的方法上面都是标注了LoginRequired注解，需对其进行拦截
        //获取一下浏览器中cookie里面的token，也是判断用户是否登录过的凭证
        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
        //获取请求中携带的newToken参数
        String newToken = request.getParameter("newToken");
        //声明一个空的token
        String token = "";

        //如果oldToken为空，说明用户是第一次登录，或者cookie已经过期了
        if (!StringUtils.isEmpty(oldToken)) {
            //说明oldToken不为空，也就是说用户已经登录过了，直接把oldToken赋给当前token进行验证
            token = oldToken;
        }

        //走到这，如果newToken为空，说明用户压根没登录过
        if (!StringUtils.isEmpty(newToken)) {
            //说明newToken不为空，也就是用户第一次登录，直接把newToken赋给当前token进行验证
            token = newToken;
        }

        //判断一下当前token是否为空
        if (!StringUtils.isEmpty(token)) {
            //说明用户已经登录过了
            //获取用户浏览器的ip
            String remoteAddr = request.getRemoteAddr();

            //token验证方案一：去中心化，给服务器一个验证算法进行独立验证
            //JwtUtil需要三个参数 key token salt
            //key可以是服务器秘钥  token是签名  salt盐值可以是浏览器ip+浏览器名称
            String atguiguKey = "atguigugmall0722";
            //验证后，相当于解算法，返回一个map
            Map decodeResult = JwtUtil.decode(atguiguKey, token, remoteAddr);
            //如果decodeResult不为空，说明解密验证成功，往cookie里面存入信息；
            //反之如果decodeResult为空说明解密失败，会抛出一个异常
            if (decodeResult != null) {
                //解密验证成功，取出用户信息，放在请求域中
                String userId = (String) decodeResult.get("userId");
                request.setAttribute("userId",userId);

                //将信息写入cookie
                CookieUtil.setCookie(request,response,"oldToken",token,60*60*24,true);
                //放行
                return true;
            }

            /*//token验证方案二：中心认证，送去校验中心进行认证，并获取认证返回结果
            String returnMap = HttpclientUtil.doGet("http://possport.gmall.com:8086/verify?token=" + token + "&currentIp=" + remoteAddr);
            //将returnMap转换成map方便取出里面携带的内容
            Map<String,String> map = new HashMap<>();
            Map<String,String> resultMap = JSON.parseObject(returnMap, map.getClass());

            //取出map里面的内容，即认证返回的结果
            String success = resultMap.get("success");
            String userId = resultMap.get("userId");
            //判断返回结果
            if ((!StringUtils.isEmpty(success)) && "success".equals(success)) {
                //将userId写入request
                request.setAttribute("userId",userId);
                //写入cookie
                CookieUtil.setCookie(request,response,"oldToken",token,60*60*24,true);
                //放行
                return true;
            }*/
        }

        //到这一步说明token为空，也意味着用户没有登录，但是购物车功能还是让用的
        if (loginRequired.isNeededSuccess()) {
            //说明需要验证且需要验证成功，才能放行，
            //但是用户并没有登录，所以给他踢到index，并且给他一个ReturnUrl
            StringBuffer requestURL = request.getRequestURL();
            response.sendRedirect("http://passport.gmall.com:8086/index?ReturnUrl="+requestURL);
            //拒绝放行
            return false;
        }
        //放行
        return true;
    }
}
