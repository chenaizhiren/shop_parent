package com.atguigu.controller;

import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class WebLoginController {

    @RequestMapping("login.html")
    public String login(HttpServletRequest request){

        //originalUrl记录用户从什么地方登录
        String originalUrl = request.getParameter("originalUrl");

        //需要存储originalUrl,因为页面需要
        request.setAttribute("originalUrl",originalUrl);

        //返回登录页面
        return "login";
    }
}
