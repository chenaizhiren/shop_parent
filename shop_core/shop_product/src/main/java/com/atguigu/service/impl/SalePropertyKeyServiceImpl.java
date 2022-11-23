package com.atguigu.service.impl;

import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.mapper.SalePropertyKeyMapper;
import com.atguigu.service.SalePropertyKeyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * spu销售属性 服务实现类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-01
 */
@Service
public class SalePropertyKeyServiceImpl extends ServiceImpl<SalePropertyKeyMapper, ProductSalePropertyKey> implements SalePropertyKeyService {

    @Override
    public List<ProductSalePropertyKey> querySalePropertyByProductId(Long productId) {
        return baseMapper.querySalePropertyByProductId(productId);
    }
}
