package com.atguigu.feign;

import com.atguigu.entity.UserAddress;
import com.atguigu.entity.UserInfo;
import com.atguigu.result.RetVal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@FeignClient(value = "shop-user")
public interface UserFeignClient {


    @PostMapping("/user/login")
    public RetVal login(@RequestBody UserInfo uiUserInfo);

    @GetMapping("/user/logout")
    public RetVal logout(HttpServletRequest request);


    //1.根据用户的id查询用户地址信息
    @GetMapping("/user/getUserAddressByUserId/{userId}")
    public List<UserAddress> getUserAddressByUserId(@PathVariable String userId);

}
