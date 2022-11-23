package com.atguigu.service;

import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.result.RetVal;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-21
 */
public interface SeckillProductService extends IService<SeckillProduct> {

    List<SeckillProduct> queryAllSeckillProduct();

    SeckillProduct querySecKillBySkuId(Long skuId);

    void prepareSecKill(UserSeckillSkuInfo userSeckillSkuInfo);

    RetVal hasQualified(Long skuId, String userId);
}
