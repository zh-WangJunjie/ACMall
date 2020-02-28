package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    PaymentInfoMapper paymentInfoMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;
    @Autowired
    AlipayClient alipayClient;
    /**
     * 添加支付信息
     * @param payment
     */
    @Override
    public void addPayment(PaymentInfo payment) {
        paymentInfoMapper.insertSelective(payment);
    }

    /**
     * 更新支付信息
     * @param paymentInfo
     * 根据orderSn更新支付信息
     */
    @Override
    public void updatePayment(PaymentInfo paymentInfo) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderSn",paymentInfo.getOrderSn());
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }

    /**
     * 发送信息队列，更新订单信息（已支付）
     * @param paymentInfo
     */
    @Override
    public void sendPaySuccessQueue(PaymentInfo paymentInfo) {
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection connection = null;
        Session session = null;
        try {
            //创建连接，并启动
            connection = connectionFactory.createConnection();
            connection.start();

            //创建会话，并开启事务
            session = connection.createSession(true,Session.SESSION_TRANSACTED);

            //创建消息队列
            Queue queue = session.createQueue("PAY_SUCCESS_QUEUE");

            //创建消息的发送者producer
            MessageProducer producer = session.createProducer(queue);

            //发送一个消息，通知系统outTradeNo订单已经支付成功
            //创建一个消息
            TextMessage textMessage = new ActiveMQTextMessage();
            //设置该条消息要传输的信息
            textMessage.setText(paymentInfo.getOrderSn());

            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("out_trade_no",paymentInfo.getOrderSn());
            mapMessage.setString("status","1");
            producer.send(mapMessage);
            //提交
            session.commit();

        } catch (Exception e) {
            //回滚
            try {
                session.rollback();
            } catch (Exception e1) {
                e1.printStackTrace();
            }

        } finally {
            try {
                //关闭会话与连接
                session.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 发送支付状态延迟检查队列
     * @param payment
     * @param count
     */
    @Override
    public void sendPayStatusCheckQueue(PaymentInfo payment, long count) {
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection connection = null;
        Session session = null;
        try {
            //创建连接
            connection = connectionFactory.createConnection();
            //创建会话，并开启事务
            session = connection.createSession(true,Session.SESSION_TRANSACTED);
            //创建消息队列
            Queue queue = session.createQueue("PAY_CHECK_QUEUE");
            //创建producer，发送消息
            MessageProducer producer = session.createProducer(queue);

            //发送消息
            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("out_trade_no",payment.getOrderSn());
            mapMessage.setLong("count",count);
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,30 * 1000);
            producer.send(mapMessage);
            //提交
            session.commit();

        } catch (Exception e) {
            //异常回滚
            try {
                session.rollback();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                //关闭会话与连接
                session.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("发送检查支付结果的延迟队列");
    }

    /**
     * 调用支付宝的接口检查支付状态
     * @param out_trade_no
     * @return
     */
    @Override
    public PaymentInfo checkPayStatus(String out_trade_no) {
        //调用支付宝的接口检查支付状态
        System.out.println("调用支付宝的接口检查支付状态");

        //
        AlipayTradeQueryRequest queryRequest = new AlipayTradeQueryRequest();
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("out_trade_no",out_trade_no);

        String json = JSON.toJSONString(paramMap);
        queryRequest.setBizContent(json);
        AlipayTradeQueryResponse payResponse = null;
        try {
            payResponse = alipayClient.execute(queryRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //调用查询接口的返回结果
        if (payResponse.isSuccess()) {
            System.out.println("调用查询接口成功");
            //封装返回结果
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn(out_trade_no);
            paymentInfo.setPaymentStatus(payResponse.getTradeStatus());
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setCallbackContent(payResponse.toString());
            paymentInfo.setAlipayTradeNo(payResponse.getTradeNo());

            return paymentInfo;
        } else {
            //调用失败
            System.out.println("调用查询接口失败");
            //封装返回结果
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn(out_trade_no);
            return paymentInfo;
        }
    }

    /**
     * 检查数据库中的支付状态
     * @param out_trade_no
     * @return
     */
    @Override
    public boolean checkPayStatusFromDB(String out_trade_no) {
        boolean checkResult = true;

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderSn(out_trade_no);
        PaymentInfo paymentInfoResult = paymentInfoMapper.selectOne(paymentInfo);
        if (paymentInfoResult.getPaymentStatus() != null && !paymentInfoResult.getPaymentStatus().equals("已支付") && !paymentInfoResult.getPaymentStatus().equals("TRADE_SUCCESS")) {
            checkResult = false;
        }
        return checkResult;
    }


}
