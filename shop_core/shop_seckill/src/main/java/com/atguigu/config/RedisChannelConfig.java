package com.atguigu.config;

import com.atguigu.constant.RedisConst;
import com.atguigu.receiver.RedisMessageReceiver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisChannelConfig {
    //发布订阅模式,当有消息的时候接收处理消息的方法

    @Bean
    MessageListenerAdapter listenerAdapter(RedisMessageReceiver receiver){
        return new MessageListenerAdapter(receiver,"receiveChannelMessage");
    }



    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, org.springframework.data.redis.listener.adapter.MessageListenerAdapter listenerAdapter){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        //订阅哪个主题
        container.addMessageListener(listenerAdapter,new PatternTopic(RedisConst.PREPARE_PUB_SUB_SECKILL));
        return container;
    }
}
