package com.atguigu.service;

import com.atguigu.entity.ProductSalePropertyKey;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * spu销售属性 服务类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-01
 */
public interface SalePropertyKeyService extends IService<ProductSalePropertyKey> {
    List<ProductSalePropertyKey> querySalePropertyByProductId(Long productId);
}
