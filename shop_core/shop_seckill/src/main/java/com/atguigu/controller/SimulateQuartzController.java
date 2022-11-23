package com.atguigu.controller;


import com.atguigu.constant.MqConst;
import com.sun.org.apache.regexp.internal.RE;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-21
 */
@RestController
public class SimulateQuartzController {
    @Autowired
    private RabbitTemplate rabbitTemplate;


    //发送上架商品秒杀通知
    @GetMapping("sendMsgToScanSeckill")
    public String sendMsgToScanSeckill(){
        rabbitTemplate.convertAndSend(MqConst.PREPARE_SECKILL_EXCHANGE,MqConst.PREPARE_SECKILL_ROUTE_KEY,"");
        return "success";
    }



    //2.发送清理秒杀善后通知
    @GetMapping("sendMsgToClearRedis")
    public String sendMsgToClearRedis(){
        rabbitTemplate.convertAndSend(MqConst.CLEAR_REDIS_EXCHANGE,MqConst.CLEAR_REDIS_ROUTE_KEY,"");
        return  "success";
    }

}

