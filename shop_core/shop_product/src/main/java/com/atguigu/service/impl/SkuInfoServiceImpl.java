package com.atguigu.service.impl;


import com.atguigu.entity.SkuImage;
import com.atguigu.entity.SkuInfo;
import com.atguigu.entity.SkuPlatformPropertyValue;
import com.atguigu.entity.SkuSalePropertyValue;
import com.atguigu.mapper.SkuInfoMapper;
import com.atguigu.service.SkuImageService;
import com.atguigu.service.SkuInfoService;
import com.atguigu.service.SkuPlatformPropertyValueService;
import com.atguigu.service.SkuSalePropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 库存单元表 服务实现类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-01
 */
@Service
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoMapper, SkuInfo> implements SkuInfoService {


    @Autowired
    private SkuImageService skuImageService;

    @Autowired
    private SkuSalePropertyValueService skuSalePropertyValueService;

    @Autowired
    private SkuPlatformPropertyValueService skuPlatformPropertyValueService;




    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        baseMapper.insert(skuInfo);

        //批量保存图片
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setId(skuInfo.getId());
            }
                skuImageService.saveBatch(skuImageList);
        }


        //批量保存平台属性
        List<SkuPlatformPropertyValue> skuPlatformPropertyValueList = skuInfo.getSkuPlatformPropertyValueList();
        if (!CollectionUtils.isEmpty(skuPlatformPropertyValueList)){
            for (SkuPlatformPropertyValue skuPlatformPropertyValue : skuPlatformPropertyValueList) {
                skuPlatformPropertyValue.setSkuId(skuInfo.getId());
            }
                skuPlatformPropertyValueService.saveBatch(skuPlatformPropertyValueList);
        }


        //批量保存销售属性
        List<SkuSalePropertyValue> skuSalePropertyValueList = skuInfo.getSkuSalePropertyValueList();
        if (!CollectionUtils.isEmpty(skuSalePropertyValueList)){
            for (SkuSalePropertyValue skuSalePropertyValue : skuSalePropertyValueList) {

                skuSalePropertyValue.setSkuId(skuInfo.getId());
                skuSalePropertyValue.setProductId(skuInfo.getProductId());
            }
            skuSalePropertyValueService.saveBatch(skuSalePropertyValueList);

        }
    }


    @Override
    public void querySkuInfoByPage(Page<SkuInfo> skuInfoPage) {
        QueryWrapper<SkuInfo> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("id");

        baseMapper.selectPage(skuInfoPage,wrapper);
    }
}
