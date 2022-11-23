package com.atguigu.mapper;

import com.atguigu.entity.ProductSalePropertyKey;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * spu销售属性 Mapper 接口
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-01
 */
public interface SalePropertyKeyMapper extends BaseMapper<ProductSalePropertyKey> {
  List<ProductSalePropertyKey> querySalePropertyByProductId(@Param("productId") Long productId);

    List<ProductSalePropertyKey> getSkuSalePropertyKeyAndValue(@Param("productId")Long productId,@Param("skuId")Long skuId);
}
