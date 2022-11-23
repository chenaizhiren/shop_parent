package com.atguigu.feign;


import com.atguigu.entity.CartInfo;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@FeignClient(value = "shop-cart")
public interface CartFeignClient {

    @GetMapping("/cart/addCart/{skuId}/{skuNum}")
    public RetVal addCart(@PathVariable Long skuId, @PathVariable Integer skuNum);
    //5.购物车的送货清单  http://api.gmall.com/cart/deleteCart/30
    @GetMapping("/cart/getSelectedCartInfo/{userId}")
    public List<CartInfo> getSelectedCartInfo(@PathVariable String userId);
}
