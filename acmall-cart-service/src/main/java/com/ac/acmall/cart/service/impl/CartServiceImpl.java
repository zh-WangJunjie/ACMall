package com.atguigu.gmall.cart.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private OmsCartItemMapper omsCartItemMapper;

    @Autowired
    private RedisUtil redisUtil;

  @Override
    public OmsCartItem isCartItemExists(OmsCartItem omsCartItem) {
        OmsCartItem omsCartItemForExample = new OmsCartItem();
        omsCartItemForExample.setMemberId(omsCartItem.getMemberId());
        omsCartItemForExample.setProductSkuId(omsCartItem.getProductSkuId());
        OmsCartItem omsCartItemForResult = omsCartItemMapper.selectOne(omsCartItemForExample);
        return omsCartItemForResult;
    }

    @Override
    public void updateCart(OmsCartItem omsCartItemFromDB) {
        Example example = new Example(OmsCartItem.class);
        //要想修改，前提条件是购物车不为空
        if (!StringUtils.isEmpty(omsCartItemFromDB.getId())){
            example.createCriteria().andEqualTo("id",omsCartItemFromDB.getId());
        } else {
            example.createCriteria().andEqualTo("memberId",omsCartItemFromDB.getMemberId())
                    .andEqualTo("productSkuId",omsCartItemFromDB.getProductSkuId());
        }
        omsCartItemMapper.updateByExampleSelective(omsCartItemFromDB,example);

        //更新redis缓存
        //更新前查一下数据库
        OmsCartItem omsCartItemForCache = omsCartItemMapper.selectOne(omsCartItemFromDB);
        updateCartCache(omsCartItemForCache);

    }

    @Override
    public OmsCartItem addCart(OmsCartItem omsCartItem) {

        omsCartItemMapper.insertSelective(omsCartItem);

        //插入同时更新一下缓存
        updateCartCache(omsCartItem);

        return omsCartItem;

    }

    @Override
    public void synchronizeCache(String userId) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(userId);
        List<OmsCartItem> cartItemList = omsCartItemMapper.select(omsCartItem);

        Jedis jedis = redisUtil.getJedis();
        Map<String,String> map = new HashMap<>();
        if (cartItemList != null && cartItemList.size() > 0) {
            //说明用户在数据库中有cart数据
            for (OmsCartItem cartItem : cartItemList) {
                map.put(cartItem.getProductSkuId(),JSON.toJSONString(cartItem));
            }
            jedis.hmset("user:"+userId+":cart",map);
        } else {
            //说明用户在数据库中没有cart数据，同时在缓存数据库中也无数据
            //将浏览器端cookie中的数据提取出来存入缓存数据库

        }
        jedis.close();
    }

    /**
     *本方法用于在获取到userId后，根据userId取出缓存中的cart数据
     */
    @Override
    public List<OmsCartItem> getCartListByUserIdFromCache(String userId) {
        List<OmsCartItem> cartListResult = new ArrayList<>();

        Jedis jedis = redisUtil.getJedis();
        List<String> cartListByUserIdStr = jedis.hvals("user:" + userId + ":cart");
        if ((!StringUtils.isEmpty(cartListByUserIdStr)) && cartListByUserIdStr.size() > 0) {
            //说明缓存数据库中有该用户的cart数据，转换成cartListResult
            for (String cartItemFromCache : cartListByUserIdStr) {
                OmsCartItem omsCartItem = JSON.parseObject(cartItemFromCache, OmsCartItem.class);
                cartListResult.add(omsCartItem);
            }
        } else {
            //说明用户在缓存数据库中没有cart数据，将浏览器中的缓存同步到缓存数据库中

        }
        jedis.close();
        return cartListResult;
    }

    /**
     *本方法用于根据userId，skuId删除已交易的购物车数据
     */
    @Override
    public void deleteCarts(List<String> skuIds, String userId) {
        Jedis jedis = redisUtil.getJedis();
        for (String skuId : skuIds) {
            OmsCartItem omsCartItemForExample = new OmsCartItem();
            omsCartItemForExample.setMemberId(userId);
            omsCartItemForExample.setProductSkuId(skuId);
            //删掉数据库中的购物车数据
            omsCartItemMapper.delete(omsCartItemForExample);
            //删掉redis中对应的购物车数据
            jedis.hdel("user:"+userId+":cart",skuId);
        }
        jedis.close();
    }

    @Override
    public void addCartListToCache(String userKey, List<OmsCartItem> omsCartItemList) {
        Jedis jedis = redisUtil.getJedis();
        Map<String,String> map = new HashMap<>();

        if (omsCartItemList != null && omsCartItemList.size()>0) {
            for (OmsCartItem omsCartItem : omsCartItemList) {
                map.put(omsCartItem.getProductSkuId(),JSON.toJSONString(omsCartItem));
            }
            jedis.hmset("userKey:"+userKey+":cart",map);
        } else {

        }
        jedis.close();
    }

    @Override
    public void updateCartListByUserKeyFromCache(String userKey, List<OmsCartItem> omsCartItemListFromCacheByUserKey) {
        Jedis jedis = redisUtil.getJedis();

        for (OmsCartItem omsCartItem : omsCartItemListFromCacheByUserKey) {
            jedis.hset("userKey:"+userKey+":cart",omsCartItem.getProductSkuId(),JSON.toJSONString(omsCartItem));
        }
        jedis.close();
    }

    @Override
    public List<OmsCartItem> getCartListByUserKey(String userKey) {
        List<OmsCartItem> omsCartItemList = new ArrayList<>();

        Jedis jedis = redisUtil.getJedis();

        List<String> cartListStr = jedis.hvals("userKey:" + userKey + ":cart");
        if ((!StringUtils.isEmpty(cartListStr))&&cartListStr.size()>0) {
            for (String cart : cartListStr) {
                OmsCartItem omsCartItem = JSON.parseObject(cart, OmsCartItem.class);
                omsCartItemList.add(omsCartItem);
            }
        }
        jedis.close();
        return omsCartItemList;
    }

    @Override
    public void deleteCartsByUserKey(String userKey) {
        Jedis jedis = redisUtil.getJedis();
        jedis.del("userKey:"+userKey+":cart");
        jedis.close();
    }

    @Override
    public void combineCartList(String userId, String userKey) {

        //申请一个购物车容器
        List<OmsCartItem> omsCartItemList = new ArrayList<>();
        //根据userId去数据库中查
        OmsCartItem omsCartItemForExample = new OmsCartItem();
        omsCartItemForExample.setMemberId(userId);
        omsCartItemList = omsCartItemMapper.select(omsCartItemForExample);
        //根据userKey去redis中查
        List<OmsCartItem> omsCartItemListFromCache = getCartListByUserKey(userKey);
        for (OmsCartItem omsCartItemCache : omsCartItemListFromCache) {
            //先比较一下双方的购物车数据是否一致
            boolean checkResult = hasSameCart(omsCartItemList, omsCartItemCache);
            if (checkResult) {
                //有相同的购物车数据
                for (OmsCartItem cartItemDB : omsCartItemList) {
                    if (cartItemDB.getProductSkuId().equals(omsCartItemCache.getProductSkuId())) {
                        //能修改的地方就两个 totalPrice  quantity
                        cartItemDB.setMemberId(userId);
                        cartItemDB.setQuantity(omsCartItemCache.getQuantity().add(omsCartItemCache.getQuantity()));
                        cartItemDB.setTotalPrice(cartItemDB.getPrice().multiply(cartItemDB.getQuantity()));
                    }
                }
            } else {
                //说明没相同的购物车数据，直接合并
                omsCartItemCache.setMemberId(userId);
                addCart(omsCartItemCache);
                omsCartItemList.add(omsCartItemCache);
            }
        }
    }

    /**
     * @param omsCartItemForCache
     * 本方法用于更新redis缓存
     */
    private void updateCartCache(OmsCartItem omsCartItemForCache) {

        Jedis jedis = redisUtil.getJedis();

        jedis.hset("user:"+omsCartItemForCache.getMemberId()+":cart",
                omsCartItemForCache.getProductSkuId(),
                JSON.toJSONString(omsCartItemForCache));
        jedis.close();
    }

    /**
     *
     * @param omsCartItemList
     * @param omsCartItem
     * @return result
     * 本方法用于判断浏览器cookie中的购物车信息 与 浏览器传来的omsCartItem购物车信息是否一致
     */
    private boolean hasSameCart(List<OmsCartItem> omsCartItemList, OmsCartItem omsCartItem) {
        //声明结果变量
        boolean result = false;

        //先遍历list
        for (OmsCartItem cartItem : omsCartItemList) {

            //判断cart的id是否相同
            if (omsCartItem.getProductSkuId().equals(cartItem.getProductSkuId())){
                result = true;
            }
        }
        return result;
    }
}
