package com.atguigu.service;

import com.atguigu.entity.CartInfo;

public interface AsyncCartInfoService {

    void updateCartInfo(CartInfo cartInfoExist);
    void saveCartInfo(CartInfo cartInfo);
    public void deleteCartInfo(String userId,Long skuId);
    void checkCart(String userId, Integer isChecked, Long skuId);
}
