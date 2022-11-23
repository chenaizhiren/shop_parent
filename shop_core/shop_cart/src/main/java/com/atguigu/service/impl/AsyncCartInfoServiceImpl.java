package com.atguigu.service.impl;

import com.atguigu.entity.CartInfo;
import com.atguigu.mapper.CartInfoMapper;
import com.atguigu.service.AsyncCartInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AsyncCartInfoServiceImpl extends ServiceImpl<CartInfoMapper, CartInfo> implements AsyncCartInfoService {




    @Async
    @Override
    public void updateCartInfo(CartInfo cartInfoExist) {
        System.out.println("updateCartInfo");
        baseMapper.updateById(cartInfoExist);
    }




    @Async
    @Override
    public void saveCartInfo(CartInfo cartInfo) {
        System.out.println("saveCartInfo");
        baseMapper.insert(cartInfo);

    }



    @Async
    @Override
    public void deleteCartInfo(String userId, Long skuId) {
        System.out.println("deleteCartInfo");
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(userId)){
            wrapper.eq("user_id",userId);
        }

        if (skuId != null){
            wrapper.eq("sku_id",skuId);
        }

        baseMapper.delete(wrapper);
    }



    @Async
    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        wrapper.eq("sku_id",skuId);
        baseMapper.update(cartInfo,wrapper);
    }
}
