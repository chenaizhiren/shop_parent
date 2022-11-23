package com.atguigu.controller;


import com.atguigu.entity.SkuInfo;
import com.atguigu.feign.CartFeignClient;
import com.atguigu.feign.ProductFeignClient;
import com.atguigu.util.AuthContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

@Controller
public class WebCartController {

    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;
    @RequestMapping("addCart.html")
    public String addCartHtml(@RequestParam Long skuId,@RequestParam Integer skuNum, HttpServletRequest request) {
        //缺少用户的信息 直接从gateway传递给shop-web
        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);
        //把购物车信息添加保存
        cartFeignClient.addCart(skuId,skuNum);
        //拿到商品的基本信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "cart/addCart";
    }

    @GetMapping("cart.html")
    public String cartHtml() {
        return "cart/index";
    }







}
