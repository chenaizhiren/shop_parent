package com.atguigu.feign;

import com.atguigu.entity.CartInfo;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.UserAddress;
import com.atguigu.result.RetVal;
import com.atguigu.util.AuthContextHolder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FeignClient(value = "shop-order")
public interface OrderFeignClient {
    //1.订单的确认信息
    @GetMapping("/order/confirm")
    public RetVal confirm();
    //2.根据订单id查询订单信息-(基本信息详情信息)
    @GetMapping("/order/getOrderInfoAndDetail/{orderId}")
    public OrderInfo getOrderInfoAndDetail(@PathVariable Long orderId);

    @GetMapping("getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId);

    //3.给秒杀提供的提交订单数据接口
    @PostMapping("/order/saveSeckillOrder")
    public Long saveSeckillOrder(@RequestBody OrderInfo orderInfo);



}
