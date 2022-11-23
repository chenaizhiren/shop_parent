package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.service.SearchService;
import com.atguigu.util.MD5;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class EsConsumer {
    @Autowired
    private SearchService searchService;
    @Autowired
    private RedisTemplate redisTemplate;


    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.ON_SALE_QUEUE,durable = "false")
                                                     ,exchange = @Exchange(value = MqConst.ON_OFF_SALE_EXCHANGE,durable = "false")
                                                      , key = {MqConst.ON_SALE_ROUTING_KEY}))
    public void onSale(Long skuId, Message message, Channel channel) throws IOException {
        if (skuId != null){
            searchService.onSale(skuId);
        }


        //如果产生异常,重复消费了一定次数,就要人工处理
        String encryptSuffix = MD5.encrypt(skuId + "");
        Long count = redisTemplate.opsForValue().increment(RedisConst.RETRY_KEY + encryptSuffix);
        if (count<RedisConst.RETRY_COUNT){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }else {
            redisTemplate.delete(RedisConst.RETRY_KEY+encryptSuffix);
        }
    }






    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.OFF_SALE_QUEUE,durable = "false"),
                                                            exchange = @Exchange(value = MqConst.ON_OFF_SALE_EXCHANGE),
                                                            key = {MqConst.OFF_SALE_ROUTING_KEY}))
    public void offSale(Long skuId,Message message,Channel channel) throws IOException {
        if (skuId != null){
            searchService.offSale(skuId);
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
