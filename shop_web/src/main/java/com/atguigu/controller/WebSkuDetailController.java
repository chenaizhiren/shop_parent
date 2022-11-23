package com.atguigu.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.feign.ProductFeignClient;
import com.atguigu.feign.SearchFeignClient;
import com.atguigu.result.RetVal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Controller
public class WebSkuDetailController {

  @Autowired
  private ThreadPoolExecutor threadPoolExecutor;
  @Autowired
  private ProductFeignClient productFeignClient;

  @Autowired
  private SearchFeignClient searchFeignClient;

    @RequestMapping("{skuId}.html")
    public String getSkuDetail(@PathVariable Long skuId, Model model) {

      HashMap<String, Object> map = new HashMap<>();
      CompletableFuture<SkuInfo> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        map.put("skuInfo", skuInfo);
        return skuInfo;
      }, threadPoolExecutor);


      CompletableFuture<Void> spuCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
        List<ProductSalePropertyKey> spuSalePropertyList = productFeignClient.getSkuSalePropertyKeyAndValue(skuInfo.getProductId(), skuId);
        map.put("spuSalePropertyList", spuSalePropertyList);
      }, threadPoolExecutor);


      CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
        map.put("price", skuPrice);
      },threadPoolExecutor);


      CompletableFuture<Void> categoryViewCompletableFuture = skuCompletableFuture.thenAcceptAsync(new Consumer<SkuInfo>() {
        @Override
        public void accept(SkuInfo skuInfo) {
          BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
          map.put("categoryView", categoryView);
        }
      },threadPoolExecutor);

      CompletableFuture<Void> salePropertyValueIdJsonCompletableFuture = skuCompletableFuture.thenAcceptAsync(new Consumer<SkuInfo>() {
        @Override
        public void accept(SkuInfo skuInfo) {
          Map<Object, Object> salePropertyValueIdJson = productFeignClient.getSkuSalePropertyValueId(skuInfo.getProductId());
          map.put("salePropertyValueIdJson", JSON.toJSONString(salePropertyValueIdJson));
        }
      }, threadPoolExecutor);


      CompletableFuture<Void> hotScoreFuture = CompletableFuture.runAsync(() -> {
        searchFeignClient.incrHotScore(skuId);
      }, threadPoolExecutor);


      CompletableFuture.allOf(skuCompletableFuture,
                              spuCompletableFuture,
                              priceCompletableFuture,
                              categoryViewCompletableFuture,
                              salePropertyValueIdJsonCompletableFuture,
                              hotScoreFuture)
                              .join();
      model.addAllAttributes(map);
      return "detail/index";
    }
}
