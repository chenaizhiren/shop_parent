package com.atguigu.receiver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

@Configuration
public class RedisMessageReceiver {

    @Autowired
    private RedisTemplate redisTemplate;


    //接收消息的方法
    public void receiveChannelMessage(String message){

        if (!StringUtils.isEmpty(message)){
            //去掉双引号""23:1""
          message = message.replaceAll("\"","");
            String[] splitMessage = StringUtils.split(message, ":");
            if (splitMessage == null || splitMessage.length == 2){
                redisTemplate.opsForValue().set(splitMessage[0],splitMessage[1]);
            }
        }


    }
}
