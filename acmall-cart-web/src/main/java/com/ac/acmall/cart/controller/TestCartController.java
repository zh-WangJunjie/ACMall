package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotation.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SpuService;
import com.atguigu.gmall.util.CookieUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Controller
public class TestCartController {
    public static void main(String[] args) {
        String s = UUID.randomUUID().toString();
        System.out.println(s);
    }

    @Reference
    SpuService spuService;
    @Reference
    CartService cartService;


    @RequestMapping("addToCart")
    @LoginRequired(isNeededSuccess = false)
    public String addToCart(OmsCartItem omsCartItem, HttpServletRequest request, HttpServletResponse response) {
        //初始化购物车（将商品sku信息存放在购物车中）
        //先根据skuid将商品信息查出来
        String productSkuId = omsCartItem.getProductSkuId();
        PmsSkuInfo pmsSkuInfo =spuService.getProductBySkuId(productSkuId);

        omsCartItem.setProductId(pmsSkuInfo.getProductId());
        omsCartItem.setIsChecked("1");
        omsCartItem.setTotalPrice(pmsSkuInfo.getPrice().multiply(omsCartItem.getQuantity()));
        omsCartItem.setProductPic(pmsSkuInfo.getSkuDefaultImg());
        omsCartItem.setProductName(pmsSkuInfo.getSkuName());
        omsCartItem.setProductCategoryId(pmsSkuInfo.getCatalog3Id());
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setPrice(pmsSkuInfo.getPrice());


        //初始化userId用于测试
        //String userId = "1";
        String userId = (String)request.getAttribute("userId");

        //初始化购物车列表集合容器，也便于将该集合塞到cookie里面
        List<OmsCartItem> omsCartItemList = new ArrayList<>();
        //添加购物车功能分析
        //1、用户是否登录
        if (!StringUtils.isEmpty(userId)) {
            //1.1、用户已登录：db中该用户的购物车是否为空
            //调用service层服务，根据联合主键查 memberid skuid
            omsCartItem.setMemberId(userId);
            omsCartItem.setProductSkuId(omsCartItem.getProductSkuId());
            OmsCartItem omsCartItemFromDB = cartService.isCartItemExists(omsCartItem);
            if (omsCartItemFromDB != null && (!StringUtils.isEmpty(omsCartItemFromDB.getId()))) {
                //1.1.1、db购物车不为空，修改购物车
                omsCartItemFromDB.setQuantity(omsCartItem.getQuantity());
                omsCartItemFromDB.setTotalPrice(omsCartItem.getQuantity().multiply(omsCartItem.getPrice()));
                cartService.updateCart(omsCartItemFromDB);
            } else {
                //1.1.2、db购物车为空，新增购物车，并接收DB返回的cart作为缓存处理
                omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
                cartService.addCart(omsCartItem);
            }
            //1.1finally：将DB购物车数据放入redis缓存（同步缓存）
            cartService.synchronizeCache(userId);

        } else {
            //1.2、用户未登录：浏览器中该用户的购物车cookie是否为空
            //获取浏览器中的cookie
            String userKey = CookieUtil.getCookieValue(request, "user-key", true);

            if (StringUtils.isEmpty(userKey)) {
                //1.2.1、cookie userKey为空 先生成userKey放入cookie，再将浏览器传来的cart对象新增到redis中
                userKey = UUID.randomUUID().toString();
                CookieUtil.setCookie(request,response,"user-key",userKey,60*60*24,true);
                omsCartItemList.add(omsCartItem);
                //往redis里面 放入user-key对应的购物车数据
                cartService.addCartListToCache(userKey,omsCartItemList);
            } else {
                //1.2.2、cookie userKey不为空，判断redis中的购物车与页面传来中的购物车是否相同
                //根据userKey去redis中取出购物车数据
                List<OmsCartItem> omsCartItemListFromCacheByUserKey = cartService.getCartListByUserKey(userKey);

                //将比较的方法提到外面去，成为公用方法hasSameCart(omsCartItemList,omsCartItem)
                boolean result = hasSameCart(omsCartItemListFromCacheByUserKey,omsCartItem);
                if (result) {
                    //1.2.2.1、双方有相同的购物车，修改购物车信息
                    //先遍历缓存中的cartList
                    for (OmsCartItem cartItem : omsCartItemListFromCacheByUserKey) {
                        //根据sku的id判断当前要修改的商品
                        if (cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())){
                            //能修改的地方就两个 totalPrice  quantity
                            cartItem.setQuantity(omsCartItem.getQuantity().add(cartItem.getQuantity()));
                            cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));
                        }
                    }
                    cartService.updateCartListByUserKeyFromCache(userKey,omsCartItemListFromCacheByUserKey);

                } else {
                    //1.2.2.2、用户添加的与redis中  双方购物车不相同，新增购物车信息
                    omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
                    omsCartItemList.add(omsCartItem);
                    cartService.addCartListToCache(userKey,omsCartItemList);
                }
            }
        }
        return "redirect:success.html";
    }

    /**
     *添加到购物车后要展示购物车列表
     * 该页面是一个内嵌页
     */
    @RequestMapping("cartList")
    @LoginRequired(isNeededSuccess = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response, ModelMap map) {

        //分析：展示购物车列表需考虑两种情况
        //1、用户未登录，直接访问cookie
        //2、用户已登录，访问redis缓存或者DB

        //首先先声明一个购物车列表容器
        List<OmsCartItem> omsCartItemList = new ArrayList<>();
        //初始化一个userId做测试
        //String userId = "1";
        String userId = (String)request.getAttribute("userId");
        //取出userKey
        String userKey = CookieUtil.getCookieValue(request, "user-key", true);

        //判断用户是否登录，就即是判断userId是否为空
        if (StringUtils.isEmpty(userId)) {
            //说明用户未登录，访问缓存
            //排除userKey的null值可能性
            if (!StringUtils.isEmpty(userKey)){
                //说明userKey不为空，去redis中取数据
                omsCartItemList = cartService.getCartListByUserKey(userKey);
            }
        } else {
            //说明用户已登录，访问redis缓存或DB
            //先判断一下redis里面有没有用户的userKey对应的购物车数据
            List<OmsCartItem> cartListByUserKey = cartService.getCartListByUserKey(userKey);
            //根据userId获取OmsCartItem
            omsCartItemList = cartService.getCartListByUserIdFromCache(userId);
            if (cartListByUserKey != null && omsCartItemList.size()>0) {
                //说明用户在登录之前添加了购物车数据，需要将缓存里面的购物车与数据库里面的购物车进行合并
                for (OmsCartItem omsCartItem : cartListByUserKey) {
                    omsCartItemList.add(omsCartItem);
                    //并将合并后的购物车数据存入数据库
                    cartService.addCart(omsCartItem);
                }
                //合并完以后删除缓存中的购物车
                cartService.deleteCartsByUserKey(userKey);

            } else {
                //说明用户在登录之前没有添加购物车数据
                if (omsCartItemList != null && omsCartItemList.size() > 0) {
                    //计算一下totalPrice
                    for (OmsCartItem omsCartItem : omsCartItemList) {
                        omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
                    }
                } else {
                    //说明数据库中没有该用户的cart数据，需要将cookie中的数据提取出来，存入数据库
                    //提取cookie中的数据
                    String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
                    //排除cookie的null值可能性
                    if (!StringUtils.isEmpty(cartListCookie)) {
                        //说明cookie不为空，将cookie转换成list，传递给购物车列表容器omsCartItemList
                        omsCartItemList = JSON.parseArray(cartListCookie, OmsCartItem.class);
                        //存入数据库
                        //cartService.addCart(omsCartItemList);
                    }
                }
            }
        }
        //把总金额合计独立出来，作为公共方法，并传给页面
        BigDecimal amountForCart = getAmountForCart(omsCartItemList);
        map.put("amount",amountForCart);
        //把omsCartItemList传给页面
        map.put("cartList",omsCartItemList);
        return "cartList";
    }

    /**
     *本方法用于计算购物车的总金额合计
     */
    private BigDecimal getAmountForCart(List<OmsCartItem> omsCartItemList) {
        BigDecimal amountForCart = new BigDecimal("0");
        if (omsCartItemList != null && omsCartItemList.size()>0) {
            for (OmsCartItem omsCartItem : omsCartItemList) {
                //计算合计之前先要判断一下该条购物车数据是否被选中，选中的才做累加计算
                if ("1".equals(omsCartItem.getIsChecked())) {
                    amountForCart = amountForCart.add(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
                }
            }
        }
        return amountForCart;
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
