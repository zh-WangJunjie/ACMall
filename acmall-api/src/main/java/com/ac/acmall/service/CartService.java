package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsCartItem;

import java.util.List;

public interface CartService {

    OmsCartItem isCartItemExists(OmsCartItem omsCartItem);

    void updateCart(OmsCartItem omsCartItemFromDB);

    OmsCartItem addCart(OmsCartItem omsCartItem);

    void synchronizeCache(String userId);

    List<OmsCartItem> getCartListByUserIdFromCache(String userId);

    void deleteCarts(List<String> skuIds, String userId);

    void addCartListToCache(String userKey, List<OmsCartItem> omsCartItemList);

    void updateCartListByUserKeyFromCache(String userKey, List<OmsCartItem> omsCartItemListFromCacheByUserKey);

    List<OmsCartItem> getCartListByUserKey(String userKey);

    void deleteCartsByUserKey(String userKey);

    void combineCartList(String userId, String userKey);
}
