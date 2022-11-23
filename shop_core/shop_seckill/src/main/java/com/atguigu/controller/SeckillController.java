package com.atguigu.controller;

import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.*;
import com.atguigu.feign.OrderFeignClient;
import com.atguigu.feign.UserFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.util.MD5;
import com.atguigu.utils.DateUtil;
import com.sun.org.apache.regexp.internal.RE;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@RequestMapping("/seckill/")
@RestController
public class SeckillController {


    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillProductService seckillProductService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private OrderFeignClient orderFeignClient;

    //1.查询所以秒杀商品
    @GetMapping("/queryAllSeckillProduct")
    public List<SeckillProduct> queryAllSeckillProduct(){

        return seckillProductService.queryAllSeckillProduct();
    }


    //2.根据skuId 获取秒杀对象数据
    @GetMapping("/querySecKillBySkuId/{skuId}")
    public SeckillProduct getSeckillProductBySkuId(@PathVariable Long skuId){
    return seckillProductService.querySecKillBySkuId(skuId);
    }



    //3.生成抢购码
    @GetMapping("/generateSeckillCode/{skuId}")
    public RetVal generateSeckillCode(@PathVariable Long skuId, HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        if (!StringUtils.isEmpty(userId)){
            SeckillProduct seckillProduct = seckillProductService.querySecKillBySkuId(skuId);
            Date currentDate = new Date();
            //如果当前时间在抢购时间内,则生成抢购码
            if (DateUtil.dateCompare(seckillProduct.getStartTime(),currentDate) && DateUtil.dateCompare(currentDate,seckillProduct.getEndTime())){
                //生成规则,将用户id用MD5加密
                String seckillCode = MD5.encrypt(userId);
                return RetVal.ok(seckillCode);
            }
        }
        return RetVal.ok().message("获取抢单码失败!!!");
    }


    //4.秒杀预下单
    @PostMapping("/prepareSeckill/{skuId}")
    public RetVal prepareSeckill(@PathVariable Long skuId,String seckillCode,HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //下单码不匹配时报异常
        if (!MD5.encrypt(userId).equals(seckillCode)){
            return RetVal.build(null, RetValCodeEnum.SECKILL_ILLEGAL);
        }

        //没有获取到状态位信息时
        String state = (String) redisTemplate.opsForValue().get(skuId.toString());
        if (StringUtils.isEmpty(state)){
            return RetVal.build(null,RetValCodeEnum.SECKILL_ILLEGAL);
        }

        if (RedisConst.CAN_SECKILL.equals(state)){
            UserSeckillSkuInfo userSeckillSkuInfo = new UserSeckillSkuInfo();
            //哪个用户购买了哪个产品
            userSeckillSkuInfo.setSkuId(skuId);
            userSeckillSkuInfo.setUserId(userId);
            rabbitTemplate.convertAndSend(MqConst.SCAN_SECKILL_EXCHANGE,MqConst.PREPARE_SECKILL_ROUTE_KEY,userSeckillSkuInfo);
        }else {
            //秒杀商品已经售完
           return  RetVal.build(null,RetValCodeEnum.SECKILL_FINISH);
        }

        return RetVal.ok();
    }

    //5.是否具有抢单资格
    @GetMapping("/hasQualified/{skuId}")
    public RetVal hasQualified(@PathVariable Long skuId,HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        return seckillProductService.hasQualified(skuId,userId);
    }

    //6.秒杀商品确认信息
    @GetMapping("/seckillConfirm")
    public RetVal seckillConfirm(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //1.秒杀到的商品预售信息
        PrepareSeckillOrder prepareSeckillOrder = (PrepareSeckillOrder) redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
        if (prepareSeckillOrder == null){
            return RetVal.fail().message("非法操作");
        }
        
        //2.获取用户收货地址列表
        List<UserAddress> userAddressList = userFeignClient.getUserAddressByUserId(userId);
        //3.用户秒杀到哪个商品
        SeckillProduct seckillProduct = prepareSeckillOrder.getSeckillProduct();

        //4.创建一个创建一个秒杀订单明细
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillProduct.getSkuId());
        orderDetail.setSkuName(seckillProduct.getSkuName());
        orderDetail.setImgUrl(seckillProduct.getSkuDefaultImg());
        orderDetail.setSkuNum(seckillProduct.getNum()+"");
        orderDetail.setOrderPrice(seckillProduct.getCostPrice());

        //由于页面要求传递一个集合 其实在这里我们只有一个商品
        ArrayList<OrderDetail> orderDetailList = new ArrayList<>();
        orderDetailList.add(orderDetail);

        //把这些数据封装到一个map中
        HashMap<String, Object> result = new HashMap<>();
        result.put("userAddressList",userAddressList);
        result.put("orderDetailList",orderDetailList);
        result.put("totalMoney",seckillProduct.getCostPrice());
        return RetVal.ok(result);
    }

    @PostMapping("/submitSecKillOrder")
    public RetVal submitSecKillOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));

        //判断用户是否有预下单
        PrepareSeckillOrder prepareSeckillOrder = (PrepareSeckillOrder) redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
        if (prepareSeckillOrder == null){
            return RetVal.fail().message("非法操作!!");
        }
        
        //远程调用保存秒杀订单
        Long orderId = orderFeignClient.saveSeckillOrder(orderInfo);
        if (orderId == null){
            return RetVal.fail().message("下单失败!");
        }

        //根据userid删除预订单
        redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).delete(userId);

        //为了让用户只购买一次,把订单保存到缓存
        redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).put(userId,orderId.toString());
        return RetVal.ok();

    }





}
