package com.atguigu.gmall.payment.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.MapMessage;

@Component
public class PaymentConsumer {
    @Reference
    PaymentService paymentService;

    /**
     * 消费支付检查队列
     */
    @JmsListener(destination = "PAY_CHECK_QUEUE" ,containerFactory = "jsmQueueListener")
    public void consumePayCheckQueue(MapMessage mapMessage) {
        try {
            //取出消息队列中携带的信息
            String out_trade_no = mapMessage.getString("out_trade_no");
            long count = mapMessage.getLong("count");
            System.out.println("消费延迟队列，检查支付结果");

            //检查支付状态
            PaymentInfo paymentInfo = paymentService.checkPayStatus(out_trade_no);
            boolean checkResult = paymentService.checkPayStatusFromDB(out_trade_no);
            if (!checkResult) {
                //说明支付成功
                if (paymentInfo.getPaymentStatus() != null && !paymentInfo.getPaymentStatus().equals("WAIT_BUYER_PAY")) {

                    System.out.println("支付完成");
                    //更新支付信息
                    paymentService.updatePayment(paymentInfo);
                    //发送系统消息给订单，更新订单信息（已支付）
                    paymentService.sendPaySuccessQueue(paymentInfo);
                }
            } else {
                //说明没有支付
                //再次发送检查的延迟队列
                if (count > 0) {
                    System.out.println("剩余检查次数："+count+">0继续发送延迟任务");
                    count--;
                    paymentInfo.setOrderSn(out_trade_no);
                    paymentService.sendPayStatusCheckQueue(paymentInfo,count);
                } else {
                    System.out.println("剩余检查次数耗尽，结束任务！");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
