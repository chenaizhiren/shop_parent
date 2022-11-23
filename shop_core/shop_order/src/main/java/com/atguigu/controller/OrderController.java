package com.atguigu.controller;


import com.alibaba.fastjson.JSON;
import com.atguigu.entity.CartInfo;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.UserAddress;
import com.atguigu.feign.CartFeignClient;
import com.atguigu.feign.UserFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.AuthContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单表 订单表 前端控制器
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-15
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private OrderInfoService orderInfoService;


    //1.订单的确认信息
    @GetMapping("confirm")
    public RetVal confirm(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //a.收货人的地址信息
        List<UserAddress> userAddressList = userFeignClient.getUserAddressByUserId(userId);
        //b.获取到勾选的订单信息
        List<CartInfo> selectedCartInfoList = cartFeignClient.getSelectedCartInfo(userId);
        //c.把购物车信息转换为订单详情信息
        int totalNum=0;
        BigDecimal totalMoney = new BigDecimal("0");
        List<OrderDetail> orderDetailList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(selectedCartInfoList)){
            for (CartInfo cartInfo : selectedCartInfoList) {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum()+"");
                //订单的价格 可以拿实时价格 可以拿购物车价格(采用)
                orderDetail.setOrderPrice(cartInfo.getRealTimePrice());
                //订单的总金额
                totalMoney=totalMoney.add(cartInfo.getRealTimePrice().multiply(new BigDecimal(cartInfo.getSkuNum())));
                totalNum+=cartInfo.getSkuNum();
                orderDetailList.add(orderDetail);
            }
        }
        //d.把这些信息放到一个map当中
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("userAddressList",userAddressList);
        retMap.put("detailArrayList",orderDetailList);
        retMap.put("totalMoney",totalMoney);
        retMap.put("totalNum",totalNum);
        //e.生成流水号给页面作用域和redis
        String tradeNo=orderInfoService.generateTradeNo(userId);
        retMap.put("tradeNo",tradeNo);
        return RetVal.ok(retMap);
    }

    //2.提交订单 http://api.gmall.com/order/submitOrder
    @PostMapping("submitOrder")
    public RetVal submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        //获取到用户的id
        String userId = AuthContextHolder.getUserId(request);
        //获取页面传递过来的流水号
        String tradeNoUI = request.getParameter("tradeNo");
        //从redis中获取tradeNo并和页面传递过来的进行比较
        boolean flag=orderInfoService.checkTradeNo(tradeNoUI,userId);
        if(!flag){
            return RetVal.fail().message("不能重复提交订单");
        }
        //验证商品的价格与库存--最好不用异步编排
        String warnningMessage=orderInfoService.checkPriceAndStock(orderInfo);
        if(!StringUtils.isEmpty(warnningMessage)){
            return RetVal.fail().message(warnningMessage);
        }
        //保存订单基本信息与详情信息
        orderInfo.setUserId(Long.parseLong(userId));
        Long orderId=orderInfoService.saveOrderAndDetail(orderInfo);
        //提交订单之后需要从redis中删除流水号
        orderInfoService.deleteTradeNo(userId);
        return RetVal.ok(orderId);
    }



    @GetMapping("getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId){
        OrderInfo orderInfo = orderInfoService.getOrderInfo(orderId);
        return orderInfo;
    }


    //根据订单id查询订单信息-(基本信息详情信息)
    @GetMapping("getOrderInfoAndDetail/{orderId}")
    public OrderInfo getOrderInfoAndDetail(@PathVariable Long orderId){
        return orderInfoService.getOrderInfoAndDetail(orderId);
    }


    //拆单接口
    @RequestMapping("/splitOrder")
    public String splitOrder(@RequestParam Long orderId,HttpServletRequest request){
        String wareHouseIdSkuIdMap = request.getParameter("wareHouseIdSkuIdMap");
        List<OrderInfo>   childOrderList =orderInfoService.splitOrder(orderId,wareHouseIdSkuIdMap);
        //组装订单信息返回给库存系统
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (OrderInfo orderInfo : childOrderList) {
           Map<String,Object> map = orderInfoService.assembWareHouseDate(orderInfo);
           dataList.add(map);
        }        
            return JSON.toJSONString(dataList);
    }


    //5.给秒杀提供的提交订单数据接口
    @PostMapping("/saveSeckillOrder")
    public Long saveSeckillOrder(@RequestBody OrderInfo orderInfo){

        return orderInfoService.saveOrderAndDetail(orderInfo);
    }



}

