package com.atguigu.controller;

import com.atguigu.entity.SeckillProduct;
import com.atguigu.feign.SeckillFeiginClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class WebSeckillController {


    @Autowired
    private SeckillFeiginClient seckillFeiginClient;


    //1.查询所有的秒杀商品
    @GetMapping("seckill-index.html")
    public String seckillIndex(Model model){
        List<SeckillProduct> seckillProductList = seckillFeiginClient.queryAllSeckillProduct();
        model.addAttribute("list",seckillProductList);
        return "seckill/index";
    }


    //2.秒杀详情页面
    @GetMapping("seckill-detail/{skuId}.html")
    public String seckillDetail(@PathVariable Long skuId,Model model){
        SeckillProduct seckillProduct = seckillFeiginClient.querySecKillBySkuId(skuId);
        model.addAttribute("item",seckillProduct);
        return "seckill/detail";
    }

    //3.获取下单码成功之后要访问的页面
    @GetMapping("/seckill-queue.html")
    public String seckillQueue(@PathVariable Long skuId, String seckillCode, HttpServletRequest request){
        request.setAttribute("skuId",skuId);
        request.setAttribute("seckillCode",seckillCode);
        return "seckill/queue";
    }

}
