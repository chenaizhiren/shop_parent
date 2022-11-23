package com.atguigu.controller;


import com.atguigu.constant.MqConst;
import com.atguigu.entity.SkuInfo;
import com.atguigu.feign.SearchFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.service.SkuInfoService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 库存单元表 前端控制器
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-01
 */
@RestController
@RequestMapping("/product")
public class SkuController {

    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private SearchFeignClient searchFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;


    @PostMapping("saveSkuInfo")
    public RetVal saveSkuInfo(@RequestBody SkuInfo skuInfo){
        skuInfoService.saveSkuInfo(skuInfo);
        return RetVal.ok();
    }


    @GetMapping("querySkuInfoByPage/{pageNum}/{pageSize}")
    public RetVal querySkuInfoByPage(@PathVariable Long pageNum,@PathVariable Long pageSize){
        Page<SkuInfo> skuInfoPage = new Page<>(pageNum, pageSize);
        skuInfoService.querySkuInfoByPage(skuInfoPage);
        return RetVal.ok(skuInfoPage);
    }

    //商品上架
    @GetMapping("onSale/{skuId}")
    public RetVal onSale(@PathVariable Long skuId){
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoService.updateById(skuInfo);
     //  searchFeignClient.onSale(skuId);
        //发送上架消息
        rabbitTemplate.convertAndSend(MqConst.ON_OFF_SALE_EXCHANGE,MqConst.ON_SALE_ROUTING_KEY,skuId);
        return RetVal.ok();
    }

    //商品下架
    @GetMapping("offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId){
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoService.updateById(skuInfo);
       // searchFeignClient.offSale(skuId);
        //发送下架消息
        rabbitTemplate.convertAndSend(MqConst.ON_OFF_SALE_EXCHANGE,MqConst.OFF_SALE_ROUTING_KEY,skuId);
        return RetVal.ok();
    }


}

