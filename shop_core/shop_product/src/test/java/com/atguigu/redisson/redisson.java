package com.atguigu.redisson;


import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;



@SpringBootTest
public class redisson {

    @Autowired
    private RedissonClient redissonClient;


   @Test
    public void TestRedisson(){
        System.out.println("redissonClient = " + redissonClient);
    }
}
