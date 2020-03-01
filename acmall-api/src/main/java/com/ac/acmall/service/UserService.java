package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {
    void saveUser(UmsMember umsMember);

    UmsMember isOuserExists(String uid);

    UmsMember saveOuser(UmsMember umsMember);

    void putTokenCache(String umsMemberId, String token);

    List<UmsMemberReceiveAddress> getUserAddressByUserId(String userId);

    UmsMemberReceiveAddress getUserAddressByAddressId(String addressId);

    UmsMember login(UmsMember umsMember);

    void sendLoginSuccessQueue(String userId, String userKey);
}
