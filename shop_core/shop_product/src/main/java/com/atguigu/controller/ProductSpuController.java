package com.atguigu.controller;


import com.atguigu.entity.ProductSpu;
import com.atguigu.result.RetVal;
import com.atguigu.service.SpuService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 商品表 前端控制器
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-10-31
 */
@RestController
@RequestMapping("/product")
public class ProductSpuController {


    @Autowired
    private SpuService spuService;

    @GetMapping("queryProductSpuByPage/{pageNum}/{pageSize}/{category3Id}")
    public RetVal queryProductSpuByPage(@PathVariable Long pageNum,@PathVariable Long pageSize,@PathVariable Long category3Id){
        IPage<ProductSpu> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ProductSpu> wrapper = new QueryWrapper<>();
        wrapper.eq("category3_id",category3Id);
        wrapper.orderByDesc("id");
        spuService.page(page,wrapper);
        return RetVal.ok(page);
    }

}

