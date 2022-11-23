package com.atguigu.controller;


import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.config.AlipayConfig;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.enums.PaymentType;
import com.atguigu.result.RetVal;
import com.atguigu.service.PaymentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <p>
 * 支付信息表 前端控制器
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-19
 */
@RestController
@RequestMapping("/payment")
public class PaymentInfoController {
    @Autowired
    private PaymentInfoService paymentInfoService;



    @RequestMapping("createQrCode/{orderId}")
    public String createQrCode(@PathVariable Long orderId){
      String form = paymentInfoService.createQrCode(orderId);
      return form;
    }

    //完成异步回调
    @PostMapping("/async/notify")
    public String asyncNotify(@RequestParam Map<String,String> aliPayParam) throws AlipayApiException {
        System.out.println("支付宝异步调用我的接口了");
        boolean signVerified = AlipaySignature.rsaCheckV1(aliPayParam, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        if (signVerified){
            String tradeStatus = aliPayParam.get("trade_status");
            if ("TRADE_SUCCESS".equals(tradeStatus)||"TRADE_FINISHED".equals(tradeStatus)){
                String outTradeNo = aliPayParam.get("out_trade_no");
                PaymentInfo paymentInfo =paymentInfoService.getPaymentInfo(outTradeNo);
                String paymentStatus = paymentInfo.getPaymentStatus();
                if (paymentStatus.equals(PaymentStatus.PAID.name()) || paymentStatus.equals(PaymentStatus.ClOSED.name())){
                    return "success";
                }
            }

            //修改支付表里面的信息
            paymentInfoService.updatePaymentInfo(aliPayParam);
        }else {
            //TODO 验签失败则记录异常日志,并在response中返回failure
        }
        return "success";
    }


    //退款
    @RequestMapping("/refund/{orderId}")
    public RetVal refund(@PathVariable Long orderId) throws Exception{
       Boolean flag = paymentInfoService.refund(orderId);
       return RetVal.ok(flag);
    }


    //查询阿里内部订单信息
    @GetMapping("/queryAlipayTrade/{orderId}")
    public Boolean queryAlipayTrade(@PathVariable Long orderId) throws Exception{
        return paymentInfoService.queryAlipayTrade(orderId);
    }


    //关闭交易接口
    @GetMapping("/closeAlipayTrade/{orderId}")
    public Boolean closeAlipayTrade(@PathVariable Long orderId) throws Exception{
        return paymentInfoService.closeAlipayTrade(orderId);
    }


    //查询paymentInfo数据接口
    @GetMapping("/getPaymentInfo/{outTradeNo}")
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo){
        return paymentInfoService.getPaymentInfo(outTradeNo);
    }

}

