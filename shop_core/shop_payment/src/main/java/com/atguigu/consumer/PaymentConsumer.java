package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.service.PaymentInfoService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentConsumer {

    @Autowired
    private PaymentInfoService paymentInfoService;


    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.CLOSE_PAYMENT_QUEUE,durable = "false"),
                                                    exchange = @Exchange(value = MqConst.CLOSE_PAYMENT_EXCHANGE),
                                                    key = {MqConst.CLOSE_PAYMENT_ROUTE_KEY}))
    public void closePaymentInfo(Long orderId, Message message, Channel channel) throws Exception{
        if (orderId!= null){
            paymentInfoService.closePaymentInfo(orderId);
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
