package com.atguigu.mapper;

import com.atguigu.entity.SkuSalePropertyValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * sku销售属性值 Mapper 接口
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-01
 */
public interface SkuSalePropertyValueMapper extends BaseMapper<SkuSalePropertyValue> {


    List<Map> getSkuSalePropertyValueId(@Param("productId") Long productId);
}
