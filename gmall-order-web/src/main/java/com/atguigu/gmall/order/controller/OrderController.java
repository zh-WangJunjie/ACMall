package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotation.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Controller
public class OrderController {
    @Reference
    UserService userService;
    @Reference
    CartService cartService;
    @Reference
    OrderService orderService;


    /**
     *@return
     * 去结算跳转到结算页面
     * 这个结算页面完全就是个静态页，展示用途
     * 商品数据不是由表单提交传到后台
     * 而是由查询数据库得到的数据
     * 这么做是为了防止页面数据不安全
     */
    @LoginRequired
    @RequestMapping("toTrade")
    public String toTrade(HttpServletRequest request, ModelMap modelMap) {
        String userId = (String) request.getAttribute("userId");
        //根据当前userId获取用户的收货地址列表
        List<UmsMemberReceiveAddress> addressList = userService.getUserAddressByUserId(userId);

        //获取当前用户要结算的商品
        List<OmsCartItem> cartItemList = cartService.getCartListByUserIdFromCache(userId);
        Iterator<OmsCartItem> iterator = cartItemList.iterator();
        while (iterator.hasNext()) {
            OmsCartItem cartItem = iterator.next();
            if (!"1".equals(cartItem.getIsChecked())) {
                iterator.remove();
            }
        }

        //将购物车对象封装成订单详情对象
        List<OmsOrderItem> orderItemList = new ArrayList<>();
        for (OmsCartItem omsCartItem : cartItemList) {
            OmsOrderItem omsOrderItem = new OmsOrderItem();
            omsOrderItem.setProductQuantity(omsCartItem.getQuantity());//商品数量
            omsOrderItem.setProductPrice(omsCartItem.getPrice());//商品单价
            omsOrderItem.setProductPic(omsCartItem.getProductPic());//商品图片
            omsOrderItem.setProductName(omsCartItem.getProductName());//商品名称
            omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());//商品分类id
            omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());//商品库存单元详情id
            omsOrderItem.setProductId(omsCartItem.getProductId());//商品id
            //塞到订单详情列表
            orderItemList.add(omsOrderItem);
        }

        //传收货地址列表
        modelMap.put("userAddressList",addressList);

        //传订单详情列表
        modelMap.put("orderDetailList",orderItemList);

        //传总金额
        modelMap.put("totalAmount",getAmountForCart(cartItemList));

        //传交易码
        String tradeCode = orderService.getTradeCode(userId);
        modelMap.put("tradeCode",tradeCode);

        return "trade";
    }

    /**
     * 本方法用于提交订单
     */
    @LoginRequired
    @RequestMapping("submitOrder")
    public String submitOrder(HttpServletRequest request,String tradeCode,String addressId) {

        //提取参数中的userId和昵称
        String userId = (String) request.getAttribute("userId");
        String nickName = (String) request.getAttribute("nickName");

        //检查页面中的tradeCode与redis中的tradeCode是否一致
        //如果redis中的tradeCode已经被删掉了，则说明用户已经提交过一次订单了
        boolean checkTradeCodeResult = orderService.checkTradeCode(userId,tradeCode);
        if (checkTradeCodeResult) {
            //说明用户是第一次提交订单，生成订单数据
            //提取用户的收货地址
            UmsMemberReceiveAddress receiveAddress = userService.getUserAddressByAddressId(addressId);
            //提取用户的购物车数据
            List<OmsCartItem> cartItemList = cartService.getCartListByUserIdFromCache(userId);

            //封装订单列表
            OmsOrder order = new OmsOrder();
            String outTradeNo = "atguigu0722";//初始化订单号
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");//初始化订单创建日期格式
            String nowTime = simpleDateFormat.format(new Date());//初始化订单创建日期
            long currentTimeMillis = System.currentTimeMillis();//初始化订单创建的系统时间
            outTradeNo += nowTime + currentTimeMillis;//创建订单号
            order.setOrderSn(outTradeNo);//设置外部订单号
            order.setStatus("1");//设置当前订单状态
            order.setTotalAmount(getAmountForCart(cartItemList));//设置订单总金额
            order.setMemberId(userId);//设置用户id
            order.setPayAmount(getAmountForCart(cartItemList));//设置支付总金额
            order.setMemberUsername(nickName);//设置用户昵称
            order.setCreateTime(new Date());//设置创建日期
            order.setReceiverRegion(receiveAddress.getRegion());//
            order.setReceiverProvince(receiveAddress.getProvince());//设置省份
            order.setReceiverPostCode("435300");//设置邮编
            order.setReceiverPhone(receiveAddress.getPhoneNumber());//设置收货电话
            order.setReceiverName(receiveAddress.getName());//
            order.setReceiverDetailAddress(receiveAddress.getDetailAddress());//设置详细地址
            order.setReceiverCity(receiveAddress.getCity());//设置城市
            order.setPayType(2);//设置支付方式“支付宝”
            order.setOrderType(1);//
            order.setSourceType(1);//
            order.setNote("测试订单");//设置备注

            //封装订单详情
            Iterator<OmsCartItem> iterator = cartItemList.iterator();
            while (iterator.hasNext()) {
                OmsCartItem cartItem = iterator.next();
                if (!"1".equals(cartItem.getIsChecked())) {
                    iterator.remove();
                }
            }

            //初始化订单列表orderItemList
            List<OmsOrderItem> orderItemList = new ArrayList<>();
            //声明一个skuIds用于批量删除购物车
            List<String> skuIds = new ArrayList<>();
            for (OmsCartItem omsCartItem : cartItemList) {
                OmsOrderItem orderItem = new OmsOrderItem();
                orderItem.setOrderSn(outTradeNo);//设置订单编号
                orderItem.setProductQuantity(omsCartItem.getQuantity());//设置商品数量
                orderItem.setProductPic(omsCartItem.getProductPic());//设置默认图片
                orderItem.setProductPrice(omsCartItem.getPrice());//设置商品单价
                orderItem.setProductName(omsCartItem.getProductName());//设置商品名称
                orderItem.setProductCategoryId(omsCartItem.getProductCategoryId());//
                orderItem.setProductSkuId(omsCartItem.getProductSkuId());//设置skuid
                orderItem.setProductId(omsCartItem.getProductId());//设置productid

                //将订单添加到订单列表中
                orderItemList.add(orderItem);
                //将已交易的购物车skuId添加到skuIds
                skuIds.add(omsCartItem.getProductSkuId());
            }
            //将订单列表封装到订单中
            order.setOmsOrderItems(orderItemList);
            //将订单添加到数据库中
            orderService.saveOrder(order);

            //删除已提交的商品
            cartService.deleteCarts(skuIds,userId);

            //跳到支付页面
            return "redirect:http://payment.gmall.com:8087/index?outTradeNo=" + outTradeNo + "&totalAmount=" + getAmountForCart(cartItemList);
        } else {
            //说明该用户重复提交订单了，给他跳到一个错误页面
            return "tradeFail";
        }
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

}
