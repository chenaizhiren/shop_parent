package com.atguigu.feign;

import com.atguigu.entity.*;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
@FeignClient(value = "shop-product")
public interface ProductFeignClient {
    //1.根据sku_id查询sku信息
    @GetMapping("sku/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable("skuId") Long skuId);
    //2.根据productId,skuId查询商品销售属性key与value
    @GetMapping("sku/getSkuSalePropertyKeyAndValue/{productId}/{skuId}")
    public List<ProductSalePropertyKey> getSkuSalePropertyKeyAndValue(@PathVariable Long productId, @PathVariable Long skuId);
    //3.根据skuId 查询商品的价格
    @GetMapping("sku/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId);
    //4.获取sku的分类信息
    @GetMapping("sku/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id);
    //5.通过spuId 获取对应的Json 字符串。
    @GetMapping("sku/getSkuSalePropertyValueId/{productId}")
    public Map getSkuSalePropertyValueId(@PathVariable Long productId);

    //获取商首页列表信息
    @GetMapping("/product/getIndexCategoryInfo")
    public RetVal getIndexCategoryInfo();

    //根据skuId获取品牌详情信息
    @GetMapping("/product/brand/getBrandById/{brandId}")
    public BaseBrand getBrandById(@PathVariable Long brandId);


    //根据skuId获取平台属性
    @GetMapping("getPlatformPropertyBySkuId/{skuId}")
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(@PathVariable Long skuId);

}
