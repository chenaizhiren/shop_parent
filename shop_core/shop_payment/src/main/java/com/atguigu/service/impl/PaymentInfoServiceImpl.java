package com.atguigu.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundApplyRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundApplyResponse;
import com.atguigu.config.AlipayConfig;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.enums.PaymentType;
import com.atguigu.feign.OrderFeignClient;
import com.atguigu.mapper.PaymentInfoMapper;
import com.atguigu.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 支付信息表 服务实现类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-19
 */
@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {


    @Autowired
    private OrderFeignClient orderFeignClient;
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @SneakyThrows
    @Override
    public String createQrCode(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

        //保存支付信息
        savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //异步通知--返回到商家系统页面的接口
        request.setNotifyUrl(AlipayConfig.notify_payment_url);
        //同步通知--返回到商家系统页面的接口
        request.setReturnUrl(AlipayConfig.return_payment_url);
        JSONObject bizContent = new JSONObject();

        //商户订单号
        bizContent.put("out_trade_no",orderInfo.getOutTradeNo());
        //订单总金额
        bizContent.put("total_amount",orderInfo.getTotalMoney());
        //订单标题
        bizContent.put("subject",orderInfo.getTradeBody());
        bizContent.put("product_code","FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());
        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
        if (response.isSuccess()){
            System.out.println("调用成功!!");
            String alipayHtml = response.getBody();
            return alipayHtml;
        }else {
            System.out.println("调用失败!!");
            return null;
        }

    }



    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("out_trade_no",outTradeNo);
        wrapper.eq("payment_type",PaymentType.ALIPAY.name());
        return baseMapper.selectOne(wrapper);

    }


    @Override
    public void updatePaymentInfo(Map<String, String> alipayParam) {
        String outTradeNo = alipayParam.get("out_trade_no");
        PaymentInfo paymentInfo = getPaymentInfo(outTradeNo);
        //修改支付表单的信息
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        String tradeNo = alipayParam.get("trade_no");
        paymentInfo.setTradeNo(tradeNo);
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(JSONObject.toJSONString(alipayParam));
        baseMapper.updateById(paymentInfo);

        //发送消息给shop-order去修改订单状态--最终一致性
        rabbitTemplate.convertAndSend(MqConst.PAY_ORDER_EXCHANGE,MqConst.PAY_ORDER_ROUTE_KEY,paymentInfo.getOrderId());
    }


    @Override
    public Boolean refund(Long orderId) throws Exception {

        //退款接口
        AlipayTradeRefundApplyRequest request = new AlipayTradeRefundApplyRequest();

        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

        HashMap<String, Object> refundDataMap = new HashMap<>();
        refundDataMap.put("out_trade_no",orderInfo.getOutTradeNo());
        refundDataMap.put("refund_amount",orderInfo.getTotalMoney());
        refundDataMap.put("refund_reason","拍错啦");

        request.setBizContent(JSON.toJSONString(refundDataMap));
        AlipayTradeRefundApplyResponse response = alipayClient.execute(request);

        if (response.isSuccess()){
            //调用成功
            System.out.println("调用成功");
            //关闭交易记录
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
            //更新
            QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("out_trade_no",paymentInfo.getOutTradeNo());
            baseMapper.update(paymentInfo,wrapper);
            return  true;
        }else {
            System.out.println("退款失败");
            return false;
        }

    }


    @Override
    public Boolean queryAlipayTrade(Long orderId) throws AlipayApiException {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        HashMap<Object, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if (response.isSuccess()){
            return true;
        }else {
            return false;
        }
    }


    @Override
    public Boolean closeAlipayTrade(Long orderId) throws Exception {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        HashMap<Object, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if (response.isSuccess()){
            return true;
        }else {
            return false;
        }
    }


    @Override
    public void closePaymentInfo(Long orderId) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id",orderId);
        Integer count = baseMapper.selectCount(wrapper);
        if (count > 0){
            return;
        }
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
        baseMapper.update(paymentInfo,wrapper);
    }

    private void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id",orderInfo.getId());
        wrapper.eq("payment_type",paymentType);

        Integer count = baseMapper.selectCount(wrapper);

        if (count >0){
            return;
        }

        //创建一个paymentInfo对象
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId()+"");
        paymentInfo.setPaymentContent(paymentType);
        paymentInfo.setPaymentMoney(orderInfo.getTotalMoney());
        paymentInfo.setPaymentContent(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setCreateTime(new Date());
        baseMapper.insert(paymentInfo);
    }

}
