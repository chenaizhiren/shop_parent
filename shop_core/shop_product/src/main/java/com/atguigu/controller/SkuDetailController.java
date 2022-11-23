package com.atguigu.controller;

import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseCategoryViewService;
import com.atguigu.service.SkuDetailService;
import com.atguigu.service.SkuInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sku")
public class SkuDetailController {


    @Autowired
    private SkuDetailService skuDetailService;
    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private BaseCategoryViewService baseCategoryViewService;




    //1.根据sku_id查询sku信息
    @GetMapping("getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable("skuId") Long skuId){
        return skuDetailService.getSkuInfo(skuId);
    }

    //2.根据productId,skuId查询商品销售属性key与value
    @GetMapping("getSkuSalePropertyKeyAndValue/{productId}/{skuId}")
    public List<ProductSalePropertyKey> getSkuSalePropertyKeyAndValue(@PathVariable Long productId, @PathVariable Long skuId){
        return  skuDetailService.getSkuSalePropertyKeyAndValue(productId, skuId);
    }


    //3.根据skuId 查询商品的价格
    @GetMapping("getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){
        SkuInfo skuInfo = skuInfoService.getById(skuId);
        return skuInfo.getPrice();
    }



    //4.获取sku的分类信息
    @GetMapping("getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id){
      return   baseCategoryViewService.getCategoryViewByCategory3Id(category3Id);
    }


    //5.通过spuId 获取对应的Json 字符串。
    @GetMapping("getSkuSalePropertyValueId/{productId}")
    public Map getSkuSalePropertyValueId(@PathVariable Long productId){
        return skuDetailService.getSkuSalePropertyValueId(productId);
    }


}
