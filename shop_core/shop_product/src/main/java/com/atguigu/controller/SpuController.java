package com.atguigu.controller;


import com.atguigu.entity.BaseSaleProperty;
import com.atguigu.entity.ProductImage;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.ProductSpu;
import com.atguigu.result.RetVal;
import com.atguigu.service.ImageService;
import com.atguigu.service.SalePropertyKeyService;
import com.atguigu.service.SalePropertyService;
import com.atguigu.service.SpuService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 基本销售属性表 前端控制器
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-01
 */
@RestController
@RequestMapping("/product")
public class SpuController {
    
    
    @Autowired
    private SalePropertyService salePropertyService;

    @Autowired
    private SpuService spuService;

    @Autowired
    private SalePropertyKeyService salePropertyKeyService;

    @Autowired
    private ImageService imageService;



    @GetMapping("queryAllSaleProperty")
    public RetVal queryAllSaleProperty(){
        List<BaseSaleProperty> salePropertyList = salePropertyService.list(null);

        return RetVal.ok(salePropertyList);
    }




    @PostMapping("saveProductSpu")
    public RetVal saveProductSpu(@RequestBody ProductSpu productSpu){
     spuService.saveProductSpu(productSpu);
     return RetVal.ok();
    }



    @GetMapping("querySalePropertyByProductId/{productId}")
    public RetVal querySalePropertyByProductId(@PathVariable Long productId){
        List<ProductSalePropertyKey> salePropertyList = salePropertyKeyService.querySalePropertyByProductId(productId);

        return RetVal.ok(salePropertyList);
    }


    @GetMapping("queryProductImageByProductId/{productId}")
    public RetVal queryProductImageByProductId(@PathVariable Long productId){
        QueryWrapper<ProductImage> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id",productId);
        List<ProductImage> productImageList = imageService.list(wrapper);
        return  RetVal.ok(productImageList);
    }



}

