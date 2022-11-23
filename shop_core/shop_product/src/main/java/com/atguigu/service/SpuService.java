package com.atguigu.service;

import com.atguigu.entity.ProductSpu;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 商品表 服务类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-10-31
 */
public interface SpuService extends IService<ProductSpu> {
    void saveProductSpu(ProductSpu productSpu);
}
