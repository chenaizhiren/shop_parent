package com.atguigu.controller;


import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.PlatformPropertyValue;
import com.atguigu.result.RetVal;
import com.atguigu.service.PlatformPropertyKeyService;
import com.atguigu.service.PlatformPropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 属性表 前端控制器
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-10-30
 */
@RestController
@RequestMapping("/product")

public class PlatformPropertyController {

    @Autowired
    private PlatformPropertyKeyService platformPropertyKeyService;

    @Autowired
    private PlatformPropertyValueService platformPropertyValueService;
    
    
    @GetMapping("getPlatformPropertyByCategoryId/{category1Id}/{category2Id}/{category3Id}")
    public RetVal getPlatformPropertyByCategoryId(@PathVariable Long category1Id,@PathVariable Long category2Id,@PathVariable Long category3Id){

        List<PlatformPropertyKey> propertyKeyList = platformPropertyKeyService.getPlatformPropertyByCategoryId(category1Id, category2Id, category3Id);

        return RetVal.ok(propertyKeyList);

    }



    @GetMapping("getPropertyValueByPropertyKeyId/{propertyKeyId}")
    public RetVal getPropertyValueByPropertyKeyId(@PathVariable Long propertyKeyId){
        QueryWrapper<PlatformPropertyValue> wrapper = new QueryWrapper<>();
        wrapper.eq("property_key_id",propertyKeyId);
        List<PlatformPropertyValue> propertyValueList = platformPropertyValueService.list(wrapper);
        return RetVal.ok(propertyValueList);
    }



    @PostMapping("savePlatformProperty")
    public RetVal savePlatformProperty(@RequestBody PlatformPropertyKey platformPropertyKey){
        platformPropertyKeyService.savePlatformProperty(platformPropertyKey);
        return RetVal.ok();
    }


    //根据skuId获取平台属性
    @GetMapping("getPlatformPropertyBySkuId/{skuId}")
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(@PathVariable Long skuId){

     List<PlatformPropertyKey> platformPropertyKeyList  = platformPropertyKeyService.getPlatformPropertyBySkuId(skuId);
    return platformPropertyKeyList;

    }








}

