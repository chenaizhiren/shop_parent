package com.atguigu.service.impl;

import com.atguigu.entity.ProductImage;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.ProductSalePropertyValue;
import com.atguigu.entity.ProductSpu;
import com.atguigu.mapper.SpuMapper;
import com.atguigu.service.ImageService;
import com.atguigu.service.SalePropertyKeyService;
import com.atguigu.service.SalePropertyValueService;
import com.atguigu.service.SpuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-10-31
 */
@Service
public class SpuServiceImpl extends ServiceImpl<SpuMapper, ProductSpu> implements SpuService {


    @Autowired
    private ImageService imageService;

    @Autowired
    private SalePropertyKeyService salePropertyKeyService;
    @Autowired
    private SalePropertyValueService salePropertyValueService;


    @Override
    @Transactional
    public void saveProductSpu(ProductSpu productSpu) {

        baseMapper.insert(productSpu);

        List<ProductImage> productImageList = productSpu.getProductImageList();
        if (!CollectionUtils.isEmpty(productImageList)){
            for (ProductImage productImage : productImageList) {
                productImage.setProductId(productImage.getId());
            }

            imageService.saveBatch(productImageList);
        }


        List<ProductSalePropertyKey> salePropertyKeyList = productSpu.getSalePropertyKeyList();
        if (!CollectionUtils.isEmpty(salePropertyKeyList)){
            for (ProductSalePropertyKey salePropertyKey : salePropertyKeyList) {

                salePropertyKey.setSalePropertyKeyId(productSpu.getId());

                List<ProductSalePropertyValue> salePropertyValueList = salePropertyKey.getSalePropertyValueList();
                if (!CollectionUtils.isEmpty(salePropertyValueList)){
                    for (ProductSalePropertyValue salePropertyValue : salePropertyValueList) {

                        salePropertyValue.setProductId(productSpu.getId());
                        salePropertyValue.setSalePropertyKeyName(salePropertyKey.getSalePropertyKeyName());
                    }
                    salePropertyValueService.saveBatch(salePropertyValueList);
                }
            }
            salePropertyKeyService.saveBatch(salePropertyKeyList);
        }


    }
}
