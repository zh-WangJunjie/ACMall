package com.atguigu.gmall.cart.consumer;

import com.atguigu.gmall.service.CartService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;


@Component
public class CartConsumer {
    @Autowired
    CartService cartService;

    @JmsListener(destination = "LOGIN_SUCCESS_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeLoginSuccessQueue(MapMessage mapMessage) {
        String userId = "";
        String userKey = "";
        try {
           userId = mapMessage.getString("userId");
           userKey = mapMessage.getString("userKey");

           //合并缓存与数据库的购物车
            cartService.combineCartList(userId,userKey);
            cartService.deleteCartsByUserKey(userKey);
        } catch (JMSException e) {
            e.printStackTrace();
        }
        System.out.println("登录成功："+userId);
    }

}
