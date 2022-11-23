package com.atguigu.controller;


import com.atguigu.entity.CartInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.CartInfoService;
import com.atguigu.util.AuthContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 前端控制器
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-14
 */
@RestController
@RequestMapping("/cart/")
public class CartController {


   @Autowired
    private CartInfoService cartInfoService;



    //1.加入购物车
    @GetMapping("addCart/{skuId}/{skuNum}")
    public RetVal addCart(@PathVariable Long skuId, @PathVariable Integer skuNum, HttpServletRequest request){
        String oneOfUserId="";
        //还差一个用户id
        String userId = AuthContextHolder.getUserId(request);
        if(StringUtils.isEmpty(userId)){
            oneOfUserId= AuthContextHolder.getUserTempId(request);
        }else{
            oneOfUserId=userId;
        }
        cartInfoService.addCart(oneOfUserId,skuId,skuNum);
        return RetVal.ok();
    }

    //2.购物车列表
    @GetMapping("getCartList")
    public RetVal getCartList(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);
        return cartInfoService.getCartList(userId,userTempId);
    }

    //3.购物车的勾选 http://api.gmall.com/cart/checkCart/24/1
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public RetVal checkCart(@PathVariable Long skuId, @PathVariable Integer isChecked,HttpServletRequest request){
        String oneOfUserId="";
        //还差一个用户id
        String userId = AuthContextHolder.getUserId(request);
        if(StringUtils.isEmpty(userId)){
            oneOfUserId= AuthContextHolder.getUserTempId(request);
        }else{
            oneOfUserId=userId;
        }
        cartInfoService.checkCart(oneOfUserId,skuId,isChecked);
        return RetVal.ok();
    }
    //4.购物车的删除  http://api.gmall.com/cart/deleteCart/30
    @GetMapping("deleteCart/{skuId}")
    public RetVal deleteCart(@PathVariable Long skuId,HttpServletRequest request){
        String oneOfUserId="";
        //还差一个用户id
        String userId = AuthContextHolder.getUserId(request);
        if(StringUtils.isEmpty(userId)){
            oneOfUserId= AuthContextHolder.getUserTempId(request);
        }else{
            oneOfUserId=userId;
        }
        cartInfoService.deleteCart(oneOfUserId,skuId);
        return RetVal.ok();
    }

    //5.购物车的送货清单  http://api.gmall.com/cart/deleteCart/30
    @GetMapping("getSelectedCartInfo/{userId}")
    public List<CartInfo> getSelectedCartInfo(@PathVariable String userId){
        return cartInfoService.getSelectedCartInfo(userId);
    }














}

