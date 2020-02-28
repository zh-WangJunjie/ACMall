package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    OmsOrderMapper omsOrderMapper;
    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;

    /**
     *本方法用于根据userId生成一个tradeCode
     */
    @Override
    public String getTradeCode(String userId) {
        //生成tradeCode
        String tradeCode = UUID.randomUUID().toString();
        //将tradeCode存入redis
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            jedis.setex("userId:"+userId+":tradeCode",60*30,tradeCode);
        } catch (Exception e) {
            //logService.printLog(e,xx,date...);
        } finally {
            jedis.close();
        }
        return tradeCode;
    }

    /**
     *本方法用于检查页面传来的tradeCode与redis里面的tradeCode的一致性
     * 以此可以判断用户要提交的订单是否是第一次提交，且仅允许提交一次
     * 可以有效防止用户对订单的重复提交
     */
    @Override
    public boolean checkTradeCode(String userId,String tradeCode) {
        boolean checkResult = false;
        //根据userId，去redis中提取tradeCode
        Jedis jedis = redisUtil.getJedis();
        String tradeCodeFromCache = jedis.get("userId:" + userId + ":tradeCode");
        if (tradeCode.equals(tradeCodeFromCache)) {
            checkResult = true;
            //交易之后清除tradeCode
            jedis.del("userId:" + userId + ":tradeCode");
        }
        jedis.close();
        return checkResult;
    }

    /**
     *保存订单
     * 同时要给订单列表设置orderId
     */
    @Override
    public void saveOrder(OmsOrder order) {
        omsOrderMapper.insertSelective(order);
        String id = order.getId();
        List<OmsOrderItem> omsOrderItems = order.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(id);
            omsOrderItemMapper.insertSelective(omsOrderItem);
        }
    }

    /**
     *根据外部订单号获取订单数据
     */
    @Override
    public OmsOrder getOrderByOutTradeNo(String outTradeNo) {
        OmsOrder omsOrderForExample = new OmsOrder();
        omsOrderForExample.setOrderSn(outTradeNo);
        OmsOrder omsOrderResult = omsOrderMapper.selectOne(omsOrderForExample);

        OmsOrderItem omsOrderItemForExample = new OmsOrderItem();
        omsOrderItemForExample.setOrderSn(outTradeNo);
        List<OmsOrderItem> omsOrderItemList = omsOrderItemMapper.select(omsOrderItemForExample);

        omsOrderResult.setOmsOrderItems(omsOrderItemList);
        return omsOrderResult;
    }

    /**
     * 更新订单信息
     * @param omsOrder
     */
    @Override
    public void updateOrder(OmsOrder omsOrder) {
        Example example = new Example(OmsOrder.class);
        example.createCriteria().andEqualTo("orderSn",omsOrder.getOrderSn());

        OmsOrder omsOrderForUpdate = new OmsOrder();
        omsOrderForUpdate.setStatus(omsOrder.getStatus());
        omsOrderForUpdate.setPaymentTime(omsOrder.getPaymentTime());

        omsOrderMapper.updateByExampleSelective(omsOrderForUpdate,example);
    }

    /**
     * 发送订单支付成功队列，由库存系统消费
     * @param omsOrder
     */
    @Override
    public void sendOrderPaySuccessQueue(OmsOrder omsOrder) {
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();

        Connection connection = null;
        Session session = null;
        try {
            //创建连接
            connection = connectionFactory.createConnection();
            //创建会话，同时开启事务
            session = connection.createSession(true,Session.SESSION_TRANSACTED);
            //创建消息队列
            Queue queue = session.createQueue("ORDER_SUCCESS_QUEUE");
            //创建producer
            MessageProducer producer = session.createProducer(queue);

            //创建消息
            TextMessage textMessage = new ActiveMQTextMessage();

            //封装要传递的订单信息
            OmsOrder omsOrderForParam = new OmsOrder();
            omsOrderForParam.setOrderSn(omsOrder.getOrderSn());
            OmsOrder omsOrderForResult = omsOrderMapper.selectOne(omsOrderForParam);
            //封装要传递的订单列表信息
            OmsOrderItem omsOrderItemForParam = new OmsOrderItem();
            omsOrderItemForParam.setOrderSn(omsOrder.getOrderSn());
            List<OmsOrderItem> orderItemListForResult = omsOrderItemMapper.select(omsOrderItemForParam);
            omsOrderForResult.setOmsOrderItems(orderItemListForResult);

            //设置要传递的信息
            textMessage.setText(JSON.toJSONString(omsOrderForResult));

            //发送消息
            producer.send(textMessage);

            //提交事务
            session.commit();
        } catch (Exception e) {
            //异常回滚
            try {
                session.rollback();
            } catch (JMSException e1) {
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

}
