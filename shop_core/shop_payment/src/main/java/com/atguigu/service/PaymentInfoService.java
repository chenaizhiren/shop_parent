package com.atguigu.service;

import com.alipay.api.AlipayApiException;
import com.atguigu.entity.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 支付信息表 服务类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-19
 */
public interface PaymentInfoService extends IService<PaymentInfo> {

    String createQrCode(Long orderId);

    PaymentInfo getPaymentInfo(String outTradeNo);

    void updatePaymentInfo(Map<String,String> alipayParam);

    Boolean refund(Long orderId) throws Exception;

    Boolean queryAlipayTrade(Long orderId) throws Exception;

    Boolean closeAlipayTrade(Long orderId) throws Exception;

    void closePaymentInfo(Long orderId);
}
