package com.atguigu.feign;

import com.atguigu.entity.PaymentInfo;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(value = "shop-payment")
public interface PaymentFeignClient {



    //查询阿里内部订单信息
    @GetMapping("/payment/queryAlipayTrade/{orderId}")
    public Boolean queryAlipayTrade(@PathVariable Long orderId) ;

    //关闭交易接口
    @GetMapping("/payment/closeAlipayTrade/{orderId}")
    public Boolean closeAlipayTrade(@PathVariable Long orderId);


    //查询paymentInfo数据接口
    @GetMapping("/payment/getPaymentInfo/{outTradeNo}")
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo);
}
