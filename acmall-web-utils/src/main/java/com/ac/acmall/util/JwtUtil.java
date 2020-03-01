package com.atguigu.gmall.util;

import io.jsonwebtoken.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @param
 * @return
 */
public class JwtUtil {

    public static void main(String[] args){

        // 浏览器ip
        String ip = "12312321312321";
        String browserName = "firefox";
        String salt = ip + browserName;
        // 用户信息
        String userId = "1";
        Map<String,String> map = new HashMap<>();
        map.put("userId",userId);
        // 服务器密钥
        String atguiguKey = "atguigugmall0722";

        String encode = encode(atguiguKey, map, salt);
        Map decode = decode(atguiguKey, "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiIxIn0.1KYFrf80FU3knFNjTmcoywcaJNvSvL7wrKInSI2WeR0", salt);
        String userId1 = (String) decode.get("userId");
        System.out.println(encode);
        System.out.println(userId1);

    }


    /***
     * jwt加密
     * @param key
     * @param map
     * @param salt
     * @return
     */
    public static String encode(String key,Map map,String salt){

        if(salt!=null){
            key+=salt;
        }
        JwtBuilder jwtBuilder = Jwts.builder().signWith(SignatureAlgorithm.HS256, key);
        jwtBuilder.addClaims(map);

        String token = jwtBuilder.compact();
        return token;
    }

    /***
     * jwt解密
     * @param key
     * @param token
     * @param salt
     * @return
     * @throws SignatureException
     */
    public static  Map decode(String key,String token,String salt)throws SignatureException{
        if(salt!=null){
            key+=salt;
        }
        Claims map = null;

        map = Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();

        return map;

    }

}
