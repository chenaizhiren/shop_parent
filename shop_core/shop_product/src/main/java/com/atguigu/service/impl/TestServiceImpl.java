package com.atguigu.service.impl;

import com.atguigu.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
@Service
public class TestServiceImpl implements TestService {


    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    public void setNum() {

        String value = (String) redisTemplate.opsForValue().get("num");


        //没有值就return
        if (StringUtils.isEmpty(value)){
            return;

        }

        //有值就转换成int
        int num = Integer.parseInt(value);

        //把redis中的num加1
        redisTemplate.opsForValue().set("num",String.valueOf(++num));


    }
}
