package com.atguigu.controller;


import com.atguigu.entity.SkuInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.SkuInfoService;
import com.atguigu.service.TestService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 库存单元表 前端控制器
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-01
 */
@RestController
@RequestMapping("/product")
public class TestController {

  @Autowired
  private TestService testService;

  @GetMapping("setNum")
    public String setNum(){
      testService.setNum();
      return "success";
  }


}

