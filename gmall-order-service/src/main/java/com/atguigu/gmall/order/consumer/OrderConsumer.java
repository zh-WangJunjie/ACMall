package com.atguigu.gmall.order.consumer;

import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.MapMessage;
import java.util.Date;

@Component
public class OrderConsumer {
    @Autowired
    OrderService orderService;

    /**
     * consumer端以监听器机制向消息队列中主动监听producer消息，
     * 然后把这个消息拉下来进行消费响应
     * @param mapMessage
     */
    @JmsListener(destination = "PAY_SUCCESS_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaySuccessQueue(MapMessage mapMessage) {
        String out_trade_no = "";
        String status = "";
        try {
            //将消息队列中的producer发送的信息提出来
            out_trade_no = mapMessage.getString("out_trade_no");
            status = mapMessage.getString("status");

            //封装要更新的订单信息
            OmsOrder omsOrder = new OmsOrder();
            omsOrder.setOrderSn(out_trade_no);
            omsOrder.setStatus(status);
            omsOrder.setPaymentTime(new Date());

            //更新订单信息
            orderService.updateOrder(omsOrder);

            //发送订单已支付的队列，由库存系统消费
            orderService.sendOrderPaySuccessQueue(omsOrder);
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.out.println("订单消费支付成功队列：" + out_trade_no);
    }
}
