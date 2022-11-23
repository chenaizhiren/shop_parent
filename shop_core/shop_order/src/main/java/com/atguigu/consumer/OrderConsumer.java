package com.atguigu.consumer;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.MqConst;

import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.feign.PaymentFeignClient;
import com.atguigu.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class OrderConsumer {

    @Autowired
    private OrderInfoService orderInfoService;
    @Autowired
    private PaymentFeignClient paymentFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;



    //1.超时未支付自动取消订单的逻辑
    @RabbitListener(queues = MqConst.CANCEL_ORDER_QUEUE)
    public void cancelOrder(Long orderId, Channel channel,Message message) throws Exception{
        if(orderId!=null){
            //把订单改为关闭状态
            OrderInfo orderInfo = orderInfoService.getById(orderId);
            //如果订单未支付
            if (orderInfo != null && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())){
                //关闭订单信息
                orderInfoService.updateOrderStatus(orderId,ProcessStatus.CLOSED);
                PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                if (paymentInfo != null && paymentInfo.getPaymentStatus().equals(ProcessStatus.UNPAID.name())){

                    rabbitTemplate.convertAndSend(MqConst.CLOSE_PAYMENT_EXCHANGE,MqConst.CLOSE_PAYMENT_ROUTE_KEY,orderId);

                    //如果阿里系统创建了交易,则在阿里内部关闭交易
                    Boolean flag = paymentFeignClient.queryAlipayTrade(orderId);
                    if (flag){
                        paymentFeignClient.closeAlipayTrade(orderId);
                    }
                }

            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }


    //2.支付成功之后,修改订单状态
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.PAY_ORDER_QUEUE,durable = "false"),
                                                    exchange = @Exchange(value = MqConst.PAY_ORDER_EXCHANGE,durable = "false"),
                                                    key = {MqConst.PAY_ORDER_ROUTE_KEY}))
            public void updateOrderAfterPaySuccess(Long orderId,Channel channel,Message message) throws  Exception{
        if (orderId != null){
            OrderInfo orderInfo = orderInfoService.getById(orderId);
            if (orderInfo != null && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.name())){
                orderInfoService.updateOrderStatus(orderId,ProcessStatus.PAID);
                //发送消息通知仓库系统减库存
                orderInfoService.sendMsgToWarehouse(orderId);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }


    //3.接收库存系统发过来的消息
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.SUCCESS_DECREASE_STOCK_QUEUE,durable = "false"),
                                                    exchange = @Exchange(value = MqConst.SUCCESS_DECREASE_STOCK_EXCHANGE,durable = "false"),
                                                    key = {MqConst.SUCCESS_DECREASE_STOCK_ROUTE_KEY}))
    public void  updateOrderStatus(String jsonMsg,Channel channel,Message message) throws Exception{
        if (!StringUtils.isEmpty(jsonMsg)){
            //获取对应数据
            JSONObject map = JSONObject.parseObject(jsonMsg);
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");
            if ("DEDUCTED".equals(status)){
                //减库存成功
                orderInfoService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.WAITING_DELEVER);
            }else{
                //库存超卖
                orderInfoService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);
            }
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
