package com.atguigu.service;

import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

public interface SkuDetailService {


    SkuInfo getSkuInfo(Long skuId);
    List<ProductSalePropertyKey> getSkuSalePropertyKeyAndValue(Long productId,Long skuId);
    Map<Object,Object> getSkuSalePropertyValueId(Long productId);
}
