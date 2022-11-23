package com.atguigu.job;

import com.atguigu.constant.MqConst;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling  //开启定时任务
 @Component
public class ScheduledJob {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //秒杀商品上架
    @Scheduled(cron = "0 0 1 * * ?")
    public void onSale(){
        rabbitTemplate.convertAndSend(MqConst.SCAN_SECKILL_EXCHANGE,MqConst.SCAN_SECKILL_ROUTE_KEY,"");
        System.out.println("开始干活了");
    }



    //秒杀商品下架
    @Scheduled(cron = "0 0 5 * * ?")
    public void offSale(){
        rabbitTemplate.convertAndSend(MqConst.CLEAR_REDIS_EXCHANGE,MqConst.CLEAR_REDIS_ROUTE_KEY,"");

    }
}
