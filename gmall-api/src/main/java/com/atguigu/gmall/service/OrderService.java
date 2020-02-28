package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsOrder;

public interface OrderService {
    String getTradeCode(String userId);

    boolean checkTradeCode(String userId,String tradeCode);

    void saveOrder(OmsOrder order);

    OmsOrder getOrderByOutTradeNo(String outTradeNo);

    void updateOrder(OmsOrder omsOrder);

    void sendOrderPaySuccessQueue(OmsOrder omsOrder);
}
