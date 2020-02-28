package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.annotation.LoginRequired;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {
    @Reference
    OrderService orderService;
    @Reference
    PaymentService paymentService;

    @Autowired
    AlipayClient alipayClient;

    @RequestMapping("index")
    @LoginRequired
    public String index(String outTradeNo, String totalAmount, HttpServletRequest request, ModelMap modelMap) {
        String nickname = (String) request.getAttribute("nickname");
        String userId = (String) request.getAttribute("userId");
        modelMap.put("nickname",nickname);
        modelMap.put("outTradeNo",outTradeNo);
        modelMap.put("totalAmount",totalAmount);
        return "index";
    }

    //alipay/submit
    @RequestMapping("alipay/submit")
    @ResponseBody
    @LoginRequired
    public String alipaySubmit(String outTradeNo,HttpServletRequest request,ModelMap modelMap) {

        //根据outTradeNo去数据库中查订单数据
        OmsOrder omsOrder = orderService.getOrderByOutTradeNo(outTradeNo);

        //提取request域中的用户信息
        String nickname = (String) request.getAttribute("nickname");
        String userId = (String) request.getAttribute("userId");

        //支付宝的客户端，调用统一收单下单页面支付接口
        //创建API对应的request
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        //
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        //在公共参数中设置回跳和通知地址
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("out_trade_no",outTradeNo);
        paramMap.put("product_code","FAST_INSTANT_TRADE_PAY");
        paramMap.put("total_amount",0.01);
        paramMap.put("subject",omsOrder.getOmsOrderItems().get(0).getProductName());

        //将参数map转换为json串
        String json = JSON.toJSONString(paramMap);

        //填充业务参数
        alipayRequest.setBizContent(json);

        String form = "";
        try {
            //调用SDK生成表单
            form = alipayClient.pageExecute(alipayRequest).getBody();
            System.out.println(form);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //生成和保存支付信息
        PaymentInfo payment = new PaymentInfo();
        payment.setOrderSn(outTradeNo);
        payment.setPaymentStatus("未支付");
        payment.setSubject(omsOrder.getOmsOrderItems().get(0).getProductName());
        payment.setTotalAmount(omsOrder.getTotalAmount());
        payment.setCreateTime(new Date());
        payment.setOrderId(omsOrder.getId());
        //往数据库中添加支付信息
        paymentService.addPayment(payment);

        //发送延迟检查支付状态的队列（定时器）
        paymentService.sendPayStatusCheckQueue(payment,7);

        modelMap.put("nickname",nickname);

        //将form表单返回给页面
        return form;
    }

    //支付完成后回跳到finsh页面
    @RequestMapping("alipay/callback/return")
    @LoginRequired
    public String callBackReturn(String outTradeNo,ModelMap modelMap,HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        String nickname = (String) request.getAttribute("nickname");

        String out_trade_no = request.getParameter("out_trade_no");
        String trade_no = request.getParameter("trade_no");
        String trade_status = request.getParameter("trade_status");
        String sign = request.getParameter("sign");

        //更新支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus("已支付");
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setAlipayTradeNo(trade_no);
        paymentInfo.setCallbackContent(request.getQueryString());
        paymentInfo.setOrderSn(out_trade_no);
        //更新支付信息
        paymentService.updatePayment(paymentInfo);

        // 发送系统信息给订单，更新订单信息（已支付）
        paymentService.sendPaySuccessQueue(paymentInfo);

        return "finish";
    }

}
