package com.atguigu.feign;


import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.PrepareSeckillOrder;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserAddress;
import com.atguigu.result.RetVal;
import com.atguigu.util.AuthContextHolder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@FeignClient(value = "shop-seckill")
public interface SeckillFeiginClient {

    //1.查询所以秒杀商品
    @GetMapping("/seckill/queryAllSeckillProduct")
    public List<SeckillProduct> queryAllSeckillProduct();


    //2.根据skuId 获取秒杀对象数据
    @GetMapping("/seckill/querySecKillBySkuId//{skuId}")
    public SeckillProduct querySecKillBySkuId(@PathVariable Long skuId);



    //3.秒杀商品确认信息
    @GetMapping("/seckill/seckillConfirm")
    public RetVal seckillConfirm();
}
