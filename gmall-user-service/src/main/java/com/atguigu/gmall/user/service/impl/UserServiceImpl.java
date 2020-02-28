package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UmsMemberMapper;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import javax.jms.*;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UmsMemberMapper umsMemberMapper;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;


    @Override
    public void saveUser(UmsMember umsMember) {
        umsMemberMapper.insertSelective(umsMember);
    }

    @Override
    public UmsMember isOuserExists(String uid) {
        UmsMember umsMember = new UmsMember();
        umsMember.setSourceUid(uid);
        UmsMember umsMemberResult = umsMemberMapper.selectOne(umsMember);
        return umsMemberResult;
    }

    @Override
    public UmsMember saveOuser(UmsMember umsMember) {
        umsMemberMapper.insertSelective(umsMember);
        return umsMember;
    }

    @Override
    public void putTokenCache(String umsMemberId, String token) {
        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();
            jedis.setex("user:" + token + ":token", 60 * 60, umsMemberId);
        } catch (Exception e) {
            //logService.addExLog(e);
        } finally {
            jedis.close();
        }
    }

    @Override
    public List<UmsMemberReceiveAddress> getUserAddressByUserId(String userId) {
        UmsMemberReceiveAddress umsMemberReceiveAddressForExample = new UmsMemberReceiveAddress();
        umsMemberReceiveAddressForExample.setMemberId(userId);
        List<UmsMemberReceiveAddress> memberReceiveAddressList = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddressForExample);
        return memberReceiveAddressList;
    }

    @Override
    public UmsMemberReceiveAddress getUserAddressByAddressId(String addressId) {
        UmsMemberReceiveAddress umsMemberReceiveAddressExample = new UmsMemberReceiveAddress();
        umsMemberReceiveAddressExample .setId(addressId);
        UmsMemberReceiveAddress umsMemberReceiveAddressResult = umsMemberReceiveAddressMapper.selectOne(umsMemberReceiveAddressExample );
        return umsMemberReceiveAddressResult;
    }

    @Override
    public UmsMember login(UmsMember umsMember) {
        UmsMember umsMemberForExample = new UmsMember();
        umsMemberForExample.setUsername(umsMember.getUsername());
        umsMemberForExample.setPassword(umsMember.getPassword());
        UmsMember umsMemberResult = umsMemberMapper.selectOne(umsMemberForExample);
        return umsMemberResult;
    }

    @Override
    public void sendLoginSuccessQueue(String userId,String userKey) {
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();

        Connection connection = null;
        Session session = null;

        try {
            connection = connectionFactory.createConnection();
            session = connection.createSession(true,Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue("LOGIN_SUCCESS_QUEUE");
            MessageProducer producer = session.createProducer(queue);

            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("userId",userId);
            mapMessage.setString("userKey",userKey);

            producer.send(mapMessage);

            session.commit();
        } catch (Exception e) {

            try {
                session.rollback();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }

        } finally {

            try {
                session.close();
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }

        }
    }
}
