package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

public interface PaymentService {
    void addPayment(PaymentInfo payment);

    void updatePayment(PaymentInfo paymentInfo);

    void sendPaySuccessQueue(PaymentInfo paymentInfo);

    void sendPayStatusCheckQueue(PaymentInfo payment, long count);

    PaymentInfo checkPayStatus(String out_trade_no);

    boolean checkPayStatusFromDB(String out_trade_no);
}
