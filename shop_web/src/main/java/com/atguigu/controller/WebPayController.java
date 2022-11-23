package com.atguigu.controller;

import com.atguigu.entity.OrderInfo;
import com.atguigu.feign.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.websocket.server.PathParam;
import java.util.Map;

@Controller
public class WebPayController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("pay.html")
    public String payment(@RequestParam Long orderId, Model model){
        OrderInfo orderInfo  = orderFeignClient.getOrderInfoAndDetail(orderId);
        model.addAttribute("orderInfo",orderInfo);
        return "payment/pay";

    }


    @GetMapping("alipay/success.html")
    public String success(){
        return "payment/success";
    }





}
