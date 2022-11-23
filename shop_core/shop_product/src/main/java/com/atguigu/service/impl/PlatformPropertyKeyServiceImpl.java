package com.atguigu.service.impl;

import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.PlatformPropertyValue;
import com.atguigu.mapper.PlatformPropertyKeyMapper;
import com.atguigu.service.PlatformPropertyKeyService;
import com.atguigu.service.PlatformPropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 属性表 服务实现类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-10-30
 */
@Service
public class PlatformPropertyKeyServiceImpl extends ServiceImpl<PlatformPropertyKeyMapper, PlatformPropertyKey> implements PlatformPropertyKeyService {


    @Autowired
    private PlatformPropertyValueService propertyValueService;




    @Override
    public List<PlatformPropertyKey> getPlatformPropertyByCategoryId(Long category1Id, Long category2Id, Long category3Id) {

        List<PlatformPropertyKey> propertyKeyList = baseMapper.getPlatformPropertyKeyByCategoryId(category1Id, category2Id, category3Id);
            if (!CollectionUtils.isEmpty(propertyKeyList)){
                for (PlatformPropertyKey platformPropertyKey : propertyKeyList) {

                    QueryWrapper<PlatformPropertyValue> wrapper = new QueryWrapper<>();
                    wrapper.eq("property_key_id",platformPropertyKey.getId());
                    List<PlatformPropertyValue> propertyValueList = propertyValueService.list(wrapper);
                    platformPropertyKey.setPropertyValueList(propertyValueList);
                }
            }

            return  propertyKeyList;
    }




    @Override
    @Transactional
    public void savePlatformProperty(PlatformPropertyKey platformPropertyKey) {
        if (platformPropertyKey.getId() != null){
            baseMapper.updateById(platformPropertyKey);
        }else {
            baseMapper.insert(platformPropertyKey);
        }

        QueryWrapper<PlatformPropertyValue> wrapper = new QueryWrapper<>();

        wrapper.eq("property_key_id",platformPropertyKey.getId());

        propertyValueService.remove(wrapper);

        List<PlatformPropertyValue> propertyValueList = platformPropertyKey.getPropertyValueList();

        if (propertyValueList != null){
            propertyValueService.saveBatch(propertyValueList);
        }
    }


    @Override
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(Long skuId) {


        return baseMapper. getPlatformPropertyBySkuId(skuId);
    }
}
