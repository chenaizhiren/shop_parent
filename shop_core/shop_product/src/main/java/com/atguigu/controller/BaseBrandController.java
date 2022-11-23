package com.atguigu.controller;


import com.atguigu.entity.BaseBrand;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseBrandService;
import com.atguigu.service.BaseCategoryViewService;
import com.atguigu.utils.MinioUploader;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 品牌表 前端控制器
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-10-31
 */
@RestController
@RequestMapping("/product/brand")

public class BaseBrandController {

    @Autowired
    private MinioUploader minioUploader;

    @Autowired
    private BaseBrandService brandService;



    @GetMapping("queryBrandByPage/{pageNum}/{pageSize}")
    public RetVal queryBrandByPage(@PathVariable Long pageNum,@PathVariable Long pageSize){
        IPage<BaseBrand> page = new Page<>(pageNum,pageSize);
        brandService.page(page,null);
        return RetVal.ok(page);
    }


    @PostMapping
    public RetVal saveBrand(@RequestBody BaseBrand baseBrand){
        brandService.save(baseBrand);
        return RetVal.ok();
    }

    @GetMapping("{brandId}")
    public RetVal saveBrand(@PathVariable Long brandId){
        BaseBrand brand = brandService.getById(brandId);
        return RetVal.ok(brand);
    }


    @PutMapping
    public RetVal updateById(@RequestBody BaseBrand baseBrand){
        brandService.updateById(baseBrand);

        return RetVal.ok();
    }


    @DeleteMapping("{brandId}")
    public RetVal remove(@PathVariable Long brandId){
        brandService.removeById(brandId);
        return RetVal.ok();
    }


    @GetMapping("getAllBrand")
    public RetVal getAllBrand(){
        List<BaseBrand> brandList = brandService.list(null);
        return RetVal.ok(brandList);
    }

    @PostMapping("fileUpload")
    public RetVal fileUpload(MultipartFile file) throws Exception {
        String retUrl = minioUploader.uploadFile(file);
        return RetVal.ok(retUrl);
    }


    //通过brandId获得品牌信息
    @GetMapping("getBrandById/{brandId}")
    public BaseBrand getBrandById(@PathVariable Long brandId){
        return brandService.getById(brandId);
    }








}

