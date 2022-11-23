package com.atguigu.controller;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.UserAddress;
import com.atguigu.entity.UserInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.UserAddressService;
import com.atguigu.service.UserInfoService;
import com.atguigu.util.IpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 用户地址表 前端控制器
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-13
 */
@RestController
@RequestMapping("/user/")
public class UserController {

    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private UserAddressService userAddressService;
    @Autowired
    private RedisTemplate redisTemplate;


    @PostMapping("/login")
    public RetVal login(@RequestBody UserInfo uiUserInfo, HttpServletRequest request){
        //1.根据用户信息查询数据库里是否有
       UserInfo userInfoDb = userInfoService.queryUserFromDb(uiUserInfo);
       if (userInfoDb != null){
           //返回给页面信息
           HashMap<String, Object> retMap = new HashMap<>();
           //2,制作一个token
           String token = UUID.randomUUID().toString();
            retMap.put("token",token);
            //3.把用户昵称也放进去
           retMap.put("nickName",userInfoDb.getNickName());

           //将用户信息缓存起来
           String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
           //userId用于判断是否登录,ip用于判断是否在同一个电脑上登录
           JSONObject loginInfo = new JSONObject();
           loginInfo.put("userId",userInfoDb.getId().toString());
           loginInfo.put("loginIp",IpUtil.getIpAddress(request));

           redisTemplate.opsForValue().set(userKey,loginInfo.toJSONString(), RedisConst.USERKEY_TIMEOUT,TimeUnit.SECONDS);
           return RetVal.ok(retMap);
       }else {
           return RetVal.ok().message("登录失败");
       }

    }


    @GetMapping("logout")
    public RetVal logout(HttpServletRequest request){
        String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + request.getHeader("token");
        redisTemplate.delete(userKey);
        return  RetVal.ok();
    }



    //3.根据用户的id查询用户地址信息
    @GetMapping("getUserAddressByUserId/{userId}")
    public List<UserAddress> getUserAddressByUserId(@PathVariable String userId) {
        QueryWrapper<UserAddress> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        return userAddressService.list(wrapper);
    }

}

